package com.moriba.skultem.infrastructure.bucket;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    // Shared across every uploadPhotoFromUrl() call rather than built per-request - the JDK client
    // pools its own connections internally.
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

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

        return downscaleAndStore(original, basePath, maxDimension);
    }

    // Re-fetches a photo that's already stored somewhere (a legacy full-resolution upload, or one
    // hosted on a since-retired provider - Supabase, in this app's case) and re-hosts it through the
    // same downscale pipeline as a fresh upload. Existing students enrolled before that pipeline
    // existed otherwise keep serving whatever was originally uploaded - often several times the
    // dimensions a UAvatar ever needs - to every list page forever. Throws if the source can't be
    // fetched at all (a dead link) so the caller can decide how to handle that student's photo field
    // instead of silently storing a broken one.
    public String uploadPhotoFromUrl(String sourceUrl, String basePath, int maxDimension) throws IOException {
        byte[] bytes;
        String contentType;
        try {
            HttpResponse<byte[]> response = HTTP_CLIENT.send(
                    HttpRequest.newBuilder(URI.create(sourceUrl)).timeout(Duration.ofSeconds(20)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                throw new IOException("Fetch failed with status " + response.statusCode() + " for " + sourceUrl);
            }

            bytes = response.body();
            contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + sourceUrl, e);
        }

        BufferedImage original;
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            original = ImageIO.read(in);
        }

        if (original == null) {
            // Can't decode as a raster image - re-host the bytes unchanged rather than drop the
            // photo entirely.
            String path = basePath + extensionFor(contentType, sourceUrl);
            try {
                client.putObject(
                        PutObjectRequest.builder().bucket(bucket).key(path).contentType(contentType).build(),
                        RequestBody.fromBytes(bytes));
            } catch (S3Exception ex) {
                throw new StorageException(
                        "Upload failed with status " + ex.statusCode(),
                        ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage(),
                        ex);
            }
            return publicUrl + "/" + path;
        }

        return downscaleAndStore(original, basePath, maxDimension);
    }

    private String downscaleAndStore(BufferedImage original, String basePath, int maxDimension) throws IOException {
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

    // Logo / signature uploads: shrunk to MAX_EMBED_DIMENSION first (see shrinkForEmbedding) so what
    // lands in R2 is small - the logo is drawn ~160px wide on reports and cards, and a 400KB original
    // made every page that shows it, and every PDF, slow. A file that can't be shrunk (SVG, icon,
    // already small) is stored as-is.
    public String uploadBranding(MultipartFile file, String path) throws IOException {
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        byte[] original = file.getBytes();
        byte[] shrunk = shrinkForEmbedding(original, contentType);

        String url;
        String storedType;
        byte[] stored;
        if (shrunk == null) {
            url = uploadFile(file, path);
            storedType = contentType;
            stored = original;
        } else {
            String pngPath = path.replaceAll("\\.[A-Za-z0-9]+$", "") + ".png";
            try {
                client.putObject(
                        PutObjectRequest.builder().bucket(bucket).key(pngPath).contentType("image/png").build(),
                        RequestBody.fromBytes(shrunk));
            } catch (S3Exception ex) {
                throw new StorageException(
                        "Upload failed with status " + ex.statusCode(),
                        ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage(),
                        ex);
            }
            url = publicUrl + "/" + pngPath;
            storedType = "image/png";
            stored = shrunk;
        }

        // The bytes are already in hand - prime the embed cache so the first PDF / print of this
        // logo doesn't have to download it straight back from R2 (slow, and can fail on a poor link).
        if (dataUriCache.size() >= MAX_CACHED_DATA_URIS) {
            dataUriCache.clear();
        }
        dataUriCache.put(url, "data:" + storedType + ";base64," + Base64.getEncoder().encodeToString(stored));
        return url;
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
    //
    // Two things keep this fast: results are cached in memory by URL (an upload always gets a new
    // timestamped URL, so a cached entry can never go stale), and large images are shrunk to
    // MAX_EMBED_DIMENSION first - a 400KB logo drawn 160px wide on a report doesn't need to cross
    // the network (or sit in the browser's cache) at full size.
    private static final int MAX_EMBED_DIMENSION = 640;
    private static final int MAX_CACHED_DATA_URIS = 64;
    private final java.util.Map<String, String> dataUriCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, java.util.concurrent.CompletableFuture<String>> dataUriInflight = new java.util.concurrent.ConcurrentHashMap<>();

    public String downloadAsDataUri(String url) {
        if (url == null || url.isBlank() || !url.startsWith(publicUrl + "/")) {
            return null;
        }
        String cached = dataUriCache.get(url);
        if (cached != null) {
            return cached;
        }

        // Single-flight: the background warm-up and the request that actually needs the image share
        // one download instead of each pulling the same bytes from R2.
        var mine = new java.util.concurrent.CompletableFuture<String>();
        var running = dataUriInflight.putIfAbsent(url, mine);
        if (running != null) {
            return running.join();
        }
        try {
            String dataUri = fetchAsDataUri(url);
            if (dataUriCache.size() >= MAX_CACHED_DATA_URIS) {
                dataUriCache.clear();
            }
            dataUriCache.put(url, dataUri);
            mine.complete(dataUri);
            return dataUri;
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            dataUriInflight.remove(url);
        }
    }

    private String fetchAsDataUri(String url) {
        String key = url.substring((publicUrl + "/").length());

        try (ResponseInputStream<GetObjectResponse> object = client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            byte[] bytes = object.readAllBytes();
            String contentType = object.response().contentType() != null
                    ? object.response().contentType()
                    : "application/octet-stream";

            var shrunk = shrinkForEmbedding(bytes, contentType);
            if (shrunk != null) {
                bytes = shrunk;
                contentType = "image/png";
            }
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

    // Scales a raster image down to MAX_EMBED_DIMENSION on its longest side, re-encoded as PNG so a
    // transparent logo keeps its transparency. Null when there's nothing to do - already small,
    // or a format ImageIO can't read (SVG, icons, WebP) - and the original bytes are used as-is.
    private static byte[] shrinkForEmbedding(byte[] bytes, String contentType) {
        if (contentType == null || !(contentType.contains("png") || contentType.contains("jpeg")
                || contentType.contains("jpg"))) {
            return null;
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null || Math.max(source.getWidth(), source.getHeight()) <= MAX_EMBED_DIMENSION) {
                return null;
            }

            double scale = (double) MAX_EMBED_DIMENSION / Math.max(source.getWidth(), source.getHeight());
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

            BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = target.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(source, 0, 0, width, height, null);
            } finally {
                g.dispose();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(target, "png", out);
            byte[] resized = out.toByteArray();
            return resized.length < bytes.length ? resized : null;
        } catch (Exception e) {
            return null;
        }
    }
}
