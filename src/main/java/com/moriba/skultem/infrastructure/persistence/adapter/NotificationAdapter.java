package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Notification;
import com.moriba.skultem.domain.model.Notification.Type;
import com.moriba.skultem.domain.repository.NotificationRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.NotificationJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class NotificationAdapter implements NotificationRepository {

    private final NotificationJpaRepository repo;

    @Override
    public void save(Notification domain) {
        var entity = NotificationMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Notification> findById(String id) {
        return repo.findById(id).map(NotificationMapper::toDomain);
    }

    @Override
    public long countAll() {
        return repo.count();
    }

    @Override
    public Optional<Notification> findByIdAndSchoolId(String id, String school) {
        return repo.findByIdAndSchoolId(id, school).map(NotificationMapper::toDomain);
    }

    @Override
    public Page<Notification> findAllByOwnerAndSchoolId(String userId, String schoolId, Pageable pageable) {
        return repo.findAllByOwner_IdAndSchoolId(userId, schoolId, pageable).map(NotificationMapper::toDomain);
    }

    @Override
    public boolean existsByOwnerAndTypeAndMeta(String schoolId, String userId, Type type, String meta) {
        return repo.existsBySchoolIdAndOwner_IdAndTypeAndMeta(schoolId, userId, schoolId, meta);
    }

}
