package com.example.video.service;

import com.example.video.dto.VideoTaskRequest;
import com.example.video.exception.NotFoundException;
import com.example.video.model.VideoTemplate;
import com.example.video.repository.VideoTemplateRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
public class VideoTaskService {

    private static final DateTimeFormatter TASK_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VideoTemplateRepository templateRepository;
    private final com.example.video.repository.TemplateConfigRepository templateConfigRepository;
    private final Path projectRoot;
    private final Path templateRoot;
    private final Path videoRoot;
    private final Path baseRoot;
    private final Path ffmpegPath;
    private final Map<String, TaskRecord> taskStore = new ConcurrentHashMap<>();
    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(2);
    private final OrderService orderService;
    private final com.fasterxml.jackson.databind.ObjectMapper jsonMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public VideoTaskService(VideoTemplateRepository templateRepository,
                            com.example.video.repository.TemplateConfigRepository templateConfigRepository,
                            OrderService orderService) {
        this.templateRepository = templateRepository;
        this.templateConfigRepository = templateConfigRepository;
        this.orderService = orderService;
        this.projectRoot = resolveProjectRoot();
        this.templateRoot = projectRoot.resolve("template");
        this.videoRoot = projectRoot.resolve("video");
        this.baseRoot = projectRoot.resolve("base");
        this.ffmpegPath = projectRoot.resolve("ffmpeg").resolve("bin").resolve("ffmpeg.exe");

        try {
            Files.createDirectories(videoRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create video output directory", e);
        }
    }

    public VideoTaskResult generate(VideoTaskRequest request) throws IOException, InterruptedException {
        validateRequest(request);

        if (request.getOrderId() != null) {
            com.example.video.model.OrderRecord order = orderService.getOrderForTask(request.getOrderId());
            int used = order.getUsedGenerateCount() == null ? 0 : order.getUsedGenerateCount();
            int max = order.getMaxGenerateCount() == null ? 5 : order.getMaxGenerateCount();
            if (used >= max) {
                throw new RuntimeException("该订单的制作次数已达上限 (" + max + "次)");
            }
        }

        VideoTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new NotFoundException("Template not found: " + request.getTemplateId()));

        Path docxPath = resolveDocxPath(template);

        String taskId = buildTaskId(request.getOrderId());
        Path taskDir = videoRoot.resolve(taskId);
        Files.createDirectories(taskDir);

        Path outputDocx = taskDir.resolve("output.docx");
        Path outputImage = taskDir.resolve("output.png");
        Path outputVideo = taskDir.resolve("output.mp4");

        replaceDocx(docxPath, outputDocx, request);
        renderDocxToImage(outputDocx, outputImage);
        generateVideo(outputImage, outputVideo);

        VideoTaskResult result = new VideoTaskResult();
        result.setTaskId(taskId);
        result.setDocxPath(outputDocx);
        result.setImagePath(outputImage);
        result.setVideoPath(outputVideo);
        result.setDocxUrl("/api/video/tasks/" + taskId + "/docx");
        result.setImageUrl("/api/video/tasks/" + taskId + "/image");
        result.setVideoUrl("/api/video/tasks/" + taskId + "/video");
        recordCompleted(result);
        if (request.getOrderId() != null) {
            orderService.incrementOrderTaskCount(request.getOrderId());
            orderService.updateOrderTask(request.getOrderId(), taskId);
        }
        return result;
    }

    public TaskRecord createAsync(VideoTaskRequest request) throws IOException {
        validateRequest(request);

        if (request.getOrderId() != null) {
            com.example.video.model.OrderRecord order = orderService.getOrderForTask(request.getOrderId());
            int used = order.getUsedGenerateCount() == null ? 0 : order.getUsedGenerateCount();
            int max = order.getMaxGenerateCount() == null ? 5 : order.getMaxGenerateCount();
            if (used >= max) {
                throw new RuntimeException("该订单的制作次数已达上限 (" + max + "次)");
            }
        }

        VideoTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new NotFoundException("Template not found: " + request.getTemplateId()));

        Path docxPath = resolveDocxPath(template);

        String taskId = buildTaskId(request.getOrderId());
        Path taskDir = videoRoot.resolve(taskId);
        Files.createDirectories(taskDir);

        TaskRecord record = initTaskRecord(taskId);
        taskStore.put(taskId, record);

