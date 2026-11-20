package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.ReportCard;

// Which report cards the broadsheet reads: the full term when they exist, else the newest kind generated - so a class
// with only mid-term cards (a set of assessments) or whole-year cards still gets a broadsheet instead of nothing.
class GetBroadsheetUseCaseTest {

    private static ReportCard card(String scope, String label, String generated) {
        var c = mock(ReportCard.class);
        lenient().when(c.getScopeKey()).thenReturn(scope);
        lenient().when(c.getScopeLabel()).thenReturn(label);
        lenient().when(c.getGeneratedAt()).thenReturn(generated == null ? null : Instant.parse(generated));
        return c;
    }

    @Test
    void theFullTermComesFirstAndIsTheDefault() {
        var scopes = GetBroadsheetUseCase.scopesOf(List.of(
                card("a1,a2", "Mid-term (Test 1, Test 2)", "2026-11-10T10:00:00Z"),
                card("ALL", null, "2026-11-01T10:00:00Z"),
                card("ALL", null, "2026-11-01T10:00:00Z")));
        assertThat(scopes).extracting(s -> s.key() + ":" + s.cards() + ":" + s.label())
                .containsExactly("ALL:2:Full term", "a1,a2:1:Mid-term (Test 1, Test 2)");
        assertThat(GetBroadsheetUseCase.chooseScope(scopes, null)).isEqualTo("ALL");
    }

    @Test
    void withoutFullTermCardsTheNewestKindIsUsed() {
        var scopes = GetBroadsheetUseCase.scopesOf(List.of(
                card("YEAR", null, "2026-07-01T10:00:00Z"),
                card("a1,a2", "Mid-term", "2026-11-10T10:00:00Z")));
        assertThat(GetBroadsheetUseCase.chooseScope(scopes, null)).isEqualTo("a1,a2");
    }

    @Test
    void aChosenKindIsUsedOnlyIfItExists() {
        var scopes = GetBroadsheetUseCase.scopesOf(List.of(card("ALL", null, null), card("YEAR", "Whole year", null)));
        assertThat(GetBroadsheetUseCase.chooseScope(scopes, "YEAR")).isEqualTo("YEAR");
        assertThat(GetBroadsheetUseCase.chooseScope(scopes, "gone")).isEqualTo("ALL");
        assertThat(GetBroadsheetUseCase.chooseScope(List.of(), null)).isNull();
    }
}
