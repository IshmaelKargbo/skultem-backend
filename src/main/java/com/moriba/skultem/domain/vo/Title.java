package com.moriba.skultem.domain.vo;

public enum Title {
    MR,
    MRS,
    MISS,
    MS,
    DR,
    PROF,
    REV,
    HON,
    ENG,
    SIR,
    MADAM;

    public static Title parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase();

        for (Title title : values()) {
            if (title.name().equals(normalized)) {
                return title;
            }
        }

        throw new IllegalArgumentException("Invalid title: " + value);
    }

    public String toSentenceCase() {
        String lower = name().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}