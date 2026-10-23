package com.moriba.skultem.infrastructure.rest;

import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;

// Lets Cloudflare (or any shared cache) serve the few public, same-for-everyone GET responses - marketing content,
// the public school directory, the school lookup by domain the login page makes - instead of every visit reaching
// the server. Everything else keeps Spring Security's default no-store: anything behind a login must never be cached
// by a shared cache. Only successful responses are marked; errors stay uncached.
//
// s-maxage is how long Cloudflare keeps it; max-age (browsers) is shorter so a change shows up quickly for people
// already on the site; stale-while-revalidate lets the edge answer instantly while it refreshes in the background.
// Cloudflare only caches these with a Cache Rule that makes the paths eligible (it skips JSON by default).
@RestControllerAdvice
public class PublicCacheHeadersAdvice implements ResponseBodyAdvice<Object> {

    private record Cached(String path, boolean prefix, String cacheControl) {
        boolean matches(String uri) {
            return prefix ? uri.startsWith(path) : uri.equals(path);
        }
    }

    private static final String CONTENT = "public, max-age=60, s-maxage=300, stale-while-revalidate=600";
    // A school's name/branding on its login page - kept short so an edit shows within a minute.
    private static final String TENANT = "public, max-age=0, s-maxage=60, stale-while-revalidate=300";

    private static final List<Cached> CACHED = List.of(
            new Cached("/api/v1/public/testimonials", false, CONTENT),
            new Cached("/api/v1/public/blog", true, CONTENT),
            new Cached("/api/v1/public/settings", false, CONTENT),
            new Cached("/api/v1/school/public", false, CONTENT),
            new Cached("/api/v1/school/count", false, CONTENT),
            new Cached("/api/v1/auth/tenant", false, TENANT));

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (!"GET".equals(request.getMethod().name())) {
            return body;
        }
        if (!(body instanceof ApiResponse<?> api) || api.getCode() != 200) {
            return body;
        }
        String uri = request.getURI().getPath();
        CACHED.stream().filter(c -> c.matches(uri)).findFirst().ifPresent(c -> {
            HttpHeaders headers = response.getHeaders();
            headers.set(HttpHeaders.CACHE_CONTROL, c.cacheControl());
            // Spring Security's no-cache defaults are skipped once Cache-Control is set, but make sure nothing
            // contradicts it.
            headers.remove(HttpHeaders.PRAGMA);
            headers.remove(HttpHeaders.EXPIRES);
        });
        return body;
    }
}
