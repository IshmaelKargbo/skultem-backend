package com.moriba.skultem.domain.vo;

// One band of an external exam's grade scale: a score in [minScore, maxScore] gets this grade, worth these points
// toward the aggregate (WASSCE: 75-100 is A1, 1 point), with a short remark ("Excellent").
public record ExamGrade(int minScore, int maxScore, String grade, int points, String remark) {
}
