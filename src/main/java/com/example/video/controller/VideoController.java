package com.example.video.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final String uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "video_maker";

    public VideoController() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateVideo(
            @RequestParam("babyName") String babyName,
            @RequestParam("coverImage") MultipartFile coverImage,
            @RequestParam("photos") List<MultipartFile> photos) {

        try {
            String taskId = UUID.randomUUID().toString();
            Path taskDir = Paths.get(uploadDir, taskId);
            Files.createDirectories(taskDir);

            Path coverPath = taskDir.resolve("cover_" + sanitizeFileName(coverImage.getOriginalFilename()));
            coverImage.transferTo(coverPath);

            for (int i = 0; i < photos.size(); i++) {
                MultipartFile photo = photos.get(i);
                Path photoPath = taskDir.resolve("photo_" + i + "_" + sanitizeFileName(photo.getOriginalFilename()));
                photo.transferTo(photoPath);
            }

            Path outputPath = taskDir.resolve("output.mp4");
            // 当前示例只演示上传与任务落盘，实际接入 FFmpeg 时在这里替换为真实渲染逻辑。
            Files.copy(coverPath, outputPath, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> body = new HashMap<>();
            body.put("status", "success");
            body.put("taskId", taskId);
            body.put("url", "/api/video/download/" + taskId);
            body.put("message", "任务创建成功，已生成示例输出文件");
            body.put("babyName", babyName);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, Object> body = new HashMap<>();
            body.put("status", "error");
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @GetMapping("/download/{taskId}")
    public ResponseEntity<Resource> downloadVideo(@PathVariable String taskId) {
        try {
            Path filePath = Paths.get(uploadDir, taskId, "output.mp4");
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new FileSystemResource(filePath.toFile());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"birthday_video.mp4\"")
                    .contentLength(Files.size(filePath))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.trim().isEmpty()) {
            return UUID.randomUUID().toString() + ".jpg";
        }
        return originalName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}
