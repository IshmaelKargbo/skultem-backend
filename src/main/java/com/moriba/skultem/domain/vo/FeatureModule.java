package com.moriba.skultem.domain.vo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * An optional feature a school can install - the catalog behind the Modules page.
 * <p>
 * Everything not listed here is <b>core</b> and always on for every school: school settings, users
 * and roles, academic year and terms, classes/sections/streams, students and enrollment, teachers,
 * parents, subjects, student attendance, fees and payments, the dashboard and notifications.
 * (Fees is core, not a module, because the platform fee is charged through it.)
 * <p>
 * {@link #starter()} modules are installed on every newly-created school; the rest are installed
 * by the school (or a system admin) when needed. {@link #requires()} lists modules that must be
 * installed first - installing one installs its requirements, and a module can't be disabled while
 * another installed module still requires it.
 * <p>
 * The {@link #key()} (e.g. {@code report-cards}) is the stable identifier used in the API and the
 * frontend; the enum name is what's stored in the database.
 */
public enum FeatureModule {

    GRADING("Grading & Assessments", ModuleCategory.ACADEMICS,
            "Record assessments, grade students, and manage the grade scale and grade approvals.",
            true),

    REPORT_CARDS("Report Cards", ModuleCategory.ACADEMICS,
            "Design report card templates and generate term report cards for students.",
            true, GRADING),

    CURRICULUM("Curriculum Planning", ModuleCategory.ACADEMICS,
            "Plan schemes of work and follow each teacher's progress through them.",
            false),

    BEHAVIOUR("Behaviour Tracking", ModuleCategory.ACADEMICS,
            "Record positive and negative student behaviour, organised by category.",
            false),

    TIMETABLE("Timetable", ModuleCategory.ACADEMICS,
            "Build and publish class and teacher timetables.",
            false),

    EXPENSES("Expenses", ModuleCategory.FINANCE,
            "Track what the school spends, organised by expense category.",
            false),

    PAYROLL("Payroll", ModuleCategory.FINANCE,
            "Salary structures and templates, payroll runs, and payslips.",
            false),

    STAFF_HR("Staff & HR", ModuleCategory.PEOPLE,
            "Teacher attendance with clock-in and clock-out, leave requests, and management reports.",
            false),

    COMMUNICATION("Communication", ModuleCategory.COMMUNICATION,
            "Notice board and broadcast messages to parents, teachers, and staff.",
            false),

    ID_CARDS("ID Cards", ModuleCategory.OPERATIONS,
            "Design and print ID cards for students and staff.",
            false),

    ATHLETIC_HOUSES("Athletic Houses", ModuleCategory.OPERATIONS,
            "Organise students into houses and manage house activities.",
            false),

    MATERIALS_AND_SUPPLIES("Materials & Supplies", ModuleCategory.OPERATIONS,
            "Stock, sell, and hand out school materials such as books and uniforms.",
            false),

    ANALYTICS("Analytics & Reports", ModuleCategory.INSIGHTS,
            "Academic, financial, and demographic reports and charts.",
            false);

    private final String label;
    private final ModuleCategory category;
    private final String description;
    private final boolean starter;
    private final List<FeatureModule> requires;

    FeatureModule(String label, ModuleCategory category, String description, boolean starter,
            FeatureModule... requires) {
        this.label = label;
        this.category = category;
        this.description = description;
        this.starter = starter;
        this.requires = List.of(requires);
    }

    public String label() {
        return label;
    }

    public ModuleCategory category() {
        return category;
    }

    public String description() {
        return description;
    }

    public boolean starter() {
        return starter;
    }

    public List<FeatureModule> requires() {
        return requires;
    }

    /** Every module that lists this one in its {@link #requires()}. */
    public List<FeatureModule> requiredBy() {
        return Arrays.stream(values()).filter(m -> m.requires.contains(this)).toList();
    }

    /** Stable, URL-friendly identifier: {@code REPORT_CARDS} becomes {@code report-cards}. */
    public String key() {
        return name().toLowerCase().replace('_', '-');
    }

    public static Optional<FeatureModule> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(m -> m.key().equalsIgnoreCase(key.trim())).findFirst();
    }

    public static List<FeatureModule> starterModules() {
        return Arrays.stream(values()).filter(FeatureModule::starter).toList();
    }
}
