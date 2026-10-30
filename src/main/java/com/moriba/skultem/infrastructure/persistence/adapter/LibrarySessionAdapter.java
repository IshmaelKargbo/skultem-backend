package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.LibrarySession;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.LibrarySessionJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.LibraryAttendanceMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LibrarySessionAdapter implements LibrarySessionRepository {
    private final LibrarySessionJpaRepository repo;

    @Override
    public void save(LibrarySession domain) {
        // Flushed now, so uq_library_sessions_entry_date refuses a second session for the same entry and date here.
        repo.saveAndFlush(LibraryAttendanceMapper.toEntity(domain));
    }

    @Override
    public Optional<LibrarySession> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(LibraryAttendanceMapper::toDomain);
    }

    @Override
    public Optional<LibrarySession> findByIdAndSchoolForUpdate(String id, String schoolId) {
        return repo.findForUpdate(id, schoolId).map(LibraryAttendanceMapper::toDomain);
    }

    @Override
    public Optional<LibrarySession> findByEntryAndDate(String schoolId, String timetableEntryId, LocalDate date) {
        return repo.findBySchoolIdAndTimetableEntryIdAndDate(schoolId, timetableEntryId, date)
                .map(LibraryAttendanceMapper::toDomain);
    }

    @Override
    public List<LibrarySession> findBetween(String schoolId, LocalDate from, LocalDate to) {
        return repo.findBySchoolIdAndDateBetweenOrderByDateAscStartTimeAsc(schoolId, from, to).stream()
                .map(LibraryAttendanceMapper::toDomain).toList();
    }

    @Override
    public List<LibrarySession> findByIds(String schoolId, Collection<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return repo.findBySchoolIdAndIdIn(schoolId, ids).stream().map(LibraryAttendanceMapper::toDomain).toList();
    }
}
