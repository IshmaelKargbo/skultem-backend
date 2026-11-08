package com.moriba.skultem.application.services;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.ForbiddenException;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.FilterOperator;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.security.PermissionService;

import lombok.RequiredArgsConstructor;

/**
 * Which report-builder data each role may ask for (backs /widget/run and /report/export/run).
 * The builder runs whatever filters the browser sends, so the role limits are enforced here:
 * - a parent only gets their own children's attendance, grades and fees, and must filter by them;
 * - a teacher never gets the school's money or staff records (payments, expenses, ledgers, staff).
 * Everyone else is already limited by the endpoint's role check and their management sections.
 */
@Service
@RequiredArgsConstructor
public class ReportAccessPolicy {

    // For each report a parent may run, the field that names the student.
    private static final Map<String, String> PARENT_STUDENT_FIELD = Map.of(
            "attendances", "enrollment.student.id",
            "assessments", "studentAssessment.enrollment.student.id",
            "grades", "studentAssessment.enrollment.student.id",
            "breakdown", "studentAssessment.enrollment.student.id",
            "fees", "student.id");

    private static final Set<String> TEACHER_DENIED = Set.of(
            "payments", "expenses", "transactions", "ledger", "teachers");

    private final ParentRepository parentRepo;
    private final StudentRepository studentRepo;

    public void check(String schoolId, String entity, List<Filter> filters) {
        Role role = PermissionService.getCurrentUser().activeRole();
        String type = entity == null ? "" : entity.trim().toLowerCase();

        if (role == Role.TEACHER && TEACHER_DENIED.contains(type)) {
            throw new ForbiddenException("Teachers can't run this report.");
        }
        if (role == Role.PARENT) {
            checkParent(schoolId, type, filters);
        }
    }

    private void checkParent(String schoolId, String type, List<Filter> filters) {
        String field = PARENT_STUDENT_FIELD.get(type);
        if (field == null) {
            throw new ForbiddenException("Parents can't run this report.");
        }

        Set<String> children = childIds(schoolId);
        // Every filter naming a student must name only this parent's children, and there must be one -
        // without it the report would cover the whole school.
        List<Filter> studentFilters = filters == null ? List.of()
                : filters.stream().filter(f -> f != null && field.equals(f.field())).toList();

        boolean onlyOwnChildren = !studentFilters.isEmpty() && studentFilters.stream().allMatch(f -> {
            if (f.operator() == FilterOperator.EQUALS) {
                return f.value() != null && children.contains(f.value());
            }
            if (f.operator() == FilterOperator.IN_LIST) {
                return f.values() != null && !f.values().isEmpty() && children.containsAll(f.values());
            }
            return false;
        });

        if (!onlyOwnChildren) {
            throw new ForbiddenException("You can only see reports for your own children.");
        }
    }

    private Set<String> childIds(String schoolId) {
        String userId = PermissionService.getCurrentUser().userId();
        return parentRepo.findByUserIdAndSchoolId(userId, schoolId)
                .map(parent -> studentRepo.findByParentAndSchoolId(parent.getId(), schoolId, Pageable.unpaged())
                        .getContent().stream().map(Student::getId)
                        .collect(java.util.stream.Collectors.toSet()))
                .orElse(Set.of());
    }
}
