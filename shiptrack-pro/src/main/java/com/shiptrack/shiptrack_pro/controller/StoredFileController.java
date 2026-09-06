package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.service.ProofOfDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class StoredFileController {

    private final ProofOfDeliveryService proofOfDeliveryService;

    @GetMapping("/api/files/{fileName:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName, Authentication authentication) {
        Resource resource = proofOfDeliveryService.getStoredFile(fileName, authentication.getName());
        MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(resource);
    }
}
