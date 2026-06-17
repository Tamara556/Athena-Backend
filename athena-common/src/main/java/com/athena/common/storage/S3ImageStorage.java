package com.athena.common.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class S3ImageStorage implements ImageStorage, InitializingBean {

    private final S3Client s3Client;
    private final StorageProperties properties;

    @Override
    public void afterPropertiesSet() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucket()).build());
        } catch (NoSuchBucketException notFound) {
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
                log.info("Created S3 bucket '{}' for image storage.", properties.getBucket());
            } catch (RuntimeException createError) {
                log.warn("Could not create S3 bucket '{}': {}", properties.getBucket(), createError.getMessage());
            }
        } catch (RuntimeException ex) {
            log.warn("S3 storage is not reachable at startup ({}). Image uploads will fail until it is.",
                    ex.getMessage());
        }
    }

    @Override
    public String store(String namespace, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String extension = extensionOf(file);
        validate(file, extension);

        String prefix = namespace == null || namespace.isBlank() ? "" : namespace + "/";
        String key = prefix + UUID.randomUUID() + "." + extension;
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));
        } catch (IOException | RuntimeException ex) {
            log.error("Failed to upload image to S3 bucket '{}'", properties.getBucket(), ex);
            throw new IllegalStateException(ImageConstants.UPLOAD_FAILED, ex);
        }
        log.info("Stored image key={}", key);
        return key;
    }

    @Override
    public Optional<StoredImage> load(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        try (ResponseInputStream<GetObjectResponse> object = s3Client.getObject(
                GetObjectRequest.builder().bucket(properties.getBucket()).key(key).build())) {
            byte[] data = object.readAllBytes();
            String contentType = object.response().contentType();
            return Optional.of(new StoredImage(data, contentType));
        } catch (NoSuchKeyException missing) {
            return Optional.empty();
        } catch (IOException | RuntimeException ex) {
            log.warn("Could not read image key={}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    private void validate(MultipartFile file, String extension) {
        if (file.getSize() > ImageConstants.MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(ImageConstants.TOO_LARGE);
        }
        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean extensionOk = ImageConstants.ALLOWED_EXTENSIONS.contains(extension);
        boolean contentTypeOk = ImageConstants.ALLOWED_CONTENT_TYPES.contains(contentType);
        if (!extensionOk || !contentTypeOk) {
            throw new IllegalArgumentException(ImageConstants.TYPE_INVALID);
        }
    }

    private String extensionOf(MultipartFile file) {
        String name = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(name);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }
}
