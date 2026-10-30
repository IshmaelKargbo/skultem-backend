package com.moriba.skultem.application.services;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Who belongs on a library session's roster (docs/LIBRARY_ATTENDANCE.md, B-5 and B-12): the students with an
 * ACTIVE enrollment in the session's class (class + section + stream + academic year). A Library period takes the
 * whole period for the class, so it is the whole class.
 */
@Component
@RequiredArgsConstructor
public class LibraryRosterResolver {

    private final EnrollmentRepository enrollmentRepo;

    // Mirrors GetClassSessionUseCase.resolveEnrollments: with a stream the repository query matches on it; without
    // one, the class's students who have no stream and are in the session's section.
    public List<Enrollment> roster(String schoolId, ClassSession session) {
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

    private static boolean sameId(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
