package com.cjbdevlabs;

import io.quarkiverse.amazon.s3.runtime.S3Crt;
import io.quarkus.logging.Log;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Path("/upload")
public class UploadResource {

    private static final String APPLICATION_TAR = "application/x-tar";

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    @S3Crt
    S3TransferManager s3TransferManager;

    @ConfigProperty(name = "com.cjbdevlabs.s3.bucket")
    String s3bucket;

    @Path("s3")
    @POST
    @Consumes(MediaType.WILDCARD)
    public Response uploadFile(InputStream inputStream, @Context HttpHeaders httpHeaders) {
        var fileNameHeader = httpHeaders.getHeaderString("filename");
        var fileName = UploadUtils.buildFileName(fileNameHeader);
        try {
            Log.infof("Received filename: %s", fileName);
            AsyncRequestBody body = AsyncRequestBody.fromInputStream(inputStream, null, executor);
            UploadRequest uploadRequest = UploadRequest.builder()
                    .putObjectRequest(req -> req.bucket(s3bucket).key(fileName).contentType(APPLICATION_TAR))
                    .requestBody(body)
                    .build();
            Log.infof("Upload Request: %s", uploadRequest.toString());
            s3TransferManager.upload(uploadRequest).completionFuture().join();
            return Response.accepted().build();
        } catch (Exception e) {
            var err = e instanceof CompletionException ? e.getCause() : e;
            Log.errorf("FAIL | fileName=%s | exception=%s | message=%s", fileName, err.getClass().getSimpleName(), err.getMessage());
            return Response.serverError().build();
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
