package com.shiptrack.shiptrack_pro.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file);
    Resource load(String fileName);
    void delete(String storedUrl);
}
