package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.ExternalExamDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.ExternalExamDefaults;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.ExternalExam;
import com.moriba.skultem.domain.model.ExternalExamCandidate;
import com.moriba.skultem.domain.model.ExternalExamSitting;
import com.moriba.skultem.domain.model.ExternalExamSitting.ExamSubject;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.ExternalExamRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.service.ExternalExamResults;
import com.moriba.skultem.domain.vo.Level;

import lombok.RequiredArgsConstructor;

/**
 * External exams (NPSE, BECE, WASSCE, ...): the school says which class sits which exam in a year; the class's
 * students become its candidates and the exam's subjects its columns; staff enter each candidate's index number
 * and scores; confirming locks them, and result slips and statements are printed from M-Compose designs
 * (RenderExternalExamDocumentUseCase).
 */
@Service
@RequiredArgsConstructor
public class ExternalExamUseCase {

    private final ExternalExamRepository repo;
    private final ClassRepository classRepo;
    private final ClassSubjectRepository classSubjectRepo;
    private final AcademicYearRepository academicYearRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;

    // ---- Exams

    /** The school's exams, the built-in ones made the first time they are asked for. */
    @Transactional
    public List<ExternalExamDTO.Exam> listExams(String schoolId) {
        return exams(schoolId).stream().map(ExternalExamUseCase::toDto).toList();
    }

    private List<ExternalExam> exams(String schoolId) {
        var existing = repo.findAllExams(schoolId);
        Set<String> codes = existing.stream().map(ExternalExam::getCode).collect(Collectors.toSet());
        boolean added = false;
        for (var d : ExternalExamDefaults.all()) {
            if (!codes.contains(d.code())) {
                repo.save(ExternalExam.create(UUID.randomUUID().toString(), schoolId, d.code(), d.name(),
                        d.examBody(), d.level(), d.scale(), d.mode(), d.bestOf(), true)
                        .withEntryMode(ExternalExamDefaults.entryMode(d.code())));
                added = true;
            }
        }
        return added ? repo.findAllExams(schoolId) : existing;
    }

    @Transactional
    public ExternalExamDTO.Exam createExam(String schoolId, ExternalExamDTO.SaveExam req) {
        String code = req.code() == null ? "" : req.code().trim().toUpperCase().replaceAll("[^A-Z0-9]+", "-");
        exams(schoolId);
        if (repo.examCodeExists(schoolId, code)) {
            throw new AlreadyExistsException("There is already an exam with the code " + code);
        }
        var exam = ExternalExam.create(UUID.randomUUID().toString(), schoolId, req.code(), req.name(),
                req.examBody(), level(req.level()), req.gradeScale(), mode(req.aggregateMode()),
                req.bestOf() == null ? 0 : req.bestOf(), false).withEntryMode(entryMode(req.entryMode(), null));
        repo.save(exam);
        return toDto(exam);
    }

    @Transactional
    public ExternalExamDTO.Exam updateExam(String schoolId, String id, ExternalExamDTO.SaveExam req) {
        var exam = exam(schoolId, id);
        exam.update(req.name(), req.examBody(), level(req.level()), req.gradeScale(), mode(req.aggregateMode()),
                req.bestOf() == null ? exam.getBestOf() : req.bestOf());
        exam.setEntryMode(entryMode(req.entryMode(), exam.getEntryMode()));
        repo.save(exam);
        return toDto(exam);
    }

    @Transactional
    public void deleteExam(String schoolId, String id) {
        var exam = exam(schoolId, id);
        if (exam.isBuiltIn()) {
            throw new RuleException("Built-in exams can't be deleted. Edit its grade scale instead.");
        }
        if (repo.examHasSittings(id)) {
            throw new RuleException("Classes have sat this exam. Delete their results first.");
        }
        repo.deleteExam(id);
    }

    /** Saves an exam's own design for its slip or statement (blank = use the school's general one). */
    @Transactional
    public ExternalExam setExamDesign(String schoolId, String examId, boolean statement, String template) {
        var exam = exam(schoolId, examId);
        exam.setDesign(statement, template);
        repo.save(exam);
        return exam;
    }

    // ---- Sittings

    /** The subjects a class takes (each once, whatever its streams), to choose the exam's subjects from. */
    public List<ExternalExamDTO.SubjectOption> classSubjects(String schoolId, String classId) {
        clazz(schoolId, classId);
        Map<String, ExternalExamDTO.SubjectOption> byId = new LinkedHashMap<>();
        for (var cs : classSubjectRepo.findAllByClassIdAndSchoolId(classId, schoolId, Pageable.unpaged())) {
            if (cs.getSubject() != null) {
                byId.putIfAbsent(cs.getSubject().getId(),
                        new ExternalExamDTO.SubjectOption(cs.getSubject().getId(), cs.getSubject().getName()));
            }
        }
        return byId.values().stream().sorted((a, b) -> a.name().compareToIgnoreCase(b.name())).toList();
    }

