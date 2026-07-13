package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageException;
import com.microservice.framework.objectstorage.api.StorageObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageOperationsTest {

    @Mock
    private S3Client s3Client;

    private ObjectStorageProperties properties;
    private S3ObjectStorageOperations operations;

    @BeforeEach
    void setUp() {
        properties = new ObjectStorageProperties();
        properties.getConnection().setBucket("default-bucket");
        operations = new S3ObjectStorageOperations(s3Client, properties);
    }

    @Test
    @DisplayName("upload should validate and send PutObjectRequest")
    void uploadShouldValidateAndSendPutObjectRequest() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("\"abc\"").build());

        StorageObject uploaded = operations.upload("photos", "a.txt",
                new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), "text/plain");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(request.capture(), body.capture());
        assertThat(request.getValue().bucket()).isEqualTo("photos");
        assertThat(request.getValue().key()).isEqualTo("a.txt");
        assertThat(request.getValue().contentType()).isEqualTo("text/plain");
        assertThat(body.getValue().contentLength()).isEqualTo(5);
        assertThat(uploaded.getBucket()).isEqualTo("photos");
        assertThat(uploaded.getKey()).isEqualTo("a.txt");
        assertThat(uploaded.getSize()).isEqualTo(5);
        assertThat(uploaded.getETag()).isEqualTo("\"abc\"");
    }

    @Test
    @DisplayName("download should return SDK response stream")
    void downloadShouldReturnSdkResponseStream() throws Exception {
        ResponseInputStream<GetObjectResponse> response = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream("file".getBytes(StandardCharsets.UTF_8)));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(response);

        byte[] bytes = operations.download("docs", "file.txt").readAllBytes();

        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("docs");
        assertThat(request.getValue().key()).isEqualTo("file.txt");
        assertThat(bytes).isEqualTo("file".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("delete should send DeleteObjectRequest")
    void deleteShouldSendDeleteObjectRequest() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        operations.delete("docs", "old.txt");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("docs");
        assertThat(request.getValue().key()).isEqualTo("old.txt");
    }

    @Test
    @DisplayName("exists should return false for missing S3 object")
    void existsShouldReturnFalseForMissingS3Object() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("not found").build());

        assertThat(operations.exists("docs", "missing.txt")).isFalse();
    }

    @Test
    @DisplayName("list should map S3 objects to StorageObject")
    void listShouldMapS3ObjectsToStorageObjects() {
        Instant modified = Instant.parse("2026-01-02T03:04:05Z");
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder()
                        .contents(S3Object.builder()
                                .key("reports/a.pdf")
                                .size(42L)
                                .lastModified(modified)
                                .eTag("etag")
                                .build())
                        .build());

        List<StorageObject> objects = operations.list("docs", "reports/");

        ArgumentCaptor<ListObjectsV2Request> request = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3Client).listObjectsV2(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("docs");
        assertThat(request.getValue().prefix()).isEqualTo("reports/");
        assertThat(objects).singleElement().satisfies(object -> {
            assertThat(object.getBucket()).isEqualTo("docs");
            assertThat(object.getKey()).isEqualTo("reports/a.pdf");
            assertThat(object.getSize()).isEqualTo(42L);
            assertThat(object.getLastModified()).isEqualTo(modified);
            assertThat(object.getETag()).isEqualTo("etag");
        });
    }

    @Test
    @DisplayName("copy and move should use SDK copy then delete source")
    void copyAndMoveShouldUseSdkCopyThenDeleteSource() {
        when(s3Client.copyObject(any(CopyObjectRequest.class)))
                .thenReturn(CopyObjectResponse.builder().build());
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(9L).contentType("text/plain").build());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        StorageObject copied = operations.copy("src", "old.txt", "dest", "new.txt");
        StorageObject moved = operations.move("src", "old.txt", "dest", "newer.txt");

        ArgumentCaptor<CopyObjectRequest> copyRequest = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3Client, org.mockito.Mockito.times(2)).copyObject(copyRequest.capture());
        assertThat(copyRequest.getAllValues().get(0).sourceBucket()).isEqualTo("src");
        assertThat(copyRequest.getAllValues().get(0).sourceKey()).isEqualTo("old.txt");
        assertThat(copyRequest.getAllValues().get(0).destinationBucket()).isEqualTo("dest");
        assertThat(copyRequest.getAllValues().get(0).destinationKey()).isEqualTo("new.txt");
        assertThat(copied.getBucket()).isEqualTo("dest");
        assertThat(copied.getKey()).isEqualTo("new.txt");
        assertThat(moved.getKey()).isEqualTo("newer.txt");
        inOrder(s3Client).verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("metadata and size should come from HeadObjectResponse")
    void metadataAndSizeShouldComeFromHeadObjectResponse() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentLength(123L)
                        .metadata(Map.of("owner", "framework"))
                        .build());

        assertThat(operations.getMetadata("docs", "file.txt")).containsEntry("owner", "framework");
        assertThat(operations.getSize("docs", "file.txt")).isEqualTo(123L);
    }

    @Test
    @DisplayName("SDK errors should become ObjectStorageException without secret text in message")
    void sdkErrorsShouldBecomeObjectStorageExceptionWithoutSecretTextInMessage() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(SdkClientException.builder()
                        .message("network failed with secretKey=super-secret")
                        .build());

        assertThatThrownBy(() -> operations.getSize("docs", "file.txt"))
                .isInstanceOf(ObjectStorageException.class)
                .hasMessageNotContaining("super-secret");
    }
}
