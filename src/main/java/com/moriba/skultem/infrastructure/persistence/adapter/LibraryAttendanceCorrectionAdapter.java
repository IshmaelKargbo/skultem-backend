package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.LibraryAttendanceCorrection;
import com.moriba.skultem.domain.repository.LibraryAttendanceCorrectionRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.LibraryAttendanceCorrectionJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.LibraryAttendanceMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LibraryAttendanceCorrectionAdapter implements LibraryAttendanceCorrectionRepository {
    private final LibraryAttendanceCorrectionJpaRepository repo;

    @Override
    public void save(LibraryAttendanceCorrection domain) {
        repo.save(LibraryAttendanceMapper.toEntity(domain));
    }

    @Override
    public List<LibraryAttendanceCorrection> findBySession(String sessionId, String schoolId) {
        return repo.findBySessionIdAndSchoolIdOrderByCorrectedAtAsc(sessionId, schoolId).stream()
                .map(LibraryAttendanceMapper::toDomain).toList();
    }
}
