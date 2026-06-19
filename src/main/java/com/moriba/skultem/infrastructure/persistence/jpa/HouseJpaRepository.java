package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.HouseEntity;

@Repository
public interface HouseJpaRepository extends JpaRepository<HouseEntity, String> {
  boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

  Optional<HouseEntity> findByIdAndSchoolId(String id, String schoolId);

  Page<HouseEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

  @Query("""
      SELECT h FROM HouseEntity h
      LEFT JOIN h.houseMasters m
      LEFT JOIN m.user u
      WHERE h.schoolId = :schoolId
      AND (
        :search IS NULL OR :search = ''
        OR LOWER(h.name) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.givenName) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.familyName) LIKE LOWER(CONCAT('%', :search, '%'))
      )
      ORDER BY h.createdAt DESC
                  """)
  Page<HouseEntity> search(@Param("schoolId") String schoolId, @Param("search") String search,
      Pageable pageable);
}
