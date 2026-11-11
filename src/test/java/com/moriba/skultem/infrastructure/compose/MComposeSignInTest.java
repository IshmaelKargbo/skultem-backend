package com.moriba.skultem.infrastructure.compose;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moriba.skultem.application.error.DocumentServiceException;

// Checks the token the way M-Compose does (app/core/sso.py): HS256 over "head.body" with the shared secret.
class MComposeSignInTest {

    private static final String SECRET = "a-shared-secret-of-at-least-32-characters-long";
    private static final Instant NOW = Instant.parse("2026-11-10T09:00:00Z");
    private final ObjectMapper json = new ObjectMapper();

    private MComposeSignIn signIn(String url, String secret) {
        return new MComposeSignIn(url, secret, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void linkCarriesAnHs256TokenTheEngineAccepts() throws Exception {
        String link = signIn("http://skultem.localhost:3000/", SECRET).linkFor("user-1", "Ada Conteh", "designer");

        assertThat(link).startsWith("http://skultem.localhost:3000/sso?token=");
        String token = URLDecoder.decode(link.substring(link.indexOf("token=") + 6), StandardCharsets.UTF_8);
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);

        JsonNode head = decode(parts[0]);
        assertThat(head.get("alg").asText()).isEqualTo("HS256");

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(MessageDigest.isEqual(Base64.getUrlDecoder().decode(parts[2]), expected)).isTrue();

        JsonNode body = decode(parts[1]);
        assertThat(body.get("aud").asText()).isEqualTo("m-compose");
        assertThat(body.get("iss").asText()).isEqualTo("skultem");
        assertThat(body.get("sub").asText()).isEqualTo("user-1");
        assertThat(body.get("name").asText()).isEqualTo("Ada Conteh");
        assertThat(body.get("role").asText()).isEqualTo("designer");
        assertThat(body.get("iat").asLong()).isEqualTo(NOW.getEpochSecond());
        // The engine refuses a token that lasts more than 10 minutes.
        assertThat(body.get("exp").asLong() - body.get("iat").asLong()).isEqualTo(300);
        assertThat(body.get("jti").asText()).isNotBlank();
    }

    @Test
    void everyLinkWorksOnce() {
        var s = signIn("http://x", SECRET);
        assertThat(s.token("u", null, "reader")).isNotEqualTo(s.token("u", null, "reader"));
    }

    @Test
    void offUntilBothSettingsAreGiven() {
        assertThat(signIn("", SECRET).isEnabled()).isFalse();
        assertThat(signIn("http://x", " ").isEnabled()).isFalse();
        assertThat(signIn("http://x", SECRET).isEnabled()).isTrue();
        assertThatThrownBy(() -> signIn("", "").linkFor("u", "n", "designer"))
                .isInstanceOf(DocumentServiceException.class);
    }

    @Test
    void refusesARoleTheEngineDoesNotKnow() {
        assertThatThrownBy(() -> signIn("http://x", SECRET).token("u", "n", "SUPER_ADMIN"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signIn("http://x", SECRET).token(" ", "n", "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private JsonNode decode(String part) throws Exception {
        return json.readTree(Base64.getUrlDecoder().decode(part));
    }
}
