package com.moriba.skultem.infrastructure.persistence.adapter;

import com.moriba.skultem.domain.model.Lesson;
import com.moriba.skultem.domain.repository.LessonRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.LessonJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.LessonMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LessonAdapter implements LessonRepository {
    private final LessonJpaRepository repo;

    @Override
    public void save(Lesson domain) {
        var entity = LessonMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public List<Lesson> findAllByWeek(String week) {
        return repo.findByWeekIdOrderByDateAsc(week).stream().map(LessonMapper::toDomain).toList();
    }

    @Override
    public Optional<Lesson> findById(String id) {
        return repo.findById(id).map(LessonMapper::toDomain);
    }

    @Override
    public boolean existsByWeekIdAndTitleAndSchoolId(String week, String title, String school) {
        return repo.existsByWeekIdAndTitleAndSchoolId(week, title, school);
    }

    @Override
    public Page<Lesson> findAllByTeacherIdAndSchoolId(String teacherId, String school, Pageable pageable) {
        return repo.findAllByTeacherIdAndSchoolId(teacherId, school, pageable).map(LessonMapper::toDomain);
    }
}
