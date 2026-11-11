package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moriba.skultem.domain.model.ExternalExam;
import com.moriba.skultem.domain.model.ExternalExamCandidate;
import com.moriba.skultem.domain.model.ExternalExamSitting;
import com.moriba.skultem.domain.repository.ExternalExamRepository;
import com.moriba.skultem.domain.vo.ExamGrade;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.infrastructure.persistence.entity.ExternalExamCandidateEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ExternalExamEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ExternalExamSittingEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.ExternalExamCandidateJpaRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ExternalExamJpaRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ExternalExamSittingJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.JsonMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ExternalExamAdapter implements ExternalExamRepository {
    private final ExternalExamJpaRepository exams;
    private final ExternalExamSittingJpaRepository sittings;
    private final ExternalExamCandidateJpaRepository candidates;

    // ---- Exams

    @Override
    public void save(ExternalExam e) {
        exams.save(ExternalExamEntity.builder().id(e.getId()).schoolId(e.getSchoolId()).code(e.getCode())
                .name(e.getName()).examBody(e.getExamBody()).level(e.getLevel() == null ? null : e.getLevel().name())
                .gradeScale(JsonMapper.toJson(e.getGradeScale())).aggregateMode(e.getAggregateMode().name())
                .bestOf(e.getBestOf()).builtIn(e.isBuiltIn()).entryMode(e.getEntryMode().name()).slipTemplate(e.getSlipTemplate())
                .statementTemplate(e.getStatementTemplate()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build());
    }

    @Override
    public List<ExternalExam> findAllExams(String schoolId) {
        return exams.findAllBySchoolIdOrderByNameAsc(schoolId).stream().map(ExternalExamAdapter::toDomain).toList();
    }

    @Override
    public Optional<ExternalExam> findExam(String id, String schoolId) {
        return exams.findByIdAndSchoolId(id, schoolId).map(ExternalExamAdapter::toDomain);
    }

    @Override
    public boolean examCodeExists(String schoolId, String code) {
        return exams.existsBySchoolIdAndCode(schoolId, code);
    }

    @Override
    public void deleteExam(String id) {
        exams.deleteById(id);
    }

    private static ExternalExam toDomain(ExternalExamEntity e) {
        List<ExamGrade> scale = JsonMapper.fromJsonList(e.getGradeScale(), ExamGrade.class);
        return new ExternalExam(e.getId(), e.getSchoolId(), e.getCode(), e.getName(), e.getExamBody(),
                e.getLevel() == null ? null : Level.valueOf(e.getLevel()), scale,
                ExternalExam.AggregateMode.valueOf(e.getAggregateMode()), e.getBestOf(), e.isBuiltIn(),
                e.getCreatedAt(), e.getUpdatedAt()).withDesigns(e.getSlipTemplate(), e.getStatementTemplate())
                .withEntryMode(e.getEntryMode() == null ? null : ExternalExam.EntryMode.valueOf(e.getEntryMode()));
    }

    // ---- Sittings

    @Override
    public void save(ExternalExamSitting s) {
        sittings.save(ExternalExamSittingEntity.builder().id(s.getId()).schoolId(s.getSchoolId())
                .examId(s.getExamId()).classId(s.getClassId()).academicYearId(s.getAcademicYearId())
                .centreNumber(s.getCentreNumber()).subjects(JsonMapper.toJson(s.getSubjects()))
                .status(s.getStatus().name()).confirmedByName(s.getConfirmedByName())
                .confirmedAt(s.getConfirmedAt()).createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt()).build());
    }

    @Override
    public List<ExternalExamSitting> findAllSittings(String schoolId) {
        return sittings.findAllBySchoolIdOrderByCreatedAtDesc(schoolId).stream().map(ExternalExamAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<ExternalExamSitting> findSitting(String id, String schoolId) {
        return sittings.findByIdAndSchoolId(id, schoolId).map(ExternalExamAdapter::toDomain);
    }

    @Override
    public boolean sittingExists(String schoolId, String examId, String classId, String academicYearId) {
        return sittings.existsBySchoolIdAndExamIdAndClassIdAndAcademicYearId(schoolId, examId, classId,
                academicYearId);
    }

    @Override
    public boolean examHasSittings(String examId) {
        return sittings.existsByExamId(examId);
    }

    @Override
    public void deleteSitting(String id) {
        sittings.deleteById(id);
    }

    private static ExternalExamSitting toDomain(ExternalExamSittingEntity e) {
        var subjects = JsonMapper.fromJsonList(e.getSubjects(), ExternalExamSitting.ExamSubject.class);
        return new ExternalExamSitting(e.getId(), e.getSchoolId(), e.getExamId(), e.getClassId(),
                e.getAcademicYearId(), e.getCentreNumber(), subjects,
                ExternalExamSitting.Status.valueOf(e.getStatus()), e.getConfirmedByName(), e.getConfirmedAt(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    // ---- Candidates

    @Override
    public void saveAll(List<ExternalExamCandidate> list) {
        candidates.saveAll(list.stream().map(c -> ExternalExamCandidateEntity.builder().id(c.getId())
                .schoolId(c.getSchoolId()).sittingId(c.getSittingId()).studentId(c.getStudentId())
                .studentName(c.getStudentName()).admissionNumber(c.getAdmissionNumber())
                .indexNumber(c.getIndexNumber()).scores(JsonMapper.toJson(c.getScores()))
                .grades(JsonMapper.toJson(c.getGrades()))
                .createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build()).toList());
    }

    @Override
    public List<ExternalExamCandidate> findCandidates(String sittingId) {
        return candidates.findAllBySittingIdOrderByStudentNameAsc(sittingId).stream()
                .map(ExternalExamAdapter::toDomain).toList();
    }

    @Override
    public void deleteCandidates(List<String> ids) {
        if (!ids.isEmpty()) {
            candidates.deleteAllById(ids);
        }
    }

    private static ExternalExamCandidate toDomain(ExternalExamCandidateEntity e) {
        Map<String, Double> scores = e.getScores() == null || e.getScores().isBlank() ? new LinkedHashMap<>()
                : JsonMapper.fromJson(e.getScores(), new TypeReference<LinkedHashMap<String, Double>>() {
                });
        Map<String, String> grades = e.getGrades() == null || e.getGrades().isBlank() ? new LinkedHashMap<>()
                : JsonMapper.fromJson(e.getGrades(), new TypeReference<LinkedHashMap<String, String>>() {
                });
        return new ExternalExamCandidate(e.getId(), e.getSchoolId(), e.getSittingId(), e.getStudentId(),
                e.getStudentName(), e.getAdmissionNumber(), e.getIndexNumber(), scores, e.getCreatedAt(),
                e.getUpdatedAt()).withGrades(grades);
    }
}
