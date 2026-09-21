package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.Clazz.Status;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.infrastructure.persistence.entity.ClassEntity;

public interface ClassJpaRepository extends JpaRepository<ClassEntity, String> {
    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    boolean existsByLevelOrderAndSchoolId(int levelOrder, String schoolId);

    @Query("select coalesce(max(c.levelOrder), 0) from ClassEntity c where c.schoolId = :schoolId")
    int maxLevelOrderBySchoolId(@Param("schoolId") String schoolId);
    
    Optional<ClassEntity> findBySchoolIdAndLevelAndTerminalTrue(String schoolId, Level level);

    int countBySchoolIdAndLevel(String schoolId, Level level);

    Optional<ClassEntity> findBySchoolIdAndLevelOrder(String schoolId, int levelOrder);
    
    Optional<ClassEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<ClassEntity> findAllBySchoolIdAndStatusOrderByLevelOrderAsc(String schoolId, Status status, Pageable pageable);
}
