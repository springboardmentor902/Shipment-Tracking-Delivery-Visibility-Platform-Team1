package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final String PUBLIC_PREFIX = "/api/files/";
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp"
    );

    private final Path uploadDirectory;
    private final long maxFileSizeBytes;

    public LocalFileStorageService(
            @Value("${app.storage.upload-directory:uploads/pod}") String uploadDirectory,
            @Value("${app.storage.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize the upload directory", exception);
        }
    }

    @Override
    public String store(MultipartFile file) {
        validate(file);
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String fileName = UUID.randomUUID() + IMAGE_EXTENSIONS.get(contentType);
        Path destination = safePath(fileName);
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            return PUBLIC_PREFIX + fileName;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store uploaded file");
        }
    }

    @Override
    public Resource load(String fileName) {
        if (fileName == null || !fileName.matches("[a-fA-F0-9-]+\\.(png|jpg|webp)")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        Path storedFile = safePath(fileName);
        if (!Files.isRegularFile(storedFile)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        try {
            return new UrlResource(storedFile.toUri());
        } catch (MalformedURLException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
    }

    @Override
    public void delete(String storedUrl) {
        if (storedUrl == null || !storedUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        String fileName = storedUrl.substring(PUBLIC_PREFIX.length());
        try {
            Files.deleteIfExists(safePath(fileName));
        } catch (IOException ignored) {
            // A failed cleanup must not hide the original request failure.
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Signature and delivery photo are required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Each image must be 5 MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !IMAGE_EXTENSIONS.containsKey(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PNG, JPEG and WebP images are accepted");
        }
    }

    private Path safePath(String fileName) {
        Path result = uploadDirectory.resolve(fileName).normalize();
        if (!result.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        return result;
    }
}
