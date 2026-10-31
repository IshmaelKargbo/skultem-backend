package com.moriba.skultem.domain.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import jakarta.servlet.http.HttpServletRequest;

// docs/HEALTH_CARE.md H-2: with includeDetails = false the audit row names the method only - no arguments, no result.
class AuditAspectDetailsTest {

    static class Sample {
        @AuditLogAnnotation(action = "PRIVATE", includeDetails = false)
        public String privateAction(String complaint) {
            return complaint;
        }

        @AuditLogAnnotation(action = "PUBLIC")
        public String publicAction(String value) {
            return value;
        }
    }

    private static String detailsFor(String method) throws Throwable {
        var publisher = mock(ApplicationEventPublisher.class);
        var request = mock(HttpServletRequest.class);
        var aspect = new AuditAspect(publisher, request);
        var joinPoint = mock(ProceedingJoinPoint.class);
        var signature = mock(Signature.class);
        when(signature.getName()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Vomiting, given paracetamol" });
        when(joinPoint.proceed()).thenReturn("Vomiting, given paracetamol");
        var annotation = Sample.class.getMethod(method, String.class).getAnnotation(AuditLogAnnotation.class);

        aspect.auditMethod(joinPoint, annotation);

        var captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(publisher).publishEvent(captor.capture());
        return captor.getValue().details();
    }

    @Test
    void privateActionsLeaveArgumentsAndResultOut() throws Throwable {
        assertThat(detailsFor("privateAction")).isEqualTo("Method: privateAction").doesNotContain("Vomiting");
    }

    @Test
    void otherActionsStillRecordTheirDetails() throws Throwable {
        assertThat(detailsFor("publicAction")).contains("Vomiting");
    }
}
