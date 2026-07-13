package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3PreSignedUrlGeneratorTest {

    @Mock
    private S3Presigner presigner;

    private S3PreSignedUrlGenerator generator;

    @BeforeEach
    void setUp() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.getConnection().setBucket("default-bucket");
        properties.getPresign().setDefaultExpiry(600);
        generator = new S3PreSignedUrlGenerator(presigner, properties);
    }

    @Test
    @DisplayName("download presign should build GetObjectPresignRequest")
    void downloadPresignShouldBuildGetObjectPresignRequest() throws Exception {
        PresignedGetObjectRequest response = mock(PresignedGetObjectRequest.class);
        when(response.url()).thenReturn(URI.create("https://example.test/download").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(response);

        String url = generator.generateDownloadUrl("docs", "file.txt");

        ArgumentCaptor<GetObjectPresignRequest> request =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(presigner).presignGetObject(request.capture());
        assertThat(request.getValue().signatureDuration()).isEqualTo(Duration.ofSeconds(600));
        assertThat(request.getValue().getObjectRequest().bucket()).isEqualTo("docs");
        assertThat(request.getValue().getObjectRequest().key()).isEqualTo("file.txt");
        assertThat(url).isEqualTo("https://example.test/download");
    }

    @Test
    @DisplayName("upload presign should build PutObjectPresignRequest")
    void uploadPresignShouldBuildPutObjectPresignRequest() throws Exception {
        PresignedPutObjectRequest response = mock(PresignedPutObjectRequest.class);
        when(response.url()).thenReturn(URI.create("https://example.test/upload").toURL());
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(response);

        String url = generator.generateUploadUrl("docs", "file.txt");

        ArgumentCaptor<PutObjectPresignRequest> request =
                ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(presigner).presignPutObject(request.capture());
        assertThat(request.getValue().signatureDuration()).isEqualTo(Duration.ofSeconds(600));
        assertThat(request.getValue().putObjectRequest().bucket()).isEqualTo("docs");
        assertThat(request.getValue().putObjectRequest().key()).isEqualTo("file.txt");
        assertThat(url).isEqualTo("https://example.test/upload");
    }
}
