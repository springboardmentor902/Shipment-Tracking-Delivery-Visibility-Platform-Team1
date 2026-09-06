package com.shiptrack.shiptrack_pro.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesLoadsAndDeletesAnImage() throws Exception {
        LocalFileStorageService service = new LocalFileStorageService(tempDirectory.toString(), 1024);
        service.initialize();
        MockMultipartFile image = new MockMultipartFile(
                "photo", "delivery.png", "image/png", new byte[]{1, 2, 3, 4});

        String url = service.store(image);
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        assertThat(url).startsWith("/api/files/").endsWith(".png");
        assertThat(service.load(fileName).exists()).isTrue();

        service.delete(url);
        assertThat(tempDirectory.resolve(fileName)).doesNotExist();
    }

    @Test
    void rejectsUnsupportedOrOversizedFiles() {
        LocalFileStorageService service = new LocalFileStorageService(tempDirectory.toString(), 3);
        service.initialize();

        assertThatThrownBy(() -> service.store(new MockMultipartFile(
                "photo", "note.txt", "text/plain", new byte[]{1})))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only PNG, JPEG and WebP");
        assertThatThrownBy(() -> service.store(new MockMultipartFile(
                "photo", "large.png", "image/png", new byte[]{1, 2, 3, 4})))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("5 MB or smaller");
    }
}
