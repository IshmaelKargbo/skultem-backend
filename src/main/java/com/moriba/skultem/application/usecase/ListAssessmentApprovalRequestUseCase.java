package com.moriba.skultem.application.usecase;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentApprovalRequestDTO;
import com.moriba.skultem.application.dto.AssessmentApprovalSummaryDTO;
import com.moriba.skultem.application.dto.AssessmentScoreDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.AssessmentScoreMapper;
import com.moriba.skultem.domain.model.AssessmentApprovalRequest;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.AssessmentApprovalRequestRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListAssessmentApprovalRequestUseCase {

        private final AssessmentApprovalRequestRepository requestRepo;
        private final AcademicYearRepository academicYearRepo;
        private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
        private final TeacherRepository teacherRepo;
        private final AssessmentScoreRepository assessmentScoreRepo;
        private final ResolveScoreGradeUseCase resolveScoreGradeUseCase;

        private static double roundTo2Dp(double value) {
                return Math.round(value * 100.0) / 100.0;
        }

        public Page<AssessmentApprovalRequestDTO> execute(String schoolId, String masterId, String status,
                        String query, String academicYearId, int page, int size) {
                var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

                return list(schoolId, masterId, academicYear.getId(), status, query, page, size);
        }

        public Page<AssessmentApprovalRequestDTO> executeByUser(String schoolId, String userId, String status,
                        String query, String academicYearId, int page, int size) {
                var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
                // findByUserId (not scoped by school) throws IncorrectResultSizeDataAccessException for a
                // teacher who works at more than one school - findByUserIdAndSchoolId resolves the one for
                // the school they're currently acting in, same fix as elsewhere this bug turned up.
                var teacher = teacherRepo.findByUserIdAndSchoolId(userId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Teacher not found"));

                return list(schoolId, teacher.getId(), academicYear.getId(), status, query, page, size);
        }

        // School-wide, unlike execute()/executeByUser() above (both scoped to one class master) -
        // the admin approval view's default list, so an admin sees everything pending across the
        // school without first having to know which teacher/class to check.
        public Page<AssessmentApprovalRequestDTO> executeForSchool(String schoolId, String status, String query,
                        String academicYearId, int page, int size) {
                var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
                AssessmentApprovalRequest.Status parsedStatus = parseStatus(status);
                Pageable pageable = createPageable(page, size);

                Page<AssessmentApprovalRequest> requests = requestRepo.findAllBySchool(schoolId,
                                academicYear.getId(), parsedStatus, normalizeQuery(query), pageable);

                return requests.map(this::toDTO);
        }

        public AssessmentApprovalSummaryDTO summaryForSchool(String schoolId) {
                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

                long pending = requestRepo.countBySchoolAndStatus(schoolId, academicYear.getId(),
                                AssessmentApprovalRequest.Status.PENDING_REVIEW);
                long approved = requestRepo.countBySchoolAndStatus(schoolId, academicYear.getId(),
                                AssessmentApprovalRequest.Status.APPROVED);
                long returned = requestRepo.countBySchoolAndStatus(schoolId, academicYear.getId(),
                                AssessmentApprovalRequest.Status.RETURNED);

                return new AssessmentApprovalSummaryDTO(pending, approved, returned);
        }

        public AssessmentApprovalSummaryDTO summary(String schoolId, String masterId) {
                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

                return summarize(masterId, academicYear.getId());
        }

        public AssessmentApprovalSummaryDTO summaryByUser(String schoolId, String userId) {
                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
                var teacher = teacherRepo.findByUserIdAndSchoolId(userId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Teacher not found"));

                return summarize(teacher.getId(), academicYear.getId());
        }

        private AssessmentApprovalSummaryDTO summarize(String masterId, String academicYearId) {
                long pending = requestRepo.countByClassMasterSchoolIdAndStatus(masterId, academicYearId,
                                AssessmentApprovalRequest.Status.PENDING_REVIEW);
                long approved = requestRepo.countByClassMasterSchoolIdAndStatus(masterId, academicYearId,
                                AssessmentApprovalRequest.Status.APPROVED);
                long returned = requestRepo.countByClassMasterSchoolIdAndStatus(masterId, academicYearId,
                                AssessmentApprovalRequest.Status.RETURNED);

                return new AssessmentApprovalSummaryDTO(pending, approved, returned);
        }

        // Powers the standalone approval-detail page (grades/approval/[id]) - same access as
        // approve/return below it (school-scoped only), so a direct link/refresh works exactly
        // like the list it was opened from.
        public AssessmentApprovalRequestDTO getOne(String schoolId, String approvalRequestId) {
                var request = requestRepo.findByIdAndSchoolId(approvalRequestId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Approval request not found"));

                return toDTO(request);
        }

        private Page<AssessmentApprovalRequestDTO> list(String schoolId, String masterId, String academicYearId,
                        String status, String query, int page, int size) {
                AssessmentApprovalRequest.Status parsedStatus = parseStatus(status);
                Pageable pageable = createPageable(page, size);

                Page<AssessmentApprovalRequest> requests = requestRepo.findAllByClassMasterSchoolId(masterId,
                                academicYearId, parsedStatus, normalizeQuery(query), pageable);

                return requests.map(this::toDTO);
        }

        private AssessmentApprovalRequest.Status parseStatus(String status) {
                if (status == null || status.isBlank()) {
                        return null;
                }

                return AssessmentApprovalRequest.Status.valueOf(status);
        }

        // Empty string, never null - a null String bound into a JPQL "lower(concat('%', :query, '%'))"
        // (see AssessmentApprovalRequestJpaRepository) leaves Postgres/the JDBC driver unable to infer
        // the parameter's type from context, and it falls back to bytea - "function lower(bytea) does
        // not exist". The repository's guard checks for "" instead of NULL for the same reason.
        private String normalizeQuery(String query) {
                return (query == null || query.isBlank()) ? "" : query.trim();
        }

        private AssessmentApprovalRequestDTO toDTO(AssessmentApprovalRequest r) {
                List<AssessmentScore> scores = assessmentScoreRepo.findAllByCycle(r.getCycle().getId());

                List<AssessmentScoreDTO> scoreDTOs = scores.stream()
                                .map(s -> {
                                        String grade = resolveScoreGradeUseCase.execute(schoolIdOf(r), s.getScore());
                                        return AssessmentScoreMapper.toDTO(s, grade);
                                }).collect(Collectors.toList());

                int totalStudents = scores.size();

                long passCount = scores.stream().filter(s -> s.getScore() >= 50).count();
                long averageCount = scores.stream().filter(s -> s.getScore() >= 40 && s.getScore() < 50).count();
                long failCount = scores.stream().filter(s -> s.getScore() < 40).count();

                double pass = totalStudents > 0 ? (passCount * 100.0) / totalStudents : 0;
                double average = totalStudents > 0 ? (averageCount * 100.0) / totalStudents : 0;
                double fail = totalStudents > 0 ? (failCount * 100.0) / totalStudents : 0;

                double avgScore = scores.stream()
                                .mapToDouble(a -> a.getScore())
                                .average()
                                .orElse(0);

                String statusLabel = switch (r.getStatus()) {
                        case PENDING_REVIEW -> "Pending Review";
                        case RETURNED -> "Returned";
                        case APPROVED -> "Approved";
                };

                var teacher = r.getTeacherSubject().getTeacher().getUser();
                var subject = r.getTeacherSubject().getSubject();

                return new AssessmentApprovalRequestDTO(
                                r.getId(),
                                teacher.getName(),
                                subject.getName(),
                                r.getCycle().getAssessment().getName(),
                                r.getCycle().getTerm().getName(),
                                r.getCycle().getSubject().getSession().getClazz().getName(),
                                totalStudents,
                                passCount,
                                roundTo2Dp(pass),
                                failCount,
                                roundTo2Dp(fail),
                                averageCount,
                                roundTo2Dp(average),
                                roundTo2Dp(avgScore),
                                r.getTeacherNote(),
                                statusLabel,
                                scoreDTOs,
                                r.getTeacherSubject().getId(),
                                r.getCycle().getAssessment().getId(),
                                r.getCycle().getTerm().getId());
        }

        private String schoolIdOf(AssessmentApprovalRequest r) {
                return r.getSchoolId();
        }

        private Pageable createPageable(int page, int size) {

                if (size <= 0) {
                        return Pageable.unpaged();
                }

                int pageNumber = Math.max(page - 1, 0);

                return PageRequest.of(pageNumber, size);
        }
}
