package com.moriba.skultem.infrastructure.rest.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ParentOrParentIdValidator.class)
@Documented
public @interface ValidParentOrParentId {

    String message() default
            "Either parentId or parent details must be provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}