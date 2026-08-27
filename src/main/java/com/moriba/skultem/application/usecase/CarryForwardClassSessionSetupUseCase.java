package com.moriba.skultem.application.usecase;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.ClassMaster;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * A brand new {@link ClassSession} starts empty - no class master, no subject/teacher assignments -
 * which is what left promoted students without assessments: {@link ProvisionStudentAssessmentsUseCase}
 * needs a {@link TeacherSubject} for the new session to attach a student's assessment to, and nothing
 * created one there automatically.
 * <p>
 * This runs right after a class session is created for a new academic year and copies forward, from
 * that same class/section/stream's session in the immediately preceding year (if one exists):
 * <ul>
 * <li>the currently active {@link ClassMaster}, so the class doesn't start the year without one</li>
 * <li>every {@link TeacherSubject} assignment, so a promoted/repeating student's assessments can be
 * provisioned immediately with last year's teacher still attached to each subject</li>
 * </ul>
 * It's additive and idempotent - it never touches the previous year's records, and skips anything the
 * new session already has (e.g. an admin who set it up by hand before any student landed there).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CarryForwardClassSessionSetupUseCase {

    private final AcademicYearRepository academicYearRepo;
    private final ClassSessionRepository classSessionRepo;
    private final ClassMasterRepository classMasterRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;
    private final ReferenceGeneratorUsecase rg;

    public void execute(ClassSession newSession) {
        var previousYear = academicYearRepo
                .findPreviousBySchool(newSession.getSchoolId(), newSession.getAcademicYear().getStartDate())
                .orElse(null);

        if (previousYear == null) {
            return;
        }

        var previousSession = findEquivalentSession(newSession, previousYear.getId()).orElse(null);
        if (previousSession == null) {
            return;
        }

        carryForwardClassMaster(newSession, previousSession);
        carryForwardTeacherSubjects(newSession, previousSession);
    }

    private Optional<ClassSession> findEquivalentSession(ClassSession newSession, String academicYearId) {
        var clazz = newSession.getClazz();
        var section = newSession.getSection();
        var stream = newSession.getStream();
        var schoolId = newSession.getSchoolId();

        return stream != null
                ? classSessionRepo.findByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(clazz.getId(),
                        academicYearId, section.getId(), stream.getId(), schoolId)
                : classSessionRepo.findByClassIdAndAcademicYearIdAndSectionIdAndSchoolId(clazz.getId(),
                        academicYearId, section.getId(), schoolId);
    }

    private void carryForwardClassMaster(ClassSession newSession, ClassSession previousSession) {
        if (classMasterRepo.existsByClassSessionIdAndSchoolId(newSession.getId(), newSession.getSchoolId())) {
            return;
        }

        classMasterRepo.findTopByClassSessionIdAndEndedAtIsNullOrderByAssignedAtDesc(previousSession.getId())
                .ifPresent(previousMaster -> {
                    var id = rg.generate("CLASS_MASTER", "CMR");
                    var master = ClassMaster.create(id, newSession.getSchoolId(), newSession,
                            previousMaster.getTeacher());
                    classMasterRepo.save(master);
                });
    }

    private void carryForwardTeacherSubjects(ClassSession newSession, ClassSession previousSession) {
        var previousAssignments = teacherSubjectRepo
                .findByClassSessionIdAndSchoolId(previousSession.getId(), newSession.getSchoolId(), Pageable.unpaged())
                .getContent();

        for (var assignment : previousAssignments) {
            if (assignment.getSubject() == null || assignment.getTeacher() == null) {
                continue;
            }

            boolean already = teacherSubjectRepo.existsByClassSessionIdAndSubjectIdAndSchoolId(
                    newSession.getId(), assignment.getSubject().getId(), newSession.getSchoolId());

            if (already) {
                continue;
            }

            var id = rg.generate("TEACHER_SUBJECT", "TSB");
            var carried = TeacherSubject.create(id, newSession.getSchoolId(), newSession, assignment.getTeacher(),
                    assignment.getSubject());
            teacherSubjectRepo.save(carried);
        }
    }
}
