package com.athena.auth.service;

import com.athena.common.storage.ImageStorage.StoredImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

public interface UserImageService {

    String store(UUID userId, MultipartFile file);

    Optional<StoredImage> load(String key);

}
