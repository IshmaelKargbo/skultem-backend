package com.moriba.skultem.application.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.AssessmentCaEntry;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.service.ContinuousAssessmentCalculator;

import lombok.RequiredArgsConstructor;

// Fills in the CA part that is taken from attendance. A student's mark is the days they were present or late over
// the term's school days, as a percentage - the same present-or-late convention every attendance report uses. The
// school days are the term's recorded, non-holiday days for the class (the most any student has a record for), so a
// student with a missing record is not given a smaller denominator than their classmates.
@Service
@RequiredArgsConstructor
public class AttendanceCaService {

    public record Result(List<AssessmentCaEntry> entries, List<AssessmentScore> scores, int schoolDays) {
        public static Result none() {
            return new Result(List.of(), List.of(), 0);
        }
    }

    private final AttendanceRepository attendanceRepo;
    private final AssessmentCaEntryRepository caEntryRepo;

    // Works out the attendance entry for each score and the CA / combined score that follows from it. Nothing is
    // saved here - the caller persists the returned entries and scores.
    public Result sync(String schoolId, TeacherSubject ts, ClassSubjectAssessmentLifeCycle cycle,
            Collection<AssessmentScore> scores, String userId) {
        int slot = cycle.attendanceEntryNumber();
        if (slot == 0 || cycle.getTerm() == null || scores.isEmpty()) {
            return Result.none();
        }
        var session = ts.getSession();
        var term = cycle.getTerm();
        String streamId = session.getStream() != null ? session.getStream().getId() : null;
        var rows = attendanceRepo.attendanceCountsBySessionAndDateRange(schoolId, session.getClazz().getId(),
                session.getSection().getId(), streamId, session.getAcademicYear().getId(), term.getStartDate(),
                term.getEndDate());

        long schoolDays = 0;
        Map<String, Long> presentOrLate = new HashMap<>();
        for (var row : rows) {
            presentOrLate.put((String) row[0], ((Number) row[6]).longValue());
            schoolDays = Math.max(schoolDays, ((Number) row[8]).longValue());
        }
        if (schoolDays == 0) {
            return Result.none();
        }

        var existing = caEntryRepo.findAllByScoreIds(scores.stream().map(AssessmentScore::getId).toList()).stream()
                .collect(Collectors.groupingBy(AssessmentCaEntry::getAssessmentScoreId));
        var weights = cycle.caWeights();
        var entriesToSave = new ArrayList<AssessmentCaEntry>();
        var scoresToSave = new ArrayList<AssessmentScore>();

        for (var score : scores) {
            Long days = presentOrLate.get(score.getStudentAssessment().getEnrollment().getId());
            if (days == null) {
                continue;
            }
            int mark = (int) Math.min(100, Math.round(days * 100.0 / schoolDays));

            Map<Integer, AssessmentCaEntry> entries = new HashMap<>();
            existing.getOrDefault(score.getId(), List.of()).forEach(e -> entries.put(e.getEntryNumber(), e));
            var current = entries.get(slot);
            if (current == null) {
                current = AssessmentCaEntry.create(schoolId, score.getId(), slot, mark, userId);
                entries.put(slot, current);
                entriesToSave.add(current);
            } else if (current.getScore() != mark) {
                current.update(mark, userId);
                entriesToSave.add(current);
            } else {
                continue;
            }

            Map<Integer, Integer> recorded = new HashMap<>();
            entries.forEach((n, e) -> recorded.put(n, e.getScore()));
            Integer ca = ContinuousAssessmentCalculator.caComponent(recorded, weights);
            Integer formal = score.getFormalScore();
            int combined = ContinuousAssessmentCalculator.combine(ca, formal, cycle.getCaPercentage(),
                    cycle.getFormalPercentage());
            score.applyContinuous(ca, formal, combined, userId);
            scoresToSave.add(score);
        }
        return new Result(entriesToSave, scoresToSave, (int) schoolDays);
    }
}
