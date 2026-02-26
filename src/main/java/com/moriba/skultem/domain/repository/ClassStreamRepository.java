package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.ClassStream;

public interface ClassStreamRepository {
    void save(ClassStream domain);

    Optional<ClassStream> findById(String id);

    Optional<ClassStream> findByIdAndSchoolId(String id, String schoolId);

    Optional<ClassStream> findByClassIdAndSchoolId(String classId, String schoolId);

    boolean existsByClassIdAndSchoolIdAndStreamId(String classId, String schoolId, String streamId);

    Optional<ClassStream> findByClassIdAndSchoolIdAndStreamId(String classId, String schoolId, String streamId);

    List<ClassStream> findAllByClassIdAndSchoolId(String classId, String schoolId);

    long countAll();
}
