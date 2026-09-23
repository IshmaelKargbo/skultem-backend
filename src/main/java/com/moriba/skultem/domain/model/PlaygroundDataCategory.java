package com.moriba.skultem.domain.model;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A group of the data a school can generate while trying the system out as a playground (see
 * {@link School#isTestSchool()}) - the unit the school picks from when deciding what to clear before
 * going live. Mostly activity/roster data; SUBJECT_SETUP and CLASS_SETUP are the only setup a school
 * can opt into clearing (the UI leaves them unticked by default). Subjects, fee categories, academic
 * years, teachers, branding and the materials catalogue are never offered.
 *
 * Some groups can't be cleared on their own without leaving rows that point at nothing - wiping
 * students has to take their fees, marks, attendance, behaviour and issued materials with them, and
 * wiping fee payments has to take the material entitlements those payments created. Setup is the same:
 * grade sheets hang off teacher assignments, and enrollments, fee structures, class sessions (and the
 * timetable periods, class masters and schemes of work under them) hang off classes. {@link #requires()}
 * lists those, and {@link #withDependencies} closes a selection over them.
 */
public enum PlaygroundDataCategory {
    MATERIALS(List.of()),
    FEES(List.of(MATERIALS)),
    ASSESSMENTS(List.of()),
    ATTENDANCE(List.of()),
    BEHAVIOUR(List.of()),
    STUDENTS(List.of(FEES, ASSESSMENTS, ATTENDANCE, BEHAVIOUR, MATERIALS)),
    // Class subjects, stream subjects, teacher assignments, subject groups and timetable entries.
    SUBJECT_SETUP(List.of(ASSESSMENTS)),
    // Classes, sections and streams - and everything bound to a class (see above).
    CLASS_SETUP(List.of(STUDENTS, SUBJECT_SETUP)),
    EXPENSES(List.of()),
    PAYROLL(List.of()),
    STAFF_ATTENDANCE(List.of()),
    COMMUNICATION(List.of()),
    NOTIFICATIONS(List.of());

    private final List<PlaygroundDataCategory> requires;

    PlaygroundDataCategory(List<PlaygroundDataCategory> requires) {
        this.requires = requires;
    }

    public List<PlaygroundDataCategory> requires() {
        return requires;
    }

    public static Set<PlaygroundDataCategory> withDependencies(Collection<PlaygroundDataCategory> selected) {
        var result = EnumSet.noneOf(PlaygroundDataCategory.class);
        var pending = new ArrayDeque<>(selected);
        while (!pending.isEmpty()) {
            var next = pending.pop();
            if (result.add(next)) {
                pending.addAll(next.requires());
            }
        }
        return result;
    }
}
