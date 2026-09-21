package com.moriba.skultem.infrastructure.security;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.moriba.skultem.application.error.ModuleNotInstalledException;
import com.moriba.skultem.application.services.ModuleAccessService;
import com.moriba.skultem.domain.vo.Role;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Enforces {@link RequiresModule}. Runs after authentication, so the caller's school is known; an
 * unauthenticated request is left to the security layer, which already rejects it. Throwing here
 * is turned into a 403 by GlobalExceptionHandler.
 */
@Component
@RequiredArgsConstructor
public class ModuleAccessInterceptor implements HandlerInterceptor {

    private final ModuleAccessService moduleAccess;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        var required = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequiresModule.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequiresModule.class);
        }
        if (required == null) {
            return true;
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            return true;
        }
        if (user.activeRole() == Role.SYSTEM_ADMIN || user.activeSchoolId() == null) {
            return true;
        }

        if (!moduleAccess.isEnabled(user.activeSchoolId(), required.value())) {
            throw new ModuleNotInstalledException(required.value());
        }
        return true;
    }
}
