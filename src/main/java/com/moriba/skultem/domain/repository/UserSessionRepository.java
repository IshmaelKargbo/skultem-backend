package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserSessionRepository {
    void save(UserSession domain);

    Optional<UserSession> findById(String id);

    List<UserSession> findAllByUserAndSchoolIdAndActive(String userId, String schoolId, boolean active);

    Page<UserSession> findAllBySchoolId(String schoolId, Pageable pageable);
}
