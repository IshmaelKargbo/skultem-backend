package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaComponent;
import com.moriba.skultem.domain.vo.CaFrequency;

import lombok.Getter;

// How a management section (or the whole school) assesses its students. Set by school administrators;
// teachers only use it. Changing it never touches assessments that are already open or recorded - each of
// those froze its own copy when it opened (see ClassSubjectAssessmentLifeCycle#freezeStructure).
@Getter
public class AssessmentConfiguration extends AggregateRoot<String> {

    public static final int MAX_CA_ENTRIES = 40;
    // A plan entry for this "term" applies in every term.
    public static final String ALL_TERMS = "*";

    private String schoolId;
    // null = the school-wide configuration.
    private String managementSectionId;
    // Set when this is an assessment template's own configuration (it wins over the section / school-wide one).
    private String assessmentTemplateId;
    private AssessmentStructure structure;
    private int caPercentage;
    private int formalPercentage;
    private CaFrequency caFrequency;
    private int caEntries;
    private int version;
    private String updatedByUserId;
    // Per-term, per-assessment overrides (see PlanEntry). Empty = every assessment follows the defaults above.
    private final java.util.List<PlanEntry> plan = new java.util.ArrayList<>();
    // Optional: the CA part split into named, weighted components (each a share of the whole assessment), and the
    // name of the formal part. Empty = the CA is N equal recordings, as before.
    private final java.util.List<CaComponent> components = new java.util.ArrayList<>();
    private String formalLabel;

    // One assessment in one term ("Test 1" in Term 2): whether it uses CA + formal test and, if so, how many CA
    // recordings it has. A short term, or a short test, simply has fewer weeks.
    // `components`/`formalLabel` are optional: when this assessment has its own named CA parts they replace the
    // defaults' (and set its CA/formal split); empty = follow the defaults.
    public record PlanEntry(String termId, String assessmentName, boolean usesCa, int caEntries,
            java.util.List<CaComponent> components, String formalLabel) {
        public PlanEntry(String termId, String assessmentName, boolean usesCa, int caEntries) {
            this(termId, assessmentName, usesCa, caEntries, java.util.List.of(), null);
        }
    }

