package com.moriba.skultem.application.usecase;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherClassMasterDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.PromotionRequest;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.PromotionRequestRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * A teacher's current class-master assignment(s) - fuels the "My Class" section of their
 * dashboard, including whether their class is ready to promote / awaiting review / done.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ListMyClassMasterAssignmentsUseCase {

    private final TeacherRepository teacherRepo;
    private final ClassMasterRepository classMasterRepo;
    private final AcademicYearRepository academicYearRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final PromotionRequestRepository promotionRequestRepo;
    private final ValidateAcademicYearTermsUseCase validateAcademicYearTermsUseCase;
    private final ValidateClassAssessmentsCompletedUseCase validateClassAssessmentsCompletedUseCase;
    private final ValidateNextAcademicYearUseCase validateNextAcademicYearUseCase;

    public List<TeacherClassMasterDTO> execute(String schoolId, String userId) {
        var teacher = teacherRepo.findByUserIdAndSchoolId(userId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher not found"));

        var activeYear = academicYearRepo.findActiveBySchool(schoolId).orElse(null);
        if (activeYear == null) {
            return List.of();
        }

        var assignments = classMasterRepo
                .findByTeacherAndAcademicYear(teacher.getId(), activeYear.getId(), Pageable.unpaged())
                .getContent();

        if (assignments.isEmpty()) {
            return List.of();
        }

        BinaryOperator<PromotionRequest> keepLatest = (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b;
        Map<String, PromotionRequest> requestsBySession = promotionRequestRepo
                .findByAcademicYearIdAndSchoolId(activeYear.getId(), schoolId).stream()
                .collect(Collectors.toMap(r -> r.getSession().getId(), r -> r, keepLatest));

        return assignments.stream()
                .sorted(Comparator.comparing(m -> m.getSession().getName()))
                .map(master -> {
                    var session = master.getSession();
                    var clazz = session.getClazz();
                    var section = session.getSection();
                    var stream = session.getStream();

                    var activeEnrollments = enrollmentRepo
                            .findActiveByClassIdAndSectionIdAndStreamIdAndAcademicYearIdAndSchoolId(clazz.getId(),
                                    section.getId(), stream != null ? stream.getId() : null, activeYear.getId(),
                                    schoolId);

                    var request = requestsBySession.get(session.getId());
                    String promotionStatus = request != null ? request.getStatus().name() : null;

                    if (promotionStatus == null && !activeEnrollments.isEmpty()) {
                        try {
                            // A terminal class's PROMOTE outcome graduates the student and needs no next
                            // year - every other class does, so the school must have one assigned before
                            // its class master ever sees a "Promote" button.
                            if (!Boolean.TRUE.equals(clazz.getTerminal())) {
                                validateNextAcademicYearUseCase.execute(schoolId, activeYear.getId());
                            }

                            validateAcademicYearTermsUseCase.execute(schoolId, activeYear.getId());
                            validateClassAssessmentsCompletedUseCase.execute(schoolId, clazz.getId(),
                                    activeYear.getId());

                            if (Boolean.TRUE.equals(clazz.getTerminal()) || clazz.getNextClass() != null) {
                                promotionStatus = "READY";
                            }
                        } catch (RuleException e) {
                            // Not ready yet - leave status null, nothing actionable for the dashboard to show.
                        }
                    }

                    return new TeacherClassMasterDTO(master.getId(), session.getId(), clazz.getId(),
                            session.getName(), clazz.getName(), activeEnrollments.size(), promotionStatus);
                })
                .toList();
    }
}
