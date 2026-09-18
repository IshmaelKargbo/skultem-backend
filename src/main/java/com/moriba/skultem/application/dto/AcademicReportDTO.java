package com.moriba.skultem.application.dto;

import java.util.List;

// Result of GetClassAcademicPerformanceUseCase - one term's worth of approved academic data for
// either a single class (classes is empty) or the whole school (classId omitted - classes then
// holds the cross-class comparison table, and subjects/students aggregate across every class),
// backing the Academic Report's Overview, Class Performance, Subject Performance and Student
// Performance sections in a single request/response. studentsPage/studentsTotal support pagination
// of the (potentially school-wide) student list without paginating the underlying aggregation
// query itself.
public record AcademicReportDTO(
        AcademicOverviewDTO overview,
        List<ClassPerformanceDTO> classes,
        List<SubjectPerformanceDTO> subjects,
        List<StudentAcademicPerformanceDTO> students,
        int studentsPage,
        int studentsSize,
        int studentsTotal) {
}
