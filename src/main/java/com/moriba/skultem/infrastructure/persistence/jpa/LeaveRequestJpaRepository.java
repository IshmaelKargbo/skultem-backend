package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.LeaveRequest.Status;
import com.moriba.skultem.domain.model.LeaveRequest.Type;
import com.moriba.skultem.infrastructure.persistence.entity.LeaveRequestEntity;

public interface LeaveRequestJpaRepository extends JpaRepository<LeaveRequestEntity, String> {
    Optional<LeaveRequestEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<LeaveRequestEntity> findAllByTeacher_IdAndSchoolIdOrderByCreatedAtDesc(String teacherId, String schoolId,
            Pageable pageable);

    long countBySchoolIdAndStatus(String schoolId, Status status);

    long countBySchoolId(String schoolId);

    // status/type are the actual enum (not String) - each used only in an IS NULL check and a
    // comparison against the entity's own enum-typed attribute, so Hibernate always has a single,
    // unambiguous type to infer for the parameter. Pass null (never "") for "no filter" - see
    // SchemeOfWorkJpaRepository for the failure mode this avoids.
    @Query("""
                SELECT l FROM LeaveRequestEntity l
                JOIN l.teacher t
                JOIN t.user u
                WHERE l.schoolId = :schoolId
                AND (:status IS NULL OR l.status = :status)
                AND (:type IS NULL OR l.type = :type)
                AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(u.givenName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.familyName) LIKE LOWER(CONCAT('%', :search, '%'))
                )
                ORDER BY l.createdAt DESC
            """)
    Page<LeaveRequestEntity> search(@Param("schoolId") String schoolId, @Param("status") Status status,
            @Param("type") Type type, @Param("search") String search, Pageable pageable);
}
