package com.moriba.skultem.application.dto;

public record TimetableSettingDTO(
        String title,
        String accentColor,
        String orientation,
        boolean showLogo,
        boolean showIcons,
        boolean showTeacher,
        boolean showRoom,
        boolean showPeriodTimes,
        String footerNote) {

    public static final String DEFAULT_TITLE = "SCHOOL TIMETABLE";
    public static final String LANDSCAPE = "LANDSCAPE";
    public static final String PORTRAIT = "PORTRAIT";
}
