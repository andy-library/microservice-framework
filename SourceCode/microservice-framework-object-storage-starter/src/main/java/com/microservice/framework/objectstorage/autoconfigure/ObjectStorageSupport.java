package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

final class ObjectStorageSupport {

    private ObjectStorageSupport() {
    }

    static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new ObjectStorageException(ObjectStorageException.OS_PARAM_NULL,
                    name + " must not be blank");
        }
        return value;
    }

    static InputStream requireInputStream(InputStream inputStream) {
        if (inputStream == null) {
            throw new ObjectStorageException(ObjectStorageException.OS_PARAM_NULL,
                    "inputStream must not be null");
        }
        return inputStream;
    }

    static void validateContentType(ObjectStorageProperties properties, String contentType) {
        List<String> allowedTypes = properties.getUpload().getAllowedTypes();
        if (contentType != null && !contentType.isBlank()
                && allowedTypes != null && !allowedTypes.isEmpty()
                && !allowedTypes.contains(contentType)) {
            throw new ObjectStorageException(ObjectStorageException.OS_UPLOAD_TYPE_NOT_ALLOWED,
                    "contentType is not allowed: " + contentType);
        }
    }

    static byte[] readBounded(InputStream inputStream, long maxFileSize) {
        requireInputStream(inputStream);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > maxFileSize) {
                    throw new ObjectStorageException(ObjectStorageException.OS_UPLOAD_SIZE_EXCEEDED,
                            "object size exceeds maxFileSize: " + maxFileSize);
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                    "failed to read object content", ex);
        }
    }

    static Duration requirePositiveExpiry(Duration expiry) {
        if (expiry == null || expiry.isZero() || expiry.isNegative()) {
            throw new ObjectStorageException(ObjectStorageException.OS_PARAM_NULL,
                    "expiry must be positive");
        }
        if (expiry.compareTo(Duration.ofDays(7)) > 0) {
            throw new ObjectStorageException(ObjectStorageException.OS_PRESIGN_FAILED,
                    "expiry must not exceed 7 days");
        }
        return expiry;
    }

    static Duration expiryUntil(Instant expiration) {
        if (expiration == null) {
            throw new ObjectStorageException(ObjectStorageException.OS_PARAM_NULL,
                    "expiration must not be null");
        }
        return requirePositiveExpiry(Duration.between(Instant.now(), expiration));
    }
}
