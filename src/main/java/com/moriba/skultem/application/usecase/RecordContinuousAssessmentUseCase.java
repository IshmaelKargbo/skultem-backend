package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.AssessmentStructureService;
import com.moriba.skultem.domain.model.AssessmentCaEntry;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.service.ContinuousAssessmentCalculator;
import com.moriba.skultem.infrastructure.security.PermissionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A teacher recording an assessment that was opened as Continuous Assessment: the CA recordings ("Week 1",
// "Week 2", ...) and/or the formal test for each student. The teacher uses the structure the assessment
// froze when it opened - CA %, formal %, how many recordings - and can't change it. The CA part is the
// average of the recordings so far; the assessment score becomes CA x CA% + formal x formal%, and that
// combined score is what approval, weighted totals and report cards already read.
@Service
@Transactional
@RequiredArgsConstructor
public class RecordContinuousAssessmentUseCase {

    public record CaValue(int entryNumber, Integer score) {
    }

    // One student's row: their score id, the recordings being set (may be a subset) and the formal test score.
    public record StudentRecord(String scoreId, List<CaValue> entries, Integer formalScore) {
    }

    private final TeacherSubjectRepository teacherSubjectRepo;
    private final StudentAssessmentRepository studentAssessmentRepo;
    private final AssessmentScoreRepository assessmentScoreRepo;
    private final AssessmentCaEntryRepository caEntryRepo;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final ClassSubjectRepository classSubjectRepo;
    private final AssessmentStructureService structureService;

    public int execute(String schoolId, String teacherSubjectId, String assessmentId, String termId,
            List<StudentRecord> records) {
        var ts = teacherSubjectRepo.findByIdAndSchoolId(teacherSubjectId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher subject not found"));
        var session = ts.getSession();
        var clazz = session.getClazz();

        var studentAssessments = studentAssessmentRepo.findAllBySubjectAndSessionAndTermId(ts.getSubject().getId(),
                session.getId(), termId);
        if (studentAssessments.isEmpty()) {
            throw new NotFoundException("No student assessments found");
        }

        Map<String, AssessmentScore> scoreById = new HashMap<>();
        for (var sa : studentAssessments) {
            for (var score : assessmentScoreRepo.findAllByStudentAssessmentIdAndAssessmentId(sa.getId(),
                    assessmentId)) {
                scoreById.put(score.getId(), score);
            }
        }
        if (scoreById.isEmpty()) {
            throw new NotFoundException("Assessment not found for these students");
        }

        // Open assessments that have not frozen their structure yet take the configuration in force now.
        var cycles = scoreById.values().stream().map(AssessmentScore::getCycle).distinct().toList();
        cycleRepo.saveAll(structureService.freezeOpened(cycles));

        var cycle = cycles.get(0);
        if (!cycle.isContinuous()) {
            throw new RuleException("This assessment is scored as a single score, not continuous assessment. "
                    + "Record it from the normal grade entry.");
        }
        if (!cycle.canEdit()) {
            throw new RuleException("This assessment is " + cycle.getStatus() + " and can't be edited");
        }

        int caPct = cycle.getCaPercentage();
        int formalPct = cycle.getFormalPercentage();
        int maxEntries = cycle.getCaEntries();
        String userId = PermissionService.getCurrentUser().userId();

        var existing = caEntryRepo.findAllByScoreIds(scoreById.keySet()).stream()
                .collect(Collectors.groupingBy(AssessmentCaEntry::getAssessmentScoreId));

        List<AssessmentCaEntry> entriesToSave = new ArrayList<>();
        List<AssessmentScore> scoresToSave = new ArrayList<>();
        for (var record : records) {
            var score = scoreById.get(record.scoreId());
            if (score == null) {
                throw new NotFoundException("Score not found: " + record.scoreId());
            }

            Map<Integer, AssessmentCaEntry> entries = new HashMap<>();
            existing.getOrDefault(score.getId(), List.of()).forEach(e -> entries.put(e.getEntryNumber(), e));

            boolean touchesEntries = record.entries() != null && record.entries().stream().anyMatch(v -> v.score() != null);
            if (touchesEntries && cycle.isCaSubmitted()) {
                throw new RuleException("The CA has already been submitted, so its recordings are closed");
            }
            // Where CA counts, it is closed first and the formal test follows. Where CA is only monitored it
            // never stands in the way of the test, so it can be entered at any time.
            if (record.formalScore() != null && !cycle.isCaSubmitted() && !cycle.isMonitorOnly()) {
                throw new RuleException("Submit the CA recordings first - the formal test is entered after them");
            }

            if (record.entries() != null) {
                for (var value : record.entries()) {
                    if (value.entryNumber() < 1 || value.entryNumber() > maxEntries) {
                        throw new RuleException("This assessment has " + maxEntries + " continuous assessment "
                                + "recording" + (maxEntries == 1 ? "" : "s") + " - no. " + value.entryNumber()
                                + " doesn't exist");
                    }
                    if (value.score() == null) {
                        continue;
                    }
                    var current = entries.get(value.entryNumber());
                    if (cycle.isWeekLocked(value.entryNumber())) {
                        // A locked recording is closed. Sending back the same mark is harmless; changing it isn't.
                        if (current != null && current.getScore() == value.score().intValue()) {
                            continue;
                        }
                        throw new RuleException("Recording no. " + value.entryNumber()
                                + " is locked. Ask an administrator to unlock it if it needs correcting");
                    }
                    if (current == null) {
                        current = AssessmentCaEntry.create(schoolId, score.getId(), value.entryNumber(),
                                value.score(), userId);
                        entries.put(value.entryNumber(), current);
                    } else {
                        current.update(value.score(), userId);
                    }
                    entriesToSave.add(current);
                }
            }

            Integer ca = ContinuousAssessmentCalculator.caComponent(
                    entries.values().stream().map(AssessmentCaEntry::getScore).toList());
            Integer formal = record.formalScore() != null ? record.formalScore() : score.getFormalScore();
            if (formal != null && (formal < 0 || formal > 100)) {
                throw new RuleException("The formal test score must be between 0 and 100");
            }
            int combined = ContinuousAssessmentCalculator.combine(ca, formal, caPct, formalPct);

            score.applyContinuous(ca, formal, combined, userId);
            scoresToSave.add(score);
        }

        lockSubject(schoolId, ts.getSubject().getId(), clazz.getId(),
                session.getStream() != null ? session.getStream().getId() : null);
        caEntryRepo.saveAll(entriesToSave);
        assessmentScoreRepo.saveAll(scoresToSave);
        return scoresToSave.size();
    }

    // Same as normal grading: once marks exist the class subject is locked so its subject setup can't shift.
    private void lockSubject(String schoolId, String subjectId, String classId, String streamId) {
        var subject = streamId == null
                ? classSubjectRepo.findByClassIdAndSubjectId(classId, subjectId, schoolId)
                : classSubjectRepo.findByClassIdAndSubjectIdAndStramId(classId, subjectId, streamId);
        subject.ifPresent(s -> {
            s.lock();
            classSubjectRepo.save(s);
        });
    }
}
