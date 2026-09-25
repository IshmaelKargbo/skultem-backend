package com.moriba.skultem.infrastructure.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint (or controller) as safe for section-limited staff because it exposes nothing
 * tied to a level: the caller's own account, school branding, academic years/terms, class-division
 * names, and the like. Not for anything that returns students, classes, fees or other per-level
 * data - that needs {@link SectionScoped} and real filtering. See {@link SectionScopeInterceptor}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface SectionNeutral {
}
