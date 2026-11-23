package com.moriba.skultem.domain.repository;

import java.util.Map;

// What still points at a section, so it is never deleted out from under classes or students.
public interface SectionUsageRepository {

    /** How many records of each kind use this section, by a label a school would recognise. Only kinds in use. */
    Map<String, Long> usage(String sectionId);
}
