package com.moriba.skultem.infrastructure.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Lets clients (the mobile app's offline sync) skip downloading data they already have.
//
// Every successful JSON GET under /api/v1 gets an ETag: a fingerprint of the response *without* its
// top-level "timestamp" (ApiResponse stamps the time on every reply, so a plain body hash - Spring's
// ShallowEtagHeaderFilter - would never match twice). A client that sends that fingerprint back in
// If-None-Match while the data is unchanged gets 304 with no body.
//
// The response is still produced in full (auth checks, queries) - this saves the transfer, which is
// what matters on mobile data. Responses are per user, so they're marked private: no shared cache
// in between may store one person's data and hand it to another.
public class DataEtagFilter extends OncePerRequestFilter {

    private static final Pattern TIMESTAMP = Pattern.compile("\"timestamp\"\\s*:\\s*(\"[^\"]*\"|-?[0-9.eE+-]+)");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"GET".equals(request.getMethod()) || !request.getRequestURI().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var wrapped = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrapped);
        } catch (IOException | ServletException | RuntimeException e) {
            wrapped.copyBodyToResponse();
            throw e;
        }

        String type = wrapped.getContentType();
        boolean json = type != null && type.toLowerCase().startsWith("application/json");
        if (wrapped.getStatus() != HttpServletResponse.SC_OK || !json || response.isCommitted()) {
            wrapped.copyBodyToResponse();
            return;
        }

        String etag = etagOf(wrapped.getContentAsByteArray());
        response.setHeader(HttpHeaders.ETAG, etag);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-cache");

        if (matches(request.getHeader(HttpHeaders.IF_NONE_MATCH), etag)) {
            wrapped.resetBuffer();
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            response.setContentLength(0);
            return;
        }
        wrapped.copyBodyToResponse();
    }

    static String etagOf(byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8);
        String stable = TIMESTAMP.matcher(text).replaceFirst("\"timestamp\":null");
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(stable.getBytes(StandardCharsets.UTF_8));
            return "W/\"" + HexFormat.of().formatHex(hash, 0, 16) + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean matches(String ifNoneMatch, String etag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
            return false;
        }
        for (String candidate : ifNoneMatch.split(",")) {
            String c = candidate.trim();
            if (c.equals(etag) || c.equals("*")) {
                return true;
            }
        }
        return false;
    }

    @Configuration
    public static class Registration {
        @Bean
        public FilterRegistrationBean<DataEtagFilter> dataEtagFilter() {
            var bean = new FilterRegistrationBean<>(new DataEtagFilter());
            bean.addUrlPatterns("/api/v1/*");
            // Innermost, so it fingerprints exactly what the controller returned, after security.
            bean.setOrder(Ordered.LOWEST_PRECEDENCE);
            return bean;
        }
    }
}
