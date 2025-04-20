package com.example.factorial.src.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataImportController {

    // 配置存储目录
    private final Path rootLocation = Paths.get("uploaded-data");

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("无法创建存储目录", e);
        }
    }

    @PostMapping("/upload-csv")
    public ResponseEntity<?> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "deviceCount", required = false) Integer deviceCount,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {

        try {
            // 1. 验证文件
            if (file.isEmpty()) {
                System.out.println("1");
                return ResponseEntity.badRequest().body("请上传非空文件");
            }

            if (!file.getOriginalFilename().endsWith(".csv")) {
                System.out.println("2");
                return ResponseEntity.badRequest().body("仅支持CSV文件");
            }

            // 2. 生成唯一文件名
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path destinationFile = this.rootLocation.resolve(
                            Paths.get(filename))
                    .normalize().toAbsolutePath();

            // 3. 安全检查：确保路径在存储目录内
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                System.out.println("3");
                return ResponseEntity.badRequest().body("无法存储文件在指定目录外");
            }

            // 4. 保存文件到本地
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            // 5. 记录元数据 (可选)
            logFileMetadata(filename, deviceCount, startTime, endTime);
            System.out.println("4");
            return ResponseEntity.ok().body(Map.of(
                    "status", "success",
                    "filename", filename,
                    "size", file.getSize(),
                    "message", "文件上传成功"
            ));

        } catch (IOException e) {
            System.out.println("5");
            return ResponseEntity.internalServerError().body("文件存储失败: " + e.getMessage());
        }
    }

    private void logFileMetadata(String filename, Integer deviceCount, String startTime, String endTime) {
        String meta = String.format(
                "%s,%s,%s,%s,%s\n",
                new java.util.Date(),
                filename,
                deviceCount != null ? deviceCount : "unknown",
                startTime != null ? startTime : "unknown",
                endTime != null ? endTime : "unknown"
        );

        try {
            Files.write(
                    this.rootLocation.resolve("_metadata.csv"),
                    meta.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("无法记录元数据: " + e.getMessage());
        }
    }
}