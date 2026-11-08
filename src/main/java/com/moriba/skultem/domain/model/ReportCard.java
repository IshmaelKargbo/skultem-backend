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
    // Which slice of the term this card covers - see V83__report_card_scope.sql. 'ALL' is the whole term.
    private String scopeKey;
    private String scopeLabel;
    private String generatedBy;
    private Instant generatedAt;
    private int downloadCount;

    /**
     * The review step between generating and releasing a card: the admin generates (PENDING_REVIEW), the class
     * teacher checks it, adds comments and approves it (APPROVED - only then do parents see it), or sends it back to
     * the admin with a note (RETURNED). Generating again puts it back up for review.
     */
    public enum ReviewStatus {
        PENDING_REVIEW,
        RETURNED,
        APPROVED
    }

    private ReviewStatus reviewStatus = ReviewStatus.APPROVED;
    private String reviewNote;
    private String reviewedByName;
    private Instant reviewedAt;
    // Who signed the card as class teacher, copied when they approve it (see approve).
    private String classTeacherName;
    private String classTeacherSignature;

    public ReportCard(String id, String schoolId, String studentId, String studentName, String admissionNumber,
            String photo, String classId, String className, int classSize, String termId, String termName,
            String academicYearName, double average, int position, String overallGrade, boolean passed,
            Double attendancePercentage, String remark, String subjects, String scopeKey, String scopeLabel,
            String generatedBy, Instant generatedAt,
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
        this.scopeKey = scopeKey;
        this.scopeLabel = scopeLabel;
        this.generatedBy = generatedBy;
        this.generatedAt = generatedAt;
        this.downloadCount = downloadCount;
        touch(updatedAt);
    }

    public static ReportCard generate(String id, String schoolId, String studentId, String studentName,
            String admissionNumber, String photo, String classId, String className, int classSize, String termId,
            String termName, String academicYearName, double average, int position, String overallGrade,
            boolean passed, Double attendancePercentage, String remark, String subjects, String scopeKey,
            String scopeLabel, String generatedBy) {
        Instant now = Instant.now();
        var card = new ReportCard(id, schoolId, studentId, studentName, admissionNumber, photo, classId, className,
                classSize, termId, termName, academicYearName, average, position, overallGrade, passed,
                attendancePercentage, remark, subjects, scopeKey, scopeLabel, generatedBy, now, 0, now, now);
        card.reviewStatus = ReviewStatus.PENDING_REVIEW;
        return card;
    }

    /** Loads the saved review state without touching updatedAt - for the persistence mapper only. */
    public ReportCard restoreSignatory(String classTeacherName, String classTeacherSignature) {
        this.classTeacherName = classTeacherName;
        this.classTeacherSignature = classTeacherSignature;
        return this;
    }

    public ReportCard restoreReview(ReviewStatus status, String note, String reviewedByName, Instant reviewedAt) {
        this.reviewStatus = status != null ? status : ReviewStatus.APPROVED;
        this.reviewNote = note;
        this.reviewedByName = reviewedByName;
        this.reviewedAt = reviewedAt;
        return this;
    }

    public boolean isApproved() {
        return reviewStatus == ReviewStatus.APPROVED;
    }

    /** The class teacher approves the card: it is final and parents can see it. */
    public void approve(String byName) {
        approve(byName, null, null);
    }

    /**
     * Approving is signing: the class teacher's name and signature go on the card. An admin approving a class with
     * no class teacher passes null for both - the card then keeps a blank signature line.
     */
    public void approve(String byName, String classTeacherName, String classTeacherSignature) {
        this.classTeacherName = classTeacherName;
        this.classTeacherSignature = classTeacherSignature;
        this.reviewStatus = ReviewStatus.APPROVED;
        this.reviewNote = null;
        this.reviewedByName = byName;
        this.reviewedAt = Instant.now();
        touch(Instant.now());
    }

    /** The class teacher sends it back to the admin - e.g. a score looks wrong - with a note saying why. */
    public void returnForChanges(String note, String byName) {
        if (isApproved()) {
            throw new com.moriba.skultem.application.error.RuleException(
                    "This report card is already approved. Generate it again to review it again.");
        }
        this.reviewStatus = ReviewStatus.RETURNED;
        this.reviewNote = note;
        this.reviewedByName = byName;
        this.reviewedAt = Instant.now();
        touch(Instant.now());
    }

    // Called when the same student/term is generated again - keeps the same id
    // (and download count/history) but refreshes every computed value.
    // The card shows the student's current photo - a photo added or replaced after generating reaches it too.
    public void refreshPhoto(String photo) {
        this.photo = photo;
    }

    public void regenerate(String studentName, String admissionNumber, String photo, String className, int classSize,
            String termName, String academicYearName, double average, int position, String overallGrade,
            boolean passed, Double attendancePercentage, String subjects, String scopeLabel, String generatedBy) {
        this.scopeLabel = scopeLabel;
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
        // New figures need checking again.
        this.reviewStatus = ReviewStatus.PENDING_REVIEW;
        this.reviewNote = null;
        touch(Instant.now());
    }

    public void updateRemark(String remark) {
        if (isApproved()) {
            throw new com.moriba.skultem.application.error.RuleException(
                    "This report card is approved, so its comment is locked. Generate it again to change it.");
        }
        this.remark = remark;
        touch(Instant.now());
    }

    public void trackDownload() {
        this.downloadCount = this.downloadCount + 1;
        touch(Instant.now());
    }
}
