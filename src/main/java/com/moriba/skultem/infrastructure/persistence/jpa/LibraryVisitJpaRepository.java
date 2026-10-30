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

import com.moriba.skultem.domain.model.LibraryVisit.Status;
import com.moriba.skultem.infrastructure.persistence.entity.LibraryVisitEntity;

import jakarta.persistence.LockModeType;

public interface LibraryVisitJpaRepository extends JpaRepository<LibraryVisitEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from LibraryVisitEntity v where v.id = :id and v.schoolId = :schoolId")
    Optional<LibraryVisitEntity> findForUpdate(@Param("id") String id, @Param("schoolId") String schoolId);

    List<LibraryVisitEntity> findBySchoolIdAndStatusAndStudentIdIn(String schoolId, Status status,
            Collection<String> studentIds);

    // Every filter is a set of statuses plus windows on check-in time and on the end of the school day,
    // so one query covers them all without nullable parameters (as LibraryLoanJpaRepository.search does).
    @Query(value = """
                select v from LibraryVisitEntity v
                where v.schoolId = :schoolId
                and v.status in :statuses
                and v.checkedInAt >= :from and v.checkedInAt < :to
                and v.schoolDayEndsAt <= :endsBy
                and (:studentId = '' or v.studentId = :studentId)
                and (lower(v.studentName) like lower(concat('%', :query, '%'))
                    or lower(coalesce(v.studentClass, '')) like lower(concat('%', :query, '%')))
                order by v.checkedInAt desc
            """, countQuery = """
                select count(v) from LibraryVisitEntity v
                where v.schoolId = :schoolId
                and v.status in :statuses
                and v.checkedInAt >= :from and v.checkedInAt < :to
                and v.schoolDayEndsAt <= :endsBy
                and (:studentId = '' or v.studentId = :studentId)
                and (lower(v.studentName) like lower(concat('%', :query, '%'))
                    or lower(coalesce(v.studentClass, '')) like lower(concat('%', :query, '%')))
            """)
    Page<LibraryVisitEntity> search(@Param("schoolId") String schoolId, @Param("statuses") Collection<Status> statuses,
            @Param("from") Instant from, @Param("to") Instant to, @Param("endsBy") Instant endsBy,
            @Param("studentId") String studentId, @Param("query") String query, Pageable pageable);

    long countBySchoolIdAndStatus(String schoolId, Status status);

    long countBySchoolIdAndStatusAndSchoolDayEndsAtLessThanEqual(String schoolId, Status status, Instant now);
}
