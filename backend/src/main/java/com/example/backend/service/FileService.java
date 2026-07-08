package com.example.backend.service;

import com.example.backend.dto.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    @Value("${spring.web.resources.static-locations:classpath:/static/}")
    private String staticLocation;

    public Result<String> uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error("文件名不能为空");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (!isImageExtension(extension)) {
            return Result.error("只支持图片格式");
        }

        String newFilename = UUID.randomUUID().toString() + extension;

        try {
            Path uploadPath = Paths.get("src/main/resources/static/images/uploads");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(newFilename);
            Files.write(filePath, file.getBytes());

            return Result.success("/images/uploads/" + newFilename);
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    private boolean isImageExtension(String extension) {
        String ext = extension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png")
                || ext.equals(".gif") || ext.equals(".webp");
    }

    public Result<String> uploadBase64Image(String filename, String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) {
            return Result.error("图片数据不能为空");
        }

        try {
            // 移除可能的 data:image/xxx;base64, 前缀
            String pureBase64 = base64Data;
            if (base64Data.contains(",")) {
                pureBase64 = base64Data.substring(base64Data.indexOf(",") + 1);
            }

            byte[] imageBytes = java.util.Base64.getDecoder().decode(pureBase64);

            // 确定文件扩展名
            String extension = ".jpg";
            if (filename != null && filename.contains(".")) {
                extension = filename.substring(filename.lastIndexOf("."));
            }

            String newFilename = UUID.randomUUID().toString() + extension;

            Path uploadPath = Paths.get("src/main/resources/static/images/uploads");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(newFilename);
            Files.write(filePath, imageBytes);

            return Result.success("/images/uploads/" + newFilename);
        } catch (IllegalArgumentException e) {
            return Result.error("无效的Base64数据");
        } catch (IOException e) {
            return Result.error("文件保存失败: " + e.getMessage());
        }
    }
}
