package com.moriba.skultem.domain.repository;

import java.util.Map;

// What still points at a stream, so it is never deleted out from under classes, students or fees.
public interface StreamUsageRepository {

    /** How many records of each kind use this stream, by a label a school would recognise. Only kinds in use. */
    Map<String, Long> usage(String streamId);

    /** The subjects assigned to the stream itself - they belong to it and go with it. */
    void deleteOwnSubjects(String streamId);
}
