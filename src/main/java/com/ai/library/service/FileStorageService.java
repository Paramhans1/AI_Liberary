package com.ai.library.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {
    private final Path root;

    public FileStorageService(@Value("${storage.upload-dir:${user.home}/ai-library-storage}") String uploadDir) {
        this.root = Path.of(StringUtils.cleanPath(uploadDir));
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory", e);
        }
    }

    public String store(MultipartFile file, String filename) {
        try {
            if (filename == null || filename.isBlank()) filename = StringUtils.cleanPath(file.getOriginalFilename());
            // sanitize filename and prevent path traversal
            filename = filename.replaceAll("\\\u0000", "");
            filename = Path.of(filename).getFileName().toString();

            // simple extension whitelist
            String lower = filename.toLowerCase();
            if (!(lower.endsWith(".pdf") || lower.endsWith(".epub"))) {
                throw new RuntimeException("Unsupported file type. Only PDF and EPUB allowed");
            }

            // size limit: 50 MB
            long maxBytes = 50L * 1024L * 1024L;
            if (file.getSize() > maxBytes) {
                throw new RuntimeException("File too large. Max 50MB");
            }

            Path target = root.resolve(filename).normalize();
            if (!target.startsWith(root)) {
                throw new RuntimeException("Invalid path");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Path load(String filename) {
        return root.resolve(filename).normalize();
    }
}
