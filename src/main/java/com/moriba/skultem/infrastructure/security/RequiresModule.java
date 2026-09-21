package com.moriba.skultem.infrastructure.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.moriba.skultem.domain.vo.FeatureModule;

/**
 * Marks a controller (or a single endpoint) as belonging to an optional {@link FeatureModule}: a
 * school that hasn't installed it gets a 403 {@code MODULE_NOT_INSTALLED} instead of the feature.
 * On a class it covers every endpoint in it; on a method it overrides the class-level value.
 * Independent of the role checks in {@code @PreAuthorize} - both must pass. System admins are
 * exempt (they aren't tied to one school). See {@link ModuleAccessInterceptor}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface RequiresModule {
    FeatureModule value();
}
