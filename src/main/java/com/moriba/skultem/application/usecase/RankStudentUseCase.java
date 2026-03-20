package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RankStudentUseCase {

    private final StudentRepository studentRepo;
    private final TermRepository termRepo;
    private final AssessmentScoreRepository assessmentScoreRepo;

    public int execute(String studentId, String termId, String schoolId) {

        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        termRepo.findByIdAndSchoolId(termId, schoolId)
                .orElseThrow(() -> new NotFoundException("Term not found"));

        var clazzId = student.getSession().getClazz().getId();

        Integer rank = assessmentScoreRepo.getStudentRank(
                schoolId,
                clazzId,
                termId,
                studentId
        );

        if (rank == null) {
            return 0;
        }

        return rank;
    }
}