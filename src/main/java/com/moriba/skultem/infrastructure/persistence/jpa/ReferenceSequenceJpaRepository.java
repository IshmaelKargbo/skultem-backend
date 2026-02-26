package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.vo.ReferenceSequenceId;
import com.moriba.skultem.infrastructure.persistence.entity.ReferenceSequenceEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface ReferenceSequenceJpaRepository extends JpaRepository<ReferenceSequenceEntity, ReferenceSequenceId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReferenceSequenceEntity r WHERE r.referenceType = :type AND r.year = :year")
    Optional<ReferenceSequenceEntity> findForUpdate(
            @Param("type") String type,
            @Param("year") Integer year);
}
