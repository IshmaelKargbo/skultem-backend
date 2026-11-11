package com.moriba.skultem.infrastructure.compose;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.moriba.skultem.application.error.DocumentServiceException;

import io.jsonwebtoken.Jwts;

/**
 * A one-time link that signs a Skultem person in to the M-Compose designer, so they need no separate password there.
 * The token is a JWT signed with HS256 using the secret M-Compose gave for Skultem's account
 * (python -m app.accounts set-sso-secret skultem, or a connection on its Connections page). It lasts 5 minutes and
 * works once (it has a jti). The secret stays on this server; only the finished link goes to the browser.
 *
 * Off (isEnabled() false) until m-compose.designer-url and m-compose.sso-secret are set.
 */
@Component
public class MComposeSignIn {
    static final String AUDIENCE = "m-compose";
    static final long LIFETIME_SECONDS = 300;
    // The roles M-Compose accepts in a sign-in token.
    private static final Set<String> ROLES = Set.of("admin", "designer", "renderer", "reader");

    private final String designerUrl;
    private final String secret;
    private final Clock clock;

    @Autowired
    public MComposeSignIn(
            @Value("${m-compose.designer-url:}") String designerUrl,
            @Value("${m-compose.sso-secret:}") String secret) {
        this(designerUrl, secret, Clock.systemUTC());
    }

    MComposeSignIn(String designerUrl, String secret, Clock clock) {
        this.designerUrl = designerUrl == null ? "" : designerUrl.trim().replaceAll("/+$", "");
        this.secret = secret == null ? "" : secret.trim();
        this.clock = clock;
    }

    public boolean isEnabled() {
        return !designerUrl.isEmpty() && !secret.isEmpty();
    }

    /** The address to send this person's browser to. */
    public String linkFor(String userId, String name, String role) {
        if (!isEnabled()) {
            throw new DocumentServiceException("Signing in to the document designer is not set up on this server yet");
        }
        return designerUrl + "/sso?token=" + URLEncoder.encode(token(userId, name, role), StandardCharsets.UTF_8);
    }

    String token(String userId, String name, String role) {
        if (userId == null || userId.isBlank() || !ROLES.contains(role)) {
            throw new IllegalArgumentException("A sign-in needs a person and one of " + ROLES);
        }
        Instant now = clock.instant();
        var builder = Jwts.builder()
                .issuer("skultem")
                .audience().single(AUDIENCE)
                .subject(userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(LIFETIME_SECONDS)))
                .id(UUID.randomUUID().toString());
        if (name != null && !name.isBlank()) {
            builder.claim("name", name);
        }
        // HS256 exactly - M-Compose accepts no other algorithm. jjwt would otherwise pick HS384/512 from a long
        // secret, and refuses a short one, which the engine's own secrets never are.
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }
}
