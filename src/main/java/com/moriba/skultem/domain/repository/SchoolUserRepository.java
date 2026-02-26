package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.SchoolUser;

public interface SchoolUserRepository {
    void save(SchoolUser domain);

    Optional<SchoolUser> findBySchoolAndUser(String schoolId, String userId);

    List<SchoolUser> findAllByUser(String userId);

    Optional<SchoolUser> findOneByUser(String userId);

    boolean existsBySchoolAndUser(String schoolId, String userId);

    List<SchoolUser> findBySchool(String schoolId);
}
