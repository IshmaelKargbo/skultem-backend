package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.LibraryBookEntity;

import jakarta.persistence.LockModeType;

public interface LibraryBookJpaRepository extends JpaRepository<LibraryBookEntity, String> {
    boolean existsByIsbnIgnoreCaseAndSchoolId(String isbn, String schoolId);

    boolean existsByCategory_IdAndSchoolId(String categoryId, String schoolId);

    @EntityGraph(attributePaths = "category")
    Optional<LibraryBookEntity> findByIdAndSchoolId(String id, String schoolId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from LibraryBookEntity b where b.id = :id and b.schoolId = :schoolId")
    Optional<LibraryBookEntity> findForUpdate(@Param("id") String id, @Param("schoolId") String schoolId);

    @EntityGraph(attributePaths = "category")
    @Query(value = """
                select b from LibraryBookEntity b
                where b.schoolId = :schoolId
                and (:categoryId = '' or b.category.id = :categoryId)
                and (:outOfStockOnly = false or b.availableCopies = 0)
                and (
                    lower(b.title) like lower(concat('%', :query, '%'))
                    or lower(b.author) like lower(concat('%', :query, '%'))
                    or lower(coalesce(b.isbn, '')) like lower(concat('%', :query, '%'))
                )
                order by b.title asc
            """, countQuery = """
                select count(b) from LibraryBookEntity b
                where b.schoolId = :schoolId
                and (:categoryId = '' or b.category.id = :categoryId)
                and (:outOfStockOnly = false or b.availableCopies = 0)
                and (
                    lower(b.title) like lower(concat('%', :query, '%'))
                    or lower(b.author) like lower(concat('%', :query, '%'))
                    or lower(coalesce(b.isbn, '')) like lower(concat('%', :query, '%'))
                )
            """)
    Page<LibraryBookEntity> search(@Param("schoolId") String schoolId, @Param("query") String query,
            @Param("categoryId") String categoryId, @Param("outOfStockOnly") boolean outOfStockOnly,
            Pageable pageable);

    @Query("""
                select b.category.id, count(b) from LibraryBookEntity b
                where b.schoolId = :schoolId and b.category.id in :categoryIds
                group by b.category.id
            """)
    List<Object[]> countByCategories(@Param("schoolId") String schoolId,
            @Param("categoryIds") Collection<String> categoryIds);

    @Query("""
                select count(b), coalesce(sum(b.totalCopies), 0), coalesce(sum(b.availableCopies), 0)
                from LibraryBookEntity b where b.schoolId = :schoolId
            """)
    List<Object[]> totals(@Param("schoolId") String schoolId);
}
