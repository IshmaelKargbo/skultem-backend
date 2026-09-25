package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.TeacherEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

public interface TeacherJpaRepository
        extends JpaRepository<TeacherEntity, String>, JpaSpecificationExecutor<TeacherEntity> {

    boolean existsByStaffIdAndSchoolId(String staffId, String schoolId);

    boolean existsByPhoneAndSchoolId(String phone, String schoolId);

    Optional<TeacherEntity> findByUserId(String userId);

    Optional<TeacherEntity> findByUserIdAndSchoolId(String userId, String schoolId);

    Optional<TeacherEntity> findByIdAndSchoolId(String id, String schoolId);

    // No ORDER BY here - the caller's Pageable carries the Sort (see TeacherService.search), and a
    // fixed order here would either dominate or conflict with it. gender is always a real (possibly
    // empty) string, never null: the enum's name, or '' for "any" (same rule as StudentJpaRepository).
    @Query("""
                SELECT t
                FROM TeacherEntity t
                LEFT JOIN t.user u
                WHERE t.schoolId = :schoolId
                  AND (
                        :search IS NULL
                     OR :search = ''
                     OR LOWER(u.givenName) LIKE LOWER(CONCAT('%', :search, '%'))
                     OR LOWER(u.familyName) LIKE LOWER(CONCAT('%', :search, '%'))
                     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                     OR LOWER(t.phone) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
                  AND (:gender = '' OR CAST(t.gender AS string) = :gender)
            """)
    Page<TeacherEntity> search(@Param("schoolId") String schoolId, @Param("search") String search,
            @Param("gender") String gender, Pageable pageable);

    @Query("""
                SELECT t
                FROM TeacherEntity t
                LEFT JOIN t.user u
                WHERE t.schoolId = :schoolId
                  AND (
                        :search IS NULL
                     OR :search = ''
                     OR LOWER(u.givenName) LIKE LOWER(CONCAT('%', :search, '%'))
                     OR LOWER(u.familyName) LIKE LOWER(CONCAT('%', :search, '%'))
                     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                     OR LOWER(t.phone) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
                  AND (:gender = '' OR CAST(t.gender AS string) = :gender)
                  AND EXISTS (
                        SELECT 1 FROM StaffManagementSectionEntity sms
                        WHERE sms.schoolId = t.schoolId
                          AND sms.userId = u.id
                          AND sms.role = com.moriba.skultem.domain.vo.Role.TEACHER
                          AND sms.managementSectionId IN :sectionIds
                  )
            """)
    Page<TeacherEntity> searchInSections(@Param("schoolId") String schoolId, @Param("search") String search,
            @Param("gender") String gender, @Param("sectionIds") java.util.Collection<String> sectionIds,
            Pageable pageable);

    long countBySchoolId(String schoolId);

    boolean existsByStaffIdAndSchoolIdAndIdNot(String staffId, String schoolId, String id);

    boolean existsByPhoneAndSchoolIdAndIdNot(String phone, String schoolId, String id);

    default Page<TeacherEntity> runTeacherReport(String schoolId, List<Filter> filters, Pageable pageable) {

        Specification<TeacherEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

        if (filters != null && !filters.isEmpty()) {
            spec = spec.and(FilterSpecificationBuilder.build(filters));
        }

        return findAll(spec, pageable);
    }
}