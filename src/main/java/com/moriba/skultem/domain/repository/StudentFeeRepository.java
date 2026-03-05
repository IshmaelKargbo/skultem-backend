package com.moriba.skultem.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.StudentFee;

public interface StudentFeeRepository {
        void save(StudentFee domain);

        void saveAll(List<StudentFee> records);

        Optional<StudentFee> findByIdAndSchoolId(String id);

        BigDecimal sumTotalFeeByStudent(String studentId, String schoolId);

        boolean existsBySchoolAndEnrollmentAndStudentAndFee(String schoolId, String enrollmentId, String studentId,
                        String feeId);

        Page<StudentFee> findBySchool(String schoolId, Pageable pageable);

        Page<StudentFee> findAllBySchoolAndStudent(String schoolId, String student, Pageable pageable);

        Page<StudentFee> findBySchoolAndEnrollment(String schoolId, String enrollmentId, Pageable pageable);

        Page<StudentFee> findBySchoolAndEnrollmentAndFee(String schoolId, String enrollmentId, String feeId,
                        Pageable pageable);

        long countByFeeAndSchool(String feeId, String schoolId);

        Page<StudentFee> findBySchoolAndEnrollmentAndStudent(String schoolId, String academicYearId, String classId,
                        Pageable pageable);
}
