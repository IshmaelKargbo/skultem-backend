package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.ReportCardEntity;

public interface ReportCardJpaRepository extends JpaRepository<ReportCardEntity, String> {

    Optional<ReportCardEntity> findByIdAndSchoolId(String id, String schoolId);

    Optional<ReportCardEntity> findBySchoolIdAndStudentIdAndTermId(String schoolId, String studentId, String termId);

    long countBySchoolId(String schoolId);

    long countBySchoolIdAndPassed(String schoolId, boolean passed);

    @Query("select coalesce(sum(r.downloadCount), 0) from ReportCardEntity r where r.schoolId = :schoolId")
    long sumDownloadCount(@Param("schoolId") String schoolId);

    // The `cast(:search as string)` is not decorative - when `search` is null (no
    // search term typed) Postgres can't infer a bind parameter's type from a null
    // value alone once it's routed through concat()/lower(), and silently defaults
    // it to `bytea`, blowing up with "function lower(bytea) does not exist" on
    // every request regardless of whether a search term was even given. Casting
    // gives it a concrete type up front so the null case works too.
    // Ordered by class position - 0 means ranking wasn't included at generation
    // time (not "1st"), so those sort last instead of jumping to the front.
    @Query("""
            select r from ReportCardEntity r
            where r.schoolId = :schoolId
            and (:classId is null or r.classId = :classId)
            and (:termId is null or r.termId = :termId)
            and (:search is null or lower(r.studentName) like lower(concat('%', cast(:search as string), '%'))
                or lower(r.admissionNumber) like lower(concat('%', cast(:search as string), '%')))
            order by case when r.position = 0 then 999999 else r.position end asc, r.studentName asc
            """)
    Page<ReportCardEntity> search(@Param("schoolId") String schoolId, @Param("classId") String classId,
            @Param("termId") String termId, @Param("search") String search, Pageable pageable);
}
