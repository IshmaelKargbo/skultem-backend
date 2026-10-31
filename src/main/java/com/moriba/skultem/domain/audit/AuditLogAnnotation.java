package com.moriba.skultem.domain.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLogAnnotation {
    String action();

    /**
     * Whether the audit row records the method's arguments and result. Off for actions whose arguments carry
     * private data - Health Care visits hold medical details, which must never reach the audit log
     * (docs/HEALTH_CARE.md H-2). The row then names the method only; the use case's return value is not logged.
     */
    boolean includeDetails() default true;
}