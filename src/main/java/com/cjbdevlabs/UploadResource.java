package com.cjbdevlabs;

import io.quarkiverse.amazon.s3.runtime.S3Crt;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
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
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletionException;

@Path("/upload")
public class UploadResource {

    private static final String APPLICATION_TAR = "application/x-tar";

    @Inject
    @S3Crt
    S3AsyncClient s3Client;

    @Inject
    UploadExecutor uploadExecutor;

    @ConfigProperty(name = "com.cjbdevlabs.s3.bucket")
    String s3bucket;

    @PostConstruct
    public void logAwsRetryConfig() {
        Log.infof("aws.maxAttempts property = %s", System.getProperty("aws.maxAttempts"));
    }

    @Path("s3")
    @POST
    @Consumes(MediaType.WILDCARD)
    public Response uploadFile(InputStream body, @Context HttpHeaders httpHeaders) {
        var fileName = httpHeaders.getHeaderString("filename") != null
                ? httpHeaders.getHeaderString("filename") + ".tar"
                : UUID.randomUUID() + ".tar";
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3bucket)
                .key(fileName)
                .contentType(APPLICATION_TAR)
                .build();
        try {
            Log.infof("Received filename: %s", fileName);
            AsyncRequestBody asyncBody = AsyncRequestBody.fromInputStream(body, null, uploadExecutor.get());
            s3Client.putObject(putObjectRequest, asyncBody).join();
            return Response.accepted().build();
        } catch (CompletionException e) {
            Log.errorf("FAIL | fileName=%s | cause=%s", fileName, e.getCause());
            return Response.serverError().build();
        } catch (Exception e) {
            Log.errorf("FAIL | fileName=%s | exception=%s | message=%s", fileName, e.getClass().getSimpleName(), e.getMessage());
            return Response.serverError().build();
        }
    }
}
