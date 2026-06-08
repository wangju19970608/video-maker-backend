package com.example.video.service;

import com.example.video.model.TemplateConfig;
import com.example.video.model.VideoTemplate;
import com.example.video.repository.TemplateConfigRepository;
import com.example.video.repository.VideoTemplateRepository;
import com.example.video.exception.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AI 视频模板分析服务。
 *
 * 流程：
 * 1. 用 FFmpeg 从模板视频中抽取关键帧（6 帧）
 * 2. 将帧 base64 编码后发给 Claude Vision API
 * 3. Claude 返回 formFields + overlayRules JSON 草稿
 * 4. 写入 TemplateConfig
 */
@Service
public class TemplateAnalysisService {

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-opus-4-5-20251101";
    private static final int FRAME_COUNT = 6;

    private final VideoTemplateRepository templateRepository;
    private final TemplateConfigRepository configRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${claude.api.key:}")
    private String claudeApiKey;

    public TemplateAnalysisService(VideoTemplateRepository templateRepository,
                                   TemplateConfigRepository configRepository) {
        this.templateRepository = templateRepository;
        this.configRepository = configRepository;
    }

    /**
     * 触发分析（立即设置状态为 analysing，异步执行分析）。
     */
    @Transactional
    public TemplateConfig startAnalysis(Long templateId, Path videoPath) {
        VideoTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));

        TemplateConfig config = configRepository.findByTemplate_Id(templateId)
                .orElseGet(() -> {
                    TemplateConfig c = new TemplateConfig();
                    c.setTemplate(template);
                    return c;
                });

        config.setAnalysisStatus("analysing");
        config.setAnalysisError(null);
        TemplateConfig saved = configRepository.save(config);

        // 异步执行，不阻塞 HTTP 响应
        new Thread(() -> doAnalyse(saved.getId(), videoPath)).start();

        return saved;
    }

    private void doAnalyse(Long configId, Path videoPath) {
        TemplateConfig config = configRepository.findById(configId).orElse(null);
        if (config == null) {
            System.err.println("[AI分析] 配置不存在: " + configId);
            return;
        }

        Long templateId = config.getTemplate() != null ? config.getTemplate().getId() : null;
        System.out.println("[AI分析] 开始分析模板 " + templateId + ", 配置ID: " + configId);

        try {
            // 1. 抽帧
            System.out.println("[AI分析] 正在从视频中提取关键帧...");
            List<String> base64Frames = extractFrames(videoPath, FRAME_COUNT);
            System.out.println("[AI分析] 成功提取 " + base64Frames.size() + " 帧");

            // 2. 调用 Claude Vision
            System.out.println("[AI分析] 正在调用 Claude Vision API...");
            String responseJson = callClaudeVision(base64Frames);
            System.out.println("[AI分析] Claude API 响应成功");

            // 3. 解析返回的 JSON
            System.out.println("[AI分析] 正在解析 AI 返回的配置...");
            String[] parsed = parseClaudeResponse(responseJson);

            config.setFormFields(parsed[0]);
            config.setOverlayRules(parsed[1]);
            config.setAnalysisStatus("done");

            System.out.println("[AI分析] 分析完成！模板 " + templateId + " 配置已生成");
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "unknown error";
            System.err.println("[AI分析] 分析失败，模板 " + templateId + ": " + errorMsg);
            e.printStackTrace();

            config.setAnalysisStatus("failed");
            config.setAnalysisError(errorMsg.substring(0, Math.min(500, errorMsg.length())));
        }

        configRepository.save(config);
    }

    /**
     * 用 FFmpeg 从视频中均匀抽取 frameCount 帧，返回 base64 JPEG 字符串列表。
     */
    private List<String> extractFrames(Path videoPath, int frameCount) throws IOException, InterruptedException {
        // 获取视频时长（秒）
        double duration = getVideoDuration(videoPath);

        List<String> result = new ArrayList<>();
        Path tempDir = Files.createTempDirectory("frame_");

        try {
            for (int i = 0; i < frameCount; i++) {
                double timestamp = duration * (i + 1.0) / (frameCount + 1.0);
                Path framePath = tempDir.resolve(String.format("frame_%02d.jpg", i));

                List<String> cmd = Arrays.asList(
                        resolveFfmpeg(),
                        "-ss", String.format("%.2f", timestamp),
                        "-i", videoPath.toString(),
                        "-frames:v", "1",
                        "-q:v", "3",
                        "-vf", "scale=960:-1",
                        "-y",
                        framePath.toString()
                );
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                drainProcess(p);
                p.waitFor(30, TimeUnit.SECONDS);

                if (Files.exists(framePath)) {
                    byte[] bytes = Files.readAllBytes(framePath);
                    result.add(Base64.getEncoder().encodeToString(bytes));
                }
            }
        } finally {
            // 清理临时帧文件
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
                for (Path f : stream) Files.deleteIfExists(f);
            }
            Files.deleteIfExists(tempDir);
        }

        if (result.isEmpty()) {
            throw new IllegalStateException("FFmpeg 未能从视频中提取任何帧，请确认视频文件有效。");
        }
        return result;
    }

    private double getVideoDuration(Path videoPath) throws IOException, InterruptedException {
        // 使用 ffprobe 获取时长
        String ffprobe = resolveFfmpeg().replace("ffmpeg", "ffprobe");
        if (!new File(ffprobe).exists()) {
            // fallback：假设 30 秒
            return 30.0;
        }
        List<String> cmd = Arrays.asList(
                ffprobe, "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                videoPath.toString()
        );
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        Process p = pb.start();
        String out = new String(readAllBytes(p.getInputStream()), StandardCharsets.UTF_8).trim();
        p.waitFor(10, TimeUnit.SECONDS);
        try {
            return Double.parseDouble(out);
        } catch (NumberFormatException e) {
            return 30.0;
        }
    }

    /**
     * 调用 Claude Vision API，发送多帧图片，获取模板配置草稿。
     */
    private String callClaudeVision(List<String> base64Frames) throws IOException {
        if (!StringUtils.hasText(claudeApiKey)) {
            throw new IllegalStateException("claude.api.key 未配置，请在 application.properties 中添加 claude.api.key=sk-...");
        }

        // 构建 content 数组：多张图片 + 文字提示
        List<Map<String, Object>> contentParts = new ArrayList<>();
        for (String b64 : base64Frames) {
            Map<String, Object> imageSource = new LinkedHashMap<>();
            imageSource.put("type", "base64");
            imageSource.put("media_type", "image/jpeg");
            imageSource.put("data", b64);

            Map<String, Object> imagePart = new LinkedHashMap<>();
            imagePart.put("type", "image");
            imagePart.put("source", imageSource);
            contentParts.add(imagePart);
        }

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("type", "text");
        textPart.put("text", buildPrompt());
        contentParts.add(textPart);

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", contentParts);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("max_tokens", 2048);
        requestBody.put("messages", Collections.singletonList(userMessage));

        String requestJson = mapper.writeValueAsString(requestBody);

        URL url = new URL(CLAUDE_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", claudeApiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestJson.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String responseBody = new String(readAllBytes(is), StandardCharsets.UTF_8);

        if (code < 200 || code >= 300) {
            throw new IOException("Claude API 返回错误 " + code + ": " + responseBody);
        }

        return responseBody;
    }

    /**
     * 从 Claude 响应中提取 formFields 和 overlayRules JSON 字符串。
     */
    private String[] parseClaudeResponse(String responseJson) throws IOException {
        JsonNode root = mapper.readTree(responseJson);
        String text = root.path("content").get(0).path("text").asText("");

        // 从文本中提取两个 JSON 数组
        String formFields = extractJsonArray(text, "formFields");
        String overlayRules = extractJsonArray(text, "overlayRules");

        // 验证是否为合法 JSON
        validateJson(formFields, "formFields");
        validateJson(overlayRules, "overlayRules");

        return new String[]{formFields, overlayRules};
    }

    private String extractJsonArray(String text, String key) throws IOException {
        // 寻找 "formFields": [...] 或 "overlayRules": [...] 模式
        String marker = "\"" + key + "\"";
        int markerIdx = text.indexOf(marker);
        if (markerIdx < 0) {
            // 尝试直接找独立的 JSON 块（Claude 可能直接输出纯 JSON 对象）
            // 回退：找第一个 [ ... ] 块
            return extractFirstJsonArray(text, key);
        }
        int start = text.indexOf('[', markerIdx + marker.length());
        if (start < 0) {
            return extractFirstJsonArray(text, key);
        }
        int end = findMatchingBracket(text, start);
        return text.substring(start, end + 1);
    }

    private String extractFirstJsonArray(String text, String hint) throws IOException {
        // 如果 Claude 以 JSON 对象形式返回，尝试解析整个响应
        int objStart = text.indexOf('{');
        if (objStart >= 0) {
            int objEnd = findMatchingBrace(text, objStart);
            String jsonObj = text.substring(objStart, objEnd + 1);
            try {
                JsonNode node = mapper.readTree(jsonObj);
                if (node.has(hint)) {
                    return mapper.writeValueAsString(node.get(hint));
                }
            } catch (Exception ignored) {}
        }
        // 最终回退：返回空数组，管理员手动配置
        return "[]";
    }

    private int findMatchingBracket(String text, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return text.length() - 1;
    }

    private int findMatchingBrace(String text, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return text.length() - 1;
    }

    private void validateJson(String json, String fieldName) throws IOException {
        try {
            mapper.readTree(json);
        } catch (Exception e) {
            throw new IOException("AI 返回的 " + fieldName + " 不是合法 JSON：" + e.getMessage());
        }
    }

    private String buildPrompt() {
        return "你是一个视频模板分析助手。我给你提供了一段视频模板的多个关键帧截图。\n\n" +
               "请分析这些帧，识别视频中哪些位置是动态内容（即需要用户填写或上传的信息），例如：\n" +
               "- 人名、称呼文字\n" +
               "- 年龄数字\n" +
               "- 日期\n" +
               "- 祝福语/祝福词\n" +
               "- 照片/人物图片占位区域\n\n" +
               "请返回一个 JSON 对象，包含两个字段：\n\n" +
               "1. formFields：用户需要填写的表单字段列表。每个字段格式：\n" +
               "   {\"key\":\"唯一标识\", \"type\":\"text|number|image\", \"label\":\"展示给用户的中文标签\", \"placeholder\":\"提示文字\", \"required\":true|false}\n\n" +
               "2. overlayRules：视频叠加规则列表，描述每个字段在视频中的位置和时间段。每条规则格式：\n" +
               "   {\"key\":\"对应formFields中的key\", \"type\":\"text|image\", " +
               "\"startTime\":开始秒数, \"endTime\":结束秒数, " +
               "\"x\":水平位置比例0到1, \"y\":垂直位置比例0到1, " +
               "\"fontSize\":字号(仅text), \"fontColor\":\"颜色(仅text)\", " +
               "\"width\":宽度比例0到1(仅image), \"height\":高度比例0到1(仅image)}\n\n" +
               "坐标原点在左上角，x/y/width/height 均为 0.0 到 1.0 的相对比例。\n" +
               "startTime/endTime 请根据帧的位置估算秒数。\n\n" +
               "只返回 JSON，不要任何解释文字。格式：\n" +
               "{\"formFields\":[...], \"overlayRules\":[...]}";
    }

    private String resolveFfmpeg() {
        Path current = Paths.get(System.getProperty("user.dir"));

        // 尝试从当前目录往上逐级查找 ffmpeg/bin/ffmpeg.exe
        Path dir = current;
        for (int i = 0; i < 4; i++) {
            if (dir == null) break;
            Path candidate = dir.resolve("ffmpeg").resolve("bin").resolve("ffmpeg.exe");
            if (Files.exists(candidate)) {
                System.out.println("[FFmpeg] 找到: " + candidate);
                return candidate.toString();
            }
            dir = dir.getParent();
        }

        // 系统 PATH（Linux/Mac）
        System.out.println("[FFmpeg] 本地未找到，尝试系统 PATH");
        return "ffmpeg";
    }

    private Path resolveProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        Path parent = current.getParent();
        return parent == null ? current : parent;
    }

    private void drainProcess(Process p) {
        Thread t = new Thread(() -> {
            try (InputStream is = p.getInputStream()) {
                byte[] buf = new byte[4096];
                while (is.read(buf) != -1) {}
            } catch (IOException ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Java 8 兼容：手动读取 InputStream 所有字节
     */
    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
