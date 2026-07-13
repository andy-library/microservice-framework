package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageException;
import com.microservice.framework.objectstorage.api.StorageObject;
import io.minio.CopyObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import io.minio.messages.Item;
import okhttp3.Headers;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioObjectStorageOperationsTest {

    @Mock
    private MinioClient minioClient;

    private MinioObjectStorageOperations operations;

    @BeforeEach
    void setUp() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.getConnection().setBucket("default-bucket");
        operations = new MinioObjectStorageOperations(minioClient, properties);
    }

    @Test
    @DisplayName("upload should validate and send PutObjectArgs")
    void uploadShouldValidateAndSendPutObjectArgs() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenReturn(new ObjectWriteResponse(Headers.of(), "photos", "us-east-1", "a.txt", "etag", null));

        StorageObject uploaded = operations.upload("photos", "a.txt",
                new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), "text/plain");

        ArgumentCaptor<PutObjectArgs> request = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("photos");
        assertThat(request.getValue().object()).isEqualTo("a.txt");
        assertThat(request.getValue().contentType()).isEqualTo("text/plain");
        assertThat(uploaded.getBucket()).isEqualTo("photos");
        assertThat(uploaded.getKey()).isEqualTo("a.txt");
        assertThat(uploaded.getSize()).isEqualTo(5);
        assertThat(uploaded.getETag()).isEqualTo("etag");
    }

    @Test
    @DisplayName("download should return MinIO response stream")
    void downloadShouldReturnMinioResponseStream() throws Exception {
        GetObjectResponse response = new GetObjectResponse(Headers.of(), "docs", "us-east-1",
                "file.txt", new ByteArrayInputStream("file".getBytes(StandardCharsets.UTF_8)));
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

        byte[] bytes = operations.download("docs", "file.txt").readAllBytes();

        ArgumentCaptor<GetObjectArgs> request = ArgumentCaptor.forClass(GetObjectArgs.class);
        verify(minioClient).getObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("docs");
        assertThat(request.getValue().object()).isEqualTo("file.txt");
        assertThat(bytes).isEqualTo("file".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("delete should send RemoveObjectArgs")
    void deleteShouldSendRemoveObjectArgs() throws Exception {
        operations.delete("docs", "old.txt");

        ArgumentCaptor<RemoveObjectArgs> request = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("docs");
        assertThat(request.getValue().object()).isEqualTo("old.txt");
    }

    @Test
    @DisplayName("exists should return false when MinIO reports object missing")
    void existsShouldReturnFalseWhenMinioReportsObjectMissing() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(errorResponseException("NoSuchKey"));

        assertThat(operations.exists("docs", "missing.txt")).isFalse();
    }

    @Test
    @DisplayName("list should map MinIO items to StorageObject")
    void listShouldMapMinioItemsToStorageObjects() {
        Item item = mock(Item.class);
        ZonedDateTime modified = ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC);
        when(item.objectName()).thenReturn("reports/a.pdf");
        when(item.size()).thenReturn(42L);
        when(item.lastModified()).thenReturn(modified);
        when(item.etag()).thenReturn("etag");
        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(List.of(new Result<>(item)));

        List<StorageObject> objects = operations.list("docs", "reports/");

        ArgumentCaptor<ListObjectsArgs> request = ArgumentCaptor.forClass(ListObjectsArgs.class);
        verify(minioClient).listObjects(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("docs");
        assertThat(request.getValue().prefix()).isEqualTo("reports/");
        assertThat(objects).singleElement().satisfies(object -> {
            assertThat(object.getBucket()).isEqualTo("docs");
            assertThat(object.getKey()).isEqualTo("reports/a.pdf");
            assertThat(object.getSize()).isEqualTo(42L);
            assertThat(object.getLastModified()).isEqualTo(modified.toInstant());
            assertThat(object.getETag()).isEqualTo("etag");
        });
    }

    @Test
    @DisplayName("copy and move should use MinIO copy then remove source")
    void copyAndMoveShouldUseMinioCopyThenRemoveSource() throws Exception {
        when(minioClient.copyObject(any(CopyObjectArgs.class)))
                .thenReturn(new ObjectWriteResponse(Headers.of(), "dest", "us-east-1", "new.txt", "etag", null));
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.size()).thenReturn(9L);
        when(stat.contentType()).thenReturn("text/plain");
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(stat);

        StorageObject copied = operations.copy("src", "old.txt", "dest", "new.txt");
        StorageObject moved = operations.move("src", "old.txt", "dest", "newer.txt");

        ArgumentCaptor<CopyObjectArgs> copyRequest = ArgumentCaptor.forClass(CopyObjectArgs.class);
        verify(minioClient, org.mockito.Mockito.times(2)).copyObject(copyRequest.capture());
        assertThat(copyRequest.getAllValues().get(0).bucket()).isEqualTo("dest");
        assertThat(copyRequest.getAllValues().get(0).object()).isEqualTo("new.txt");
        assertThat(copyRequest.getAllValues().get(0).source().bucket()).isEqualTo("src");
        assertThat(copyRequest.getAllValues().get(0).source().object()).isEqualTo("old.txt");
        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        assertThat(copied.getKey()).isEqualTo("new.txt");
        assertThat(moved.getKey()).isEqualTo("newer.txt");
    }

    @Test
    @DisplayName("metadata and size should come from StatObjectResponse")
    void metadataAndSizeShouldComeFromStatObjectResponse() throws Exception {
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.userMetadata()).thenReturn(Map.of("owner", "framework"));
        when(stat.size()).thenReturn(123L);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(stat);

        assertThat(operations.getMetadata("docs", "file.txt")).containsEntry("owner", "framework");
        assertThat(operations.getSize("docs", "file.txt")).isEqualTo(123L);
    }

    @Test
    @DisplayName("SDK errors should become ObjectStorageException without secret text in message")
    void sdkErrorsShouldBecomeObjectStorageExceptionWithoutSecretTextInMessage() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(new IOException("network failed with secretKey=super-secret"));

        assertThatThrownBy(() -> operations.getSize("docs", "file.txt"))
                .isInstanceOf(ObjectStorageException.class)
                .hasMessageNotContaining("super-secret");
    }

    private ErrorResponseException errorResponseException(String code) {
        ErrorResponse errorResponse = new ErrorResponse(code, code, "docs", "missing.txt",
                "/docs/missing.txt", "request-id", "host-id");
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://localhost:9000/docs/missing.txt").build())
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .build();
        return new ErrorResponseException(errorResponse, response, "http");
    }
}