    @Transactional
    public List<ExternalExamDTO.SittingSummary> listSittings(String schoolId, java.util.Collection<Level> levels) {
        var exams = exams(schoolId).stream().collect(Collectors.toMap(ExternalExam::getId, Function.identity()));
        List<ExternalExamDTO.SittingSummary> out = new ArrayList<>();
        for (var s : repo.findAllSittings(schoolId)) {
            var clazz = classRepo.findByIdAndSchool(s.getClassId(), schoolId).orElse(null);
            if (levels != null && clazz != null && !levels.contains(clazz.getLevel())) {
                continue;
            }
            out.add(summary(s, exams.get(s.getExamId()), clazz, repo.findCandidates(s.getId()), schoolId));
        }
        return out;
    }

    @Transactional
    public ExternalExamDTO.Sitting createSitting(String schoolId, ExternalExamDTO.CreateSitting req) {
        var exam = exam(schoolId, req.examId());
        var clazz = clazz(schoolId, req.classId());
        AcademicYear year = req.academicYearId() == null || req.academicYearId().isBlank()
                ? academicYearRepo.findActiveBySchool(schoolId)
                        .orElseThrow(() -> new RuleException("Choose the academic year - there is no active one"))
                : academicYearRepo.findByIdAndSchoolId(req.academicYearId(), schoolId)
                        .orElseThrow(() -> new NotFoundException("Academic year not found"));
        if (repo.sittingExists(schoolId, exam.getId(), clazz.getId(), year.getId())) {
            throw new AlreadyExistsException(clazz.getName() + " is already sitting " + exam.getCode() + " in "
                    + year.getName());
        }
        var sitting = ExternalExamSitting.create(UUID.randomUUID().toString(), schoolId, exam.getId(),
                clazz.getId(), year.getId(), req.centreNumber(), subjects(schoolId, clazz.getId(), req.subjectIds()));
        repo.save(sitting);
        syncCandidates(sitting, List.of());
        return sitting(schoolId, sitting.getId());
    }

    @Transactional(readOnly = true)
    public ExternalExamDTO.Sitting sitting(String schoolId, String id) {
        var s = findSitting(schoolId, id);
        var exam = exam(schoolId, s.getExamId());
        var clazz = classRepo.findByIdAndSchool(s.getClassId(), schoolId).orElse(null);
        var candidates = repo.findCandidates(s.getId());
        var rows = candidates.stream().map(c -> {
            var r = ExternalExamResults.of(exam, s.getSubjects(), c.getScores(), c.getGrades());
            return new ExternalExamDTO.Candidate(c.getId(), c.getStudentId(), c.getStudentName(),
                    c.getAdmissionNumber(), c.getIndexNumber(), c.getScores(), c.getGrades(),
                    r.subjects().stream().map(x -> new ExternalExamDTO.SubjectResult(x.subjectId(), x.subject(),
                            x.score(), x.grade(), x.points(), x.remark())).toList(),
                    r.subjectsSat(), r.aggregate());
        }).toList();
        String label = ExternalExamResults.of(exam, s.getSubjects(), Map.of()).aggregateLabel();
        return new ExternalExamDTO.Sitting(summary(s, exam, clazz, candidates, schoolId), toDto(exam),
                s.getSubjects().stream().map(x -> new ExternalExamDTO.SubjectOption(x.subjectId(), x.name())).toList(),
                label, rows);
    }

    @Transactional
    public ExternalExamDTO.Sitting updateSitting(String schoolId, String id, ExternalExamDTO.UpdateSitting req) {
        var s = findSitting(schoolId, id);
        var subjects = subjects(schoolId, s.getClassId(), req.subjectIds());
        s.edit(req.centreNumber(), subjects);
        repo.save(s);
        // Scores of a subject taken off the exam go with it.
        Set<String> ids = subjects.stream().map(ExamSubject::subjectId).collect(Collectors.toSet());
        var candidates = repo.findCandidates(id);
        for (var c : candidates) {
            var kept = new LinkedHashMap<String, Double>(c.getScores());
            kept.keySet().retainAll(ids);
            var keptGrades = new LinkedHashMap<String, String>(c.getGrades());
            keptGrades.keySet().retainAll(ids);
            c.record(c.getIndexNumber(), kept, ids);
            c.withGrades(keptGrades);
        }
        repo.saveAll(candidates);
        return sitting(schoolId, id);
    }

