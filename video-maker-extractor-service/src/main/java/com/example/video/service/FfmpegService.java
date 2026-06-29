package com.example.video.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FfmpegService {

    private final Path ffmpegPath;
    private final Path projectRoot;

    public FfmpegService(@Value("${video.isOnline}") boolean isOnline,
                         @Value("${video.ffmpeg.path.dev}") String ffmpegPathDev,
                         @Value("${video.ffmpeg.path.prod}") String ffmpegPathProd) {
        this.projectRoot = resolveProjectRoot();
        String ffmpegConfigPath = isOnline ? ffmpegPathProd : ffmpegPathDev;

        if (new File(ffmpegConfigPath).isAbsolute()) {
            this.ffmpegPath = Paths.get(ffmpegConfigPath);
        } else {
            this.ffmpegPath = projectRoot.resolve(ffmpegConfigPath);
        }
        log.info("FfmpegService initialized, projectRoot={}, ffmpegPath={}", projectRoot, ffmpegPath);
    }

    private Path resolveProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        if (current.getFileName().toString().equals("backend")) {
            return current.getParent();
        } else if (current.getParent() != null && current.getParent().getFileName().toString().equals("backend")) {
            return current.getParent().getParent();
        }
        return current;
    }

    public Path getUploadsDirectory() {
        Path uploads = projectRoot.resolve("uploads");
        try {
            Files.createDirectories(uploads);
        } catch (IOException ignored) {}
        return uploads;
    }

    public void extractAudio(Path videoFile, Path audioFile) throws IOException, InterruptedException {
        boolean useLocalFfmpeg = Files.exists(ffmpegPath);
        if (!useLocalFfmpeg) {
            log.warn("Ffmpeg binary not found at {}, trying fallback 'ffmpeg' in system PATH", ffmpegPath);
        }

        List<String> command = new ArrayList<>();
        command.add(useLocalFfmpeg ? ffmpegPath.toString() : "ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(videoFile.toString());
        command.add("-vn");
        command.add("-acodec");
        command.add("libmp3lame");
        command.add("-q:a");
        command.add("2");
        command.add(audioFile.toString());

        log.info("Running ffmpeg command: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder logBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logBuilder.append(line).append("\n");
            }
        }

        boolean completed = p.waitFor(60, TimeUnit.SECONDS);
        if (!completed) {
            p.destroyForcibly();
            throw new IllegalStateException("FFmpeg timeout, output log:\n" + logBuilder);
        }

        if (p.exitValue() != 0) {
            throw new IllegalStateException("FFmpeg failed with exit code " + p.exitValue() + ", log:\n" + logBuilder);
        }

        log.info("Successfully extracted audio to {}", audioFile);
    }
}
