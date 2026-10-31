package com.moriba.skultem.domain.model;

import java.util.List;
import java.util.Set;

/**
 * One entry on a school's complaint list (docs/HEALTH_CARE.md §6.6). The standard list is the same for every
 * school; a school can hide standard entries, add its own, and mark more of them urgent.
 * <p>
 * {@code urgent} complaints are never refused by a limit rule (L-4). The six in {@link #LOCKED_URGENT} are
 * always urgent and always shown, and "Other" is always shown - no school setting can change that (C-2).
 */
public record HealthCareComplaint(String code, String label, boolean urgent, boolean standard, boolean hidden) {

    public static final String OTHER = "OTHER";

    public static final Set<String> LOCKED_URGENT = Set.of("BREATHING", "DIZZINESS_FAINTING", "ALLERGIC_REACTION",
            "INJURY_CUT", "INJURY_SPRAIN", "NOSEBLEED");

    /** The standard list, in the order it's shown. Codes never change: visits and reports refer to them. */
    public static final List<HealthCareComplaint> STANDARD = List.of(
            standard("HEADACHE", "Headache"),
            standard("STOMACH_ACHE", "Stomach ache"),
            standard("FEVER", "Fever / feeling hot"),
            standard("VOMITING", "Vomiting"),
            standard("DIARRHOEA", "Diarrhoea"),
            standard("COUGH_COLD", "Cough / cold"),
            standard("BREATHING", "Breathing difficulty"),
            standard("DIZZINESS_FAINTING", "Dizziness / fainting"),
            standard("INJURY_CUT", "Injury: cut or graze"),
            standard("INJURY_SPRAIN", "Injury: sprain, bump or fall"),
            standard("NOSEBLEED", "Nosebleed"),
            standard("TOOTHACHE", "Toothache"),
            standard("EYE", "Eye problem"),
            standard("RASH", "Skin rash / itching"),
            standard("MENSTRUAL_PAIN", "Menstrual pain"),
            standard("ALLERGIC_REACTION", "Allergic reaction"),
            standard("UNWELL", "Feeling unwell (general)"),
            standard(OTHER, "Other"));

    private static HealthCareComplaint standard(String code, String label) {
        return new HealthCareComplaint(code, label, LOCKED_URGENT.contains(code), true, false);
    }

    /** Can't be hidden: the locked urgent ones and "Other" (C-2). */
    public static boolean isAlwaysShown(String code) {
        return LOCKED_URGENT.contains(code) || OTHER.equals(code);
    }

    public static boolean isStandard(String code) {
        return STANDARD.stream().anyMatch(c -> c.code().equals(code));
    }
}
