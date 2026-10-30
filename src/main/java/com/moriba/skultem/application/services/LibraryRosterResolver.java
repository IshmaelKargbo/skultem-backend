package com.moriba.skultem.application.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.EnrollmentSubjectRepository;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;

import lombok.RequiredArgsConstructor;

/**
 * Who belongs on a library session's roster (docs/LIBRARY_ATTENDANCE.md, B-5 and B-12): the students with an
 * ACTIVE enrollment in the session's class (class + section + stream + academic year). If the Library subject
 * is an optional subject for that class or stream, only the students who take it.
 */
@Component
@RequiredArgsConstructor
public class LibraryRosterResolver {

    private final EnrollmentRepository enrollmentRepo;
    private final ClassSubjectRepository classSubjectRepo;
    private final StreamSubjectRepository streamSubjectRepo;
    private final EnrollmentSubjectRepository enrollmentSubjectRepo;

    public List<Enrollment> roster(String schoolId, ClassSession session, String subjectId) {
        var students = activeEnrollments(schoolId, session);
        if (subjectId == null || !isOptional(schoolId, session, subjectId)) {
            return students;
        }
        return students.stream()
                .filter(e -> enrollmentSubjectRepo.existsByEnrollmentIdAndSubjectIdAndSchoolId(e.getId(), subjectId,
                        schoolId))
                .toList();
    }

    // Mirrors GetClassSessionUseCase.resolveEnrollments: with a stream the repository query matches on it; without
    // one, the class's students who have no stream and are in the session's section.
    private List<Enrollment> activeEnrollments(String schoolId, ClassSession session) {
        String classId = session.getClazz().getId();
        String yearId = session.getAcademicYear().getId();
        String sectionId = session.getSection() == null ? null : session.getSection().getId();
        List<Enrollment> found;
        if (session.getStream() != null) {
            found = enrollmentRepo.findActiveByClassIdAndSectionIdAndStreamIdAndAcademicYearIdAndSchoolId(classId,
                    sectionId, session.getStream().getId(), yearId, schoolId);
        } else {
            found = enrollmentRepo.findAllByClassAndAcademicAndSchoolId(classId, yearId, schoolId, Pageable.unpaged())
                    .stream()
                    .filter(e -> e.getStream() == null)
                    .filter(e -> sameId(e.getSection() == null ? null : e.getSection().getId(), sectionId))
                    .toList();
        }
        return found.stream().filter(e -> e.getStatus() == Enrollment.Status.ACTIVE).toList();
    }

    private boolean isOptional(String schoolId, ClassSession session, String subjectId) {
        Optional<Boolean> mandatory = session.getStream() != null
                ? streamSubjectRepo.findByStreamIdAndSubjectIdAndSchoolId(session.getStream().getId(), subjectId,
                        schoolId).map(s -> s.getMandatory())
                : Optional.empty();
        if (mandatory.isEmpty()) {
            mandatory = classSubjectRepo.findByClassIdAndSubjectIdAndSchoolId(session.getClazz().getId(), subjectId,
                    schoolId).map(c -> c.getMandatory());
        }
        // No assignment at all, or no flag: treat it as a whole-class subject.
        return mandatory.map(Boolean.FALSE::equals).orElse(false);
    }

    private static boolean sameId(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
