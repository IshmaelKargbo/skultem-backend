package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ExpenseCategory;
import com.moriba.skultem.domain.repository.ExpenseCategoryRepository;

class ExpenseCategoryEditDeleteUseCaseTest {

    private final ExpenseCategoryRepository repo = mock(ExpenseCategoryRepository.class);
    private final LogActivityUseCase log = mock(LogActivityUseCase.class);
    private final UpdateExpenseCategoryUseCase update = new UpdateExpenseCategoryUseCase(repo, log);
    private final DeleteExpenseCategoryUseCase delete = new DeleteExpenseCategoryUseCase(repo, log);

    private ExpenseCategory fuel() {
        var c = ExpenseCategory.create("c1", "s", "Fuel", "Generator fuel");
        when(repo.findByIdAndSchool("c1", "s")).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void renames() {
        var c = fuel();
        update.execute("s", "c1", " Fuel & Oil ", "All fuel");
        assertThat(c.getName()).isEqualTo("Fuel & Oil");
        verify(repo).save(c);
    }

    @Test
    void refusesADuplicateName() {
        fuel();
        when(repo.existsByNameAndSchoolExcept("Transport", "s", "c1")).thenReturn(true);
        assertThatThrownBy(() -> update.execute("s", "c1", "Transport", "")).isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    void deletesOnlyWhenNoExpenseUsesIt() {
        var c = fuel();
        when(repo.countExpenses("c1", "s")).thenReturn(4L);
        assertThatThrownBy(() -> delete.execute("s", "c1")).isInstanceOf(RuleException.class)
                .hasMessageContaining("4 expenses");
        verify(repo, never()).delete(any());

        when(repo.countExpenses("c1", "s")).thenReturn(0L);
        delete.execute("s", "c1");
        verify(repo).delete(c);
    }
}
