package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.repository.StreamRepository;
import com.moriba.skultem.domain.repository.StreamUsageRepository;

class StreamEditDeleteUseCaseTest {

    private final StreamRepository repo = mock(StreamRepository.class);
    private final StreamUsageRepository usage = mock(StreamUsageRepository.class);
    private final LogActivityUseCase log = mock(LogActivityUseCase.class);
    private final UpdateStreamUseCase update = new UpdateStreamUseCase(repo, log);
    private final DeleteStreamUseCase delete = new DeleteStreamUseCase(repo, usage, log);

    private Stream science() {
        var s = Stream.create("STM1", "Science", "s", "Sciences");
        when(repo.findByIdAndSchoolId("STM1", "s")).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void renamesAStream() {
        var s = science();
        var dto = update.execute("s", "STM1", "  Pure Science ", "Physics, Chemistry");
        assertThat(s.getName()).isEqualTo("Pure Science");
        assertThat(dto.name()).isEqualTo("Pure Science");
        verify(repo).save(s);
    }

    @Test
    void refusesANameAnotherStreamHas() {
        science();
        when(repo.existsByNameAndSchoolExcept("Art", "s", "STM1")).thenReturn(true);
        assertThatThrownBy(() -> update.execute("s", "STM1", "Art", null)).isInstanceOf(AlreadyExistsException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void deletesAnUnusedStreamWithItsOwnSubjects() {
        var s = science();
        when(usage.usage("STM1")).thenReturn(Map.of());
        delete.execute("s", "STM1");
        verify(usage).deleteOwnSubjects("STM1");
        verify(repo).delete(s);
    }

    @Test
    void refusesAStreamStillInUseAndSaysWhatUsesIt() {
        science();
        Map<String, Long> used = new LinkedHashMap<>();
        used.put("class(es)", 1L);
        used.put("student enrollment(s)", 40L);
        when(usage.usage("STM1")).thenReturn(used);
        assertThatThrownBy(() -> delete.execute("s", "STM1")).isInstanceOf(RuleException.class)
                .hasMessageContaining("1 class(es), 40 student enrollment(s)");
        verify(repo, never()).delete(any());
        verify(usage, never()).deleteOwnSubjects(any());
    }
}
