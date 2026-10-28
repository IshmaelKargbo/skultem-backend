package com.moriba.skultem.domain.service;

import java.util.List;
import java.util.Set;

import com.moriba.skultem.domain.model.Assessment;

// A template's tests share each term's mark. When every test runs in every term this is the plain total; when tests are
// limited to some terms (Test 1 + Test 2 in terms 1-2, an Exam in term 3) each term is checked on its own.
public final class TermWeights {

    public record Line(String name, int weight, Set<Integer> terms) {
        boolean runsIn(int term) {
            return terms == null || terms.isEmpty() || terms.contains(term);
        }
    }

    private TermWeights() {
    }

    // null when no term goes over 100%, otherwise what to fix.
    public static String overweight(List<Line> lines) {
        if (lines.stream().allMatch(l -> l.terms() == null || l.terms().isEmpty())) {
            int total = lines.stream().mapToInt(Line::weight).sum();
            return total > 100 ? "Total assessment weight cannot exceed 100%" : null;
        }
        for (int term = 1; term <= lastTerm(lines); term++) {
            final int t = term;
            int total = lines.stream().filter(l -> l.runsIn(t)).mapToInt(Line::weight).sum();
            if (total > 100) {
                return "The tests in term " + t + " total " + total + "% - a term's tests cannot exceed 100%";
            }
        }
        return null;
    }

    // At least the usual three terms, more if a test names a later one.
    private static int lastTerm(List<Line> lines) {
        int max = 3;
        for (var l : lines) {
            if (l.terms() != null) {
                for (var t : l.terms()) {
                    max = Math.max(max, t);
                }
            }
        }
        return Math.min(max, Assessment.MAX_TERM);
    }
}
