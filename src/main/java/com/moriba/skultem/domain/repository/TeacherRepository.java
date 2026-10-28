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

    // What a section-limited caller (principal / section admin) sees: teachers limited (under the TEACHER
    // role) to at least one of these management sections, plus any teacher - tagged elsewhere or
    // whole-school - who teaches a subject or is class master at one of these levels this year.
    Page<Teacher> searchInSections(String value, String gender, String schoolId, java.util.Collection<String> sectionIds,
            java.util.Collection<com.moriba.skultem.domain.vo.Level> levels,
            Pageable pageable);

    // A section VIEW (owner-level): teachers limited to these sections, those not limited to any, and
    // those teaching / class-mastering at these levels this year.
    Page<Teacher> searchInSectionsOrUnlimited(String value, String gender, String schoolId,
            java.util.Collection<String> sectionIds,
            java.util.Collection<com.moriba.skultem.domain.vo.Level> levels, Pageable pageable);

    Page<Teacher> runReport(String schoolId, List<Filter> filters, Pageable pageable);

    // Teaches a subject or is class master at one of these levels in the active academic year.
    boolean teachesAtLevels(String teacherId, java.util.Collection<com.moriba.skultem.domain.vo.Level> levels);
}
