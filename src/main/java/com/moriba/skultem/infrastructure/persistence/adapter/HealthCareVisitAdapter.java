package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.HealthCareVisit;
import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;
import com.moriba.skultem.domain.model.HealthCareVisit.Status;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.HealthCareVisitJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.HealthCareMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class HealthCareVisitAdapter implements HealthCareVisitRepository {
    // Stand-ins for "no limit" on a time window; both fit in a Postgres timestamptz column.
    private static final Instant EARLIEST = Instant.parse("1900-01-01T00:00:00Z");
    private static final Instant LATEST = Instant.parse("9999-12-31T00:00:00Z");

    private final HealthCareVisitJpaRepository repo;

    @Override
    public void save(HealthCareVisit domain) {
        // Flushed now, so uq_health_care_visits_one_open refuses a second open visit here (as a
        // DataIntegrityViolationException the use case turns into a refusal), not at commit.
        repo.saveAndFlush(HealthCareMapper.toEntity(domain));
    }

    @Override
    public Optional<HealthCareVisit> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(HealthCareMapper::toDomain);
    }

    @Override
    public Optional<HealthCareVisit> findByIdAndSchoolForUpdate(String id, String schoolId) {
        return repo.findForUpdate(id, schoolId).map(HealthCareMapper::toDomain);
    }

    @Override
    public Optional<HealthCareVisit> findOpenByStudent(String schoolId, String studentId) {
        return repo.findFirstBySchoolIdAndStudentIdAndStatus(schoolId, studentId, Status.OPEN)
                .map(HealthCareMapper::toDomain);
    }

    @Override
    public List<HealthCareVisit> findByStudentSince(String schoolId, String studentId, Instant since) {
        return repo.findBySchoolIdAndStudentIdAndArrivedAtGreaterThanEqualOrderByArrivedAtAsc(schoolId, studentId,
                since).stream().map(HealthCareMapper::toDomain).toList();
    }

    @Override
    public List<HealthCareVisit> findByStudent(String schoolId, String studentId) {
        return repo.findBySchoolIdAndStudentIdOrderByArrivedAtDesc(schoolId, studentId).stream()
                .map(HealthCareMapper::toDomain).toList();
    }

    @Override
    public List<HealthCareVisit> findBetween(String schoolId, Instant from, Instant to) {
        return repo.findBySchoolIdAndArrivedAtGreaterThanEqualAndArrivedAtLessThanOrderByArrivedAtDesc(schoolId, from,
                to).stream().map(HealthCareMapper::toDomain).toList();
    }

    @Override
    public List<HealthCareVisit> findOpen(String schoolId) {
        return repo.findBySchoolIdAndStatusOrderByArrivedAtAsc(schoolId, Status.OPEN).stream()
                .map(HealthCareMapper::toDomain).toList();
    }

    @Override
    public Page<HealthCareVisit> search(String schoolId, Search s, Pageable pageable) {
        var statuses = EnumSet.allOf(Status.class);
        Instant endsBy = LATEST;
        switch (s.filter() == null ? Filter.ALL : s.filter()) {
            case OPEN -> statuses = EnumSet.of(Status.OPEN);
            case FOLLOW_UP -> {
                statuses = EnumSet.of(Status.OPEN);
                endsBy = s.now();
            }
            case CLOSED -> statuses = EnumSet.of(Status.CLOSED);
            case VOIDED -> statuses = EnumSet.of(Status.VOIDED);
            case ALL -> {
            }
        }
        boolean anyOutcome = s.outcome() == null;
        List<Outcome> outcomes = anyOutcome ? List.of(Outcome.values()) : List.of(s.outcome());
        return repo.search(schoolId, statuses, anyOutcome, outcomes, s.from() == null ? EARLIEST : s.from(),
                s.to() == null ? LATEST : s.to(), endsBy, blank(s.complaint()), s.overLimitOnly(),
                blank(s.studentId()), blank(s.query()), pageable).map(HealthCareMapper::toDomain);
    }

    @Override
    public boolean existsWithComplaint(String schoolId, String code) {
        return repo.existsBySchoolIdAndComplaintCodesContaining(schoolId, "," + code + ",");
    }

    private static String blank(String value) {
        return value == null ? "" : value.trim();
    }
}
