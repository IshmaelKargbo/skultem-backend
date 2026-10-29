package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.LibraryLoan.Status;
import com.moriba.skultem.infrastructure.persistence.entity.LibraryLoanEntity;

public interface LibraryLoanJpaRepository extends JpaRepository<LibraryLoanEntity, String> {

    @EntityGraph(attributePaths = { "book", "book.category" })
    Optional<LibraryLoanEntity> findByIdAndSchoolId(String id, String schoolId);

    @Query("select l.book.id from LibraryLoanEntity l where l.id = :id and l.schoolId = :schoolId")
    Optional<String> findBookId(@Param("id") String id, @Param("schoolId") String schoolId);

    boolean existsByBook_IdAndSchoolIdAndStatus(String bookId, String schoolId, Status status);

    @Query("""
                select count(l) > 0 from LibraryLoanEntity l
                where l.schoolId = :schoolId and l.book.id = :bookId and l.status = :status
                and (l.studentId = :borrowerId or l.teacherId = :borrowerId)
            """)
    boolean existsByBookAndBorrower(@Param("bookId") String bookId, @Param("borrowerId") String borrowerId,
            @Param("schoolId") String schoolId, @Param("status") Status status);

    @Query("""
                select count(l) from LibraryLoanEntity l
                where l.schoolId = :schoolId and l.status = :status
                and (l.studentId = :borrowerId or l.teacherId = :borrowerId)
            """)
    long countByBorrower(@Param("borrowerId") String borrowerId, @Param("schoolId") String schoolId,
            @Param("status") Status status);

    @Query("""
                select count(l) from LibraryLoanEntity l
                where l.schoolId = :schoolId and l.status = :status
                and (l.studentId = :borrowerId or l.teacherId = :borrowerId)
                and l.dueDate < :dueBefore
            """)
    long countDueBeforeByBorrower(@Param("borrowerId") String borrowerId, @Param("schoolId") String schoolId,
            @Param("status") Status status, @Param("dueBefore") LocalDate dueBefore);

    // The status filter is expressed as a set of statuses plus a due-date window, so every filter
    // (all / issued / overdue / returned) runs as one query without nullable parameters.
    @EntityGraph(attributePaths = { "book", "book.category" })
    @Query(value = """
                select l from LibraryLoanEntity l
                where l.schoolId = :schoolId
                and l.status in :statuses
                and l.dueDate >= :dueFrom and l.dueDate <= :dueTo
                and (
                    lower(l.borrowerName) like lower(concat('%', :query, '%'))
                    or lower(l.book.title) like lower(concat('%', :query, '%'))
                )
                order by l.issueDate desc, l.createdAt desc
            """, countQuery = """
                select count(l) from LibraryLoanEntity l
                where l.schoolId = :schoolId
                and l.status in :statuses
                and l.dueDate >= :dueFrom and l.dueDate <= :dueTo
                and (
                    lower(l.borrowerName) like lower(concat('%', :query, '%'))
                    or lower(l.book.title) like lower(concat('%', :query, '%'))
                )
            """)
    Page<LibraryLoanEntity> search(@Param("schoolId") String schoolId, @Param("query") String query,
            @Param("statuses") Collection<Status> statuses, @Param("dueFrom") LocalDate dueFrom,
            @Param("dueTo") LocalDate dueTo, Pageable pageable);

    @EntityGraph(attributePaths = { "book", "book.category" })
    Page<LibraryLoanEntity> findAllByBook_IdAndSchoolIdOrderByIssueDateDescCreatedAtDesc(String bookId,
            String schoolId, Pageable pageable);

    @Modifying
    @Query("delete from LibraryLoanEntity l where l.book.id = :bookId and l.schoolId = :schoolId")
    void deleteByBook(@Param("bookId") String bookId, @Param("schoolId") String schoolId);

    long countBySchoolIdAndStatusAndDueDateBefore(String schoolId, Status status, LocalDate date);
}
