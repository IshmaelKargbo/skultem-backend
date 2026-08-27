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
                        String academicYearId, int page, int size) {
                var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

                return list(schoolId, masterId, academicYear.getId(), status, page, size);
        }

        public Page<AssessmentApprovalRequestDTO> executeByUser(String schoolId, String userId, String status,
                        String academicYearId, int page, int size) {
                var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
                var teacher = teacherRepo.findByUserId(userId)
                                .orElseThrow(() -> new NotFoundException("Teacher not found"));

                return list(schoolId, teacher.getId(), academicYear.getId(), status, page, size);
        }

        public AssessmentApprovalSummaryDTO summary(String schoolId, String masterId) {
                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

                return summarize(masterId, academicYear.getId());
        }

        public AssessmentApprovalSummaryDTO summaryByUser(String schoolId, String userId) {
                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
                var teacher = teacherRepo.findByUserId(userId)
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

        private Page<AssessmentApprovalRequestDTO> list(String schoolId, String masterId, String academicYearId,
                        String status, int page, int size) {
                AssessmentApprovalRequest.Status parsedStatus = parseStatus(status);
                Pageable pageable = createPageable(page, size);

                Page<AssessmentApprovalRequest> requests = requestRepo.findAllByClassMasterSchoolId(masterId,
                                academicYearId, parsedStatus, pageable);

                return requests.map(this::toDTO);
        }

        private AssessmentApprovalRequest.Status parseStatus(String status) {
                if (status == null || status.isBlank()) {
                        return null;
                }

                return AssessmentApprovalRequest.Status.valueOf(status);
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
