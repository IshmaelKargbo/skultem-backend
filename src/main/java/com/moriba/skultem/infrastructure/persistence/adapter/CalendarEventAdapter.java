package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.CalendarEvent;
import com.moriba.skultem.domain.repository.CalendarEventRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.CalendarEventJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.CalendarEventMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CalendarEventAdapter implements CalendarEventRepository {
    private final CalendarEventJpaRepository repo;

    @Override
    public void save(CalendarEvent domain) {
        var entity = CalendarEventMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public void delete(CalendarEvent domain) {
        repo.deleteById(domain.getId());
    }

    @Override
    public Optional<CalendarEvent> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(CalendarEventMapper::toDomain);
    }

    @Override
    public Page<CalendarEvent> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByStartDateAsc(schoolId, pageable).map(CalendarEventMapper::toDomain);
    }
}
