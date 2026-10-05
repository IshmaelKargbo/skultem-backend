package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;

import lombok.Getter;

@Getter
public class ClassSubjectAssessmentLifeCycle extends AggregateRoot<String> {

    private String schoolId;
    private Term term;
    private Assessment assessment;
    private TeacherSubject subject;
    private Status status;

    // The structure this assessment was opened with. Frozen once, when it opens (null until then), so a later
    // change to the section's configuration only affects assessments that open afterwards.
    private AssessmentStructure structure;
    private Integer caPercentage;
    private Integer formalPercentage;
    private CaFrequency caFrequency;
    private Integer caEntries;
    private Integer configVersion;
    private Instant structureFrozenAt;
    // Continuous assessment only: when the CA recordings were submitted as complete. Until then the formal test
    // can't be entered; after it, the recordings are closed.
    private Instant caSubmittedAt;
    // The CA recording slots (1 = "Week 1") that are locked: completed for every student and closed to editing.
    private final java.util.TreeSet<Integer> caLockedWeeks = new java.util.TreeSet<>();

    public enum Status {
        DRAFT,
        SUBMITTED,
        RETURNED,
        APPROVED,
        COMPLETED,
        LOCKED
    }

    public ClassSubjectAssessmentLifeCycle(String id, String schoolId, TeacherSubject subject, Term term,
            Assessment assessment, Status status, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.term = term;
        this.schoolId = schoolId;
        this.assessment = assessment;
        this.subject = subject;
        this.status = status;
        touch(updatedAt);
    }

    public static ClassSubjectAssessmentLifeCycle create(String id, String schoolId, TeacherSubject subject, Term term,
            Assessment assessment, Status status) {
        Instant now = Instant.now();
        return new ClassSubjectAssessmentLifeCycle(id, schoolId, subject, term, assessment, status, now, now);
    }

    // Copies the configuration in force NOW onto this assessment, once. Later calls do nothing - an assessment
    // keeps the structure and weights it was opened with.
    public void freezeStructure(AssessmentConfiguration config) {
        if (structureFrozenAt != null) {
            return;
        }
        applyConfiguration(config);
        this.structureFrozenAt = Instant.now();
        touch(Instant.now());
    }

    // Copies what applies to THIS assessment (its term and name may have their own number of CA recordings, or not
    // use CA at all) from the configuration.
    private void applyConfiguration(AssessmentConfiguration config) {
        var resolved = config.forAssessment(term == null ? null : term.getId(),
                assessment == null ? null : assessment.getName());
        this.structure = resolved.structure();
        this.caPercentage = resolved.caPercentage();
        this.formalPercentage = resolved.formalPercentage();
        this.caFrequency = resolved.caFrequency();
        this.caEntries = resolved.caEntries();
        this.configVersion = config.getVersion();
    }

    // Re-syncs this assessment onto the configuration in force now, even though it was already frozen -
    // unlike freezeStructure, which only ever applies once. Only ever called for an assessment nobody
    // has graded yet (see RefreshUnstartedAssessmentsUseCase): it lets a section that has just turned on
    // Continuous Assessment bring its still-blank assessments onto it immediately, without waiting for a
    // new term, while an assessment with any recorded score is untouched exactly as freezeStructure keeps it.
    // Returns whether anything actually changed (an assessment already on the right setup is left alone).
    public boolean resyncStructure(AssessmentConfiguration config) {
        var before = List.of(String.valueOf(structure), String.valueOf(caPercentage), String.valueOf(formalPercentage),
                String.valueOf(caFrequency), String.valueOf(caEntries));
        applyConfiguration(config);
        var after = List.of(String.valueOf(structure), String.valueOf(caPercentage), String.valueOf(formalPercentage),
                String.valueOf(caFrequency), String.valueOf(caEntries));
        boolean changed = !before.equals(after);
        if (changed) {
            this.structureFrozenAt = Instant.now();
            this.caSubmittedAt = null;
            this.caLockedWeeks.clear();
            touch(Instant.now());
        }
        return changed;
    }

    // Rebuilds the frozen copy read back from storage.
    public void restoreStructure(AssessmentStructure structure, Integer caPercentage, Integer formalPercentage,
            CaFrequency caFrequency, Integer caEntries, Integer configVersion, Instant structureFrozenAt) {
        this.structure = structure;
        this.caPercentage = caPercentage;
        this.formalPercentage = formalPercentage;
        this.caFrequency = caFrequency;
        this.caEntries = caEntries;
        this.configVersion = configVersion;
        this.structureFrozenAt = structureFrozenAt;
    }

    public void restoreCaSubmitted(Instant caSubmittedAt) {
        this.caSubmittedAt = caSubmittedAt;
    }

