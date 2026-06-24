package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.infrastructure.persistence.entity.RoomEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TimingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TimingJpaRepository extends JpaRepository<TimingEntity, String> {
    boolean existsBySchoolId(String schoolId);

    Optional<TimingEntity> findBySchoolId(String schoolId);

    Optional<TimingEntity> findBy(String schoolId);
}
