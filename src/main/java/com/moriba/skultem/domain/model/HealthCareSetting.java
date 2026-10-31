package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;
import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Level;

import lombok.Getter;

/**
 * A school's health care rules (docs/HEALTH_CARE.md §6): how often a student may visit, medication, what must be
 * recorded, who is told, and the complaint list. One per school; until a school saves its own it gets
 * {@link #defaults}. Changing a rule applies to visits started afterwards only (S-3).
 */
@Getter
public class HealthCareSetting extends AggregateRoot<String> {

    public static final int MAX_RULES = 20;
    public static final int MAX_CUSTOM_COMPLAINTS = 40;

    /** Calendar periods in the school's time zone (L-2). A week is Monday to Sunday. */
    public enum Period {
        DAY, WEEK, MONTH, TERM
    }

    /** What happens when a limit is reached. Declared from mildest to strictest - the strictest wins (S-5). */
    public enum LimitAction {
        WARN, REASON, REFUSE
    }

    /** When the same medicine is given again too soon (M-2). Never refuses. */
    public enum RepeatAction {
        WARN, REASON
    }

    /** Max {@code maxVisits} counted visits per {@code period}. No levels = the whole school. */
    public record LimitRule(String id, String name, List<Level> levels, int maxVisits, Period period,
            LimitAction action, boolean enabled) {

        public boolean appliesTo(Level level) {
            return enabled && (levels == null || levels.isEmpty() || (level != null && levels.contains(level)));
        }
    }

    public record CustomComplaint(String code, String label, boolean urgent, boolean hidden) {
    }

    /** How the school changed the standard list, plus its own complaints. */
    public record ComplaintSettings(List<String> hidden, List<String> urgent, List<CustomComplaint> custom) {
        public static ComplaintSettings empty() {
            return new ComplaintSettings(List.of(), List.of(), List.of());
        }
    }

    /** Everything a principal can change, in one go. Nulls in the optional numbers mean "off". */
    public record Values(List<LimitRule> limitRules, boolean medicationAllowed, Integer sameMedicineHours,
            RepeatAction sameMedicineAction, Integer maxMedicationLines, boolean requireBroughtBy,
            boolean requireTemperature, boolean requireTreatment, boolean requireNotesOnReturn,
            Set<Outcome> notifyOutcomes, boolean allowManualNotify, boolean notifyPrincipalOnReferral,
            Integer principalAlertVisits, Period principalAlertPeriod, Integer longStayMinutes,
            ComplaintSettings complaints) {
    }

    private final String schoolId;
    private List<LimitRule> limitRules;
    private boolean medicationAllowed;
    private Integer sameMedicineHours;
    private RepeatAction sameMedicineAction;
    private Integer maxMedicationLines;
    private boolean requireBroughtBy;
    private boolean requireTemperature;
    private boolean requireTreatment;
    private boolean requireNotesOnReturn;
    private Set<Outcome> notifyOutcomes;
    private boolean allowManualNotify;
    private boolean notifyPrincipalOnReferral;
    private Integer principalAlertVisits;
    private Period principalAlertPeriod;
    private Integer longStayMinutes;
    private ComplaintSettings complaints;
    private String updatedBy;
    private String updatedByName;

    public HealthCareSetting(String id, String schoolId, Values v, String updatedBy, String updatedByName,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        apply(v);
        this.updatedBy = updatedBy;
        this.updatedByName = updatedByName;
        touch(updatedAt);
    }

    /** §6.7: what a school gets until it changes anything. */
    public static HealthCareSetting defaults(String schoolId) {
        Instant now = Instant.now();
        return new HealthCareSetting(UUID.randomUUID().toString(), schoolId, defaultValues(), null, null, now, now);
    }

    public static Values defaultValues() {
        return new Values(List.of(), true, 4, RepeatAction.REASON, null, false, false, true, false,
                EnumSet.of(Outcome.SENT_HOME, Outcome.REFERRED), true, true, null, null, 60,
                ComplaintSettings.empty());
    }