    public void restoreLockedWeeks(String text) {
        caLockedWeeks.clear();
        if (text != null && !text.isBlank()) {
            for (var part : text.split(",")) {
                caLockedWeeks.add(Integer.parseInt(part.trim()));
            }
        }
    }

    // Storage form: "1,2,3", or null when none are locked.
    public String lockedWeeksAsText() {
        return caLockedWeeks.isEmpty() ? null
                : caLockedWeeks.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    public java.util.Set<Integer> getCaLockedWeeks() {
        return java.util.Collections.unmodifiableSet(caLockedWeeks);
    }

    public boolean isWeekLocked(int week) {
        return caLockedWeeks.contains(week);
    }

    // Locks one completed recording slot.
    public void lockWeek(int week) {
        requireEditableContinuous();
        if (caEntries == null || week < 1 || week > caEntries) {
            throw new com.moriba.skultem.application.error.RuleException("There is no recording no. " + week);
        }
        if (caLockedWeeks.contains(week)) {
            throw new com.moriba.skultem.application.error.RuleException("Recording no. " + week + " is already locked");
        }
        caLockedWeeks.add(week);
        touch(Instant.now());
    }

    // An administrator re-opens a locked slot (a mistake found after the fact). If the CA had been submitted it is
    // re-opened too, so it has to be submitted again once the correction is in.
    public void unlockWeek(int week) {
        requireEditableContinuous();
        if (!caLockedWeeks.remove(week)) {
            throw new com.moriba.skultem.application.error.RuleException("Recording no. " + week + " is not locked");
        }
        this.caSubmittedAt = null;
        touch(Instant.now());
    }

    private void requireEditableContinuous() {
        if (!isContinuous()) {
            throw new com.moriba.skultem.application.error.RuleException(
                    "This assessment is scored as a single score - it has no CA recordings to lock");
        }
        if (!canEdit()) {
            throw new com.moriba.skultem.application.error.RuleException(
                    "This assessment is " + status + " and can't be edited");
        }
    }

    public boolean isCaSubmitted() {
        return caSubmittedAt != null;
    }

    // Closes the CA step so the formal test can be entered.
    public void submitCa() {
        if (!isContinuous()) {
            throw new com.moriba.skultem.application.error.RuleException(
                    "This assessment is scored as a single score - there is no CA step to submit");
        }
        if (!canEdit()) {
            throw new com.moriba.skultem.application.error.RuleException(
                    "This assessment is " + status + " and can't be edited");
        }
        if (caSubmittedAt != null) {
            throw new com.moriba.skultem.application.error.RuleException("The CA has already been submitted");
        }
        for (int w = 1; caEntries != null && w <= caEntries; w++) {
            caLockedWeeks.add(w);
        }
        this.caSubmittedAt = Instant.now();
        touch(Instant.now());
    }

    public boolean isStructureFrozen() {
        return structureFrozenAt != null;
    }

    // CA is only for monitoring: recorded, but worth 0% of the score. The CA step then never gates the formal test or
    // the approval - there is nothing to finish before the test counts.
    public boolean isMonitorOnly() {
        return isContinuous() && caPercentage != null && caPercentage == 0;
    }

    // Scored as CA + formal test (only once frozen that way; an unfrozen or legacy assessment is a plain score).
    public boolean isContinuous() {
        return structure == AssessmentStructure.CA_AND_TEST;
    }

    public boolean isDraft() {
        return status.equals(Status.DRAFT);
    }
    
public boolean canEdit() {
    return status == Status.DRAFT || status == Status.RETURNED;
}

    public void markDraft() {
        status = Status.DRAFT;
        touch(Instant.now());
    }

    public void submit() {
        if (status != Status.DRAFT && status != Status.RETURNED) {
            throw new RuleException("Only DRAFT or RETURNED assessments can be submitted");
        }
        status = Status.SUBMITTED;
        touch(Instant.now());
    }

    public void approve() {
        if (status != Status.SUBMITTED) {
            throw new RuleException("Only SUBMITTED assessments can be approved");
        }
        status = Status.APPROVED;
        touch(Instant.now());
    }

    public void returnForCorrection() {
        if (status != Status.SUBMITTED) {
            throw new RuleException("Only SUBMITTED assessments can be returned");
        }
        status = Status.RETURNED;
        touch(Instant.now());
    }

    public void lock() {
        if (status != Status.APPROVED) {
            throw new RuleException("Only APPROVED assessments can be locked");
        }
        status = Status.LOCKED;
        touch(Instant.now());
    }

    public void complete() {
        if (status != Status.APPROVED) {
            throw new RuleException("Only APPROVED assessments can be completed");
        }
        status = Status.COMPLETED;
        touch(Instant.now());
    }
}
