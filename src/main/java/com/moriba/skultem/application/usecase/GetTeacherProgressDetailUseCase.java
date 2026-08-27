package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SubjectCoverageDTO;
import com.moriba.skultem.application.dto.TeacherProgressDetailDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.SchemeOfWork;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.Week;
import com.moriba.skultem.domain.repository.LessonRepository;
import com.moriba.skultem.domain.repository.SchemeOfWorkRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.repository.WeekRepository;

import lombok.RequiredArgsConstructor;

/**
 * Per-teacher curriculum coverage: overall progress plus a breakdown by
 * subject (a teacher may teach the same subject to more than one class).
 */
@Service
@RequiredArgsConstructor
public class GetTeacherProgressDetailUseCase {

    private final TeacherRepository teacherRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;
    private final SchemeOfWorkRepository schemeRepo;
    private final WeekRepository weekRepo;
    private final LessonRepository lessonRepo;

    public TeacherProgressDetailDTO execute(String schoolId, String teacherId) {
        var teacher = teacherRepo.findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> new NotFoundException("teacher not found"));

        var assignments = teacherSubjectRepo.findByTeacherIdAndSchoolId(teacherId, schoolId);

        int subjects = (int) assignments.stream().map(a -> a.getSubject().getId()).distinct().count();
        int classes = (int) assignments.stream().map(a -> a.getSession().getId()).distinct().count();

        long lessonNotes = lessonRepo.findAllByTeacherIdAndSchoolId(teacherId, schoolId, Pageable.unpaged())
                .getTotalElements();

        var schemes = schemeRepo.findAllByTeacherIdAndSchoolId(teacherId, schoolId, Pageable.unpaged())
                .getContent();

        Map<String, List<SchemeOfWork>> bySubject = new LinkedHashMap<>();
        for (var scheme : schemes) {
            bySubject.computeIfAbsent(scheme.getSubject().getId(), k -> new ArrayList<>()).add(scheme);
        }

        List<SubjectCoverageDTO> subjectCoverage = new ArrayList<>();
        int totalWeeks = 0;
        int completedWeeks = 0;

        for (var entry : bySubject.entrySet()) {
            var subjectSchemes = entry.getValue();
            String subjectName = subjectSchemes.get(0).getSubject().getName();

            String classNames = subjectSchemes.stream()
                    .map(s -> s.getSession().getName())
                    .distinct()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            int subjectTotal = 0;
            int subjectCompleted = 0;

            for (var scheme : subjectSchemes) {
                var weeks = weekRepo.findAllByScheme(scheme.getId());
                subjectTotal += weeks.size();
                subjectCompleted += (int) weeks.stream().filter(w -> w.getState() == Week.State.COMPLETED).count();
            }

            int subjectCoveragePct = subjectTotal == 0 ? 0 : (int) Math.round((subjectCompleted * 100.0) / subjectTotal);

            subjectCoverage.add(new SubjectCoverageDTO(subjectName, classNames, subjectCompleted, subjectTotal, subjectCoveragePct));

            totalWeeks += subjectTotal;
            completedWeeks += subjectCompleted;
        }

        int coverage = totalWeeks == 0 ? 0 : (int) Math.round((completedWeeks * 100.0) / totalWeeks);

        String status;
        if (totalWeeks == 0) {
            status = "NO_DATA";
        } else if (coverage >= 100) {
            status = "COMPLETED";
        } else if (coverage >= 50) {
            status = "ON_TRACK";
        } else {
            status = "BEHIND";
        }

        return new TeacherProgressDetailDTO(
                teacher.getId(),
                teacherName(teacher),
                subjects,
                classes,
                lessonNotes,
                completedWeeks,
                totalWeeks,
                coverage,
                status,
                subjectCoverage);
    }

    private String teacherName(Teacher teacher) {
        String title = teacher.getTitle() != null ? teacher.getTitle().toSentenceCase() + " " : "";
        return (title + teacher.getUser().getGivenNames() + " " + teacher.getUser().getFamilyName()).trim();
    }
}
