package com.moriba.skultem.infrastructure.bucket;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.moriba.skultem.application.error.SupabaseStorageException;

import reactor.util.retry.Retry;

@Service
public class SupabaseStorageService {

    private final WebClient webClient;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final String bucket;

    public SupabaseStorageService(
            @Value("${supabase.url}") String url,
            @Value("${supabase.key}") String key,
            @Value("${supabase.bucket:students}") String bucket) {

        this.supabaseUrl = url;
        this.supabaseKey = key;
        this.bucket = bucket;

        this.webClient = WebClient.builder()
                .baseUrl(url)
                .build();
    }

    public String uploadStudentFile(MultipartFile file, String path) throws IOException {

        byte[] bytes = file.getBytes();
        MediaType contentType = resolveContentType(file);

        String objectPath = encodeObjectPath(path);
        String uploadUrl = "/storage/v1/object/" + encodePathSegment(bucket) + "/" + objectPath;

        try {
            webClient.put()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .header("x-upsert", "true")
                    .contentType(contentType)
                    .bodyValue(bytes)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(applyRetry())
                    .block();

        } catch (WebClientResponseException ex) {
            throw new SupabaseStorageException(
                    "Upload failed with status " + ex.getStatusCode(),
                    cleanErrorBody(ex.getResponseBodyAsString()),
                    ex
            );
        } catch (Exception ex) {
            throw new SupabaseStorageException(
                    "Unexpected upload failure",
                    ex.getMessage(),
                    ex
            );
        }

        return getPublicUrl(path);
    }

    public String getPublicUrl(String path) {
        return supabaseUrl + "/storage/v1/object/public/"
                + encodePathSegment(bucket) + "/"
                + encodeObjectPath(path);
    }

    private Retry applyRetry() {
        return Retry.backoff(3, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(5))
                .filter(this::isRetryable)
                .onRetryExhaustedThrow((spec, signal) ->
                        new SupabaseStorageException(
                                "Retry exhausted after upload failure",
                                "Upload failed after multiple attempts",
                                signal.failure()
                        ));
    }

    private boolean isRetryable(Throwable ex) {
        return ex instanceof WebClientResponseException.TooManyRequests
                || ex instanceof WebClientResponseException.ServiceUnavailable
                || ex instanceof WebClientResponseException.GatewayTimeout
                || ex instanceof IOException;
    }

    private String cleanErrorBody(String body) {
        if (body == null || body.isBlank()) return "No error details from Supabase";
        return body.length() > 500 ? body.substring(0, 500) : body;
    }

    private MediaType resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }

    private String encodeObjectPath(String path) {
        return Arrays.stream(path.split("/"))
                .filter(segment -> !segment.isBlank())
                .map(this::encodePathSegment)
                .reduce((l, r) -> l + "/" + r)
                .orElseThrow(() -> new IllegalArgumentException("Storage path is required"));
    }

    private String encodePathSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }
}