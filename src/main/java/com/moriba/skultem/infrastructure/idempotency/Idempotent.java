package com.moriba.skultem.infrastructure.idempotency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes a record-creating endpoint safe to retry: when the request carries an {@code Idempotency-Key}
 * header, the first call runs and its response is remembered; a repeat with the same key (and the same
 * body) gets that response back instead of creating a second record. Without the header the endpoint
 * behaves exactly as before. See {@link IdempotencyAspect}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Idempotent {
    // Names what the key is for - the same key value on two different operations never collides.
    String operation();
}
