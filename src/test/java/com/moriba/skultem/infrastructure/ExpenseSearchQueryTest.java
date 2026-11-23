package com.moriba.skultem.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.moriba.skultem.domain.repository.ExpenseCategoryRepository;
import com.moriba.skultem.domain.repository.ExpenseRepository;

// The search queries are only checked against the entities when the app starts - this runs them for real.
@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class ExpenseSearchQueryTest {

    @Autowired
    ExpenseRepository expenses;

    @Autowired
    ExpenseCategoryRepository categories;

    @Test
    void searchQueriesRun() {
        assertThat(expenses.search("no-school", "fuel", PageRequest.of(0, 5)).getContent()).isEmpty();
        assertThat(categories.search("no-school", "fuel", PageRequest.of(0, 5)).getContent()).isEmpty();
    }
}
