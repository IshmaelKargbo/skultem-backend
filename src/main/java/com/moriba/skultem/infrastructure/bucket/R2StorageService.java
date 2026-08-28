package com.moriba.skultem.infrastructure.bucket;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.moriba.skultem.application.error.StorageException;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

// Cloudflare R2 is S3-compatible, so the AWS SDK's S3 client works against it
// as-is once pointed at the account's R2 endpoint with "auto" as the region.
// Backs both school branding uploads (logo, principal signature) and student
// photo uploads - Supabase storage has been fully retired.
@Service
public class R2StorageService {

    private final S3Client client;
    private final String bucket;
    private final String publicUrl;

    public R2StorageService(
            @Value("${r2.account-id}") String accountId,
            @Value("${r2.access-key-id}") String accessKeyId,
            @Value("${r2.secret-access-key}") String secretAccessKey,
            @Value("${r2.bucket}") String bucket,
            @Value("${r2.public-url}") String publicUrl) {

        this.bucket = bucket;
        this.publicUrl = publicUrl.replaceAll("/+$", "");

        this.client = S3Client.builder()
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }

    public String uploadFile(MultipartFile file, String path) throws IOException {
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(path)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (S3Exception ex) {
            throw new StorageException(
                    "Upload failed with status " + ex.statusCode(),
                    ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage(),
                    ex);
        }

        return publicUrl + "/" + path;
    }

    // R2's public bucket URL sends no CORS headers, which breaks browser-side
    // canvas capture (html2canvas) of cross-origin images like a school's logo
    // or principal signature - the fetch this method does is server-to-server
    // and isn't subject to that restriction at all, so the frontend embeds the
    // result directly as a same-origin data: URI instead of loading the R2 URL.
    public String downloadAsDataUri(String url) {
        if (url == null || url.isBlank() || !url.startsWith(publicUrl + "/")) {
            return null;
        }
        String key = url.substring((publicUrl + "/").length());

        try (ResponseInputStream<GetObjectResponse> object = client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            byte[] bytes = object.readAllBytes();
            String contentType = object.response().contentType() != null
                    ? object.response().contentType()
                    : "application/octet-stream";
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (S3Exception ex) {
            throw new StorageException(
                    "Download failed with status " + ex.statusCode(),
                    ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage(),
                    ex);
        } catch (IOException ex) {
            throw new StorageException("Unexpected download failure", ex.getMessage(), ex);
        }
    }
}
