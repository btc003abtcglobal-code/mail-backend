package com.yourcompany.mailapp.util;


import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class MimeUtil {

    private static final Map<String, String> MIME_TYPES;

    static {
        Map<String, String> map = new HashMap<>();

        // Images
        map.put("jpg", "image/jpeg");
        map.put("jpeg", "image/jpeg");
        map.put("png", "image/png");
        map.put("gif", "image/gif");
        map.put("bmp", "image/bmp");
        map.put("svg", "image/svg+xml");
        map.put("webp", "image/webp");

        // Documents
        map.put("pdf", "application/pdf");
        map.put("doc", "application/msword");
        map.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        map.put("xls", "application/vnd.ms-excel");
        map.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        map.put("ppt", "application/vnd.ms-powerpoint");
        map.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        map.put("txt", "text/plain");
        map.put("csv", "text/csv");

        // Archives
        map.put("zip", "application/zip");
        map.put("rar", "application/x-rar-compressed");
        map.put("7z", "application/x-7z-compressed");
        map.put("tar", "application/x-tar");
        map.put("gz", "application/gzip");

        // Audio
        map.put("mp3", "audio/mpeg");
        map.put("wav", "audio/wav");
        map.put("ogg", "audio/ogg");

        // Video
        map.put("mp4", "video/mp4");
        map.put("avi", "video/x-msvideo");
        map.put("mov", "video/quicktime");
        map.put("wmv", "video/x-ms-wmv");

        // Others
        map.put("json", "application/json");
        map.put("xml", "application/xml");
        map.put("html", "text/html");

        MIME_TYPES = Collections.unmodifiableMap(map);
    }

    /**
     * Get MIME type from filename extension.
     */
    public String getMimeType(String filename) {
        if (filename == null || filename.isBlank()) {
            return "application/octet-stream";
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "application/octet-stream";
        }

        String extension = filename.substring(dotIndex + 1).toLowerCase();
        return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    /**
     * Extract file extension without dot.
     */
    public String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * Check if content type is image.
     */
    public boolean isImage(String contentType) {
        return contentType != null && contentType.toLowerCase().startsWith("image/");
    }

    /**
     * Check if content type is PDF.
     */
    public boolean isPdf(String contentType) {
        return "application/pdf".equalsIgnoreCase(contentType);
    }

    /**
     * Check if content type is a common document type.
     */
    public boolean isDocument(String contentType) {
        if (contentType == null) return false;

        contentType = contentType.toLowerCase();

        return contentType.startsWith("application/msword")
                || contentType.startsWith("application/vnd.openxmlformats-officedocument")
                || contentType.equals("application/pdf")
                || contentType.equals("text/plain")
                || contentType.equals("text/csv");
    }

    /**
     * Format file size in human-readable form.
     */
    public String formatFileSize(long bytes) {
        if (bytes < 0) return "0 B";
        if (bytes < 1024) return bytes + " B";

        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);

        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }
}