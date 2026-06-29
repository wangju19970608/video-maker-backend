package com.example.video.controller;

import com.example.video.entity.DyVideoExtractorTask;
import com.example.video.model.SysUser;
import com.example.video.repository.DyVideoExtractorTaskRepository;
import com.example.video.service.FfmpegService;
import com.example.video.service.SpeechToTextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/extractor/video")
public class ExtractorVideoController {

    @Autowired
    private FfmpegService ffmpegService;

    @Autowired
    private SpeechToTextService speechToTextService;

    @Autowired
    private DyVideoExtractorTaskRepository taskRepository;

    @PostMapping("/upload")
    public Result<UploadResponse> upload(@RequestParam("file") MultipartFile file, HttpServletRequest servletRequest) {
        SysUser user = (SysUser) servletRequest.getAttribute("USER");
        if (user == null) {
            return Result.fail("User not authenticated");
        }

        if (file == null || file.isEmpty()) {
            return Result.fail("Uploaded file is empty");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            log.info("Starting video extraction upload: originalFilename={}, size={}, userId={}", 
                    originalFilename, file.getSize(), user.getId());

            String ext = ".mp4";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Generate unique filenames for outputs
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String videoFileName = "dy_video_" + uuid + ext;
            String audioFileName = "dy_audio_" + uuid + ".mp3";
            String textFileName = "dy_text_" + uuid + ".txt";

            Path uploadsDir = ffmpegService.getUploadsDirectory();
            Path videoFilePath = uploadsDir.resolve(videoFileName);
            Path audioFilePath = uploadsDir.resolve(audioFileName);
            Path textFilePath = uploadsDir.resolve(textFileName);

            // 1. Save uploaded video file
            log.info("Saving uploaded video to: {}", videoFilePath);
            file.transferTo(videoFilePath.toFile());

            // 2. Extract audio using FFmpeg
            log.info("Extracting audio as MP3 to: {}", audioFilePath);
            ffmpegService.extractAudio(videoFilePath, audioFilePath);

            // 3. Transcribe audio to text
            log.info("Transcribing audio content...");
            String textContent = speechToTextService.transcribe(audioFilePath);

            // 4. Save text file
            log.info("Saving transcribed text to: {}", textFilePath);
            Files.write(textFilePath, textContent.getBytes(StandardCharsets.UTF_8));

            // 5. Delete temp video file
            try {
                Files.deleteIfExists(videoFilePath);
            } catch (Exception e) {
                log.warn("Failed to delete temp video file: {}", e.getMessage());
            }

            // 6. Record task in DB
            DyVideoExtractorTask task = new DyVideoExtractorTask();
            task.setUserId(user.getId());
            task.setVideoFileName(originalFilename);
            task.setAudioFileName(audioFileName);
            task.setTextFileName(textFileName);
            task.setTextContent(textContent);
            task = taskRepository.save(task);

            log.info("Created extraction task in DB: taskId={}", task.getId());

            UploadResponse response = new UploadResponse();
            response.setTaskId(task.getId());
            response.setOriginalFileName(originalFilename);
            response.setAudioFileName(audioFileName);
            response.setTextFileName(textFileName);
            response.setAudioUrl("/api/assets/uploads/" + audioFileName);
            response.setTextUrl("/api/assets/uploads/" + textFileName);
            response.setTextContent(textContent);

            return Result.success(response);

        } catch (Exception e) {
            log.error("Video processing failed", e);
            return Result.fail("Processing failed: " + e.getMessage());
        }
    }

