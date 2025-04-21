package com.example.factorial.src.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

@RestController
public class CsvExportController {

    @GetMapping("/export/csv")
    public ResponseEntity<InputStreamResource> exportToCsv() throws IOException {
        // 示例数据 - 替换为你的实际数据
        List<List<String>> data = Arrays.asList(
                Arrays.asList("Name", "Age", "Email"),
                Arrays.asList("John Doe", "30", "john@example.com"),
                Arrays.asList("Jane Smith", "25", "jane@example.com")
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);

        // 写入CSV内容
        for (List<String> row : data) {
            writer.println(String.join(",", row));
        }
        writer.flush();
        writer.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=export.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(in));
    }
}