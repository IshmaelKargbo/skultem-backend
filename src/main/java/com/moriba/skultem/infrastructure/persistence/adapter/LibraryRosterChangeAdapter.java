package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.LibraryRosterChange;
import com.moriba.skultem.domain.repository.LibraryRosterChangeRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.LibraryRosterChangeJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.LibraryAttendanceMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LibraryRosterChangeAdapter implements LibraryRosterChangeRepository {
    private final LibraryRosterChangeJpaRepository repo;

    @Override
    public void save(LibraryRosterChange domain) {
        repo.save(LibraryAttendanceMapper.toEntity(domain));
    }

    @Override
    public List<LibraryRosterChange> findBySession(String sessionId, String schoolId) {
        return repo.findBySessionIdAndSchoolIdOrderByChangedAtAsc(sessionId, schoolId).stream()
                .map(LibraryAttendanceMapper::toDomain).toList();
    }
}
