package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.ExternalExam;
import com.moriba.skultem.domain.model.ExternalExamCandidate;
import com.moriba.skultem.domain.model.ExternalExamSitting;

// External exams, their sittings and candidates (see ExternalExamSitting).
public interface ExternalExamRepository {
    void save(ExternalExam exam);

    List<ExternalExam> findAllExams(String schoolId);

    Optional<ExternalExam> findExam(String id, String schoolId);

    boolean examCodeExists(String schoolId, String code);

    void deleteExam(String id);

    void save(ExternalExamSitting sitting);

    List<ExternalExamSitting> findAllSittings(String schoolId);

    Optional<ExternalExamSitting> findSitting(String id, String schoolId);

    boolean sittingExists(String schoolId, String examId, String classId, String academicYearId);

    boolean examHasSittings(String examId);

    void deleteSitting(String id);

    void saveAll(List<ExternalExamCandidate> candidates);

    List<ExternalExamCandidate> findCandidates(String sittingId);

    void deleteCandidates(List<String> ids);
}
