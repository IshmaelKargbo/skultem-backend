package com.moriba.skultem.infrastructure.push;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moriba.skultem.application.dto.NotificationDTO;
import com.moriba.skultem.domain.model.PushSubscription;
import com.moriba.skultem.domain.repository.PushSubscriptionRepository;

// Sends native notifications to a user's devices (Web Push). Off when no VAPID keys are configured. Sending never
// throws into the caller: a device that can't be reached is skipped, and one the push service says is gone (404/410)
// is forgotten.
@Service
public class WebPushService {
    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushSubscriptionRepository repo;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String publicKey;
    private final String privateKey;
    private final String subject;

    public WebPushService(PushSubscriptionRepository repo,
            @Value("${push.vapid.public-key:}") String publicKey,
            @Value("${push.vapid.private-key:}") String privateKey,
            @Value("${push.vapid.subject:mailto:support@skultem.space}") String subject) {
        this.repo = repo;
        this.publicKey = publicKey == null ? "" : publicKey.trim();
        this.privateKey = privateKey == null ? "" : privateKey.trim();
        this.subject = subject;
    }

    public boolean isEnabled() {
        return !publicKey.isEmpty() && !privateKey.isEmpty();
    }

    public String publicKey() {
        return publicKey;
    }

    // Pushes run off the request thread - talking to push services must not slow down whatever created the notification.
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "web-push");
        t.setDaemon(true);
        return t;
    });

    public void notifyAsync(String userId, NotificationDTO n) {
        if (!isEnabled() || n == null) return;
        executor.submit(() -> notify(userId, n));
    }

    /** A notification from the app, as a native one on every device the user turned them on for. */
    public void notify(String userId, NotificationDTO n) {
        if (n == null) return;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", n.title());
        payload.put("body", n.message());
        payload.put("tag", n.id());
        payload.put("url", "/communicate/notifications");
        payload.put("urgent", n.priority() != null && ("URGENT".equals(n.priority().name()) || "HIGH".equals(n.priority().name())));
        sendToUser(userId, payload);
    }

    /** Sends to all of a user's devices. Returns how many accepted it. */
    public int sendToUser(String userId, Map<String, Object> payload) {
        if (!isEnabled() || userId == null) return 0;
        int sent = 0;
        for (var sub : repo.findAllByUserId(userId)) {
            if (send(sub, payload)) sent++;
        }
        return sent;
    }

    private boolean send(PushSubscription sub, Map<String, Object> payload) {
        try {
            byte[] body = WebPushCrypto.encrypt(json.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8),
                    sub.getP256dh(), sub.getAuth());
            boolean urgent = Boolean.TRUE.equals(payload.get("urgent"));
            HttpRequest request = HttpRequest.newBuilder(URI.create(sub.getEndpoint()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", WebPushCrypto.vapidAuthorization(sub.getEndpoint(), subject, publicKey, privateKey))
                    .header("Content-Encoding", "aes128gcm")
                    .header("Content-Type", "application/octet-stream")
                    .header("TTL", "86400")
                    .header("Urgency", urgent ? "high" : "normal")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
            int status = res.statusCode();
            if (status == 404 || status == 410) {
                repo.deleteByEndpoint(sub.getEndpoint()); // the device unsubscribed or the app was removed
                return false;
            }
            if (status >= 400) {
                log.warn("Push to {} refused: {} {}", host(sub.getEndpoint()), status, res.body());
                return false;
            }
            sub.markUsed();
            repo.save(sub);
            return true;
        } catch (Exception e) {
            log.warn("Push to {} failed: {}", host(sub.getEndpoint()), e.getMessage());
            return false;
        }
    }

    private static String host(String endpoint) {
        try {
            return URI.create(endpoint).getHost();
        } catch (Exception e) {
            return "?";
        }
    }
}
