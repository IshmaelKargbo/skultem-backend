package com.moriba.skultem.application.services;

import java.util.Comparator;
import java.util.List;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.vo.RemarkBand;

// A school's report card remark scale: score ranges mapped to a ready-made remark, so a teacher
// gets "Excellent work" picked from the average instead of typing "very good" on every card.
public final class RemarkScale {

    private RemarkScale() {
    }

    // Trims and sorts the bands low-to-high, rejecting a bad range or one that overlaps another.
    public static List<RemarkBand> normalize(List<RemarkBand> bands) {
        if (bands == null || bands.isEmpty()) {
            return List.of();
        }

        var sorted = bands.stream()
                .map(b -> new RemarkBand(b.minScore(), b.maxScore(), b.remark() == null ? "" : b.remark().trim()))
                .sorted(Comparator.comparingInt(RemarkBand::minScore))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            var band = sorted.get(i);
            if (band.remark().isEmpty()) {
                throw new RuleException("Remark text is required for every range");
            }
            if (band.minScore() < 0 || band.maxScore() > 100) {
                throw new RuleException("Remark ranges must be within 0 and 100");
            }
            if (band.minScore() > band.maxScore()) {
                throw new RuleException("A remark range's minimum can't be above its maximum");
            }
            if (i > 0 && band.minScore() <= sorted.get(i - 1).maxScore()) {
                throw new RuleException("Remark ranges can't overlap");
            }
        }

        return sorted;
    }

    // The remark whose range holds this average (rounded like the grade is), or null when none does.
    public static String resolve(List<RemarkBand> bands, double average) {
        if (bands == null) {
            return null;
        }
        int score = (int) Math.round(average);
        return bands.stream()
                .filter(b -> score >= b.minScore() && score <= b.maxScore())
                .map(RemarkBand::remark)
                .findFirst()
                .orElse(null);
    }
}