    /** Adds students enrolled in the class since the sitting was made, and updates names. Nobody is removed. */
    @Transactional
    public ExternalExamDTO.Sitting refreshCandidates(String schoolId, String id) {
        var s = findSitting(schoolId, id);
        s.requireDraft();
        syncCandidates(s, repo.findCandidates(id));
        return sitting(schoolId, id);
    }

    @Transactional
    public ExternalExamDTO.Sitting removeCandidate(String schoolId, String id, String candidateId) {
        var s = findSitting(schoolId, id);
        s.requireDraft();
        boolean found = repo.findCandidates(id).stream().anyMatch(c -> c.getId().equals(candidateId));
        if (!found) {
            throw new NotFoundException("Candidate not found");
        }
        repo.deleteCandidates(List.of(candidateId));
        return sitting(schoolId, id);
    }

    @Transactional
    public ExternalExamDTO.Sitting saveScores(String schoolId, String id, ExternalExamDTO.SaveScores req) {
        var s = findSitting(schoolId, id);
        s.requireDraft();
        var exam = exam(schoolId, s.getExamId());
        Set<String> subjectIds = s.getSubjects().stream().map(ExamSubject::subjectId).collect(Collectors.toSet());
        var byId = repo.findCandidates(id).stream()
                .collect(Collectors.toMap(ExternalExamCandidate::getId, Function.identity()));
        List<ExternalExamCandidate> changed = new ArrayList<>();
        if (req.candidates() != null) {
            for (var row : req.candidates()) {
                var c = byId.get(row.candidateId());
                if (c == null) {
                    throw new NotFoundException("A candidate in the list is not part of this exam");
                }
                if (exam.gradesEntered()) {
                    c.recordGrades(row.indexNumber(), row.grades(), subjectIds,
                            g -> ExternalExamResults.ABSENT.equalsIgnoreCase(g) ? ExternalExamResults.ABSENT
                                    : exam.band(g) == null ? null : exam.band(g).grade());
                } else {
                    c.record(row.indexNumber(), row.scores(), subjectIds);
                }
                changed.add(c);
            }
        }
        // Two candidates can't share an index number.
        Map<String, String> owner = new LinkedHashMap<>();
        for (var c : byId.values()) {
            if (c.getIndexNumber() == null) {
                continue;
            }
            String other = owner.putIfAbsent(c.getIndexNumber().toUpperCase(), c.getStudentName());
            if (other != null) {
                throw new RuleException("Index number " + c.getIndexNumber() + " is given to both " + other
                        + " and " + c.getStudentName());
            }
        }
        repo.saveAll(changed);
        return sitting(schoolId, id);
    }

    /** Locks the results so they can be printed. Every candidate needs an index number by now. */
    @Transactional
    public ExternalExamDTO.Sitting confirm(String schoolId, String id, String userId) {
        var s = findSitting(schoolId, id);
        var candidates = repo.findCandidates(id);
        if (candidates.isEmpty()) {
            throw new RuleException("This exam has no candidates");
        }
        long missing = candidates.stream().filter(c -> c.getIndexNumber() == null).count();
        if (missing > 0) {
            throw new RuleException(missing + (missing == 1 ? " candidate has" : " candidates have")
                    + " no index number yet");
        }
        var exam = exam(schoolId, s.getExamId());
        String what = exam.gradesEntered() ? "grades" : "scores";
        long noScores = candidates.stream().filter(c -> !c.hasResults()).count();
        if (noScores > 0) {
            throw new RuleException(noScores + (noScores == 1 ? " candidate has" : " candidates have")
                    + " no " + what + " at all. Enter them, or remove the candidate if they did not sit.");
        }
        s.confirm(userRepo.findById(userId).map(User::getName).orElse(null));
        repo.save(s);
        return sitting(schoolId, id);
    }

    @Transactional
    public ExternalExamDTO.Sitting reopen(String schoolId, String id) {
        var s = findSitting(schoolId, id);
        s.reopen();
        repo.save(s);
        return sitting(schoolId, id);
    }

    @Transactional
    public void deleteSitting(String schoolId, String id) {
        var s = findSitting(schoolId, id);
        if (s.isConfirmed()) {
            throw new RuleException("Confirmed results can't be deleted. Reopen them first.");
        }
        repo.deleteSitting(id);
    }

    // ---- Shared with the documents

    public ExternalExamSitting findSitting(String schoolId, String id) {
        return repo.findSitting(id, schoolId).orElseThrow(() -> new NotFoundException("Exam results not found"));
    }

