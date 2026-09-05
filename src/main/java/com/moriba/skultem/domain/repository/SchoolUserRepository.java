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

    // Whether anyone anywhere already holds this role, ignoring school - only meaningful for
    // SYSTEM_ADMIN, which BootstrapSystemAdminUseCase uses to self-disable once one exists.
    boolean existsByRole(Role role);

    // Every school membership a user holds, across every school - the cross-tenant view
    // SearchUsersAcrossSchoolsUseCase needs, unlike findAllByUser_IdAndSchoolId above which is
    // scoped to one school.
    List<SchoolUser> findAllByUser_Id(String userId);
}
