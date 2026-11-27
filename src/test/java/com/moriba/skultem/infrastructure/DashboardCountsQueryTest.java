package com.moriba.skultem.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.Level;

// The dashboard's teacher count and monthly revenue queries, run for real (they're only checked at startup).
@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class DashboardCountsQueryTest {

    @Autowired
    TeacherRepository teachers;

    @Autowired
    PaymentRepository payments;

    @Test
    void dashboardQueriesRun() {
        assertThat(teachers.countActiveTeachers("no-school")).isZero();
        assertThat(payments.sumPaymentsBySchoolAndDateRange("no-school", Instant.EPOCH, Instant.now(),
                List.of(Level.values()))).isZero();
        assertThat(payments.sumRevenueByCategory("no-school", List.of(Level.values()))).isEmpty();
    }
}
