package com.example.video.feign;

import com.example.video.dto.HistoricalTaskView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(name = "video-maker-biz-birthday", contextId = "birthdayVideoTaskClient")
public interface VideoTaskFeignClient {

    @GetMapping("/api/video/tasks/internal/history/{orderId}")
    List<HistoricalTaskView> listHistoricalTasks(@PathVariable("orderId") Long orderId);

    @PostMapping(value = "/api/video/tasks/internal/custom/{orderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Object createFromCustomDocx(@PathVariable("orderId") Long orderId,
                                @RequestPart("file") MultipartFile file);
}
