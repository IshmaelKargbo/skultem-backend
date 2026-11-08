package com.moriba.skultem.application.dto;

// How the school's exam timetable PDFs look. accentColor null = the school's brand colour.
public record ExamTimetableDesignDTO(
        String heading,
        String accentColor,
        String orientation,
        boolean showLogo,
        boolean showVenue,
        boolean showDuration,
        boolean showInvigilators,
        boolean showInstructions,
        boolean showSignatures,
        String leftSignature,
        String rightSignature,
        String footerNote) {

    public static ExamTimetableDesignDTO defaults() {
        return new ExamTimetableDesignDTO("EXAMINATION TIMETABLE", null, "PORTRAIT", true, true, true, false, true,
                true, "Exam Officer", "Principal", null);
    }
}
