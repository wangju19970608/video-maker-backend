package com.example.video.controller;

import com.example.video.dto.VideoTaskRequest;
import com.example.video.service.VideoTaskService;
import com.example.video.service.VideoTaskService.VideoTaskResult;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/video/tasks")
public class VideoTaskController {

    private final VideoTaskService videoTaskService;

    public VideoTaskController(VideoTaskService videoTaskService) {
        this.videoTaskService = videoTaskService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody VideoTaskRequest request,
                                                         @RequestParam(value = "async", defaultValue = "false") boolean async) {
        try {
            VideoTaskService.TaskRecord record;
            if (async) {
                record = videoTaskService.createAsync(request);
            } else {
                VideoTaskResult result = videoTaskService.generate(request);
                record = videoTaskService.recordCompleted(result);
            }
            return ResponseEntity.ok(buildTaskBody(record));
        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
        VideoTaskService.TaskRecord record = videoTaskService.getTaskStatus(taskId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(buildTaskBody(record));
    }


    @GetMapping("/{taskId}/image")
    public ResponseEntity<Resource> downloadImage(@PathVariable String taskId,
                                                  @RequestParam(value = "download", defaultValue = "false") boolean download) {
        Path filePath = videoTaskService.resolveTaskFile(taskId, "output.png");
        return buildFileResponse(filePath, MediaType.IMAGE_PNG, "output.png", download);
    }

    @GetMapping("/{taskId}/video")
    public ResponseEntity<Resource> downloadVideo(@PathVariable String taskId) {
        Path filePath = videoTaskService.resolveTaskFile(taskId, "output.mp4");
        return buildFileResponse(filePath, MediaType.valueOf("video/mp4"), "output.mp4", false);
    }

    @GetMapping("/{taskId}/docx")
    public ResponseEntity<Resource> downloadDocx(@PathVariable String taskId) {
        Path filePath = videoTaskService.resolveTaskFile(taskId, "output.docx");
        return buildFileResponse(filePath, MediaType.APPLICATION_OCTET_STREAM, "output.docx", false);
    }

    private Map<String, Object> buildTaskBody(VideoTaskService.TaskRecord record) {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", record.getTaskId());
        body.put("status", record.getStatus());
        body.put("message", record.getMessage());
        body.put("imageUrl", record.getImageUrl());
        body.put("videoUrl", record.getVideoUrl());
        body.put("docxUrl", record.getDocxUrl());
        return body;
    }

    private ResponseEntity<Resource> buildFileResponse(Path filePath, MediaType mediaType, String fallbackName, boolean download) {
        try {
            if (filePath == null || !Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(filePath.toFile());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, (download ? "attachment" : "inline") + "; filename=\"" + fallbackName + "\"")
                    .contentLength(Files.size(filePath))
                    .contentType(mediaType)
                    .body(resource);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
