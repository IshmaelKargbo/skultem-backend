package com.moriba.skultem.domain.vo;

public enum Role {
    OWNER,
    SYSTEM_ADMIN,
    PROPRIETOR,
    PRINCIPAL,
    SUPER_ADMIN,
    ADMIN,
    ACCOUNTANT,
    // Runs the school library (see LibraryController) and nothing else.
    LIBRARIAN,
    // Runs the school sick bay (see HealthCareController, docs/HEALTH_CARE.md) and nothing else.
    NURSE,
    // Runs the exam office: approves grades where the school routes them here, tracks submissions, moves the
    // assessment cycle on, generates report cards and keeps the exam timetable (see ExamScheduleController).
    EXAM_OFFICER,
    TEACHER,
    PARENT
}
