package com.moriba.skultem.infrastructure.security;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.moriba.skultem.application.error.OutsideManagementScopeException;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.vo.Role;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SectionScopeInterceptor implements HandlerInterceptor {

    private final SectionScopeService sectionScopeService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            return true;
        }
        if (user.activeRole() == Role.SYSTEM_ADMIN || user.activeSchoolId() == null) {
            return true;
        }

        var scope = sectionScopeService.current();
        if (scope.wholeSchool()) {
            return true;
        }
        if (scope.levels().isEmpty()) {
            throw new OutsideManagementScopeException(
                    "Your management sections don't include any school levels. Ask the school owner to check them.");
        }

        if (isMarked(method, SectionScoped.class) || isMarked(method, SectionNeutral.class)) {
            return true;
        }
        throw new OutsideManagementScopeException();
    }

    private static boolean isMarked(HandlerMethod method, Class<? extends java.lang.annotation.Annotation> type) {
        return AnnotatedElementUtils.hasAnnotation(method.getMethod(), type)
                || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), type);
    }
}
