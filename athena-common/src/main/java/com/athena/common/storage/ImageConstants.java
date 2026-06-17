package com.athena.common.storage;

import java.util.Set;

public final class ImageConstants {

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif");
    public static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/jpg", "image/gif");

    public static final String TYPE_INVALID = "Image must be a PNG, JPEG, JPG or GIF";
    public static final String TOO_LARGE = "Image must be 5 MB or smaller";
    public static final String UPLOAD_FAILED = "Could not store the image, please try again";
}
