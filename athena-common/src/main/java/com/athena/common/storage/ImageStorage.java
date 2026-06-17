package com.athena.common.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface ImageStorage {

    String store(String namespace, MultipartFile file);

    Optional<StoredImage> load(String key);

    record StoredImage(byte[] data, String contentType) {
    }
}
