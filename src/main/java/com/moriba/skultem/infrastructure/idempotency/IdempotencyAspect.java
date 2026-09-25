package com.moriba.skultem.infrastructure.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.infrastructure.idempotency.IdempotencyStore.State;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.AuthUser;

import lombok.RequiredArgsConstructor;

/**
 * Runs after method security (lowest precedence), so an unauthorised caller can't probe keys.
 * The request is fingerprinted from the handler's arguments; the same key with a different body is
 * rejected rather than silently answered with the first request's result.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class IdempotencyAspect {

    public static final String HEADER = "Idempotency-Key";
    public static final String REPLAY_HEADER = "Idempotent-Replay";
    private static final int MAX_KEY_LENGTH = 200;

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final IdempotencyStore store;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        var attributes = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a ? a : null;
        String key = attributes == null ? null : attributes.getRequest().getHeader(HEADER);
        if (key == null || key.isBlank()) {
            return joinPoint.proceed();
        }
        key = key.trim();
        if (key.length() > MAX_KEY_LENGTH) {
            throw new RuleException("The " + HEADER + " header is too long (max " + MAX_KEY_LENGTH + " characters).");
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            return joinPoint.proceed();
        }

        String operation = idempotent.operation();
        String schoolId = user.activeSchoolId();
        var outcome = store.begin(schoolId, user.userId(), operation, key, fingerprint(joinPoint.getArgs()));

        switch (outcome.state()) {
            case REPLAY -> {
                if (attributes.getResponse() != null) {
                    attributes.getResponse().setHeader(REPLAY_HEADER, "true");
                }
                return rebuild(outcome.responseBody());
            }
            case IN_PROGRESS -> throw new IllegalStateException(
                    "This request is already being processed. Wait a moment before trying again.");
            case MISMATCH -> throw new RuleException(
                    "This " + HEADER + " was already used for a different request.");
            case NEW -> {
                // fall through to running it
            }
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            release(schoolId, operation, key);
            throw ex;
        }

        try {
            store.complete(schoolId, operation, key, MAPPER.writeValueAsString(result));
        } catch (Exception e) {
            // The record IS created - only the replay is lost. Free the key rather than leave it stuck.
            log.error("Could not store idempotent response for {} ({}): {}", operation, key, e.getMessage(), e);
            release(schoolId, operation, key);
        }
        return result;
    }

    private void release(String schoolId, String operation, String key) {
        try {
            store.release(schoolId, operation, key);
        } catch (Exception e) {
            log.error("Could not release idempotency key {} ({}): {}", key, operation, e.getMessage(), e);
        }
    }

    private static Object rebuild(String body) throws Exception {
        JsonNode node = MAPPER.readTree(body);
        Object data = node.hasNonNull("data") ? MAPPER.convertValue(node.get("data"), Object.class) : null;
        Object meta = node.hasNonNull("meta") ? MAPPER.convertValue(node.get("meta"), Object.class) : null;
        return new ApiResponse<Object>(node.path("status").asText("success"), node.path("code").asInt(200),
                node.hasNonNull("message") ? node.get("message").asText() : null, data, meta, Instant.now());
    }

    // Stable fingerprint of the handler's arguments; an uploaded file counts by name and size.
    private static String fingerprint(Object[] args) {
        List<Object> parts = new ArrayList<>();
        for (Object arg : args) {
            parts.add(arg instanceof MultipartFile f ? "file:" + f.getOriginalFilename() + ":" + f.getSize() : arg);
        }
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(MAPPER.writeValueAsString(parts).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Could not fingerprint the request", e);
        }
    }
}
