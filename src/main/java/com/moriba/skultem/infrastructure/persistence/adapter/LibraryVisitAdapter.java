package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.LibraryVisit;
import com.moriba.skultem.domain.model.LibraryVisit.Status;
import com.moriba.skultem.domain.repository.LibraryVisitRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.LibraryVisitJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.LibraryAttendanceMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LibraryVisitAdapter implements LibraryVisitRepository {
    // Stand-ins for "no limit" on a time window; both fit in a Postgres timestamptz column.
    private static final Instant EARLIEST = Instant.parse("1900-01-01T00:00:00Z");
    private static final Instant LATEST = Instant.parse("9999-12-31T00:00:00Z");

    private final LibraryVisitJpaRepository repo;

    @Override
    public void save(LibraryVisit domain) {
        // Flushed now, so uq_library_visits_one_open refuses a second open visit here (as a
        // DataIntegrityViolationException the use case turns into a refusal), not at commit.
        repo.saveAndFlush(LibraryAttendanceMapper.toEntity(domain));
    }

    @Override
    public Optional<LibraryVisit> findByIdAndSchoolForUpdate(String id, String schoolId) {
        return repo.findForUpdate(id, schoolId).map(LibraryAttendanceMapper::toDomain);
    }

    @Override
    public List<LibraryVisit> findOpenByStudents(String schoolId, Collection<String> studentIds) {
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return repo.findBySchoolIdAndStatusAndStudentIdIn(schoolId, Status.OPEN, studentIds).stream()
                .map(LibraryAttendanceMapper::toDomain).toList();
    }

    @Override
    public Page<LibraryVisit> search(String schoolId, Instant from, Instant to, String studentId, String query,
            Filter filter, Instant now, Pageable pageable) {
        List<Status> statuses = List.of(Status.OPEN, Status.CHECKED_OUT, Status.RESOLVED);
        Instant endsBy = LATEST;

        switch (filter == null ? Filter.ALL : filter) {
            case OPEN -> statuses = List.of(Status.OPEN);
            case UNRESOLVED -> {
                statuses = List.of(Status.OPEN);
                endsBy = now;
            }
            case CHECKED_OUT -> statuses = List.of(Status.CHECKED_OUT);
            case RESOLVED -> statuses = List.of(Status.RESOLVED);
            case ALL -> {
            }
        }

        return repo.search(schoolId, statuses, from == null ? EARLIEST : from, to == null ? LATEST : to, endsBy,
                studentId == null ? "" : studentId, query == null ? "" : query, pageable)
                .map(LibraryAttendanceMapper::toDomain);
    }

    @Override
    public long countOpen(String schoolId) {
        return repo.countBySchoolIdAndStatus(schoolId, Status.OPEN);
    }

    @Override
    public long countUnresolved(String schoolId, Instant now) {
        return repo.countBySchoolIdAndStatusAndSchoolDayEndsAtLessThanEqual(schoolId, Status.OPEN, now);
    }
}
