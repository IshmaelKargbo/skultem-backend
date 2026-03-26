package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.persistence.entity.SchoolUserEntity;

@Repository
public interface SchoolUserJpaRepository extends JpaRepository<SchoolUserEntity, String> {

    @Query("""
        select su
        from SchoolUserEntity su
        join fetch su.user
        where su.schoolId = :schoolId
        and su.user.id = :userId
        and su.role = :role
    """)
    Optional<SchoolUserEntity> findBySchoolIdAndUserIdWithUser(String schoolId, String userId, Role role);


    @Query("""
        select su
        from SchoolUserEntity su
        join fetch su.user
        where su.user.id = :userId
        and su.schoolId = :schoolId
    """)
    List<SchoolUserEntity> findAllByUserIdWithUser(String userId, String schoolId);


    @Query("""
        select su
        from SchoolUserEntity su
        join fetch su.user
        where su.user.id = :userId
    """)
    Optional<SchoolUserEntity> findOneByUserIdWithUser(String userId);


    @Query("""
        select su
        from SchoolUserEntity su
        join fetch su.user
        where su.user.id = :userId
        and su.schoolId = :schoolId
    """)
    Optional<SchoolUserEntity> findOneBySchoolIdUserIdWithUser(String schoolId, String userId);


    boolean existsBySchoolIdAndUser_IdAndRole(String schoolId, String userId, Role role);

    List<SchoolUserEntity> findAllBySchoolId(String schoolId);
}
