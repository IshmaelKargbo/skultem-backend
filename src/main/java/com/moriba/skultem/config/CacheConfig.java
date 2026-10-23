package com.moriba.skultem.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

// Short-lived, in-memory caches for a few expensive reads that are asked for far more often than they change.
// Per server instance; every entry is keyed by school (and by user where the answer depends on who asks), so nothing
// crosses schools or permissions. Each cache is cleared early by the writes that change it (see its constants).
@Configuration
@EnableCaching
public class CacheConfig {

    // ComputeClassAttentionUseCase - the "needs attention" check, run for every class on the classes list and
    // dashboard (~7 queries each). Not user-specific. Cleared on attendance, grade approval and enrollment changes.
    public static final String CLASS_ATTENTION = "classAttention";

    // WidgetController - dashboard / analytics widgets, per user (role, section view and permissions shape them).
    // Time-limited only: a figure can lag a write by up to WIDGET_TTL.
    public static final String WIDGETS = "widgets";

    private static final Duration CLASS_ATTENTION_TTL = Duration.ofSeconds(60);
    private static final Duration WIDGET_TTL = Duration.ofSeconds(30);

    @Bean
    public CacheManager cacheManager() {
        var manager = new CaffeineCacheManager();
        // No caches created on demand - only the two below exist, so a typo in a cache name fails loudly instead of
        // silently creating an unbounded one. (Set first: setCacheNames replaces caches already registered.)
        manager.setCacheNames(java.util.List.of());
        manager.registerCustomCache(CLASS_ATTENTION, Caffeine.newBuilder()
                .expireAfterWrite(CLASS_ATTENTION_TTL)
                .maximumSize(5_000)
                .build());
        manager.registerCustomCache(WIDGETS, Caffeine.newBuilder()
                .expireAfterWrite(WIDGET_TTL)
                .maximumSize(10_000)
                .build());
        return manager;
    }
}
