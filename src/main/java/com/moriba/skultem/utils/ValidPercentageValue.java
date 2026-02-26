package com.moriba.skultem.utils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = PercentageValueValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPercentageValue {
    String message() default "Percentage value must be between 1 and 100 when kind is PERCENTAGE";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}