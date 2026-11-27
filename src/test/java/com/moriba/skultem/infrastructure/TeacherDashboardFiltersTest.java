package com.moriba.skultem.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.FilterOperator;
import com.moriba.skultem.infrastructure.persistence.entity.AssessmentScoreEntity;
import com.moriba.skultem.infrastructure.persistence.entity.AttendanceEntity;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.AssessmentScoreJpaRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.AttendanceJpaRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.EnrollmentJpaRepository;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

// The teacher dashboard's report filters, run against the real schema: a wrong path or value type only fails here.
@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class TeacherDashboardFiltersTest {

    @Autowired AttendanceJpaRepository attendance;

    @Autowired AssessmentScoreJpaRepository scores;
    @Autowired EnrollmentJpaRepository enrollments;

    private static Filter eq(String field, String value) {
        return new Filter(field, FilterOperator.EQUALS, "select", value, null, null);
    }

    @Test
    void attendanceForOneSectionThisTerm() {
        var f = List.of(eq("enrollment.clazz.id", "c"), eq("enrollment.section.id", "s"), eq("enrollment.stream.id", "st"),
                new Filter("date", FilterOperator.BETWEEN, "date", "2026-09-01", "2026-11-27", null));
        assertThat(attendance.findAll(FilterSpecificationBuilder.<AttendanceEntity>build(f))).isEmpty();
    }

    @Test
    void approvedMarksForOneTeacherSubject() {
        var f = List.of(eq("studentAssessment.teacherSubject.id", "ts"), eq("cycle.term.id", "t"), eq("cycle.assessment.id", "a"),
                new Filter("cycle.status", FilterOperator.IN_LIST, "select", null, null, List.of("APPROVED", "COMPLETED")));
        assertThat(scores.findAll(FilterSpecificationBuilder.<AssessmentScoreEntity>build(f))).isEmpty();
    }

    @Test
    void activeStudentsInOneSectionThisYear() {
        var f = List.of(eq("clazz.id", "c"), eq("section.id", "s"), eq("status", "ACTIVE"),
                new Filter("academicYear.active", FilterOperator.EQUALS, "boolean", "true", null, null));
        assertThat(enrollments.findAll(FilterSpecificationBuilder.<EnrollmentEntity>build(f))).isEmpty();
    }
}
