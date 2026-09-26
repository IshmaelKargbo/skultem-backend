package com.moriba.skultem.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.ActivityEntity;

public interface ActivityJpaRepository extends JpaRepository<ActivityEntity, String> {
    Page<ActivityEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            select a from ActivityEntity a
            where a.schoolId = :schoolId
              and (a.managementSectionId in :sectionIds
                   or (a.managementSectionId is null and a.type = com.moriba.skultem.domain.vo.ActivityType.SCHOOL))
            order by a.createdAt desc
            """)
    Page<ActivityEntity> findVisibleToSections(
            @org.springframework.data.repository.query.Param("schoolId") String schoolId,
            @org.springframework.data.repository.query.Param("sectionIds") java.util.Collection<String> sectionIds,
            Pageable pageable);
}
