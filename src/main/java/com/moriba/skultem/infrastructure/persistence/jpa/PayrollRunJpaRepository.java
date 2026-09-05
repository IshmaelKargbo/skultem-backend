package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.PayrollRun;
import com.moriba.skultem.infrastructure.persistence.entity.PayrollRunEntity;

public interface PayrollRunJpaRepository extends JpaRepository<PayrollRunEntity, String> {
    Optional<PayrollRunEntity> findByIdAndSchoolId(String id, String schoolId);

    // No OrderBy suffix - the caller's Pageable carries the Sort (see PayrollService.listRuns), and
    // a hardcoded suffix here would take precedence over it rather than just being overridden.
    Page<PayrollRunEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    Optional<PayrollRunEntity> findFirstBySchoolIdOrderByCreatedAtDesc(String schoolId);

    @Query("""
                SELECT r FROM PayrollRunEntity r
                WHERE r.schoolId = :schoolId
                AND (:search IS NULL OR :search = '' OR LOWER(r.period) LIKE LOWER(CONCAT('%', :search, '%')))
                AND (:status IS NULL OR r.status = :status)
            """)
    Page<PayrollRunEntity> search(@Param("schoolId") String schoolId, @Param("search") String search,
            @Param("status") PayrollRun.Status status, Pageable pageable);
}
