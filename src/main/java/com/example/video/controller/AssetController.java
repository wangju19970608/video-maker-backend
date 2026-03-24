package com.example.video.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final Path templateRoot;

    public AssetController() {
        Path current = Paths.get(System.getProperty("user.dir"));
        Path projectRoot = current.getParent() == null ? current : current.getParent();
        this.templateRoot = projectRoot.resolve("template");
    }

    @GetMapping("/templates/{fileName:.+}")
    public ResponseEntity<Resource> getTemplateAsset(@PathVariable String fileName) {
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        Path filePath = templateRoot.resolve(fileName);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        try {
            String contentType = Files.probeContentType(filePath);
            MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
            Resource resource = new FileSystemResource(filePath.toFile());
            return ResponseEntity.ok().contentType(mediaType).body(resource);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/templates/{type}/{fileName:.+}")
    public ResponseEntity<Resource> getTemplateAssetWithType(@PathVariable String type, @PathVariable String fileName) {
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\") || type.contains(".") || type.contains("/") || type.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        Path filePath = templateRoot.resolve(type).resolve(fileName);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        try {
            String contentType = Files.probeContentType(filePath);
            MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
            Resource resource = new FileSystemResource(filePath.toFile());
            return ResponseEntity.ok().contentType(mediaType).body(resource);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
