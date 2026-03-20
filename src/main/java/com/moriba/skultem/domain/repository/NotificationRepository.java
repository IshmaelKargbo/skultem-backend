package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Notification;
import com.moriba.skultem.domain.model.Notification.Type;

public interface NotificationRepository {
    void save(Notification domain);

    Optional<Notification> findById(String id);

    Optional<Notification> findByIdAndSchoolId(String id, String school);

    Page<Notification> findAllByOwnerAndSchoolId(String userId, String schoolId, Pageable pageable);

    boolean existsByOwnerAndTypeAndMeta(String schoolId, String userId, Type type, String meta);

    long countAll();
}
