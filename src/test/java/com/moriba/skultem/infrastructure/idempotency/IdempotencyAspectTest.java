package com.moriba.skultem.infrastructure.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.idempotency.IdempotencyStore.Outcome;
import com.moriba.skultem.infrastructure.idempotency.IdempotencyStore.State;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.AuthUser;

class IdempotencyAspectTest {

    private final IdempotencyStore store = mock(IdempotencyStore.class);
    private final IdempotencyAspect aspect = new IdempotencyAspect(store);
    private final ProceedingJoinPoint jp = mock(ProceedingJoinPoint.class);
    private final Idempotent idempotent = mock(Idempotent.class);
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser("user-1", "school-1", Role.ADMIN), null, java.util.List.of()));
        when(idempotent.operation()).thenReturn("fee.structure.create");
        when(jp.getArgs()).thenReturn(new Object[] { "school-1", "body" });
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void withoutTheHeaderTheRequestJustRuns() throws Throwable {
        var result = new ApiResponse<>("success", 200, "ok", "x");
        when(jp.proceed()).thenReturn(result);

        assertThat(aspect.around(jp, idempotent)).isSameAs(result);
        verify(store, never()).begin(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void aFirstRequestRunsAndItsResponseIsStored() throws Throwable {
        request.addHeader(IdempotencyAspect.HEADER, "key-1");
        var result = new ApiResponse<>("success", 200, "created", "x");
        when(store.begin(eq("school-1"), eq("user-1"), eq("fee.structure.create"), eq("key-1"), anyString()))
                .thenReturn(new Outcome(State.NEW, null));
        when(jp.proceed()).thenReturn(result);

        assertThat(aspect.around(jp, idempotent)).isSameAs(result);
        verify(store).complete(eq("school-1"), eq("fee.structure.create"), eq("key-1"), anyString());
    }

    @Test
    void aRepeatGetsTheFirstResponseWithoutRunningAgain() throws Throwable {
        request.addHeader(IdempotencyAspect.HEADER, "key-1");
        String stored = "{\"status\":\"success\",\"code\":200,\"message\":\"created\",\"data\":{\"id\":\"f-1\"},\"meta\":null}";
        when(store.begin(anyString(), anyString(), anyString(), eq("key-1"), anyString()))
                .thenReturn(new Outcome(State.REPLAY, stored));

        var replayed = (ApiResponse<?>) aspect.around(jp, idempotent);

        assertThat(replayed.getMessage()).isEqualTo("created");
        assertThat(replayed.getData()).isEqualTo(java.util.Map.of("id", "f-1"));
        assertThat(response.getHeader(IdempotencyAspect.REPLAY_HEADER)).isEqualTo("true");
        verify(jp, never()).proceed();
    }

    @Test
    void theSameKeyWithADifferentBodyIsRejected() throws Throwable {
        request.addHeader(IdempotencyAspect.HEADER, "key-1");
        when(store.begin(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new Outcome(State.MISMATCH, null));

        assertThatThrownBy(() -> aspect.around(jp, idempotent)).isInstanceOf(RuleException.class);
        verify(jp, never()).proceed();
    }

    @Test
    void aRequestStillRunningIsAConflict() throws Throwable {
        request.addHeader(IdempotencyAspect.HEADER, "key-1");
        when(store.begin(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new Outcome(State.IN_PROGRESS, null));

        assertThatThrownBy(() -> aspect.around(jp, idempotent)).isInstanceOf(IllegalStateException.class);
        verify(jp, never()).proceed();
    }

    @Test
    void aFailedRequestFreesItsKeySoItCanBeRetried() throws Throwable {
        request.addHeader(IdempotencyAspect.HEADER, "key-1");
        when(store.begin(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new Outcome(State.NEW, null));
        when(jp.proceed()).thenThrow(new RuleException("Amount is wrong"));

        assertThatThrownBy(() -> aspect.around(jp, idempotent)).isInstanceOf(RuleException.class);
        verify(store).release("school-1", "fee.structure.create", "key-1");
        verify(store, never()).complete(anyString(), anyString(), anyString(), any());
    }
}
