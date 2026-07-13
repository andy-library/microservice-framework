package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioPreSignedUrlGeneratorTest {

    @Mock
    private MinioClient minioClient;

    private MinioPreSignedUrlGenerator generator;

    @BeforeEach
    void setUp() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.getConnection().setBucket("default-bucket");
        properties.getPresign().setDefaultExpiry(600);
        generator = new MinioPreSignedUrlGenerator(minioClient, properties);
    }

    @Test
    @DisplayName("download presign should request GET URL")
    void downloadPresignShouldRequestGetUrl() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://example.test/download");

        String url = generator.generateDownloadUrl("docs", "file.txt");

        ArgumentCaptor<GetPresignedObjectUrlArgs> request =
                ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient).getPresignedObjectUrl(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("docs");
        assertThat(request.getValue().object()).isEqualTo("file.txt");
        assertThat(request.getValue().method()).isEqualTo(Method.GET);
        assertThat(request.getValue().expiry()).isEqualTo(600);
        assertThat(url).isEqualTo("https://example.test/download");
    }

    @Test
    @DisplayName("upload presign should request PUT URL")
    void uploadPresignShouldRequestPutUrl() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://example.test/upload");

        String url = generator.generateUploadUrl("docs", "file.txt");

        ArgumentCaptor<GetPresignedObjectUrlArgs> request =
                ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient).getPresignedObjectUrl(request.capture());
        assertThat(request.getValue().method()).isEqualTo(Method.PUT);
        assertThat(request.getValue().expiry()).isEqualTo(600);
        assertThat(url).isEqualTo("https://example.test/upload");
    }

    @Test
    @DisplayName("custom expiry should be rounded to whole seconds")
    void customExpiryShouldBeRoundedToWholeSeconds() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://example.test/custom");

        generator.generateUrl("docs", "file.txt", Duration.ofSeconds(90));

        ArgumentCaptor<GetPresignedObjectUrlArgs> request =
                ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient).getPresignedObjectUrl(request.capture());
        assertThat(request.getValue().method()).isEqualTo(Method.GET);
        assertThat(request.getValue().expiry()).isEqualTo(90);
    }
}
