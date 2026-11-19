package com.moriba.skultem.infrastructure.push;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moriba.skultem.application.dto.NotificationDTO;
import com.moriba.skultem.domain.model.MobileDevice;
import com.moriba.skultem.domain.repository.MobileDeviceRepository;

// Sends notifications to a user's phones through Firebase Cloud Messaging (HTTP v1), alongside the browsers
// WebPushService reaches. Off until a Firebase service account is configured (push.fcm.credentials: the JSON
// itself, or a path to it). Like WebPushService it never throws into the caller and runs off the request
// thread; a phone Firebase says is gone (app uninstalled, token replaced) is forgotten.
@Service
public class FcmPushService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushService.class);
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final MobileDeviceRepository repo;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "fcm-push");
        t.setDaemon(true);
        return t;
    });

    private final String projectId;
    private final String clientEmail;
    private final PrivateKey privateKey;
    private final String tokenUri;

    private String accessToken;
    private Instant accessTokenExpires = Instant.EPOCH;

    public FcmPushService(MobileDeviceRepository repo, @Value("${push.fcm.credentials:}") String credentials) {
        this.repo = repo;
        String projectId = null, clientEmail = null, tokenUri = null;
        PrivateKey key = null;
        try {
            String raw = credentials == null ? "" : credentials.trim();
            if (!raw.isEmpty() && !raw.startsWith("{")) {
                raw = Files.readString(Path.of(raw));
            }
            if (!raw.isEmpty()) {
                JsonNode account = json.readTree(raw);
                projectId = account.path("project_id").asText(null);
                clientEmail = account.path("client_email").asText(null);
                tokenUri = account.path("token_uri").asText("https://oauth2.googleapis.com/token");
                key = parseKey(account.path("private_key").asText(""));
            }
        } catch (Exception e) {
            log.error("Firebase push is off: the service account couldn't be read ({})", e.getMessage());
            projectId = null;
        }
        this.projectId = projectId;
        this.clientEmail = clientEmail;
        this.privateKey = key;
        this.tokenUri = tokenUri;
        if (isEnabled()) {
            log.info("Firebase push is on for project {}", projectId);
        }
    }

    public boolean isEnabled() {
        return projectId != null && clientEmail != null && privateKey != null;
    }

    public void notifyAsync(String userId, NotificationDTO n) {
        if (!isEnabled() || n == null) {
            return;
        }
        executor.submit(() -> notify(userId, n));
    }

    void notify(String userId, NotificationDTO n) {
        for (MobileDevice device : repo.findAllByUserId(userId)) {
            try {
                send(device, n);
            } catch (Exception e) {
                log.warn("Firebase push to a device of user {} failed: {}", userId, e.getMessage());
            }
        }
    }

    private void send(MobileDevice device, NotificationDTO n) throws Exception {
        String body = json.writeValueAsString(Map.of("message", message(device.getToken(), n)));
        var request = HttpRequest.newBuilder(
                URI.create("https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + accessToken())
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var res = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) {
            device.markUsed();
            repo.save(device);
        } else if (res.statusCode() == 404 || res.body().contains("UNREGISTERED")
                || res.body().contains("registration-token-not-registered")) {
            // The app was uninstalled or the token replaced: stop sending to it.
            repo.deleteByToken(device.getToken());
        } else {
            log.warn("Firebase push rejected ({}): {}", res.statusCode(), res.body());
        }
    }

    // What the phone receives: shown by the OS when the app is closed, and handled by the app when it's open.
    // `open` tells the app which screen a tap should go to.
    static Map<String, Object> message(String token, NotificationDTO n) {
        var data = new LinkedHashMap<String, String>();
        data.put("open", "alerts");
        if (n.id() != null) {
            data.put("notificationId", n.id());
        }
        var message = new LinkedHashMap<String, Object>();
        message.put("token", token);
        message.put("notification", Map.of(
                "title", n.title() == null ? "Skultem" : n.title(),
                "body", n.message() == null ? "" : n.message()));
        message.put("data", data);
        message.put("android", Map.of(
                "priority", "HIGH",
                "notification", Map.of("channel_id", "school_alerts", "icon", "ic_stat_skultem")));
        message.put("apns", Map.of("payload", Map.of("aps", Map.of("sound", "default"))));
        return message;
    }

    // A Google OAuth access token for the service account, from a signed JWT; reused until shortly before it
    // expires.
    private synchronized String accessToken() throws Exception {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpires.minusSeconds(300))) {
            return accessToken;
        }
        long now = Instant.now().getEpochSecond();
        String header = b64(json.writeValueAsBytes(Map.of("alg", "RS256", "typ", "JWT")));
        String claims = b64(json.writeValueAsBytes(Map.of(
                "iss", clientEmail, "scope", SCOPE, "aud", tokenUri, "iat", now, "exp", now + 3600)));
        var signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update((header + "." + claims).getBytes(StandardCharsets.US_ASCII));
        String jwt = header + "." + claims + "." + b64(signer.sign());

        String form = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer",
                StandardCharsets.UTF_8) + "&assertion=" + jwt;
        var res = http.send(HttpRequest.newBuilder(URI.create(tokenUri))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IllegalStateException("Google token request failed (" + res.statusCode() + ")");
        }
        JsonNode token = json.readTree(res.body());
        accessToken = token.path("access_token").asText();
        accessTokenExpires = Instant.now().plusSeconds(token.path("expires_in").asLong(3600));
        return accessToken;
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static PrivateKey parseKey(String pem) throws Exception {
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        if (base64.isEmpty()) {
            return null;
        }
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }
}
