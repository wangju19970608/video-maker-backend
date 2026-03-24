package com.example.video.dto;

public class HistoricalTaskView {
    private String taskId;
    private String status;
    private String message;
    private String docxUrl;
    private String imageUrl;
    private String videoUrl;
    private String parametersUrl;

    public HistoricalTaskView() {}

    public HistoricalTaskView(String taskId, String status, String message, String docxUrl, String imageUrl, String videoUrl, String parametersUrl) {
        this.taskId = taskId;
        this.status = status;
        this.message = message;
        this.docxUrl = docxUrl;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.parametersUrl = parametersUrl;
    }

    public String getParametersUrl() { return parametersUrl; }
    public void setParametersUrl(String parametersUrl) { this.parametersUrl = parametersUrl; }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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
