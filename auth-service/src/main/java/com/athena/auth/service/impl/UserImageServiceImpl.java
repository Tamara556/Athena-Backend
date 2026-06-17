package com.athena.auth.service.impl;

import com.athena.auth.service.UserImageService;
import com.athena.common.storage.ImageStorage;
import com.athena.common.storage.ImageStorage.StoredImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserImageServiceImpl implements UserImageService {

    private final ImageStorage imageStorage;

    @Override
    public String store(UUID userId, MultipartFile file) {
        return imageStorage.store(userId.toString(), file);
    }

    @Override
    public Optional<StoredImage> load(String key) {
        return imageStorage.load(key);
    }
}
