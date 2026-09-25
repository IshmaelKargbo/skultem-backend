package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.vo.Role;

public interface UserRepository {
    void save(User domain);

    Optional<User> findById(String id);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    // Users at this school whose guardian or staff/teacher phone ends in these digits - phone
    // login (see LoginUseCase). Phones live on those per-school records, not on the User.
    List<String> findIdsByPhoneInSchool(String schoolId, String phoneDigitsSuffix);

    Page<User> findBySchool(String school, Pageable pageable);

    // Unscoped by school - matches on email/given name/family name, for
    // SearchUsersAcrossSchoolsUseCase's cross-tenant lookup.
    Page<User> search(String query, Pageable pageable);

    // Same, narrowed to users holding `role` in at least one school membership - the System
    // Admins roster on /system/users.
    Page<User> searchByRole(Role role, String query, Pageable pageable);

    long countAll();

    void delete(User domain);
}
