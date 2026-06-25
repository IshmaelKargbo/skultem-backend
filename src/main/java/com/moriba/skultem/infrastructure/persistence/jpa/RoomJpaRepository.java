package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.infrastructure.persistence.entity.RoomEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomJpaRepository extends JpaRepository<RoomEntity, String> {
    boolean existsByNameIgnoreCaseAndSchoolId(String name, String school);

    Optional<RoomEntity> findByIdAndSchoolId(String id, String schoolId);

    @Query("""
                SELECT r
                FROM RoomEntity r
                WHERE r.schoolId = :schoolId
                  AND (
                        :search IS NULL
                     OR :search = ''
                     OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
                ORDER BY r.updatedAt DESC
            """)
    Page<RoomEntity> search(@Param("schoolId") String schoolId, @Param("search") String search, Pageable pageable);
}