        taskExecutor.submit(() -> {
            try {
                Path outputDocx = taskDir.resolve("output.docx");
                Path outputImage = taskDir.resolve("output.png");
                Path outputVideo = taskDir.resolve("output.mp4");

                replaceDocx(docxPath, outputDocx, request);
                renderDocxToImage(outputDocx, outputImage);
                generateVideo(outputImage, outputVideo);

                record.setStatus("completed");
                record.setMessage("????");
                if (request.getOrderId() != null) {
                    orderService.incrementOrderTaskCount(request.getOrderId());
                    orderService.updateOrderTask(request.getOrderId(), taskId);
                }
            } catch (Exception ex) {
                record.setStatus("failed");
                record.setMessage(ex.getMessage() == null ? "????" : ex.getMessage());
            }
        });

        return record;
    }

    public TaskRecord createFromCustomDocx(Long orderId, org.springframework.web.multipart.MultipartFile file) throws IOException {
        String taskId = buildTaskId(orderId);
        Path taskDir = videoRoot.resolve(taskId);
        Files.createDirectories(taskDir);

        TaskRecord record = initTaskRecord(taskId);
        taskStore.put(taskId, record);

        Path outputDocx = taskDir.resolve("output.docx");
        file.transferTo(outputDocx.toFile());

        taskExecutor.submit(() -> {
            try {
                Path outputImage = taskDir.resolve("output.png");
                Path outputVideo = taskDir.resolve("output.mp4");

                renderDocxToImage(outputDocx, outputImage);
                generateVideo(outputImage, outputVideo);

                record.setStatus("completed");
                record.setMessage("????");
                if (orderId != null) {
                    orderService.incrementOrderTaskCount(orderId);
                    orderService.updateOrderTask(orderId, taskId);
                }
            } catch (Exception ex) {
                record.setStatus("failed");
                record.setMessage(ex.getMessage() == null ? "????" : ex.getMessage());
            }
        });

        return record;
    }

    public TaskRecord getTaskStatus(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            return null;
        }
        TaskRecord record = taskStore.get(taskId);
        if (record != null) {
            return record;
        }

        Path taskDir = videoRoot.resolve(taskId);
        if (!Files.exists(taskDir)) {
            return null;
        }

        TaskRecord fallback = initTaskRecord(taskId);
        Path outputDocx = taskDir.resolve("output.docx");
        Path outputImage = taskDir.resolve("output.png");
        Path outputVideo = taskDir.resolve("output.mp4");
        if (Files.exists(outputDocx) && Files.exists(outputImage) && Files.exists(outputVideo)) {
            fallback.setStatus("completed");
            fallback.setMessage("????");
        } else {
            fallback.setStatus("processing");
            fallback.setMessage("???");
        }
        taskStore.put(taskId, fallback);
        return fallback;
    }

    public TaskRecord recordCompleted(VideoTaskResult result) {
        TaskRecord record = initTaskRecord(result.getTaskId());
        record.setStatus("completed");
        record.setMessage("????");
        taskStore.put(record.getTaskId(), record);
        return record;
    }

    public Path resolveTaskFile(String taskId, String filename) {
        if (!StringUtils.hasText(taskId) || taskId.contains("..") || taskId.contains("/") || taskId.contains("\\")) {
            return null;
        }
        Path file = videoRoot.resolve(taskId).resolve(filename);
        if (!file.normalize().startsWith(videoRoot)) {
            return null;
        }
        return file;
    }

    private void validateRequest(VideoTaskRequest request) {
        if (request == null || request.getTemplateId() == null) {
            throw new IllegalArgumentException("templateId is required");
        }
        // 兼容动态表单：如果有 dynamicFields 则不强制要求 name/age/time
        boolean hasDynamicFields = request.getDynamicFields() != null && !request.getDynamicFields().isEmpty();
        if (!hasDynamicFields) {
            if (!StringUtils.hasText(request.getName())
                    || !StringUtils.hasText(request.getAge())
                    || !StringUtils.hasText(request.getTime())) {
                throw new IllegalArgumentException("name, age and time are required");
            }
        }
    }

    private Path resolveProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        Path parent = current.getParent();
        return parent == null ? current : parent;
    }

    public Path resolveDocxPath(VideoTemplate template) {
        String previewUrl = template.getPreviewUrl();
        if (StringUtils.hasText(previewUrl)) {
            Path candidate = Paths.get(previewUrl);
            if (!candidate.isAbsolute()) {
                candidate = projectRoot.resolve(previewUrl);
            }
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        Path fallback = templateRoot.resolve("1.docx");
        if (Files.exists(fallback)) {
            return fallback;
        }
        throw new IllegalArgumentException("Template docx not found");
    }

    private String buildTaskId(Long orderId) {
        String base = "task-" + TASK_TIME_FORMAT.format(LocalDateTime.now()) + "-" + UUID.randomUUID().toString().substring(0, 6);
        if (orderId != null) {
            return "order-" + orderId + "-" + base;
        }
        return base;
    }

    public java.util.List<com.example.video.dto.HistoricalTaskView> listHistoricalTasks(Long orderId) {
        if (orderId == null) return new java.util.ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.list(videoRoot)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.startsWith("order-" + orderId + "-"))
                    .map(this::getTaskStatus)
                    .filter(java.util.Objects::nonNull)
                    .map(r -> new com.example.video.dto.HistoricalTaskView(
                            r.getTaskId(), r.getStatus(), r.getMessage(),
                            r.getDocxUrl(), r.getImageUrl(), r.getVideoUrl(),
                            "/api/video/tasks/" + r.getTaskId() + "/parameters"
                    ))
                    .sorted((a, b) -> b.getTaskId().compareTo(a.getTaskId()))
                    .collect(java.util.stream.Collectors.toList());
        } catch (IOException e) {
            return new java.util.ArrayList<>();
        }
    }

    private TaskRecord initTaskRecord(String taskId) {
        TaskRecord record = new TaskRecord(taskId);
        record.setStatus("processing");
        record.setMessage("???");
        record.setDocxUrl("/api/video/tasks/" + taskId + "/docx");
        record.setImageUrl("/api/video/tasks/" + taskId + "/image");
        record.setVideoUrl("/api/video/tasks/" + taskId + "/video");
        record.setParametersUrl("/api/video/tasks/" + taskId + "/parameters");
        return record;
    }

    private void replaceDocx(Path inputDocx, Path outputDocx, VideoTaskRequest request) throws IOException {
        Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put("${1}", request.getName());
        replaceMap.put("${2}", request.getAge());
        replaceMap.put("${3}", request.getTime());
        replaceMap.put("${4}", request.getHotel());

        try (ZipFile zipFile = new ZipFile(inputDocx.toFile());
             ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputDocx.toFile()))) {
            zipFile.stream().forEach(entry -> {
                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    ZipEntry newEntry = new ZipEntry(entry.getName());
                    zipOut.putNextEntry(newEntry);
                    if ("word/document.xml".equals(entry.getName())) {
                        String xml = new String(readAllBytes(inputStream), StandardCharsets.UTF_8);
                        String replaced = replacePlaceholdersWithStyles(xml, replaceMap);
                        zipOut.write(replaced.getBytes(StandardCharsets.UTF_8));
                    } else {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = inputStream.read(buffer)) != -1) {
                            zipOut.write(buffer, 0, len);
                        }
                    }
                    zipOut.closeEntry();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException) {
                throw (IOException) ex.getCause();
            }
            throw ex;
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int len;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        }
    }

    private void drainProcessOutput(Process process) {
        Thread drainThread = new Thread(() -> {
            try (InputStream inputStream = process.getInputStream()) {
                byte[] buffer = new byte[8192];
                while (inputStream.read(buffer) != -1) {
                    // drain output to avoid blocking process
                }
            } catch (IOException ignored) {
                // ignore drain errors
            }
        });
        drainThread.setDaemon(true);
        drainThread.start();
    }


    private String replacePlaceholdersWithStyles(String xml, Map<String, String> replaceMap) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList textNodes = (NodeList) xpath.evaluate("//*[local-name()='t']", document, XPathConstants.NODESET);
            List<TextNode> nodes = new ArrayList<>();
            for (int i = 0; i < textNodes.getLength(); i++) {
                Node node = textNodes.item(i);
                nodes.add(new TextNode(node, node.getTextContent()));
            }

            for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
                replaceAcrossNodes(nodes, entry.getKey(), entry.getValue());
            }

            for (TextNode node : nodes) {
                node.node.setTextContent(node.text);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception ex) {
            throw new IOException("Failed to replace placeholders in docx", ex);
        }
    }

    private void replaceAcrossNodes(List<TextNode> nodes, String placeholder, String value) {
        if (!StringUtils.hasText(placeholder) || nodes.isEmpty()) {
            return;
        }
        String replacement = value == null ? "" : value;
        if (placeholder.equals(replacement)) {
            return;
        }

        int guard = 0;
        while (guard++ < 10000) {
            int[] starts = new int[nodes.size()];
            int[] ends = new int[nodes.size()];
            StringBuilder full = new StringBuilder();
            int cursor = 0;
            for (int i = 0; i < nodes.size(); i++) {
                starts[i] = cursor;
                String text = nodes.get(i).text == null ? "" : nodes.get(i).text;
                full.append(text);
                cursor += text.length();
                ends[i] = cursor;
            }

            int idx = full.indexOf(placeholder);
            if (idx < 0) {
                break;
            }
            int end = idx + placeholder.length();

            int startNode = -1;
            int endNode = -1;
            for (int i = 0; i < nodes.size(); i++) {
                if (startNode == -1 && idx >= starts[i] && idx < ends[i]) {
                    startNode = i;
                }
                if (end <= ends[i]) {
                    endNode = i;
                    break;
                }
            }

            if (startNode == -1 || endNode == -1) {
                break;
            }

            TextNode first = nodes.get(startNode);
            TextNode last = nodes.get(endNode);
            String firstText = first.text == null ? "" : first.text;
            String lastText = last.text == null ? "" : last.text;

            int startOffset = idx - starts[startNode];
            int endOffset = end - starts[endNode];

            String prefix = firstText.substring(0, Math.min(startOffset, firstText.length()));
            String suffix = lastText.substring(Math.min(endOffset, lastText.length()));
            first.text = prefix + replacement + suffix;

            for (int i = startNode + 1; i <= endNode; i++) {
                nodes.get(i).text = "";
            }
        }
    }

    private static class TextNode {
        private final Node node;
        private String text;

        private TextNode(Node node, String text) {
            this.node = node;
            this.text = text == null ? "" : text;
        }
    }

    private String applyReplacements(String text, Map<String, String> replaceMap) {
        String replaced = text;
        for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
            replaced = replaced.replace(entry.getKey(), entry.getValue());
        }
        return replaced;
    }

    private void renderDocxToImage(Path docxPath, Path outputImage) throws IOException, InterruptedException {
        Path pdfPath = outputImage.resolveSibling("output.pdf");
        convertDocxToPdf(docxPath, pdfPath);
        renderPdfToImage(pdfPath, outputImage);
    }

    private void convertDocxToPdf(Path docxPath, Path pdfPath) throws IOException, InterruptedException {
        Path soffice = findOfficeConverter();
        if (soffice == null) {
            throw new IllegalStateException("未检测到 LibreOffice，请安装后重试，或将 soffice.exe 放到 base/libreoffice/program 目录。");
        }

        Files.deleteIfExists(pdfPath);

        List<String> command = Arrays.asList(
                soffice.toString(),
                "--headless",
                "--nologo",
                "--nolockcheck",
                "--nodefault",
                "--norestore",
                "--convert-to", "pdf",
                "--outdir", pdfPath.getParent().toString(),
                docxPath.toString()
        );

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        drainProcessOutput(process);
        boolean finished = process.waitFor(180, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("LibreOffice 转换超时");
        }
        if (process.exitValue() != 0) {
            String output = new String(readAllBytes(process.getInputStream()), StandardCharsets.UTF_8);
            throw new IllegalStateException("LibreOffice 转换失败：" + output);
        }
        if (!Files.exists(pdfPath)) {
            throw new IllegalStateException("PDF 文件未生成，请检查 LibreOffice 是否可用");
        }
    }

    private Path findOfficeConverter() {
        String customPath = System.getenv("LIBREOFFICE_PATH");
        if (StringUtils.hasText(customPath)) {
            Path path = Paths.get(customPath);
            if (Files.exists(path)) {
                return path;
            }
        }

        List<Path> candidates = Arrays.asList(
                baseRoot.resolve("libreoffice").resolve("program").resolve("soffice.exe"),
                baseRoot.resolve("LibreOffice").resolve("program").resolve("soffice.exe"),
                baseRoot.resolve("LibreOfficePortable").resolve("App").resolve("libreoffice").resolve("program").resolve("soffice.exe"),
                baseRoot.resolve("LibreOfficePortablePrevious").resolve("App").resolve("libreoffice").resolve("program").resolve("soffice.exe"),
                Paths.get("C:\\Program Files\\LibreOffice\\program\\soffice.exe"),
                Paths.get("C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private void renderPdfToImage(Path pdfPath, Path outputImage) throws IOException {
        try (PDDocument pdf = PDDocument.load(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            BufferedImage image = renderer.renderImageWithDPI(0, 200, ImageType.RGB);
            ImageIO.write(image, "png", outputImage.toFile());
        }
    }

    private void generateVideo(Path imagePath, Path videoPath) throws IOException, InterruptedException {
        if (!Files.exists(ffmpegPath)) {
            throw new IllegalStateException("ffmpeg not found at " + ffmpegPath);
        }

        ProcessBuilder builder = new ProcessBuilder(
                ffmpegPath.toString(),
                "-y",
                "-hide_banner",
                "-loglevel", "error",
                "-loop", "1",
                "-i", imagePath.toString(),
                "-t", "5",
                "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p",
                "-r", "25",
                videoPath.toString()
        );
        builder.redirectErrorStream(true);
        Process process = builder.start();
        drainProcessOutput(process);
        boolean finished = process.waitFor(40, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("ffmpeg timeout");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("ffmpeg failed with code " + process.exitValue());
        }
    }

    public static class TaskRecord {
        private final String taskId;
        private String status;
        private String message;
        private String docxUrl;
        private String imageUrl;
        private String videoUrl;
        private String parametersUrl;

        public TaskRecord(String taskId) {
            this.taskId = taskId;
        }

        public String getParametersUrl() { return parametersUrl; }
        public void setParametersUrl(String parametersUrl) { this.parametersUrl = parametersUrl; }

        public String getTaskId() {
            return taskId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getDocxUrl() {
            return docxUrl;
        }

        public void setDocxUrl(String docxUrl) {
            this.docxUrl = docxUrl;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }
    }

    public static class VideoTaskResult {
        private String taskId;
        private Path docxPath;
        private Path imagePath;
        private Path videoPath;
        private String docxUrl;
        private String imageUrl;
        private String videoUrl;

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public Path getDocxPath() {
            return docxPath;
        }

        public void setDocxPath(Path docxPath) {
            this.docxPath = docxPath;
        }

        public Path getImagePath() {
            return imagePath;
        }

        public void setImagePath(Path imagePath) {
            this.imagePath = imagePath;
        }

        public Path getVideoPath() {
            return videoPath;
        }

        public void setVideoPath(Path videoPath) {
            this.videoPath = videoPath;
        }

        public String getDocxUrl() {
            return docxUrl;
        }

        public void setDocxUrl(String docxUrl) {
            this.docxUrl = docxUrl;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }
    }

    private void saveTaskParameters(Path taskDir, VideoTaskRequest request) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writeValue(taskDir.resolve("parameters.json").toFile(), request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public VideoTaskResult generateWithFile(VideoTaskRequest request, org.springframework.web.multipart.MultipartFile coverImage) throws IOException, InterruptedException {
        validateRequest(request);

        if (request.getOrderId() != null) {
            com.example.video.model.OrderRecord order = orderService.getOrderForTask(request.getOrderId());
            int used = order.getUsedGenerateCount() == null ? 0 : order.getUsedGenerateCount();
            int max = order.getMaxGenerateCount() == null ? 5 : order.getMaxGenerateCount();
            if (used >= max) {
                throw new RuntimeException("该订单的制作次数已达上限 (" + max + "次)");
            }
        }

        VideoTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new NotFoundException("Template not found: " + request.getTemplateId()));

        String taskId = buildTaskId(request.getOrderId());
        Path taskDir = videoRoot.resolve(taskId);
        Files.createDirectories(taskDir);

        Path outputImage = taskDir.resolve("output.png");
        if (coverImage != null && !coverImage.isEmpty()) {
            coverImage.transferTo(outputImage.toFile());
        } else {
            Files.write(outputImage, new byte[0]); 
        }

        saveTaskParameters(taskDir, request);

        Path outputVideo = taskDir.resolve("output.mp4");
        Path outputDocx = taskDir.resolve("output.docx");

        if ("video".equals(template.getTemplateType())) {
            Path baseMp4 = templateRoot.resolve("video").resolve("1.mp4");
            generateNativeVideo(request, outputImage, baseMp4, outputVideo);
        } else {
            Path docxPath = resolveDocxPath(template);
            replaceDocx(docxPath, outputDocx, request);
            renderDocxToImage(outputDocx, outputImage);
            generateVideo(outputImage, outputVideo);
        }

        VideoTaskResult result = new VideoTaskResult();
        result.setTaskId(taskId);
        result.setDocxPath(outputDocx);
        result.setImagePath(outputImage);
        result.setVideoPath(outputVideo);
        result.setDocxUrl("/api/video/tasks/" + taskId + "/docx");
        result.setImageUrl("/api/video/tasks/" + taskId + "/image");
        result.setVideoUrl("/api/video/tasks/" + taskId + "/video");
        
        if (request.getOrderId() != null) {
            orderService.incrementOrderTaskCount(request.getOrderId());
            orderService.updateOrderTask(request.getOrderId(), taskId);
        }
        return result;
    }

    public TaskRecord createAsyncWithFile(VideoTaskRequest request, org.springframework.web.multipart.MultipartFile coverImage) throws IOException {
        validateRequest(request);

        if (request.getOrderId() != null) {
            com.example.video.model.OrderRecord order = orderService.getOrderForTask(request.getOrderId());
            int used = order.getUsedGenerateCount() == null ? 0 : order.getUsedGenerateCount();
            int max = order.getMaxGenerateCount() == null ? 5 : order.getMaxGenerateCount();
            if (used >= max) {
                throw new RuntimeException("该订单的制作次数已达上限 (" + max + "次)");
            }
        }

        VideoTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new NotFoundException("Template not found: " + request.getTemplateId()));

        String taskId = buildTaskId(request.getOrderId());
        Path taskDir = videoRoot.resolve(taskId);
        Files.createDirectories(taskDir);

        TaskRecord record = initTaskRecord(taskId);
        taskStore.put(taskId, record);

        Path outputImage = taskDir.resolve("output.png");
        if (coverImage != null && !coverImage.isEmpty()) {
            coverImage.transferTo(outputImage.toFile());
        }

        saveTaskParameters(taskDir, request);

        taskExecutor.submit(() -> {
            try {
                Path outputVideo = taskDir.resolve("output.mp4");
                Path outputDocx = taskDir.resolve("output.docx");

                if ("video".equals(template.getTemplateType())) {
                    Path baseMp4 = templateRoot.resolve("video").resolve("1.mp4");
                    generateNativeVideo(request, outputImage, baseMp4, outputVideo);
                } else {
                    Path docxPath = resolveDocxPath(template);
                    replaceDocx(docxPath, outputDocx, request);
                    renderDocxToImage(outputDocx, outputImage);
                    generateVideo(outputImage, outputVideo);
                }

                record.setStatus("completed");
                record.setMessage("????");
                if (request.getOrderId() != null) {
                    orderService.incrementOrderTaskCount(request.getOrderId());
                    orderService.updateOrderTask(request.getOrderId(), taskId);
                }
            } catch (Exception ex) {
                record.setStatus("failed");
                record.setMessage(ex.getMessage() == null ? "????" : ex.getMessage());
            }
        });

        return record;
    }

    private void generateNativeVideo(VideoTaskRequest request, Path imagePath, Path baseMp4, Path outputVideo) throws IOException, InterruptedException {
        // 优先读取 overlayRules 动态配置
        String overlayRulesJson = null;
        if (request.getTemplateId() != null) {
            overlayRulesJson = templateConfigRepository.findByTemplate_Id(request.getTemplateId())
                    .map(c -> c.getOverlayRules())
                    .orElse(null);
        }

        if (StringUtils.hasText(overlayRulesJson) && !overlayRulesJson.equals("[]")) {
            generateNativeVideoFromRules(request, imagePath, baseMp4, outputVideo, overlayRulesJson);
        } else {
            // 回退到原有硬编码逻辑（兼容老模板）
            generateNativeVideoLegacy(request, imagePath, baseMp4, outputVideo);
        }
    }

    /**
     * 根据 overlayRules JSON 动态构建 FFmpeg 命令合成视频。
     *
     * overlayRules 示例：
     * [
     *   {"key":"name","type":"text","startTime":2.0,"endTime":20.0,"x":0.60,"y":0.28,"fontSize":80,"fontColor":"#ffffff"},
     *   {"key":"photo","type":"image","startTime":5.0,"endTime":15.0,"x":0.0,"y":0.0,"width":1.0,"height":1.0}
     * ]
     *
     * 用户字段值从 request 的 dynamicFields Map 中获取（key 对应 formField.key）。
     * 兼容老字段：name→name, age→age, time→time, hotel→hotel。
     */
    private void generateNativeVideoFromRules(VideoTaskRequest request, Path imagePath,
                                              Path baseMp4, Path outputVideo,
                                              String overlayRulesJson) throws IOException, InterruptedException {
        com.fasterxml.jackson.databind.JsonNode rules = jsonMapper.readTree(overlayRulesJson);
        if (!rules.isArray() || rules.size() == 0) {
            generateNativeVideoLegacy(request, imagePath, baseMp4, outputVideo);
            return;
        }

        // 获取视频实际分辨率
        int[] resolution = getVideoResolution(baseMp4);
        int videoWidth = resolution[0];
        int videoHeight = resolution[1];

        // 分离 text 规则和 image 规则
        List<com.fasterxml.jackson.databind.JsonNode> textRules = new ArrayList<>();
        List<com.fasterxml.jackson.databind.JsonNode> imageRules = new ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode rule : rules) {
            String type = rule.path("type").asText("text");
            if ("image".equals(type)) {
                imageRules.add(rule);
            } else {
                textRules.add(rule);
            }
        }

        String fontFile = "../base/simhei.ttf";

        // 构建 FFmpeg 命令
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath.toString());
        command.add("-y");
        // 输入1：基础视频
        command.add("-i");
        command.add(baseMp4.toString());

        // 如果有 image 规则且用户上传了照片
        boolean hasImage = !imageRules.isEmpty() && Files.exists(imagePath) && Files.size(imagePath) > 0;
        if (hasImage) {
            command.add("-i");
            command.add(imagePath.toString());
        }

        // 构建 filter_complex
        StringBuilder filter = new StringBuilder();

        // 起点：[0:v]
        String lastLabel = "[0:v]";

        // 先叠加所有 image
        int imgInputIdx = 1;
        for (int i = 0; i < imageRules.size(); i++) {
            if (!hasImage) break;
            com.fasterxml.jackson.databind.JsonNode rule = imageRules.get(i);
            double startTime = rule.path("startTime").asDouble(0);
            double endTime   = rule.path("endTime").asDouble(999);
            double rx = rule.path("x").asDouble(0);
            double ry = rule.path("y").asDouble(0);
            double rw = rule.path("width").asDouble(1);
            double rh = rule.path("height").asDouble(1);

            int px = (int)(rx * videoWidth);
            int py = (int)(ry * videoHeight);
            int pw = (int)(rw * videoWidth);
            int ph = (int)(rh * videoHeight);
            // 保证偶数（libx264 要求）
            pw = pw % 2 == 0 ? pw : pw - 1;
            ph = ph % 2 == 0 ? ph : ph - 1;

            String scaledLabel = "[img" + i + "scaled]";
            String outLabel = "[v_img" + i + "]";

            filter.append("[").append(imgInputIdx).append(":v]")
                  .append("scale=").append(pw).append(":").append(ph)
                  .append(":force_original_aspect_ratio=increase,crop=").append(pw).append(":").append(ph)
                  .append(scaledLabel).append(";");

            filter.append(lastLabel).append(scaledLabel)
                  .append("overlay=").append(px).append(":").append(py)
                  .append(":enable='between(t,").append(startTime).append(",").append(endTime).append(")'")
                  .append(outLabel).append(";");

            lastLabel = outLabel;
            imgInputIdx++;
        }

        // 再叠加所有 text
        for (int i = 0; i < textRules.size(); i++) {
            com.fasterxml.jackson.databind.JsonNode rule = textRules.get(i);
            String key = rule.path("key").asText("");
            double startTime = rule.path("startTime").asDouble(0);
            double endTime   = rule.path("endTime").asDouble(999);
            double rx = rule.path("x").asDouble(0.5);
            double ry = rule.path("y").asDouble(0.5);
            int fontSize = rule.path("fontSize").asInt(48);
            String fontColor = rule.path("fontColor").asText("white");

            int px = (int)(rx * videoWidth);
            int py = (int)(ry * videoHeight);

            String value = getFieldValue(request, key);
            String safeValue = value.replace("'", "").replace(":", "").replace(",", "").replace("\\", "").replace("=", "");

            String outLabel = i < textRules.size() - 1 ? "[t" + i + "]" : "[vout]";

            filter.append(lastLabel)
                  .append("drawtext=text='").append(safeValue).append("'")
                  .append(":x=").append(px)
                  .append(":y=").append(py)
                  .append(":fontsize=").append(fontSize)
                  .append(":fontcolor=").append(fontColor.replace("#", "0x"))
                  .append(":fontfile='").append(fontFile).append("'")
                  .append(":enable='between(t,").append(startTime).append(",").append(endTime).append(")'")
                  .append(outLabel).append(";");

            lastLabel = outLabel;
        }

        // 删除末尾多余的 ";"
        String filterStr = filter.toString();
        if (filterStr.endsWith(";")) {
            filterStr = filterStr.substring(0, filterStr.length() - 1);
        }

        // 如果没有 filter 内容（无任何规则），直接 copy
        if (filterStr.isEmpty() || filterStr.trim().isEmpty()) {
            command.add("-c");
            command.add("copy");
        } else {
            command.add("-filter_complex");
            command.add(filterStr);
            // 如果最后 label 是 [vout] 或 [t...] 则用 map，否则让 ffmpeg 自动选
            if (lastLabel.equals("[vout]") || lastLabel.startsWith("[t") || lastLabel.startsWith("[v_img")) {
                command.add("-map");
                command.add(lastLabel);
                command.add("-map");
                command.add("0:a?");
            }
            command.add("-c:a");
            command.add("copy");
        }

        command.add(outputVideo.toString());

        runFfmpegCommand(command, 120);
    }

    /**
     * 原有硬编码逻辑，保留作为兼容回退。
     */
    private void generateNativeVideoLegacy(VideoTaskRequest request, Path imagePath,
                                            Path baseMp4, Path outputVideo) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath.toString());
        command.add("-y");
        command.add("-i");
        command.add(baseMp4.toString());

        boolean hasImage = Files.exists(imagePath) && Files.size(imagePath) > 0;
        if (hasImage) {
            command.add("-i");
            command.add(imagePath.toString());
        }

        String safeName = sanitize(request.getName());
        String safeAge  = sanitize(request.getAge());
        String safeTime = sanitize(request.getTime());
        String fontFile = "../base/simhei.ttf";

        String filter = "[0:v]drawtext=text='" + safeName + "':x=1150:y=300:fontsize=80:fontcolor=white:fontfile='" + fontFile + "'[t1];" +
                        "[t1]drawtext=text='" + safeAge + "':x=1200:y=450:fontsize=160:fontcolor=white:fontfile='" + fontFile + "'[t2];" +
                        "[t2]drawtext=text='" + safeTime + "':x=1150:y=700:fontsize=40:fontcolor=white:fontfile='" + fontFile + "'[bg];" +
                        "[1:v]scale=1920:1080:force_original_aspect_ratio=increase,crop=1920:1080[img];" +
                        "[bg][img]overlay=0:0:enable='between(t,2,5)'";

        if (hasImage) {
            command.add("-filter_complex");
            command.add(filter);
        } else {
            command.add("-vf");
            command.add("drawtext=text='" + safeName + "':x=1150:y=300:fontsize=80:fontcolor=white:fontfile='" + fontFile + "'," +
                        "drawtext=text='" + safeAge + "':x=1200:y=450:fontsize=160:fontcolor=white:fontfile='" + fontFile + "'");
        }

        command.add("-c:a");
        command.add("copy");
        command.add(outputVideo.toString());

        runFfmpegCommand(command, 60);
    }

    private String getFieldValue(VideoTaskRequest request, String key) {
        // 先从动态字段 Map 取
        if (request.getDynamicFields() != null) {
            String val = request.getDynamicFields().get(key);
            if (val != null) return val;
        }
        // 再从老字段兼容
        switch (key) {
            case "name": return request.getName() != null ? request.getName() : "";
            case "age":  return request.getAge()  != null ? request.getAge()  : "";
            case "time": return request.getTime() != null ? request.getTime() : "";
            case "hotel":return request.getHotel()!= null ? request.getHotel(): "";
            default:     return "";
        }
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replace("'", "").replace(":", "").replace(",", "");
    }

    private int[] getVideoResolution(Path videoPath) throws IOException, InterruptedException {
        // 尝试用 ffprobe 获取分辨率，失败时默认 1920x1080
        String ffprobe = ffmpegPath.toString().replace("ffmpeg.exe", "ffprobe.exe").replace("ffmpeg", "ffprobe");
        if (!new java.io.File(ffprobe).exists()) {
            return new int[]{1920, 1080};
        }
        List<String> cmd = Arrays.asList(
                ffprobe, "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=p=0",
                videoPath.toString()
        );
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        Process p = pb.start();
        String out = new String(readAllBytes(p.getInputStream()), StandardCharsets.UTF_8).trim();
        p.waitFor(10, TimeUnit.SECONDS);
        String[] parts = out.split(",");
        if (parts.length >= 2) {
            try {
                return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
            } catch (NumberFormatException ignored) {}
        }
        return new int[]{1920, 1080};
    }

    private void runFfmpegCommand(List<String> command, int timeoutSeconds) throws IOException, InterruptedException {
        if (!Files.exists(ffmpegPath)) {
            throw new IllegalStateException("ffmpeg not found at " + ffmpegPath);
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder log = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append("\n");
            }
        }
        boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new IllegalStateException("FFmpeg timeout. Log: " + log);
        }
        if (p.exitValue() != 0) {
            throw new IllegalStateException("FFmpeg exited with code " + p.exitValue() + ". Log: " + log);
        }
    }
}
