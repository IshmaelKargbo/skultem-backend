package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;
import com.moriba.skultem.domain.model.HealthCareVisit.Status;
import com.moriba.skultem.infrastructure.persistence.entity.HealthCareVisitEntity;

import jakarta.persistence.LockModeType;

public interface HealthCareVisitJpaRepository extends JpaRepository<HealthCareVisitEntity, String> {

    Optional<HealthCareVisitEntity> findByIdAndSchoolId(String id, String schoolId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from HealthCareVisitEntity v where v.id = :id and v.schoolId = :schoolId")
    Optional<HealthCareVisitEntity> findForUpdate(@Param("id") String id, @Param("schoolId") String schoolId);

    List<HealthCareVisitEntity> findBySchoolIdAndStudentIdAndArrivedAtGreaterThanEqualOrderByArrivedAtAsc(
            String schoolId, String studentId, Instant since);

    Optional<HealthCareVisitEntity> findFirstBySchoolIdAndStudentIdAndStatus(String schoolId, String studentId,
            Status status);

    List<HealthCareVisitEntity> findBySchoolIdAndStudentIdOrderByArrivedAtDesc(String schoolId, String studentId);

    List<HealthCareVisitEntity> findBySchoolIdAndArrivedAtGreaterThanEqualAndArrivedAtLessThanOrderByArrivedAtDesc(
            String schoolId, Instant from, Instant to);

    List<HealthCareVisitEntity> findBySchoolIdAndStatusOrderByArrivedAtAsc(String schoolId, Status status);

    boolean existsBySchoolIdAndComplaintCodesContaining(String schoolId, String code);

    // Every filter is a set of statuses and outcomes plus windows on arrival and on the end of the school day, so
    // one query covers them all without nullable parameters (as LibraryVisitJpaRepository.search does). Open
    // visits have no outcome, so "any outcome" is its own flag rather than a list that would leave them out.
    @Query(value = """
                select v from HealthCareVisitEntity v
                where v.schoolId = :schoolId
                and v.status in :statuses
                and (:anyOutcome = true or v.outcome in :outcomes)
                and v.arrivedAt >= :from and v.arrivedAt < :to
                and v.schoolDayEndsAt <= :endsBy
                and (:complaint = '' or v.complaintCodes like concat('%,', :complaint, ',%'))
                and (:overLimitOnly = false or v.overLimit is not null)
                and (:studentId = '' or v.studentId = :studentId)
                and (lower(v.studentName) like lower(concat('%', :query, '%'))
                    or lower(coalesce(v.studentClass, '')) like lower(concat('%', :query, '%')))
                order by v.arrivedAt desc
            """, countQuery = """
                select count(v) from HealthCareVisitEntity v
                where v.schoolId = :schoolId
                and v.status in :statuses
                and (:anyOutcome = true or v.outcome in :outcomes)
                and v.arrivedAt >= :from and v.arrivedAt < :to
                and v.schoolDayEndsAt <= :endsBy
                and (:complaint = '' or v.complaintCodes like concat('%,', :complaint, ',%'))
                and (:overLimitOnly = false or v.overLimit is not null)
                and (:studentId = '' or v.studentId = :studentId)
                and (lower(v.studentName) like lower(concat('%', :query, '%'))
                    or lower(coalesce(v.studentClass, '')) like lower(concat('%', :query, '%')))
            """)
    Page<HealthCareVisitEntity> search(@Param("schoolId") String schoolId,
            @Param("statuses") Collection<Status> statuses, @Param("anyOutcome") boolean anyOutcome,
            @Param("outcomes") Collection<Outcome> outcomes, @Param("from") Instant from, @Param("to") Instant to,
            @Param("endsBy") Instant endsBy, @Param("complaint") String complaint,
            @Param("overLimitOnly") boolean overLimitOnly, @Param("studentId") String studentId,
            @Param("query") String query, Pageable pageable);
}
