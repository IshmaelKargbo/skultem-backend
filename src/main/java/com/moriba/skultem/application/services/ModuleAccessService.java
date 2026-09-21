package com.moriba.skultem.application.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.SchoolModuleRepository;
import com.moriba.skultem.domain.vo.FeatureModule;

import lombok.RequiredArgsConstructor;

/**
 * The one place that answers "does this school have this module?" - used by the request
 * interceptor that guards {@code @RequiresModule} endpoints (see ModuleAccessInterceptor) and by
 * the Modules page's use cases. Every guarded request asks this, so answers are cached briefly per
 * school; installing/disabling clears that school's entry straight away (see {@link #evict}), and
 * the short expiry bounds staleness if another instance made the change.
 */
@Service
@RequiredArgsConstructor
public class ModuleAccessService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private record CachedModules(Set<FeatureModule> modules, Instant loadedAt) {
        boolean isFresh() {
            return loadedAt.plus(CACHE_TTL).isAfter(Instant.now());
        }
    }

    private final SchoolModuleRepository repo;
    private final Map<String, CachedModules> cache = new ConcurrentHashMap<>();

    public boolean isEnabled(String schoolId, FeatureModule module) {
        return enabledModules(schoolId).contains(module);
    }

    public Set<FeatureModule> enabledModules(String schoolId) {
        var cached = cache.get(schoolId);
        if (cached != null && cached.isFresh()) {
            return cached.modules();
        }

        var loaded = Set.copyOf(repo.findEnabledBySchool(schoolId));
        cache.put(schoolId, new CachedModules(loaded, Instant.now()));
        return loaded;
    }

    public void evict(String schoolId) {
        cache.remove(schoolId);
    }
}
