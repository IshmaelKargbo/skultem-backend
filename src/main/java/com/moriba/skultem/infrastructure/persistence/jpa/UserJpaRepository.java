package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.persistence.entity.UserEntity;

public interface UserJpaRepository extends JpaRepository<UserEntity, String> {
    boolean existsByEmailIgnoreCase(String email);

    Optional<UserEntity> findByEmail(String email);

    // Compares digits only, on the trailing :len of them, so "076 123 456", "+232 76 123456" and
    // "076123456" all match - phones are stored however they were typed.
    @Query(value = """
            SELECT DISTINCT user_id FROM (
                SELECT user_id, phone FROM parents WHERE school_id = :schoolId
                UNION ALL
                SELECT user_id, phone FROM teachers WHERE school_id = :schoolId
            ) p
            WHERE user_id IS NOT NULL
              AND right(regexp_replace(coalesce(phone, ''), '[^0-9]', '', 'g'), :len) = :suffix
            """, nativeQuery = true)
    List<String> findIdsByPhoneSuffixInSchool(@Param("schoolId") String schoolId, @Param("suffix") String suffix,
            @Param("len") int len);

    // Excludes PARENT - this backs the school-admin "Team Access" list (see
    // ListUserBySchoolUseCase), which is for managing staff/admin portal access, not browsing
    // every parent account at the school. Order is baked into the query (rather than left to the
    // caller's Pageable) because Spring Data resolves an external Sort's property names against
    // this query's FROM-clause root (SchoolUserEntity), not the projected UserEntity - appending
    // "order by createdAt" would sort by school_users.created_at while only users.* is selected,
    // which fails outright under SELECT DISTINCT (Postgres requires ORDER BY expressions to
    // appear in the select list). Callers should pass an unsorted Pageable for this method.
    @Query("""
                select distinct su.user
                from SchoolUserEntity su
                where su.schoolId = :schoolId
                and su.role <> :excludedRole
                order by su.user.createdAt desc
            """)
    Page<UserEntity> findAllBySchoolId(String schoolId, Role excludedRole, Pageable pageable);

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
