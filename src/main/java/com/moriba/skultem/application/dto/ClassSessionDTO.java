package com.moriba.skultem.application.dto;

public record ClassSessionDTO(String id, String clazz, String clazzId, String teacherName, String teacherId, long totalStudent, String streamName, String streamId, String sectionName, String sectionId, String classLevel, String grade, FeeDetail feeDetail, boolean needsAttention,
        // The first class master's profile photo (the one teacherId points at) - null when none, or not loaded.
        String teacherPhoto) {

    public ClassSessionDTO(String id, String clazz, String clazzId, String teacherName, String teacherId, long totalStudent, String streamName, String streamId, String sectionName, String sectionId, String classLevel, String grade, FeeDetail feeDetail, boolean needsAttention) {
        this(id, clazz, clazzId, teacherName, teacherId, totalStudent, streamName, streamId, sectionName, sectionId, classLevel, grade, feeDetail, needsAttention, null);
    }
}