    @PostMapping("/rename")
    public Result<RenameResponse> rename(@RequestBody RenameRequest request, HttpServletRequest servletRequest) {
        SysUser user = (SysUser) servletRequest.getAttribute("USER");
        if (user == null) {
            return Result.fail("User not authenticated");
        }

        if (request == null || request.getTaskId() == null) {
            return Result.fail("task ID is required");
        }

        try {
            DyVideoExtractorTask task = taskRepository.findById(request.getTaskId()).orElse(null);
            if (task == null) {
                return Result.fail("Task not found");
            }

            if (!task.getUserId().equals(user.getId())) {
                return Result.fail("Access denied: task does not belong to user");
            }

            Path uploadsDir = ffmpegService.getUploadsDirectory();

            String oldAudioName = task.getAudioFileName();
            String oldTextName = task.getTextFileName();

            String newAudioName = request.getNewAudioFileName();
            String newTextName = request.getNewTextFileName();

            if (newAudioName != null && !newAudioName.trim().isEmpty()) {
                newAudioName = newAudioName.trim();
                if (!newAudioName.toLowerCase().endsWith(".mp3")) {
                    newAudioName += ".mp3";
                }
                if (!newAudioName.equals(oldAudioName)) {
                    Path oldAudioPath = uploadsDir.resolve(oldAudioName);
                    Path newAudioPath = uploadsDir.resolve(newAudioName);
                    if (Files.exists(oldAudioPath)) {
                        Files.move(oldAudioPath, newAudioPath);
                        task.setAudioFileName(newAudioName);
                        log.info("Renamed audio file from {} to {}", oldAudioName, newAudioName);
                    }
                }
            }

            if (newTextName != null && !newTextName.trim().isEmpty()) {
                newTextName = newTextName.trim();
                if (!newTextName.toLowerCase().endsWith(".txt")) {
                    newTextName += ".txt";
                }
                if (!newTextName.equals(oldTextName)) {
                    Path oldTextPath = uploadsDir.resolve(oldTextName);
                    Path newTextPath = uploadsDir.resolve(newTextName);
                    if (Files.exists(oldTextPath)) {
                        Files.move(oldTextPath, newTextPath);
                        task.setTextFileName(newTextName);
                        log.info("Renamed text file from {} to {}", oldTextName, newTextName);
                    }
                }
            }

            task = taskRepository.save(task);

            RenameResponse response = new RenameResponse();
            response.setTaskId(task.getId());
            response.setAudioFileName(task.getAudioFileName());
            response.setTextFileName(task.getTextFileName());
            response.setAudioUrl("/api/assets/uploads/" + task.getAudioFileName());
            response.setTextUrl("/api/assets/uploads/" + task.getTextFileName());

            return Result.success(response);

        } catch (Exception e) {
            log.error("File renaming failed", e);
            return Result.fail("Renaming failed: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public Result<List<DyVideoExtractorTask>> getHistory(HttpServletRequest servletRequest) {
        SysUser user = (SysUser) servletRequest.getAttribute("USER");
        if (user == null) {
            return Result.fail("User not authenticated");
        }

        log.info("Fetching conversion history for userId={}", user.getId());
        List<DyVideoExtractorTask> tasks = taskRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return Result.success(tasks);
    }

    // Response and Request DTOs
    public static class Result<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> Result<T> success(T data) {
            Result<T> result = new Result<>();
            result.success = true;
            result.message = "success";
            result.data = data;
            return result;
        }

        public static <T> Result<T> fail(String message) {
            Result<T> result = new Result<>();
            result.success = false;
            result.message = message;
            return result;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }

    public static class UploadResponse {
        private Long taskId;
        private String originalFileName;
        private String audioFileName;
        private String textFileName;
        private String audioUrl;
        private String textUrl;
        private String textContent;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getOriginalFileName() { return originalFileName; }
        public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
        public String getAudioFileName() { return audioFileName; }
        public void setAudioFileName(String audioFileName) { this.audioFileName = audioFileName; }
        public String textFileName() { return textFileName; }
        public String getTextFileName() { return textFileName; }
        public void setTextFileName(String textFileName) { this.textFileName = textFileName; }
        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
        public String getTextUrl() { return textUrl; }
        public void setTextUrl(String textUrl) { this.textUrl = textUrl; }
        public String getTextContent() { return textContent; }
        public void setTextContent(String textContent) { this.textContent = textContent; }
    }

    public static class RenameRequest {
        private Long taskId;
        private String newAudioFileName;
        private String newTextFileName;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getNewAudioFileName() { return newAudioFileName; }
        public void setNewAudioFileName(String newAudioFileName) { this.newAudioFileName = newAudioFileName; }
        public String getNewTextFileName() { return newTextFileName; }
        public void setNewTextFileName(String newTextFileName) { this.newTextFileName = newTextFileName; }
    }

    public static class RenameResponse {
        private Long taskId;
        private String audioFileName;
        private String textFileName;
        private String audioUrl;
        private String textUrl;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getAudioFileName() { return audioFileName; }
        public void setAudioFileName(String audioFileName) { this.audioFileName = audioFileName; }
        public String getTextFileName() { return textFileName; }
        public void setTextFileName(String textFileName) { this.textFileName = textFileName; }
        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
        public String getTextUrl() { return textUrl; }
        public void setTextUrl(String textUrl) { this.textUrl = textUrl; }
    }
}
