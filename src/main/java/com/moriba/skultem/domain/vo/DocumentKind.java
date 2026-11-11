package com.moriba.skultem.domain.vo;

import java.util.Arrays;
import java.util.Optional;

/**
 * A kind of document Skultem prints with M-Compose, and the design categories that suit it, best first (the engine
 * names designs "Report card: classic", whose category is "Report card"). Exam results also print well on a report
 * card design, and a statement on a certificate. Each school can pick its own design per kind.
 */
public enum DocumentKind {
    REPORT_CARD("report-card", "Report card"),
    EXAM_RESULT_SLIP("exam-result-slip", "Exam result", "Report card"),
    EXAM_STATEMENT("exam-statement", "Exam result", "Certificate", "Report card");

    private final String key;
    private final java.util.List<String> categories;

    DocumentKind(String key, String... categories) {
        this.key = key;
        this.categories = java.util.List.of(categories);
    }

    public String key() {
        return key;
    }

    public java.util.List<String> categories() {
        return categories;
    }

    /** Is a design of this category made for (or suited to) this kind of document? */
    public boolean suits(String category) {
        return category != null && categories.stream().anyMatch(c -> c.equalsIgnoreCase(category.trim()));
    }

    public static Optional<DocumentKind> fromKey(String key) {
        return Arrays.stream(values()).filter(k -> k.key.equalsIgnoreCase(key == null ? "" : key.trim())).findFirst();
    }
}
