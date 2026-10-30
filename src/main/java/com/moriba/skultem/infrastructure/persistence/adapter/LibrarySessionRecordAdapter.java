package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.LibrarySessionRecord;
import com.moriba.skultem.domain.repository.LibrarySessionRecordRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.LibrarySessionRecordJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.LibraryAttendanceMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LibrarySessionRecordAdapter implements LibrarySessionRecordRepository {
    private final LibrarySessionRecordJpaRepository repo;

    @Override
    public void save(LibrarySessionRecord domain) {
        repo.save(LibraryAttendanceMapper.toEntity(domain));
    }

    @Override
    public void saveAll(Collection<LibrarySessionRecord> records) {
        repo.saveAll(records.stream().map(LibraryAttendanceMapper::toEntity).toList());
    }

    @Override
    public Optional<LibrarySessionRecord> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(LibraryAttendanceMapper::toDomain);
    }

    @Override
    public Optional<LibrarySessionRecord> findBySessionAndStudent(String sessionId, String studentId,
            String schoolId) {
        return repo.findBySessionIdAndStudentIdAndSchoolId(sessionId, studentId, schoolId)
                .map(LibraryAttendanceMapper::toDomain);
    }

    @Override
    public List<LibrarySessionRecord> findBySession(String sessionId, String schoolId) {
        return repo.findBySessionIdAndSchoolIdOrderByStudentNameAsc(sessionId, schoolId).stream()
                .map(LibraryAttendanceMapper::toDomain).toList();
    }

    @Override
    public List<LibrarySessionRecord> findBySessions(Collection<String> sessionIds, String schoolId) {
        if (sessionIds.isEmpty()) {
            return List.of();
        }
        return repo.findBySessionIdInAndSchoolId(sessionIds, schoolId).stream()
                .map(LibraryAttendanceMapper::toDomain).toList();
    }

    @Override
    public List<LibrarySessionRecord> findByStudent(String studentId, String schoolId) {
        return repo.findByStudentIdAndSchoolId(studentId, schoolId).stream()
                .map(LibraryAttendanceMapper::toDomain).toList();
    }
}
