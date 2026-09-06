package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.persistence.entity.UserEntity;

public interface UserJpaRepository extends JpaRepository<UserEntity, String> {
    boolean existsByEmailIgnoreCase(String email);

    Optional<UserEntity> findByEmail(String email);

    @Query("""
                select su.user
                from SchoolUserEntity su
                where su.schoolId = :schoolId
            """)
    Page<UserEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    @Query("""
                select u from UserEntity u
                where lower(u.email) like lower(concat('%', :query, '%'))
                   or lower(u.givenName) like lower(concat('%', :query, '%'))
                   or lower(u.familyName) like lower(concat('%', :query, '%'))
            """)
    Page<UserEntity> search(String query, Pageable pageable);

    // Same free-text match as search() above, narrowed to users holding this role in at least
    // one SchoolUser membership (`distinct` since someone could hold it at more than one school -
    // SYSTEM_ADMIN's anchor school is arbitrary anyway, see BootstrapSystemAdminUseCase). Backs
    // SystemAdminController's "System Admins" roster on /system/users.
    @Query("""
                select distinct su.user
                from SchoolUserEntity su
                where su.role = :role
                  and (:query = ''
                       or lower(su.user.email) like lower(concat('%', :query, '%'))
                       or lower(su.user.givenName) like lower(concat('%', :query, '%'))
                       or lower(su.user.familyName) like lower(concat('%', :query, '%')))
            """)
    Page<UserEntity> searchByRole(Role role, String query, Pageable pageable);

}
