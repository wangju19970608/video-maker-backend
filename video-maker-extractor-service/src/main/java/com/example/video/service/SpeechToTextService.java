package com.example.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;

@Slf4j
@Service
public class SpeechToTextService {

    @Value("${extractor.whisper.api-key}")
    private String apiKey;

    @Value("${extractor.whisper.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String transcribe(Path audioFile) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("Whisper API key is empty. Falling back to high-quality simulated transcription.");
            return "你好，这是通过音视频提取技术从您上传的视频中提取的音频内容文本。当前系统运行于开发环境，如需体验真实的语音识别（ASR）转写，请在 video-maker-extractor-service 的 application.properties 配置文件中填写有效的 extractor.whisper.api-key 参数。提取出的音频已被转换为高品质 MP3，您可以在界面中进行播放收听、修改文件名或保存文件。";
        }

        try {
            log.info("Transcribing audio file {} using Whisper API...", audioFile);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(audioFile.toFile()));
            body.add("model", "whisper-1");
            body.add("language", "zh");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String text = root.path("text").asText();
                log.info("Whisper transcription success, length={}", text.length());
                return text;
            } else {
                log.error("Whisper API returned non-OK status: {}, response: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Whisper API error: " + response.getBody());
            }

        } catch (Exception e) {
            log.error("Whisper API call failed, falling back to simulated transcription", e);
            return "【语音转写失败，已启用备用文本】你好，当前调用 ASR (Whisper) 服务发生异常：" + e.getMessage() + "。请确认配置的 API URL、密钥和网络代理设置。视频已成功提取到独立 MP3 文件中。";
        }
    }
}
