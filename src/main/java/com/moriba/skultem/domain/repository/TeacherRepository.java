package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.vo.Filter;

public interface TeacherRepository {
    void save(Teacher domain);

    Optional<Teacher> findById(String id);

    Optional<Teacher> findByIdAndSchoolId(String id, String school);

    Optional<Teacher> findByUserId(String userId);

    Optional<Teacher> findByUserIdAndSchoolId(String userId, String schoolId);

    boolean existsByStaffIdAndSchool(String staffId, String schoolId);

    boolean existsByPhoneAndSchool(String phone, String schoolId);

    boolean existsByStaffIdAndSchoolAndIdNot(String schoolId, String staffId, String teacherId);

    boolean existsByPhoneAndSchoolAndIdNot(String schoolId, String phone, String teacherId);

    long countAll();

    long countAllBySchool(String schoolid);

    default Page<Teacher> search(String value, String schoolId, Pageable pageable) {
        return search(value, null, schoolId, pageable);
    }

    // gender is the Gender enum's name; null/blank means "any".
    Page<Teacher> search(String value, String gender, String schoolId, Pageable pageable);

    Page<Teacher> runReport(String schoolId, List<Filter> filters, Pageable pageable);
}
