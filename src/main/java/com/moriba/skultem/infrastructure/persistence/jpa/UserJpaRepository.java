package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.UserEntity;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, String> {
    boolean existsByEmailIgnoreCase(String email);

    Optional<UserEntity> findByEmail(String email);

    @Query("""
                select su.user
                from SchoolUserEntity su
                where su.schoolId = :schoolId
            """)
    Page<UserEntity> findAllBySchoolId(String schoolId, Pageable pageable);

}
