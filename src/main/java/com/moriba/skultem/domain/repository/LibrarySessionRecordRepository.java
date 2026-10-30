package com.moriba.skultem.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.LibrarySessionRecord;

public interface LibrarySessionRecordRepository {

    void save(LibrarySessionRecord domain);

    void saveAll(Collection<LibrarySessionRecord> records);

    Optional<LibrarySessionRecord> findByIdAndSchool(String id, String schoolId);

    Optional<LibrarySessionRecord> findBySessionAndStudent(String sessionId, String studentId, String schoolId);

    // Ordered by student name, the order the register is shown in.
    List<LibrarySessionRecord> findBySession(String sessionId, String schoolId);

    List<LibrarySessionRecord> findBySessions(Collection<String> sessionIds, String schoolId);

    List<LibrarySessionRecord> findByStudent(String studentId, String schoolId);
}
