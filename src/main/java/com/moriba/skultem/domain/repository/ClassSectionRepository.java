package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.ClassSection;

public interface ClassSectionRepository {
    void save(ClassSection domain);

    boolean existsByClassIdAndSchoolIdAndSectionId(String classId, String schoolId, String sectionId);

    Optional<ClassSection> findByIdAndSchoolId(String id, String schoolId);

    Optional<ClassSection> findByIdAndClassIdAndSchoolId(String id, String classId, String schoolId);

    boolean existsBySchoolAndClassId(String schoolId, String classId);

    List<ClassSection> findByClassIdAndSchoolId(String classId, String schoolId);

    void delete(ClassSection domain);
}
