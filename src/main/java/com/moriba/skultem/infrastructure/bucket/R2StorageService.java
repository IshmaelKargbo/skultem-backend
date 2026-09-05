package com.moriba.skultem.infrastructure.bucket;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;

import javax.imageio.ImageIO;

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

    // Profile-photo uploads (student, teacher/parent/user) go through here instead of
    // uploadFile() directly - a phone camera photo routinely arrives several MB and several
    // thousand pixels on a side, and serving that unchanged to every UAvatar in a table (10+
    // students/teachers on one page) makes the browser download and decode all of it just to
    // paint a 40px circle, which is what was freezing list pages with many photos. This downscales
    // to at most maxDimension on the longest side and re-encodes as JPEG before it ever reaches R2,
    // so every consumer gets a small file regardless of what was uploaded.
    //
    // Deliberately NOT used for school branding assets (logo, principal signature) - those get
    // reproduced on printed report cards/receipts via html2canvas and need to stay at their
    // original resolution.
    public String uploadPhoto(MultipartFile file, String basePath, int maxDimension) throws IOException {
        BufferedImage original;
        try (InputStream in = file.getInputStream()) {
            original = ImageIO.read(in);
        }

        // ImageIO couldn't decode this as a raster image (e.g. an unsupported format like HEIC,
        // which some phones still upload by default) - fall back to the original bytes rather than
        // fail the upload outright. Same behavior as before this method existed.
        if (original == null) {
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            return uploadFile(file, basePath + extensionFor(contentType, file.getOriginalFilename()));
        }

        int width = original.getWidth();
        int height = original.getHeight();
        double scale = Math.min(1.0, Math.min((double) maxDimension / width, (double) maxDimension / height));
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        // TYPE_INT_RGB (no alpha channel) is fine here - these are photos of people, not graphics
        // that rely on transparency.
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", out);
        byte[] bytes = out.toByteArray();

        String path = basePath + ".jpg";
        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(path)
                            .contentType("image/jpeg")
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (S3Exception ex) {
            throw new StorageException(
                    "Upload failed with status " + ex.statusCode(),
                    ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage(),
                    ex);
        }

        return publicUrl + "/" + path;
    }

    private String extensionFor(String contentType, String originalFilename) {
        String name = originalFilename != null ? originalFilename : "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            return name.substring(dot).toLowerCase(java.util.Locale.ROOT);
        }
        return contentType.contains("png") ? ".png" : ".jpg";
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
