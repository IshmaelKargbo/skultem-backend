package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;

import lombok.Getter;

// How a management section (or the whole school) assesses its students. Set by school administrators;
// teachers only use it. Changing it never touches assessments that are already open or recorded - each of
// those froze its own copy when it opened (see ClassSubjectAssessmentLifeCycle#freezeStructure).
@Getter
public class AssessmentConfiguration extends AggregateRoot<String> {

    public static final int MAX_CA_ENTRIES = 40;

    private String schoolId;
    // null = the school-wide configuration.
    private String managementSectionId;
    private AssessmentStructure structure;
    private int caPercentage;
    private int formalPercentage;
    private CaFrequency caFrequency;
    private int caEntries;
    private int version;
    private String updatedByUserId;
    // Per-term, per-assessment overrides (see PlanEntry). Empty = every assessment follows the defaults above.
    private final java.util.List<PlanEntry> plan = new java.util.ArrayList<>();

    // One assessment in one term ("Test 1" in Term 2): whether it uses CA + formal test and, if so, how many CA
    // recordings it has. A short term, or a short test, simply has fewer weeks.
    public record PlanEntry(String termId, String assessmentName, boolean usesCa, int caEntries) {
    }

    // What an assessment actually gets: the plan's entry for it, else the section defaults.
    public record Resolved(AssessmentStructure structure, int caPercentage, int formalPercentage,
            CaFrequency caFrequency, int caEntries) {
    }

    public AssessmentConfiguration(String id, String schoolId, String managementSectionId,
            AssessmentStructure structure, int caPercentage, int formalPercentage, CaFrequency caFrequency,
            int caEntries, int version, String updatedByUserId, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.managementSectionId = managementSectionId;
        this.structure = structure;
        this.caPercentage = caPercentage;
        this.formalPercentage = formalPercentage;
        this.caFrequency = caFrequency;
        this.caEntries = caEntries;
        this.version = version;
        this.updatedByUserId = updatedByUserId;
        touch(updatedAt);
    }

    public static AssessmentConfiguration create(String schoolId, String managementSectionId,
            AssessmentStructure structure, int caPercentage, int formalPercentage, CaFrequency caFrequency,
            int caEntries, String userId) {
        validate(structure, caPercentage, formalPercentage, caFrequency, caEntries);
        Instant now = Instant.now();
        return new AssessmentConfiguration(UUID.randomUUID().toString(), schoolId, managementSectionId, structure,
                normalisedCa(structure, caPercentage), normalisedFormal(structure, formalPercentage),
                structure == AssessmentStructure.SIMPLE ? null : caFrequency,
                structure == AssessmentStructure.SIMPLE ? 0 : caEntries, 1, userId, now, now);
    }

    // The configuration in force when nothing has been set: exactly how assessments always worked.
    public static AssessmentConfiguration simple(String schoolId, String managementSectionId) {
        Instant now = Instant.now();
        return new AssessmentConfiguration(null, schoolId, managementSectionId, AssessmentStructure.SIMPLE, 0, 100,
                null, 0, 0, null, now, now);
    }

    public boolean isDefault() {
        return getId() == null;
    }

    // The configuration for one assessment in one term. Names match case-insensitively; nulls just get the defaults.
    public Resolved forAssessment(String termId, String assessmentName) {
        if (structure == AssessmentStructure.CA_AND_TEST && termId != null && assessmentName != null) {
            for (var entry : plan) {
                if (entry.termId().equals(termId) && entry.assessmentName().equalsIgnoreCase(assessmentName.trim())) {
                    return entry.usesCa()
                            ? new Resolved(structure, caPercentage, formalPercentage, caFrequency, entry.caEntries())
                            : new Resolved(AssessmentStructure.SIMPLE, 0, 100, null, 0);
                }
            }
        }
        return new Resolved(structure, caPercentage, formalPercentage, caFrequency, caEntries);
    }

