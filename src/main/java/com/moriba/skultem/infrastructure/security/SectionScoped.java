package com.moriba.skultem.infrastructure.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint (or every endpoint of a controller) as enforcing management-section scope: its
 * list queries filter by the caller's {@code SectionScope} and its single-record reads/writes check
 * the record's level (usually via {@code @sectionScope.*} in {@code @PreAuthorize}).
 * <p>
 * Section-limited staff are denied every endpoint that is neither {@code @SectionScoped} nor
 * {@link SectionNeutral} - see {@link SectionScopeInterceptor}. Only add this once the endpoint really
 * filters; that deny-by-default is what keeps an unconverted endpoint from leaking another section.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface SectionScoped {
}