    // What an assessment actually gets: the plan's entry for it, else the section defaults.
    public record Resolved(AssessmentStructure structure, int caPercentage, int formalPercentage,
            CaFrequency caFrequency, int caEntries, java.util.List<CaComponent> components, String formalLabel) {
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

    public AssessmentConfiguration forTemplate(String templateId) {
        this.assessmentTemplateId = templateId;
        return this;
    }

    public boolean isDefault() {
        return getId() == null;
    }

    // The configuration for one assessment in one term. Names match case-insensitively; nulls just get the defaults.
    public Resolved forAssessment(String termId, String assessmentName) {
        if (structure == AssessmentStructure.CA_AND_TEST && assessmentName != null) {
            var entry = planEntryFor(termId, assessmentName.trim());
            if (entry != null) {
                if (!entry.usesCa()) {
                    return new Resolved(AssessmentStructure.SIMPLE, 0, 100, null, 0, java.util.List.of(), null);
                }
                if (!entry.components().isEmpty()) {
                    int ca = CaComponent.total(entry.components());
                    return new Resolved(structure, ca, 100 - ca, CaFrequency.CUSTOM, entry.components().size(),
                            java.util.List.copyOf(entry.components()), entry.formalLabel());
                }
                // With components, they decide how many recordings there are.
                return new Resolved(structure, caPercentage, formalPercentage, caFrequency,
                        components.isEmpty() ? entry.caEntries() : components.size(),
                        java.util.List.copyOf(components), formalLabel);
            }
            // A template's own setup lists exactly the tests that use CA ("Test 1"); every other test (the Exam, or
            // one added later) stays a single score. The older section / school-wide setup still applies to all.
            if (assessmentTemplateId != null) {
                return new Resolved(AssessmentStructure.SIMPLE, 0, 100, null, 0, java.util.List.of(), null);
            }
        }
        if (structure != AssessmentStructure.CA_AND_TEST) {
            return new Resolved(structure, caPercentage, formalPercentage, caFrequency, caEntries,
                    java.util.List.of(), null);
        }
        return new Resolved(structure, caPercentage, formalPercentage, caFrequency, caEntries,
                java.util.List.copyOf(components), formalLabel);
    }

    // Splits the CA into named, weighted components. Each weight is a share of the whole assessment, so together
    // with the formal test they make 100: Attendance 20 + Assignment 20 + Participation 20 + Test 40. Call after
    // change(), which sets the CA/formal split these must add up to. Passing none goes back to equal recordings.
    public void replaceComponents(java.util.List<CaComponent> list, String formalLabel) {
        components.clear();
        this.formalLabel = null;
        if (structure != AssessmentStructure.CA_AND_TEST || list == null || list.isEmpty()) {
            return;
        }
        if (caPercentage == 0) {
            throw new RuleException("CA components need the CA to count towards the score");
        }
        var clean = CaComponent.clean(list, MAX_CA_ENTRIES);
        int sum = CaComponent.total(clean);
        if (sum != caPercentage) {
            throw new RuleException("The CA components add up to " + sum + "% but the CA is " + caPercentage + "%");
        }
        components.addAll(clean);
        this.formalLabel = formalLabel == null || formalLabel.isBlank() ? null : formalLabel.trim();
        this.caEntries = clean.size();
        this.caFrequency = CaFrequency.CUSTOM;
    }

    public void restoreComponents(String text, String formalLabel) {
        components.clear();
        components.addAll(CaComponent.decode(text));
        this.formalLabel = formalLabel;
    }

    public String componentsAsText() {
        return CaComponent.encode(components);
    }

    public java.util.List<CaComponent> getComponents() {
        return java.util.Collections.unmodifiableList(components);
    }

    // The plan entry for a test: the one for this term, else one that covers every term (termId "*").
    private PlanEntry planEntryFor(String termId, String assessmentName) {
        PlanEntry anyTerm = null;
        for (var entry : plan) {
            if (!entry.assessmentName().equalsIgnoreCase(assessmentName)) {
                continue;
            }
            if (termId != null && entry.termId().equals(termId)) {
                return entry;
            }
            if (ALL_TERMS.equals(entry.termId())) {
                anyTerm = entry;
            }
        }
        return anyTerm;
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
                var own = e.usesCa() ? CaComponent.clean(e.components(), MAX_CA_ENTRIES) : java.util.List.<CaComponent>of();
                if (!own.isEmpty()) {
                    int ca = CaComponent.total(own);
                    if (ca < 1 || ca > 99) {
                        throw new RuleException(name + ": the CA parts must add up to between 1% and 99%, leaving the rest for "
                                + "the formal test (now " + ca + "%)");
                    }
                    if (!seen.add(e.termId() + "|" + name.toLowerCase())) {
                        throw new RuleException(name + " is listed twice for the same term");
                    }
                    var label = e.formalLabel() == null || e.formalLabel().isBlank() ? null : e.formalLabel().trim();
                    clean.add(new PlanEntry(e.termId(), name, true, own.size(), own, label));
                    continue;
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
                .map(e -> e.termId() + "|" + e.assessmentName() + "|" + (e.usesCa() ? 1 : 0) + "|" + e.caEntries()
                        + (e.components().isEmpty() ? ""
                                : "|" + CaComponent.encodeInline(e.components()) + "|"
                                        + (e.formalLabel() == null ? "" : e.formalLabel())))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    public void restorePlan(String text) {
        plan.clear();
        if (text == null || text.isBlank()) {
            return;
        }
        for (var line : text.split("\n")) {
            var p = line.split("\\|", -1);
            if (p.length == 4) {
                plan.add(new PlanEntry(p[0], p[1], "1".equals(p[2]), Integer.parseInt(p[3])));
            } else if (p.length == 6) {
                plan.add(new PlanEntry(p[0], p[1], "1".equals(p[2]), Integer.parseInt(p[3]),
                        CaComponent.decodeInline(p[4]), p[5].isEmpty() ? null : p[5]));
            }
        }
    }

    public boolean isContinuous() {
        return structure == AssessmentStructure.CA_AND_TEST;
    }

    // CA is recorded to watch a student's strength but counts for nothing in the score (CA 0% / formal 100%).
    public boolean isMonitorOnly() {
        return isContinuous() && caPercentage == 0;
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
            this.components.clear();
            this.formalLabel = null;
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
        // CA at 0% is "monitor only": it is still recorded (a strength indicator) but isn't part of the score,
        // so the formal test is the whole 100%.
        if (ca == 0) {
            if (formal != 100) {
                throw new RuleException("When continuous assessment is only for monitoring, the formal test is 100%");
            }
        } else if (ca < 1 || ca > 99 || formal < 1 || formal > 99) {
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
        if (isMonitorOnly()) {
            return "CA_AND_TEST (CA monitor only, formal 100%): " + caFrequency + " x" + caEntries
                    + (plan.isEmpty() ? "" : ", " + plan.size() + " term/assessment override(s)");
        }
        return "CA_AND_TEST: CA " + caPercentage + "% / Formal " + formalPercentage + "%, " + caFrequency + " x"
                + caEntries
                + (components.isEmpty() ? "" : ", components " + components.stream()
                        .map(c -> c.name() + " " + c.weight() + "%").collect(java.util.stream.Collectors.joining(", ")))
                + (plan.isEmpty() ? "" : ", " + plan.size() + " term/assessment override(s)");
    }
}
