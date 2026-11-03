package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.vo.KindCount;
import com.moriba.skultem.infrastructure.persistence.entity.BehaviourEntity;

public interface BehaviourJpaRepository extends JpaRepository<BehaviourEntity, String> {

  boolean existsByEnrollment_IdAndSchoolId(String enrollmentId, String schoolId);

  @Query("""
          SELECT b.kind AS kind, COUNT(b) AS count
          FROM BehaviourEntity b
          JOIN b.enrollment e
          WHERE e.academicYear.id = :yearId
            AND e.schoolId = :schoolId
            AND (NULLIF(:classId, '') IS NULL OR e.clazz.id = :classId)
            AND e.clazz.level IN :levels
          GROUP BY b.kind
      """)
  List<KindCount> countByKindForClassOrAll(@Param("yearId") String academicYearId,
      @Param("schoolId") String schoolId,
      @Param("classId") String classId,
      @Param("levels") java.util.Collection<com.moriba.skultem.domain.vo.Level> levels);

  // levels: the caller's management-section levels (every level for whole-school staff).
  Page<BehaviourEntity> findAllByEnrollment_AcademicYear_IdAndSchoolIdAndEnrollment_Clazz_LevelInOrderByCreatedAtAsc(
      String academicYearId, String schoolId, java.util.Collection<com.moriba.skultem.domain.vo.Level> levels, Pageable pageable);

  Page<BehaviourEntity> findAllByEnrollment_AcademicYear_IdAndEnrollment_Clazz_IdAndSchoolIdAndEnrollment_Clazz_LevelInOrderByCreatedAtAsc(
      String academicYearId, String classId, String schoolId, java.util.Collection<com.moriba.skultem.domain.vo.Level> levels, Pageable pageable);
}
