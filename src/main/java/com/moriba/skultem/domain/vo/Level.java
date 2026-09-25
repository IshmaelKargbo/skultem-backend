package com.moriba.skultem.domain.vo;

// The educational levels a class can belong to - the catalog every school picks its offered levels
// from (see SchoolLevel). Declaration order is the natural youngest-to-oldest display order.
//
// Level-specific rules live here as properties instead of `level == SSS` checks scattered around
// the use cases, so adding a level later is one new constant here (plus the CHECK constraints in a
// migration) rather than a hunt through every rule.
public enum Level {
    DAYCARE("Daycare", false, true),
    NURSERY("Nursery", false, true),
    PRIMARY("Primary", false, true),
    JSS("JSS", false, false),
    SSS("SSS", true, false);

    private final String label;
    private final boolean streamed;
    private final boolean allSubjectsCore;

    Level(String label, boolean streamed, boolean allSubjectsCore) {
        this.label = label;
        this.streamed = streamed;
        this.allSubjectsCore = allSubjectsCore;
    }

    public String getLabel() {
        return label;
    }

    // Classes are split into streams (Science/Arts/Commercial): every class session and enrollment
    // needs one, and subjects/subject groups hang off the stream rather than the class.
    public boolean isStreamed() {
        return streamed;
    }

    // Every subject is core - no optional subjects, so no subject groups either.
    public boolean isAllSubjectsCore() {
        return allSubjectsCore;
    }

    // Subject groups attach to the class itself (streamed levels attach them to a stream instead).
    public boolean allowsClassSubjectGroups() {
        return !streamed && !allSubjectsCore;
    }
}
