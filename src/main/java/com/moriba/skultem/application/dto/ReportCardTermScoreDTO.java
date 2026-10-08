package com.moriba.skultem.application.dto;

// One term's score for a subject on a whole-year report card - the card shows these side by side, then the
// subject's final (year) score.
public record ReportCardTermScoreDTO(String term, Integer score) {
}
