package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Broadcast;

public interface BroadcastRepository {
    void save(Broadcast domain);

    Optional<Broadcast> findByIdAndSchool(String id, String schoolId);

    Page<Broadcast> findAllBySchoolId(String schoolId, Pageable pageable);
}
