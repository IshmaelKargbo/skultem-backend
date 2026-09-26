package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ContinuousAssessmentReportDTO;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.model.AssessmentCaEntry;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.service.ContinuousAssessmentCalculator;
import com.moriba.skultem.domain.service.ContinuousAssessmentCalculator.Trend;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// The CA report. The "Students Requiring Attention" report only sees an assessment once it is submitted, which for
// continuous assessment is the very end - after the CA weeks and the formal test are both done. This reads the CA
// recordings as they are made, so a falling or low classwork average shows up while there is still time to help.
// Signals stay separate and explained (never folded into one score):
//   - classwork (CA) average below the assessment's pass mark
//   - classwork trending down over the recordings so far
//   - formal test 20+ points below the classwork - the child knows the work but not the exam
@Service
@Transactional
@RequiredArgsConstructor
public class ContinuousAssessmentReportUseCase {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int TEST_GAP = 20;

    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final AssessmentScoreRepository scoreRepo;
    private final AssessmentCaEntryRepository caEntryRepo;
    private final SectionScopeService sectionScopeService;

    public ContinuousAssessmentReportDTO execute(String schoolId, String termId, String classId, Level level) {
        return execute(schoolId, termId, classId, level, 1, DEFAULT_PAGE_SIZE);
    }

    public ContinuousAssessmentReportDTO execute(String schoolId, String termId, String classId, Level level, int page,
            int size) {
        var levels = sectionScopeService.levels();

        var cycles = cycleRepo.findAllBySchoolAndTerm(schoolId, termId).stream()
                .filter(ClassSubjectAssessmentLifeCycle::isContinuous)
                // Not opened yet (waiting for its turn): nothing recorded, nothing to report.
                .filter(c -> c.getStatus() != ClassSubjectAssessmentLifeCycle.Status.LOCKED)
                .filter(c -> levels == null || levels.contains(clazzOf(c).getLevel()))
                .filter(c -> classId == null || classId.isBlank() || clazzOf(c).getId().equals(classId))
                .filter(c -> level == null || clazzOf(c).getLevel() == level)
                .toList();

        var assessmentRows = new ArrayList<ContinuousAssessmentReportDTO.AssessmentRow>();
        var studentRows = new ArrayList<ContinuousAssessmentReportDTO.StudentRow>();
        var allCa = new ArrayList<Integer>();
        var allFormal = new ArrayList<Integer>();
        int fullyRecorded = 0, students = 0;

        for (var cycle : cycles) {
            var scores = scoreRepo.findAllByCycle(cycle.getId());
            var entries = caEntryRepo.findAllByScoreIds(scores.stream().map(AssessmentScore::getId).toList()).stream()
                    .collect(Collectors.groupingBy(AssessmentCaEntry::getAssessmentScoreId));
            int expected = cycle.getCaEntries() == null ? 0 : cycle.getCaEntries();
            int passMark = cycle.getAssessment().getTemplate() != null ? cycle.getAssessment().getTemplate().getPassMark() : 50;
            var clazz = clazzOf(cycle);
            var subjectName = cycle.getSubject().getSubject().getName();
            var assessmentName = cycle.getAssessment().getName();

            var cas = new ArrayList<Integer>();
            var formals = new ArrayList<Integer>();
            int improving = 0, declining = 0, recordedSlots = 0;

            for (var score : scores) {
                var recordings = new ArrayList<Integer>();
                for (int i = 0; i < expected; i++) {
                    recordings.add(null);
                }
                for (var e : entries.getOrDefault(score.getId(), List.of())) {
                    if (e.getEntryNumber() >= 1 && e.getEntryNumber() <= expected) {
                        recordings.set(e.getEntryNumber() - 1, e.getScore());
                        recordedSlots++;
                    }
                }
                if (score.getCaScore() != null) {
                    cas.add(score.getCaScore());
                }
                if (score.getFormalScore() != null) {
                    formals.add(score.getFormalScore());
                }
                var trend = ContinuousAssessmentCalculator.trend(recordings);
                if (trend == Trend.IMPROVING) {
                    improving++;
                }
                if (trend == Trend.DECLINING) {
                    declining++;
                }

                var reasons = new ArrayList<String>();
                if (score.getCaScore() != null && score.getCaScore() < passMark) {
                    reasons.add("Classwork average " + score.getCaScore() + "% is below the pass mark (" + passMark + "%)");
                }
                if (trend == Trend.DECLINING) {
                    reasons.add("Classwork marks are trending down");
                }
                if (score.getCaScore() != null && score.getFormalScore() != null
                        && score.getCaScore() - score.getFormalScore() >= TEST_GAP) {
                    reasons.add("Formal test (" + score.getFormalScore() + "%) is well below classwork ("
                            + score.getCaScore() + "%)");
                }
                if (!reasons.isEmpty()) {
                    studentRows.add(new ContinuousAssessmentReportDTO.StudentRow(
                            score.getStudentAssessment().getEnrollment().getStudent().getName(), clazz.getName(),
                            subjectName, assessmentName, score.getCaScore(), score.getFormalScore(),
                            trend.name(), recordings, reasons));
                }
            }

            int possible = expected * scores.size();
            int recordedPercent = possible == 0 ? 0 : (int) Math.round(recordedSlots * 100.0 / possible);
            if (recordedPercent == 100) {
                fullyRecorded++;
            }
            students += scores.size();
            allCa.addAll(cas);
            allFormal.addAll(formals);

            assessmentRows.add(new ContinuousAssessmentReportDTO.AssessmentRow(cycle.getId(), clazz.getName(),
                    subjectName, assessmentName, cycle.getSubject().getTeacher().getUser().getName(),
                    cycle.getStatus().name(), cycle.isCaSubmitted(), cycle.getCaLockedWeeks().size(), expected,
                    recordedPercent, scores.size(), average(cas), average(formals), improving, declining));
        }

        studentRows.sort(Comparator.<ContinuousAssessmentReportDTO.StudentRow>comparingInt(s -> -s.reasons().size())
                .thenComparing(s -> s.caAverage() == null ? 101 : s.caAverage()));
        int flagged = studentRows.size();
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 100);
        int pageNumber = Math.max(page, 1);
        int from = Math.min((pageNumber - 1) * pageSize, flagged);
        var shown = studentRows.subList(from, Math.min(from + pageSize, flagged));

        assessmentRows.sort(Comparator.comparing(ContinuousAssessmentReportDTO.AssessmentRow::clazz)
                .thenComparing(ContinuousAssessmentReportDTO.AssessmentRow::subject));

        return new ContinuousAssessmentReportDTO(
                new ContinuousAssessmentReportDTO.Summary(cycles.size(), fullyRecorded, cycles.size() - fullyRecorded,
                        students, flagged, average(allCa), average(allFormal)),
                assessmentRows, shown, pageNumber, pageSize, flagged);
    }

    private static com.moriba.skultem.domain.model.Clazz clazzOf(ClassSubjectAssessmentLifeCycle cycle) {
        return cycle.getSubject().getSession().getClazz();
    }

    private static Integer average(List<Integer> values) {
        return values.isEmpty() ? null : (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }
}