    /** Saves new rules after checking every one of them. Nothing changes if any is invalid. */
    public void update(Values v, String userId, String userName, Instant now) {
        var checked = validate(v);
        apply(checked);
        this.updatedBy = userId;
        this.updatedByName = userName;
        touch(now);
    }

    private void apply(Values v) {
        this.limitRules = List.copyOf(v.limitRules() == null ? List.of() : v.limitRules());
        this.medicationAllowed = v.medicationAllowed();
        this.sameMedicineHours = v.sameMedicineHours();
        this.sameMedicineAction = v.sameMedicineAction() == null ? RepeatAction.REASON : v.sameMedicineAction();
        this.maxMedicationLines = v.maxMedicationLines();
        this.requireBroughtBy = v.requireBroughtBy();
        this.requireTemperature = v.requireTemperature();
        this.requireTreatment = v.requireTreatment();
        this.requireNotesOnReturn = v.requireNotesOnReturn();
        this.notifyOutcomes = v.notifyOutcomes() == null || v.notifyOutcomes().isEmpty() ? EnumSet.noneOf(Outcome.class)
                : EnumSet.copyOf(v.notifyOutcomes());
        this.allowManualNotify = v.allowManualNotify();
        this.notifyPrincipalOnReferral = v.notifyPrincipalOnReferral();
        this.principalAlertVisits = v.principalAlertVisits();
        this.principalAlertPeriod = v.principalAlertVisits() == null ? null : v.principalAlertPeriod();
        this.longStayMinutes = v.longStayMinutes();
        this.complaints = v.complaints() == null ? ComplaintSettings.empty() : v.complaints();
    }

    private static Values validate(Values v) {
        List<LimitRule> rules = new ArrayList<>();
        var given = v.limitRules() == null ? List.<LimitRule>of() : v.limitRules();
        if (given.size() > MAX_RULES) {
            throw new IllegalArgumentException("A school can have at most " + MAX_RULES + " limit rules");
        }
        for (LimitRule r : given) {
            String name = r.name() == null ? "" : r.name().trim();
            if (name.isEmpty() || name.length() > 100) {
                throw new IllegalArgumentException("Each limit rule needs a name of up to 100 characters");
            }
            if (r.maxVisits() < 1 || r.maxVisits() > 50) {
                throw new IllegalArgumentException("\"" + name + "\": the visit limit must be between 1 and 50");
            }
            if (r.period() == null || r.action() == null) {
                throw new IllegalArgumentException("\"" + name + "\": choose a period and what happens at the limit");
            }
            var levels = r.levels() == null ? List.<Level>of() : List.copyOf(new LinkedHashSet<>(r.levels()));
            String id = r.id() == null || r.id().isBlank() ? UUID.randomUUID().toString() : r.id();
            rules.add(new LimitRule(id, name, levels, r.maxVisits(), r.period(), r.action(), r.enabled()));
        }

        between(v.sameMedicineHours(), 1, 24, "The same-medicine check must be between 1 and 24 hours");
        between(v.maxMedicationLines(), 1, 10, "The medication limit per visit must be between 1 and 10");
        between(v.longStayMinutes(), 10, 240, "The long-stay alert must be between 10 and 240 minutes");
        between(v.principalAlertVisits(), 1, 50, "The principal alert must be between 1 and 50 visits");
        if (v.principalAlertVisits() != null
                && (v.principalAlertPeriod() == null || v.principalAlertPeriod() == Period.DAY)) {
            throw new IllegalArgumentException("Choose a week, month or term for the principal alert");
        }

        return new Values(rules, v.medicationAllowed(), v.sameMedicineHours(), v.sameMedicineAction(),
                v.maxMedicationLines(), v.requireBroughtBy(), v.requireTemperature(), v.requireTreatment(),
                v.requireNotesOnReturn(), v.notifyOutcomes(), v.allowManualNotify(), v.notifyPrincipalOnReferral(),
                v.principalAlertVisits(), v.principalAlertPeriod(), v.longStayMinutes(),
                validateComplaints(v.complaints()));
    }

