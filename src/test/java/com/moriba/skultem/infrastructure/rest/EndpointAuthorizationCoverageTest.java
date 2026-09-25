package com.moriba.skultem.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Every endpoint must say who may call it - on the method or its controller. An endpoint with no
// @PreAuthorize is open to ANY signed-in user (and to anyone at all where SecurityConfig permits the
// path), so a new one that forgets the rule fails here rather than shipping open. The few genuinely
// public endpoints are listed below, each with why.
class EndpointAuthorizationCoverageTest {

    private static final Set<String> PUBLIC = Set.of(
            // Sign-in / session flow: there is no signed-in user yet (or it is the user's own session).
            "AuthController#login", "AuthController#systemAdminLogin", "AuthController#refresh",
            "AuthController#tenant", "AuthController#logout",
            // Marketing site / onboarding, before any account exists.
            "RequestDemoController#create", "SchoolController#create", "SchoolController#count",
            "SystemAdminController#bootstrap");

    @Test
    void everyEndpointDeclaresWhoMayCallIt() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> open = new ArrayList<>();
        for (var bean : scanner.findCandidateComponents("com.moriba.skultem.infrastructure.rest")) {
            Class<?> controller = Class.forName(bean.getBeanClassName());
            boolean classRule = AnnotatedElementUtils.findMergedAnnotation(controller, PreAuthorize.class) != null;
            for (Method method : controller.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                    continue;
                }
                boolean rule = classRule || AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class) != null;
                String name = controller.getSimpleName() + "#" + method.getName();
                if (!rule && !PUBLIC.contains(name)) {
                    open.add(name);
                }
            }
        }
        assertThat(open).as("endpoints with no @PreAuthorize").isEmpty();
    }
}
