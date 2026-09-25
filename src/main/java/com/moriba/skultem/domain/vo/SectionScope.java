package com.moriba.skultem.domain.vo;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// How much of a school the current caller can see. Whole-school for owners/proprietors/super admins,
// every UNIFIED school, and any staff member with no management sections assigned; otherwise the
// levels managed by the sections they're assigned. Level is the key because every scoped record
// reaches one through its class (Enrollment / ClassSession / FeeStructure -> Clazz.level).
public record SectionScope(boolean wholeSchool, Set<Level> levels, List<String> sectionIds,
        List<String> sectionNames) {

    private static final SectionScope WHOLE_SCHOOL = new SectionScope(true, EnumSet.allOf(Level.class), List.of(),
            List.of());

    public static SectionScope all() {
        return WHOLE_SCHOOL;
    }

    public static SectionScope sections(Set<Level> levels, List<String> sectionIds, List<String> sectionNames) {
        return new SectionScope(false, levels.isEmpty() ? EnumSet.noneOf(Level.class) : EnumSet.copyOf(levels),
                List.copyOf(sectionIds), List.copyOf(sectionNames));
    }

    public boolean allows(Level level) {
        return wholeSchool || (level != null && levels.contains(level));
    }

    // For `level IN :levels` query filters: every level when whole-school, so one query shape
    // serves everyone. Never empty for a scoped caller - sections always manage at least one level.
    public Collection<Level> queryLevels() {
        return levels;
    }
}
