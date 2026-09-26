package com.moriba.skultem.domain.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Broadcast;

public interface BroadcastRepository {
    void save(Broadcast domain);

    Optional<Broadcast> findByIdAndSchool(String id, String schoolId);

    Page<Broadcast> findAllBySchoolId(String schoolId, Pageable pageable);

    // Whole-school rows plus those for one of these sections; sectionIds null = no limit.
    Page<Broadcast> findVisibleBySchoolId(String schoolId, Collection<String> sectionIds, Pageable pageable);
}
