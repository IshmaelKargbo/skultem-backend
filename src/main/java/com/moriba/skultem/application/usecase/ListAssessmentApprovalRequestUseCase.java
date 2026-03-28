package com.moriba.skultem.application.usecase;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentApprovalRequestDTO;
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
        private final TeacherRepository teacherRepo;
        private final AssessmentScoreRepository assessmentScoreRepo;
        private final ResolveScoreGradeUseCase resolveScoreGradeUseCase;

        private static double roundTo2Dp(double value) {
                return Math.round(value * 100.0) / 100.0;
        }

        public List<AssessmentApprovalRequestDTO> execute(String schoolId, String masterId) {
                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
                List<AssessmentApprovalRequest> requests = requestRepo
                                .findAllByClassMasterSchoolId(masterId, academicYear.getId());

                return requests.stream().map(r -> {

                        List<AssessmentScore> scores = assessmentScoreRepo.findAllByCycle(r.getCycle().getId());

                        List<AssessmentScoreDTO> scoreDTOs = scores.stream()
                                        .map(s -> {
                                                String grade = resolveScoreGradeUseCase.execute(schoolId, s.getScore(),
                                                                s.getStatus());
                                                return AssessmentScoreMapper.toDTO(s, grade);
                                        }).collect(Collectors.toList());

                        int totalStudents = scores.size();

                        long passCount = scores.stream().filter(s -> s.getScore() >= 50).count();
                        long averageCount = scores.stream().filter(s -> s.getScore() >= 40 && s.getScore() < 50)
                                        .count();
                        long failCount = scores.stream().filter(s -> s.getScore() < 40).count();

                        double pass = totalStudents > 0 ? (passCount * 100.0) / totalStudents : 0;
                        double average = totalStudents > 0 ? (averageCount * 100.0) / totalStudents : 0;
                        double fail = totalStudents > 0 ? (failCount * 100.0) / totalStudents : 0;

                        double avgScore = scores.stream()
                                        .mapToDouble(AssessmentScore::getScore)
                                        .average()
                                        .orElse(0);

                        String status = switch (r.getStatus()) {
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
                                        status,
                                        scoreDTOs);

                }).collect(Collectors.toList());
        }

        public List<AssessmentApprovalRequestDTO> executeByUser(String schoolId, String userId) {
                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
                var teacher = teacherRepo.findByUserId(userId)
                                .orElseThrow(() -> new NotFoundException("Teacher not found"));
                List<AssessmentApprovalRequest> requests = requestRepo
                                .findAllByClassMasterSchoolId(teacher.getId(), academicYear.getId());

                return requests.stream().map(r -> {

                        List<AssessmentScore> scores = assessmentScoreRepo.findAllByCycle(r.getCycle().getId());

                        List<AssessmentScoreDTO> scoreDTOs = scores.stream()
                                        .map(s -> {
                                                String grade = resolveScoreGradeUseCase.execute(schoolId, s.getScore(),
                                                                s.getStatus());
                                                return AssessmentScoreMapper.toDTO(s, grade);
                                        }).collect(Collectors.toList());

                        int totalStudents = scores.size();

                        long passCount = scores.stream().filter(s -> s.getScore() >= 50).count();
                        long averageCount = scores.stream().filter(s -> s.getScore() >= 40 && s.getScore() < 50)
                                        .count();
                        long failCount = scores.stream().filter(s -> s.getScore() < 40).count();

                        double pass = totalStudents > 0 ? (passCount * 100.0) / totalStudents : 0;
                        double average = totalStudents > 0 ? (averageCount * 100.0) / totalStudents : 0;
                        double fail = totalStudents > 0 ? (failCount * 100.0) / totalStudents : 0;

                        double avgScore = scores.stream()
                                        .mapToDouble(AssessmentScore::getScore)
                                        .average()
                                        .orElse(0);

                        String status = switch (r.getStatus()) {
                                case PENDING_REVIEW -> "Pending Review";
                                case RETURNED -> "Returned";
                                case APPROVED -> "Approved";
                        };

                        var user = r.getTeacherSubject().getTeacher().getUser();
                        var subject = r.getTeacherSubject().getSubject();

                        return new AssessmentApprovalRequestDTO(
                                        r.getId(),
                                        user.getName(),
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
                                        status,
                                        scoreDTOs);

                }).collect(Collectors.toList());
        }
}
