package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Supply;

public interface SupplyRepository {
    void save(Supply domain);

    boolean existsByStudentIdAndMaterialIdAndSchoolId(String studentId, String materialId, String schoolId);

    Optional<Supply> findByIdAndSchool(String id, String schoolId);

    Page<Supply> findBySchool(String schoolId, Pageable pageable);

    Page<Supply> findByStudentAndSchool(String studentId, String schoolId, Pageable pageable);

    // Matches on student name/admission number or material name - backs the supply list's search box.
    Page<Supply> search(String schoolId, String query, Pageable pageable);

    // PENDING or PARTIAL - a student is still owed some or all of it. Backs the "Pending Pickups"
    // view, which combines these with unfulfilled MaterialSales - see GetPendingPickupsUseCase.
    Page<Supply> findUncollectedBySchool(String schoolId, Pageable pageable);

    /** Wipes every row for this school - see WipeTestSchoolDataUseCase. */
    void deleteAllBySchoolId(String schoolId);
}