    public void replacePlan(java.util.List<PlanEntry> entries) {
        var seen = new java.util.HashSet<String>();
        var clean = new java.util.ArrayList<PlanEntry>();
        if (structure == AssessmentStructure.CA_AND_TEST && entries != null) {
            for (var e : entries) {
                if (e.termId() == null || e.termId().isBlank() || e.assessmentName() == null
                        || e.assessmentName().isBlank()) {
                    continue;
                }
                var name = e.assessmentName().trim();
                if (name.contains("|") || name.contains("\n")) {
                    throw new RuleException("Assessment name can't contain | or a line break");
                }
                if (e.usesCa() && (e.caEntries() < 1 || e.caEntries() > MAX_CA_ENTRIES)) {
                    throw new RuleException(name + " needs between 1 and " + MAX_CA_ENTRIES + " CA recordings");
                }
                if (!seen.add(e.termId() + "|" + name.toLowerCase())) {
                    throw new RuleException(name + " is listed twice for the same term");
                }
                clean.add(new PlanEntry(e.termId(), name, e.usesCa(), e.usesCa() ? e.caEntries() : 0));
            }
        }
        plan.clear();
        plan.addAll(clean);
    }

    public java.util.List<PlanEntry> getPlan() {
        return java.util.Collections.unmodifiableList(plan);
    }

    // Storage form: one "termId|name|1|6" line per entry, null when there is no plan.
    public String planAsText() {
        return plan.isEmpty() ? null : plan.stream()
                .map(e -> e.termId() + "|" + e.assessmentName() + "|" + (e.usesCa() ? 1 : 0) + "|" + e.caEntries())
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    public void restorePlan(String text) {
        plan.clear();
        if (text == null || text.isBlank()) {
            return;
        }
        for (var line : text.split("\n")) {
            var p = line.split("\\|");
            if (p.length == 4) {
                plan.add(new PlanEntry(p[0], p[1], "1".equals(p[2]), Integer.parseInt(p[3])));
            }
        }
    }

    public boolean isContinuous() {
        return structure == AssessmentStructure.CA_AND_TEST;
    }

    public void change(AssessmentStructure structure, int caPercentage, int formalPercentage,
            CaFrequency caFrequency, int caEntries, String userId) {
        validate(structure, caPercentage, formalPercentage, caFrequency, caEntries);
        this.structure = structure;
        this.caPercentage = normalisedCa(structure, caPercentage);
        this.formalPercentage = normalisedFormal(structure, formalPercentage);
        this.caFrequency = structure == AssessmentStructure.SIMPLE ? null : caFrequency;
        this.caEntries = structure == AssessmentStructure.SIMPLE ? 0 : caEntries;
        if (structure == AssessmentStructure.SIMPLE) {
            this.plan.clear();
        }
        this.version++;
        this.updatedByUserId = userId;
        touch(Instant.now());
    }

    private static int normalisedCa(AssessmentStructure structure, int ca) {
        return structure == AssessmentStructure.SIMPLE ? 0 : ca;
    }

    private static int normalisedFormal(AssessmentStructure structure, int formal) {
        return structure == AssessmentStructure.SIMPLE ? 100 : formal;
    }

    private static void validate(AssessmentStructure structure, int ca, int formal, CaFrequency frequency,
            int entries) {
        if (structure == null) {
            throw new RuleException("Choose an assessment structure");
        }
        if (structure == AssessmentStructure.SIMPLE) {
            return;
        }
        if (ca < 1 || ca > 99 || formal < 1 || formal > 99) {
            throw new RuleException("Continuous assessment and the formal test must each be between 1% and 99%");
        }
        if (ca + formal != 100) {
            throw new RuleException("Continuous assessment and the formal test must add up to 100% (now "
                    + (ca + formal) + "%)");
        }
        if (frequency == null) {
            throw new RuleException("Choose how often continuous assessment is recorded");
        }
        if (entries < 1 || entries > MAX_CA_ENTRIES) {
            throw new RuleException("Continuous assessment needs between 1 and " + MAX_CA_ENTRIES
                    + " recordings per assessment");
        }
    }

    // A readable one-liner for audit/activity details - "CA_AND_TEST: CA 30% / Test 70%, WEEKLY x6".
    public String summary() {
        if (structure == AssessmentStructure.SIMPLE) {
            return "SIMPLE (single score)";
        }
        return "CA_AND_TEST: CA " + caPercentage + "% / Formal " + formalPercentage + "%, " + caFrequency + " x"
                + caEntries + (plan.isEmpty() ? "" : ", " + plan.size() + " term/assessment override(s)");
    }
}