    private static ComplaintSettings validateComplaints(ComplaintSettings c) {
        if (c == null) {
            return ComplaintSettings.empty();
        }
        Set<String> known = new HashSet<>();
        HealthCareComplaint.STANDARD.forEach(s -> known.add(s.code()));

        var customs = c.custom() == null ? List.<CustomComplaint>of() : c.custom();
        if (customs.size() > MAX_CUSTOM_COMPLAINTS) {
            throw new IllegalArgumentException("A school can add at most " + MAX_CUSTOM_COMPLAINTS + " complaints");
        }
        Set<String> labels = new HashSet<>();
        HealthCareComplaint.STANDARD.forEach(s -> labels.add(s.label().toLowerCase(Locale.ROOT)));
        List<CustomComplaint> custom = new ArrayList<>();
        for (CustomComplaint cc : customs) {
            String label = cc.label() == null ? "" : cc.label().trim();
            if (label.isEmpty() || label.length() > 60) {
                throw new IllegalArgumentException("Each added complaint needs a name of up to 60 characters");
            }
            if (!labels.add(label.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("\"" + label + "\" is already on the complaint list");
            }
            String code = cc.code() == null || cc.code().isBlank()
                    ? "CUSTOM_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT)
                    : cc.code();
            if (HealthCareComplaint.isStandard(code)) {
                throw new IllegalArgumentException("\"" + label + "\" can't reuse a standard complaint's code");
            }
            known.add(code);
            custom.add(new CustomComplaint(code, label, cc.urgent(), cc.hidden()));
        }

        var hidden = distinct(c.hidden());
        for (String code : hidden) {
            if (HealthCareComplaint.isAlwaysShown(code)) {
                throw new IllegalArgumentException(label(code) + " can't be hidden");
            }
            if (!HealthCareComplaint.isStandard(code)) {
                throw new IllegalArgumentException("Unknown complaint: " + code);
            }
        }
        var urgent = distinct(c.urgent());
        for (String code : urgent) {
            if (!HealthCareComplaint.isStandard(code)) {
                throw new IllegalArgumentException("Unknown complaint: " + code);
            }
        }
        return new ComplaintSettings(hidden, urgent, custom);
    }

    private static List<String> distinct(List<String> codes) {
        return codes == null ? List.of() : List.copyOf(new LinkedHashSet<>(codes));
    }

    private static String label(String code) {
        return HealthCareComplaint.STANDARD.stream().filter(s -> s.code().equals(code)).findFirst()
                .map(HealthCareComplaint::label).orElse(code);
    }

    private static void between(Integer value, int min, int max, String message) {
        if (value != null && (value < min || value > max)) {
            throw new IllegalArgumentException(message);
        }
    }

    /** The school's whole complaint list: standard ones (with its changes), then its own. Hidden ones included. */
    public List<HealthCareComplaint> complaintList() {
        var hidden = new HashSet<>(complaints.hidden());
        var urgent = new HashSet<>(complaints.urgent());
        List<HealthCareComplaint> list = new ArrayList<>();
        for (var s : HealthCareComplaint.STANDARD) {
            list.add(new HealthCareComplaint(s.code(), s.label(), s.urgent() || urgent.contains(s.code()), true,
                    hidden.contains(s.code())));
        }
        for (var c : complaints.custom()) {
            list.add(new HealthCareComplaint(c.code(), c.label(), c.urgent(), false, c.hidden()));
        }
        return list;
    }

    public Optional<HealthCareComplaint> complaint(String code) {
        return complaintList().stream().filter(c -> c.code().equals(code)).findFirst();
    }

    /** Codes of the school's own complaints, so a use case can refuse deleting one that visits use (C-3). */
    public Set<String> customCodes() {
        Set<String> codes = new HashSet<>();
        complaints.custom().forEach(c -> codes.add(c.code()));
        return codes;
    }

    public boolean notifiesParentsFor(Outcome outcome) {
        return notifyOutcomes.contains(outcome);
    }
}
