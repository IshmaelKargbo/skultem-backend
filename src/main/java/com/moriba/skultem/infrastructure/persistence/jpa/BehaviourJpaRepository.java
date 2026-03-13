package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.vo.KindCount;
import com.moriba.skultem.infrastructure.persistence.entity.BehaviourEntity;

@Repository
public interface BehaviourJpaRepository extends JpaRepository<BehaviourEntity, String> {

  @Query("""
          SELECT b.kind AS kind, COUNT(b) AS count
          FROM BehaviourEntity b
          JOIN b.enrollment e
          WHERE e.academicYear.id = :yearId
            AND e.schoolId = :schoolId
            AND (NULLIF(:classId, '') IS NULL OR e.clazz.id = :classId)
          GROUP BY b.kind
      """)
  List<KindCount> countByKindForClassOrAll(@Param("yearId") String academicYearId,
      @Param("schoolId") String schoolId,
      @Param("classId") String classId);

  Page<BehaviourEntity> findAllByEnrollment_AcademicYear_IdAndSchoolIdOrderByCreatedAtAsc(String academicYearId,
      String schoolId, Pageable pageable);

  Page<BehaviourEntity> findAllByEnrollment_AcademicYear_IdAndEnrollment_Clazz_IdAndSchoolIdOrderByCreatedAtAsc(
      String academicYearId, String classId, String schoolId, Pageable pageable);
}
