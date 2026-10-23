package com.moriba.skultem.infrastructure.security;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Request rate limits, so one client (a script, a stuck retry loop, someone guessing passwords) can't slow the server
// down for every school. Token buckets: each client gets `limit` requests per `windowSeconds`, refilled smoothly, so
// short bursts are fine and only sustained floods are turned away with 429 + Retry-After.
//
// Who counts as "one client":
//   * logged-in API calls - per user, so a whole school behind one shared IP (common on school networks) never
//     limits itself;
//   * everything anonymous (login, refresh, public website forms, bootstrap) - per client IP.
// The client IP comes from `clientIpHeader` (e.g. CF-Connecting-IP behind Cloudflare) when set, else the TCP peer.
// Only set it when the server is reachable through that proxy alone - otherwise anyone can forge the header.
//
// In memory, per server instance: with several instances each enforces its own limits (move the buckets to Redis
// if that ever matters). Not a Spring @Component on purpose - SecurityConfig places it after JwtAuthFilter so the
// user is known.
public class RateLimitFilter extends OncePerRequestFilter {

    // One limit. `path` is a prefix; `method` null = any method.
    public record Rule(String name, HttpMethod method, String path, int limit, int windowSeconds, boolean perUser) {
        boolean matches(HttpServletRequest request) {
            return (method == null || method.matches(request.getMethod()))
                    && request.getRequestURI().startsWith(path);
        }
    }

    private static final class Bucket {
        private final double capacity;
        private final double refillPerNano;
        private double tokens;
        private long last;
        private volatile long lastSeen;

        Bucket(int limit, int windowSeconds) {
            this.capacity = limit;
            this.refillPerNano = limit / (windowSeconds * 1_000_000_000.0);
            this.tokens = limit;
            this.last = System.nanoTime();
            this.lastSeen = last;
        }

        // Seconds until a request would be allowed again; 0 = allowed (and counted).
        synchronized long take() {
            long now = System.nanoTime();
            tokens = Math.min(capacity, tokens + (now - last) * refillPerNano);
            last = now;
            lastSeen = now;
            if (tokens >= 1) {
                tokens -= 1;
                return 0;
            }
            return Math.max(1, (long) Math.ceil((1 - tokens) / refillPerNano / 1_000_000_000.0));
        }
    }

    private static final long SWEEP_EVERY = 10_000;
    private static final long IDLE_NANOS = 30L * 60 * 1_000_000_000; // forget a client after 30 idle minutes

    private final List<Rule> rules;
    private final String clientIpHeader;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requests = new AtomicLong();

    public RateLimitFilter(List<Rule> rules, String clientIpHeader) {
        this.rules = rules;
        this.clientIpHeader = clientIpHeader == null ? "" : clientIpHeader.trim();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod()) || !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = rules.stream().filter(r -> r.matches(request)).findFirst().orElse(null);
        if (rule != null) {
            String who = rule.perUser() ? userOrIp(request) : "ip:" + clientIp(request);
            long retryAfter = buckets
                    .computeIfAbsent(rule.name() + "|" + who, k -> new Bucket(rule.limit(), rule.windowSeconds()))
                    .take();
            if (retryAfter > 0) {
                reject(response, retryAfter);
                return;
            }
        }
        if (requests.incrementAndGet() % SWEEP_EVERY == 0) {
            long cutoff = System.nanoTime() - IDLE_NANOS;
            buckets.values().removeIf(b -> b.lastSeen < cutoff);
        }
        chain.doFilter(request, response);
    }

    private String userOrIp(HttpServletRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user && user.userId() != null) {
            return "user:" + user.userId();
        }
        return "ip:" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        if (!clientIpHeader.isEmpty()) {
            String value = request.getHeader(clientIpHeader);
            if (value != null && !value.isBlank()) {
                // X-Forwarded-For style lists: the first entry is the original client.
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // Same shape as ApiResponse, so the app's error handling shows the message.
        response.getWriter().write("{\"status\":\"error\",\"code\":429,\"message\":\"Too many requests - please wait "
                + retryAfterSeconds + " second" + (retryAfterSeconds == 1 ? "" : "s")
                + " and try again.\",\"data\":null,\"meta\":null,\"timestamp\":\"" + Instant.now() + "\"}");
    }
}
