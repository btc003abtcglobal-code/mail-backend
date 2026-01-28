package com.yourcompany.mailapp.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Component
public class FileUtil {
    
    @Value("${file.upload.directory:./uploads}")
    private String uploadDirectory;
    
    public String saveFile(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("File saved: {}", filePath);
        return filePath.toString();
    }
    
    public String saveFile(InputStream inputStream, String filename) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String extension = FilenameUtils.getExtension(filename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("File saved: {}", filePath);
        return filePath.toString();
    }
    
    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
            log.info("File deleted: {}", filePath);
        } catch (IOException e) {
            log.error("Error deleting file: {}", filePath, e);
        }
    }
    
    public File getFile(String filePath) {
        return new File(filePath);
    }
    
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
    
    public String sanitizeFilename(String filename) {
        // Remove any path separators and special characters
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    public long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            log.error("Error getting file size: {}", filePath, e);
            return 0;
        }
    }
}