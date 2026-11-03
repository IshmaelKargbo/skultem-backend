package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BroadsheetDTO;
import com.moriba.skultem.application.dto.ReportCardSubjectDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.model.ReportCard;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.JsonMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A class's broadsheet for a term: every student against every subject, from the full-term report cards
// (scope "ALL"). Nothing is recalculated - if the cards are out of date, regenerating them updates this too.
@Service
@Transactional
@RequiredArgsConstructor
public class GetBroadsheetUseCase {

    private static final double PASS_MARK = 50;

    private final ReportCardRepository reportCardRepo;
    private final ClassRepository classRepo;
    private final TermRepository termRepo;
    private final SectionScopeService sectionScopeService;

    public BroadsheetDTO execute(String schoolId, String classId, String termId, String sectionId, String streamId) {
        var clazz = classRepo.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));
        var term = termRepo.findByIdAndSchoolId(termId, schoolId)
                .orElseThrow(() -> new NotFoundException("Term not found"));

        List<ReportCard> cards = reportCardRepo.search(schoolId, classId, termId, null, blank(sectionId),
                blank(streamId), sectionScopeService.levels(), Pageable.unpaged()).getContent().stream()
                .filter(c -> "ALL".equals(c.getScopeKey()))
                .toList();

        // Subjects in the order they first appear on the cards.
        var subjects = new LinkedHashSet<String>();
        var parsed = new LinkedHashMap<ReportCard, List<ReportCardSubjectDTO>>();
        for (var card : cards) {
            var lines = card.getSubjects() == null ? List.<ReportCardSubjectDTO>of()
                    : JsonMapper.fromJsonList(card.getSubjects(), ReportCardSubjectDTO.class);
            parsed.put(card, lines);
            lines.forEach(l -> subjects.add(l.subject()));
        }

        var rows = new ArrayList<BroadsheetDTO.Row>();
        Instant lastGenerated = null;
        for (var entry : parsed.entrySet()) {
            var card = entry.getKey();
            Map<String, BroadsheetDTO.Cell> scores = new LinkedHashMap<>();
            entry.getValue().forEach(l -> scores.put(l.subject(), new BroadsheetDTO.Cell(l.score(), l.grade())));
            rows.add(new BroadsheetDTO.Row(card.getStudentId(), card.getId(), card.getStudentName(),
                    card.getAdmissionNumber(), scores, round(card.getAverage()), 0, card.getOverallGrade(),
                    card.isPassed()));
            if (card.getGeneratedAt() != null && (lastGenerated == null || card.getGeneratedAt().isAfter(lastGenerated))) {
                lastGenerated = card.getGeneratedAt();
            }
        }

        // Rank on this broadsheet by average; equal averages share a place (1, 2, 2, 4).
        rows.sort(Comparator.comparingDouble(BroadsheetDTO.Row::average).reversed()
                .thenComparing(BroadsheetDTO.Row::studentName, String.CASE_INSENSITIVE_ORDER));
        var ranked = new ArrayList<BroadsheetDTO.Row>();
        for (int i = 0; i < rows.size(); i++) {
            var r = rows.get(i);
            int position = i > 0 && rows.get(i - 1).average() == r.average() ? ranked.get(i - 1).position() : i + 1;
            ranked.add(new BroadsheetDTO.Row(r.studentId(), r.reportCardId(), r.studentName(), r.admissionNumber(),
                    r.scores(), r.average(), position, r.overallGrade(), r.passed()));
        }

        var summaries = subjects.stream().map(subject -> {
            var scores = ranked.stream().map(r -> r.scores().get(subject)).filter(Objects::nonNull)
                    .map(BroadsheetDTO.Cell::score).filter(Objects::nonNull).toList();
            return new BroadsheetDTO.SubjectSummary(subject, scores.size(),
                    round(scores.stream().mapToDouble(Double::doubleValue).average().orElse(0)),
                    scores.stream().max(Double::compare).orElse(null),
                    scores.stream().min(Double::compare).orElse(null),
                    (int) scores.stream().filter(s -> s >= PASS_MARK).count());
        }).toList();

        int passed = (int) ranked.stream().filter(BroadsheetDTO.Row::passed).count();
        double classAverage = round(ranked.stream().mapToDouble(BroadsheetDTO.Row::average).average().orElse(0));

        return new BroadsheetDTO(clazz.getId(), clazz.getName(), term.getId(), term.getName(),
                List.copyOf(subjects), ranked, summaries, ranked.size(), passed, classAverage, lastGenerated);
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
