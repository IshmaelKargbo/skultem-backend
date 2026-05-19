package com.moriba.skultem.infrastructure.bucket;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

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

        webClient.put()
                .uri(uploadUrl)
                .header("Authorization", "Bearer " + supabaseKey)
                .header("apikey", supabaseKey)
                .header("x-upsert", "true")
                .contentType(contentType)
                .bodyValue(bytes)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        res -> res.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Supabase upload failed: " + body))
                )
                .bodyToMono(String.class)
                .block();

        return getPublicUrl(path);
    }

    public String getPublicUrl(String path) {
        return supabaseUrl + "/storage/v1/object/public/" + encodePathSegment(bucket) + "/" + encodeObjectPath(path);
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
                .reduce((left, right) -> left + "/" + right)
                .orElseThrow(() -> new IllegalArgumentException("Storage path is required"));
    }

    private String encodePathSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
