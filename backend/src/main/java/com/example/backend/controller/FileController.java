package com.example.backend.controller;

import com.example.backend.dto.Result;
import com.example.backend.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@Tag(name = "文件", description = "文件上传接口")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/api/files/upload")
    @Operation(summary = "上传图片文件")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return fileService.uploadImage(file);
    }

    @PostMapping("/api/upload/base64")
    @Operation(summary = "Base64方式上传图片")
    public Result<String> uploadBase64Image(@RequestBody Map<String, String> request) {
        String filename = request.get("filename");
        String data = request.get("data");
        return fileService.uploadBase64Image(filename, data);
    }
}
