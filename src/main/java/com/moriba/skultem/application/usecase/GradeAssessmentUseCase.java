package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.StudentAssessment;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.infrastructure.security.PermissionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GradeAssessmentUseCase {

    private final TeacherSubjectRepository teacherSubjectRepo;
    private final AssessmentScoreRepository assessmentScoreRepo;
    private final ClassSubjectRepository classSubjectRepo;
    private final StudentAssessmentRepository studentAssessmentRepo;

    public void execute(
            String schoolId,
            String teacherSubjectId,
            String assessmentId,
            String termId,
            List<Grade> grades) {

        var ts = teacherSubjectRepo
                .findByIdAndSchoolId(teacherSubjectId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher subject not found"));

        var session = ts.getSession();
        var clazz = session.getClazz();

        lockSubject(schoolId, ts.getSubject().getId(), clazz.getId(), clazz.getLevel(), session.getStream().getId());

        List<StudentAssessment> studentAssessments = studentAssessmentRepo
                .findAllBySubjectAndSessionAndTermId(ts.getSubject().getId(), ts.getSession().getId(), termId);

        if (studentAssessments.isEmpty()) {
            throw new NotFoundException("No student assessments found");
        }

        var gradeMap = grades.stream()
                .collect(Collectors.toMap(a -> a.id(), b -> b.score()));

        String gradedByUserId = PermissionService.getCurrentUser().userId();
        List<AssessmentScore> scoresToUpdate = new ArrayList<>();

        for (StudentAssessment sa : studentAssessments) {

            var scores = assessmentScoreRepo
                    .findAllByStudentAssessmentIdAndAssessmentId(
                            sa.getId(),
                            assessmentId);

            if (!scores.isEmpty()) {
                for (AssessmentScore score : scores) {

                    if (!score.canMark()) {
                        throw new IllegalStateException("Assessment score not in DRAFT state");
                    }

                    Integer newScore = gradeMap.get(score.getId());

                    if (newScore == null) {
                        throw new IllegalStateException(
                                "Missing grade for score id: " + score.getId());
                    }

                    score.updateScore(newScore, gradedByUserId);
                    scoresToUpdate.add(score);
                }
            }
        }

        assessmentScoreRepo.saveAll(scoresToUpdate);
    }

    private void lockSubject(String schoolId, String subjectId, String classId, Level level, String streamId) {
        if (streamId == null) {
            var subject = classSubjectRepo.findByClassIdAndSubjectId(classId, subjectId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Class subject not found"));
            subject.lock();
            classSubjectRepo.save(subject);
        } else {
            var subject = classSubjectRepo.findByClassIdAndSubjectIdAndStramId(classId, subjectId, streamId)
                    .orElseThrow(() -> new NotFoundException("Class subject not found"));
            subject.lock();
            classSubjectRepo.save(subject);
        }
    }

    public record Grade(String id, Integer score) {
    }
}
