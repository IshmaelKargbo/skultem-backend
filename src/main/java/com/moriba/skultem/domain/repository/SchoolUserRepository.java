package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.vo.Role;

public interface SchoolUserRepository {
    void save(SchoolUser domain);

    Optional<SchoolUser> findBySchoolAndUserAndRole(String schoolId, String userId, Role role);

    List<SchoolUser> findAllByUser_IdAndSchoolId(String userId, String schoolId);

    Optional<SchoolUser> findOneByUserAndRole(String userId, Role role);

    Optional<SchoolUser> findBySchoolAndUser(String schoolId, String userId);

    boolean existsBySchoolAndUserAndRole(String schoolId, String userId, Role role);

    List<SchoolUser> findBySchool(String schoolId);
}
