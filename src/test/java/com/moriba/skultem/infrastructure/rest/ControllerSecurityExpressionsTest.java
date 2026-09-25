package com.moriba.skultem.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.infrastructure.security.SectionNeutral;
import com.moriba.skultem.infrastructure.security.SectionScoped;

// @PreAuthorize expressions are only evaluated when an endpoint is hit, so a typo there (or a
// #variable that isn't one of the method's parameters) would surface as a runtime failure on that
// one endpoint. This checks every controller up front.
class ControllerSecurityExpressionsTest {

    private static final Pattern VARIABLE = Pattern.compile("#([a-zA-Z_][a-zA-Z0-9_]*)");

    private static List<Class<?>> controllers() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Class<?>> result = new ArrayList<>();
        for (var bean : scanner.findCandidateComponents("com.moriba.skultem.infrastructure.rest")) {
            result.add(Class.forName(bean.getBeanClassName()));
        }
        return result;
    }

    private static boolean isEndpoint(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
    }

    @Test
    void everyPreAuthorizeParsesAndOnlyUsesRealParameters() throws Exception {
        var parser = new SpelExpressionParser();
        List<String> problems = new ArrayList<>();
        var classes = controllers();
        assertThat(classes).isNotEmpty();

        for (var controller : classes) {
            for (var method : controller.getDeclaredMethods()) {
                var rule = method.getAnnotation(PreAuthorize.class);
                if (rule == null) {
                    continue;
                }
                String where = controller.getSimpleName() + "#" + method.getName();
                try {
                    parser.parseExpression(rule.value());
                } catch (Exception e) {
                    problems.add(where + ": does not parse - " + e.getMessage());
                    continue;
                }
                Set<String> params = Arrays.stream(method.getParameters()).map(Parameter::getName)
                        .collect(Collectors.toSet());
                var matcher = VARIABLE.matcher(rule.value());
                while (matcher.find()) {
                    if (!params.contains(matcher.group(1))) {
                        problems.add(where + ": #" + matcher.group(1) + " is not a parameter (has " + params + ")");
                    }
                }
            }
        }

        assertThat(problems).isEmpty();
    }

    // A section-scope guard only protects the endpoint if the interceptor lets the request reach it,
    // and an endpoint marked scoped/neutral without being an endpoint is a sign of a misplaced annotation.
    @Test
    void sectionAnnotationsOnlySitOnEndpoints() throws Exception {
        List<String> problems = new ArrayList<>();
        for (var controller : controllers()) {
            for (var method : controller.getDeclaredMethods()) {
                boolean marked = method.isAnnotationPresent(SectionScoped.class)
                        || method.isAnnotationPresent(SectionNeutral.class);
                if (marked && !isEndpoint(method)) {
                    problems.add(controller.getSimpleName() + "#" + method.getName());
                }
                if (method.isAnnotationPresent(SectionScoped.class) && method.isAnnotationPresent(SectionNeutral.class)) {
                    problems.add(controller.getSimpleName() + "#" + method.getName() + " is both scoped and neutral");
                }
            }
        }
        assertThat(problems).isEmpty();
    }

    // Endpoints whose path id isn't a scoped record and whose list query filters by level itself.
    // Each one needs a reason - add to this list only when that's genuinely the case.
    private static final Set<String> FILTERED_IN_QUERY = Set.of(
            // {classMasterId} is a teacher id; the approval list/counts filter by level in the query.
            "AssessmentController#listAssessmentApprovals",
            "AssessmentController#assessmentApprovalSummary");

    // Guards that take a record id must actually be wired into the rule - a @SectionScoped endpoint
    // with a path id but no @sectionScope check is almost always a forgotten guard.
    @Test
    void scopedEndpointsWithAPathIdHaveAGuard() throws Exception {
        List<String> problems = new ArrayList<>();
        for (var controller : controllers()) {
            for (var method : controller.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(SectionScoped.class)) {
                    continue;
                }
                boolean hasPathId = Arrays.stream(method.getParameters()).anyMatch(p -> p.isAnnotationPresent(
                        org.springframework.web.bind.annotation.PathVariable.class));
                var rule = method.getAnnotation(PreAuthorize.class);
                boolean guarded = rule != null && rule.value().contains("@sectionScope.");
                String name = controller.getSimpleName() + "#" + method.getName();
                if (hasPathId && !guarded && !FILTERED_IN_QUERY.contains(name)) {
                    problems.add(name);
                }
            }
        }
        assertThat(problems).isEmpty();
    }
}