    public ExternalExam exam(String schoolId, String id) {
        return repo.findExam(id, schoolId).orElseThrow(() -> new NotFoundException("Exam not found"));
    }

    public List<ExternalExamCandidate> candidates(String sittingId) {
        return repo.findCandidates(sittingId);
    }

    // ---- Helpers

    private void syncCandidates(ExternalExamSitting s, List<ExternalExamCandidate> existing) {
        var byStudent = existing.stream()
                .collect(Collectors.toMap(ExternalExamCandidate::getStudentId, Function.identity()));
        List<ExternalExamCandidate> toSave = new ArrayList<>();
        for (Enrollment e : enrollmentRepo.findAllByClassAndAcademicAndSchoolId(s.getClassId(),
                s.getAcademicYearId(), s.getSchoolId(), Pageable.unpaged())) {
            if (e.getStatus() == Enrollment.Status.LEFT || e.getStudent() == null) {
                continue;
            }
            var student = e.getStudent();
            var c = byStudent.get(student.getId());
            if (c == null) {
                toSave.add(ExternalExamCandidate.create(UUID.randomUUID().toString(), s.getSchoolId(), s.getId(),
                        student.getId(), student.getName(), student.getAdmissionNumber()));
            } else {
                c.refreshStudent(student.getName(), student.getAdmissionNumber());
                toSave.add(c);
            }
        }
        repo.saveAll(toSave);
    }

    private List<ExamSubject> subjects(String schoolId, String classId, List<String> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            throw new RuleException("Choose the subjects this exam covers");
        }
        var offered = classSubjects(schoolId, classId).stream()
                .collect(Collectors.toMap(ExternalExamDTO.SubjectOption::id, Function.identity()));
        List<ExamSubject> out = new ArrayList<>();
        for (String sid : new java.util.LinkedHashSet<>(subjectIds)) {
            var o = offered.get(sid);
            if (o == null) {
                throw new RuleException("A chosen subject is not taught in this class");
            }
            out.add(new ExamSubject(o.id(), o.name()));
        }
        return out;
    }

    private ExternalExamDTO.SittingSummary summary(ExternalExamSitting s, ExternalExam exam, Clazz clazz,
            List<ExternalExamCandidate> candidates, String schoolId) {
        var year = academicYearRepo.findByIdAndSchoolId(s.getAcademicYearId(), schoolId).orElse(null);
        Set<String> subjectIds = s.getSubjects().stream().map(ExamSubject::subjectId).collect(Collectors.toSet());
        int entered = candidates.stream()
                .mapToInt(c -> (int) java.util.stream.Stream.concat(c.getScores().keySet().stream(),
                        c.getGrades().keySet().stream()).distinct().filter(subjectIds::contains).count()).sum();
        return new ExternalExamDTO.SittingSummary(s.getId(), s.getExamId(), exam == null ? null : exam.getCode(),
                exam == null ? null : exam.getName(), s.getClassId(), clazz == null ? null : clazz.getName(),
                s.getAcademicYearId(), year == null ? null : year.getName(), s.getCentreNumber(),
                s.getStatus().name(), candidates.size(),
                (int) candidates.stream().filter(c -> c.getIndexNumber() != null).count(), entered,
                candidates.size() * subjectIds.size(), s.getConfirmedAt(), s.getConfirmedByName());
    }

    private Clazz clazz(String schoolId, String classId) {
        if (classId == null || classId.isBlank()) {
            throw new RuleException("Choose the class sitting the exam");
        }
        return classRepo.findByIdAndSchool(classId, schoolId).orElseThrow(() -> new NotFoundException("Class not found"));
    }

    private static Level level(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Level.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuleException("Unknown level " + value);
        }
    }

    private static ExternalExam.EntryMode entryMode(String value, ExternalExam.EntryMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null ? ExternalExam.EntryMode.SCORES : fallback;
        }
        try {
            return ExternalExam.EntryMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuleException("Results are entered as SCORES or GRADES");
        }
    }

    private static ExternalExam.AggregateMode mode(String value) {
        if (value == null || value.isBlank()) {
            return ExternalExam.AggregateMode.POINTS;
        }
        try {
            return ExternalExam.AggregateMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuleException("The aggregate is either POINTS or TOTAL");
        }
    }

    static ExternalExamDTO.Exam toDto(ExternalExam e) {
        return new ExternalExamDTO.Exam(e.getId(), e.getCode(), e.getName(), e.getExamBody(),
                e.getLevel() == null ? null : e.getLevel().name(), e.getGradeScale(), e.getAggregateMode().name(),
                e.getBestOf(), e.isBuiltIn(), e.getSlipTemplate(), e.getStatementTemplate(), e.getEntryMode().name());
    }
}
