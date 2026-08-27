package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherProgressDTO;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.Week;
import com.moriba.skultem.domain.repository.SchemeOfWorkRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.repository.WeekRepository;

import lombok.RequiredArgsConstructor;

/**
 * Aggregates each teacher's scheme of work coverage: how many of their
 * assigned weeks (across every scheme tied to a subject/class they teach)
 * have been marked completed.
 */
@Service
@RequiredArgsConstructor
public class GetTeacherProgressUseCase {

    private final TeacherRepository teacherRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;
    private final SchemeOfWorkRepository schemeRepo;
    private final WeekRepository weekRepo;

    public List<TeacherProgressDTO> execute(String schoolId) {
        var teachers = teacherRepo.search("", schoolId, Pageable.unpaged()).getContent();

        return teachers.stream()
                .filter(t -> t.getStatus() == Teacher.Status.ACTIVE)
                .map(t -> build(schoolId, t))
                .toList();
    }

    private TeacherProgressDTO build(String schoolId, Teacher teacher) {
        var assignments = teacherSubjectRepo.findByTeacherIdAndSchoolId(teacher.getId(), schoolId);

        int subjects = (int) assignments.stream().map(a -> a.getSubject().getId()).distinct().count();
        int classes = (int) assignments.stream().map(a -> a.getSession().getId()).distinct().count();

        var schemes = schemeRepo.findAllByTeacherIdAndSchoolId(teacher.getId(), schoolId, Pageable.unpaged())
                .getContent();

        int totalWeeks = 0;
        int completedWeeks = 0;

        for (var scheme : schemes) {
            var weeks = weekRepo.findAllByScheme(scheme.getId());
            totalWeeks += weeks.size();
            completedWeeks += (int) weeks.stream().filter(w -> w.getState() == Week.State.COMPLETED).count();
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

        String name = teacherName(teacher);

        return new TeacherProgressDTO(teacher.getId(), name, subjects, classes, completedWeeks, totalWeeks, coverage,
                status);
    }

    private String teacherName(Teacher teacher) {
        String title = teacher.getTitle() != null ? teacher.getTitle().toSentenceCase() + " " : "";
        return (title + teacher.getUser().getGivenNames() + " " + teacher.getUser().getFamilyName()).trim();
    }
}
