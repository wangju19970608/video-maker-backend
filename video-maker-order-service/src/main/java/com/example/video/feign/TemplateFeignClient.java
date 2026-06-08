package com.example.video.feign;

import com.example.video.dto.TemplateView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "video-maker-biz-birthday", contextId = "birthdayTemplateClient")
public interface TemplateFeignClient {

    @GetMapping("/api/templates/{templateId}")
    TemplateView getTemplate(@PathVariable("templateId") Long templateId);

    @GetMapping("/api/templates/internal/{templateId}/docx")
    byte[] downloadTemplateDocx(@PathVariable("templateId") Long templateId);
}
