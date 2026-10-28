package com.moriba.skultem.application.usecase;

import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;

import lombok.RequiredArgsConstructor;

// Which classes (and which class subjects) are graded in a term. A class that joins part-way through the year has no
// assessments for the earlier terms, so Grade Entry leaves it out of that term's list instead of showing it empty.
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListTermGradedSessionsUseCase {

    public record Result(List<String> classSessionIds, List<String> teacherSubjectIds) {
    }

    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final com.moriba.skultem.application.services.SectionScopeService sectionScopeService;

    public Result execute(String schoolId, String termId) {
        var sessions = new LinkedHashSet<String>();
        var subjects = new LinkedHashSet<String>();
        // Only classes in the caller's section(s) - whole-school callers get every level.
        var levels = sectionScopeService.levels();
        for (var cycle : cycleRepo.findAllBySchoolAndTerm(schoolId, termId)) {
            if (!levels.contains(cycle.getSubject().getSession().getClazz().getLevel())) {
                continue;
            }
            subjects.add(cycle.getSubject().getId());
            sessions.add(cycle.getSubject().getSession().getId());
        }
        return new Result(List.copyOf(sessions), List.copyOf(subjects));
    }
}
