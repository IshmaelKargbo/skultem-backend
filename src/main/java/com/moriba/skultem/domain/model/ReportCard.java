package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

// A generated report card is a snapshot: once produced for a student/term it
// freezes that term's grades, attendance and position as they stood at
// generation time, the same way a printed report card would - re-running
// "Generate" for the same student/term overwrites the snapshot rather than
// creating a second one (see ReportCardRepository#findBySchoolIdAndStudentIdAndTermId).
// `subjects` is a raw JSON array string (subject/teacher/score/weight/grade)
// for the same reason IdCardSetting keeps `fields` as JSON - the backend
// doesn't need to model every column shape, the frontend and this snapshot
// agree on it once, at generation time.
@Getter
public class ReportCard extends AggregateRoot<String> {

    private String schoolId;
    private String studentId;
    private String studentName;
    private String admissionNumber;
    private String photo;
    private String classId;
    private String className;
    private int classSize;
    private String termId;
    private String termName;
    private String academicYearName;
    private double average;
    private int position;
    private String overallGrade;
    private boolean passed;
    private Double attendancePercentage;
    private String remark;
    private String subjects;
    private String generatedBy;
    private Instant generatedAt;
    private int downloadCount;

    public ReportCard(String id, String schoolId, String studentId, String studentName, String admissionNumber,
            String photo, String classId, String className, int classSize, String termId, String termName,
            String academicYearName, double average, int position, String overallGrade, boolean passed,
            Double attendancePercentage, String remark, String subjects, String generatedBy, Instant generatedAt,
            int downloadCount, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.admissionNumber = admissionNumber;
        this.photo = photo;
        this.classId = classId;
        this.className = className;
        this.classSize = classSize;
        this.termId = termId;
        this.termName = termName;
        this.academicYearName = academicYearName;
        this.average = average;
        this.position = position;
        this.overallGrade = overallGrade;
        this.passed = passed;
        this.attendancePercentage = attendancePercentage;
        this.remark = remark;
        this.subjects = subjects;
        this.generatedBy = generatedBy;
        this.generatedAt = generatedAt;
        this.downloadCount = downloadCount;
        touch(updatedAt);
    }

    public static ReportCard generate(String id, String schoolId, String studentId, String studentName,
            String admissionNumber, String photo, String classId, String className, int classSize, String termId,
            String termName, String academicYearName, double average, int position, String overallGrade,
            boolean passed, Double attendancePercentage, String remark, String subjects, String generatedBy) {
        Instant now = Instant.now();
        return new ReportCard(id, schoolId, studentId, studentName, admissionNumber, photo, classId, className,
                classSize, termId, termName, academicYearName, average, position, overallGrade, passed,
                attendancePercentage, remark, subjects, generatedBy, now, 0, now, now);
    }

    // Called when the same student/term is generated again - keeps the same id
    // (and download count/history) but refreshes every computed value.
    public void regenerate(String studentName, String admissionNumber, String photo, String className, int classSize,
            String termName, String academicYearName, double average, int position, String overallGrade,
            boolean passed, Double attendancePercentage, String subjects, String generatedBy) {
        this.studentName = studentName;
        this.admissionNumber = admissionNumber;
        this.photo = photo;
        this.className = className;
        this.classSize = classSize;
        this.termName = termName;
        this.academicYearName = academicYearName;
        this.average = average;
        this.position = position;
        this.overallGrade = overallGrade;
        this.passed = passed;
        this.attendancePercentage = attendancePercentage;
        this.subjects = subjects;
        this.generatedBy = generatedBy;
        this.generatedAt = Instant.now();
        touch(Instant.now());
    }

    public void updateRemark(String remark) {
        this.remark = remark;
        touch(Instant.now());
    }

    public void trackDownload() {
        this.downloadCount = this.downloadCount + 1;
        touch(Instant.now());
    }
}
