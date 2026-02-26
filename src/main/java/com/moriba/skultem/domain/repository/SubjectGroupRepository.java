package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.SubjectGroup;

public interface SubjectGroupRepository {
    void save(SubjectGroup domain);

    Optional<SubjectGroup> findByIdAndSchoolId(String id, String schoolId);

    Optional<SubjectGroup> findByIdAndStreamAndSchoolId(String id, String streamId, String schoolId);

    Optional<SubjectGroup> findByIdAndClassSchoolId(String id, String classId, String schoolId);

    Page<SubjectGroup> findBySchool(String schoolId, Pageable pageable);

    List<SubjectGroup> findAllByIdsAndSchoolId(Set<String> ids, String schoolId);

    List<SubjectGroup> findAllByIdInAndStreamAndSchoolId(Set<String> ids, String streamId, String schoolId);

    Page<SubjectGroup> findByStreamIdAndSchoolId(String streamId, String schoolId, Pageable pageable);

    Page<SubjectGroup> findByClassAndSchool(String classId, String schoolId, Pageable pageable);
}
