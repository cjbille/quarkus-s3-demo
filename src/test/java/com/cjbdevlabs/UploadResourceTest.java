package com.cjbdevlabs;

import io.quarkiverse.amazon.s3.runtime.S3Crt;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@Disabled
class UploadResourceTest {

    @InjectMock
    @S3Crt
    S3TransferManager s3TransferManager;

    @BeforeEach
    void setup() {
        Upload mockUpload = Mockito.mock(Upload.class);
        CompletedUpload completedUpload = CompletedUpload.builder().build();
        CompletableFuture<CompletedUpload> future = CompletableFuture.completedFuture(completedUpload);
        when(s3TransferManager.upload(any(UploadRequest.class))).thenReturn(mockUpload);
        when(mockUpload.completionFuture()).thenReturn(future);
    }

    @Test
    void uploadFile_ShouldReturnAccepted_WhenUploadIsSuccessful() {
        String testContent = "This is some test content for the tar file.";
        String filename = "my-test-archive";
        given()
                .header("filename", filename)
                .body(testContent)
                .contentType("application/octet-stream")
                .when()
                .post("/upload/s3")
                .then()
                .statusCode(202);
        ArgumentCaptor<UploadRequest> captor = ArgumentCaptor.forClass(UploadRequest.class);
        verify(s3TransferManager).upload(captor.capture());
        UploadRequest capturedRequest = captor.getValue();
        assertEquals("my-test-archive.tar", capturedRequest.putObjectRequest().key());
        assertEquals("application/x-tar", capturedRequest.putObjectRequest().contentType());
    }

    @Test
    void uploadFile_ShouldHandleS3Failure_AndReturn500() {
        when(s3TransferManager.upload(any(UploadRequest.class)))
                .thenThrow(new RuntimeException("S3 is down!"));
        given()
                .header("filename", "crash-test")
                .body("content")
                .when()
                .post("/upload/s3")
                .then()
                .statusCode(500);
    }
}
