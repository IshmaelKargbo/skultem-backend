package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Lesson;

public interface LessonRepository {
    void save(Lesson domain);

    List<Lesson> findAllByWeek(String week);

    Optional<Lesson> findById(String id);

    boolean existsByWeekIdAndTitleAndSchoolId(String week, String title, String school);

    Page<Lesson> findAllByTeacherIdAndSchoolId(String teacherId, String school, Pageable pageable);
}
