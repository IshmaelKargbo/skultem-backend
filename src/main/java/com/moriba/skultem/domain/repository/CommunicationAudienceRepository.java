package com.moriba.skultem.domain.repository;

import java.util.Set;

// Who an announcement for one management section reaches - and which sections a parent belongs to.
public interface CommunicationAudienceRepository {

    // The management sections that a parent's children are currently enrolled in (via their class's level).
    Set<String> sectionIdsOfChildren(String schoolId, String parentUserId);

    Counts countsForSection(String schoolId, String sectionId);

    record Counts(int students, int parents, int teachers, int staff) {
    }
}
