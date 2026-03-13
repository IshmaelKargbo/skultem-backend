package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.UserSessionEntity;

@Repository
public interface UserSessionJpaRepository extends JpaRepository<UserSessionEntity, String> {
    @Query("SELECT s FROM UserSessionEntity s JOIN FETCH s.user WHERE s.id = :id")
    Optional<UserSessionEntity> findByIdWithUser(@Param("id") String id);

    List<UserSessionEntity> findAllByUser_IdAndSchoolIdAndActive(String userId, String schoolId, boolean active);

    @Query(value = "SELECT s FROM UserSessionEntity s JOIN FETCH s.user WHERE s.schoolId = :schoolId",
            countQuery = "SELECT count(s) FROM UserSessionEntity s WHERE s.schoolId = :schoolId")
    Page<UserSessionEntity> findAllBySchoolId(@Param("schoolId") String schoolId, Pageable pageable);

}
