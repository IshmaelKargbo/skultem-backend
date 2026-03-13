package com.moriba.skultem.domain.audit;

import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.moriba.skultem.domain.model.AuditLog.Status;
import com.moriba.skultem.infrastructure.security.AuthUser;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final int MAX_DETAILS_LENGTH = 2000;

    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest request;

    public AuditAspect(
            ApplicationEventPublisher eventPublisher,
            HttpServletRequest request) {

        this.eventPublisher = eventPublisher;
        this.request = request;
    }

    @Around("@annotation(auditAnnotation)")
    public Object auditMethod(
            ProceedingJoinPoint joinPoint,
            AuditLogAnnotation auditAnnotation) throws Throwable {

        Status status = Status.SUCCESS;
        String details = "";

        try {
            Object result = joinPoint.proceed();
            details = generateDetails(joinPoint, result);
            return result;

        } catch (Throwable ex) {
            status = Status.FAILURE;
            details = truncate(ex.getMessage(), MAX_DETAILS_LENGTH);
            throw ex;

        } finally {
            try {
                Optional<AuthUser> authUser = resolveAuthUser();

                eventPublisher.publishEvent(new AuditEvent(
                        auditAnnotation.action(),
                        authUser.map(AuthUser::activeSchoolId).orElse(null),
                        authUser.map(AuthUser::userId).orElse(null),
                        getClientIp(),
                        getUserAgent(),
                        status,
                        details));

            } catch (Exception e) {
                log.error("Failed to publish audit event for action '{}': {}",
                        auditAnnotation.action(), e.getMessage(), e);
            }
        }
    }

    private Optional<AuthUser> resolveAuthUser() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthUser authUser) {
                return Optional.of(authUser);
            }
        } catch (Exception e) {
            log.warn("Could not resolve auth user for audit: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private String getUserAgent() {
        String ua = request.getHeader("User-Agent");
        return (ua == null || ua.isBlank()) ? "Unknown" : ua;
    }

    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String generateDetails(ProceedingJoinPoint joinPoint, Object result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Method: ").append(joinPoint.getSignature().getName());

        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            sb.append(" | Args: [");
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    sb.append(args[i]);
                    if (i < args.length - 1)
                        sb.append(", ");
                }
            }
            sb.append("]");
        }

        if (result != null) {
            sb.append(" | Result: ").append(result);
        }

        return truncate(sb.toString(), MAX_DETAILS_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        if (value == null)
            return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }
}