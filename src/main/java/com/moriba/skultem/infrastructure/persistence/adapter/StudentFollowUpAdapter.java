package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.StudentFollowUp;
import com.moriba.skultem.domain.repository.StudentFollowUpRepository;
import com.moriba.skultem.infrastructure.persistence.entity.StudentFollowUpEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.StudentFollowUpJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StudentFollowUpAdapter implements StudentFollowUpRepository {
    private final StudentFollowUpJpaRepository repo;

    @Override
    public void save(StudentFollowUp f) {
        repo.save(StudentFollowUpEntity.builder().id(f.getId()).schoolId(f.getSchoolId()).studentId(f.getStudentId())
                .classId(f.getClassId()).action(f.getAction()).note(f.getNote()).resolved(f.isResolved())
                .recordedByUserId(f.getRecordedByUserId()).recordedByName(f.getRecordedByName())
                .createdAt(f.getCreatedAt()).build());
    }

    @Override
    public List<StudentFollowUp> findAllByClass(String schoolId, String classId) {
        return repo.findAllBySchoolIdAndClassIdOrderByCreatedAtDesc(schoolId, classId).stream()
                .map(e -> new StudentFollowUp(e.getId(), e.getSchoolId(), e.getStudentId(), e.getClassId(),
                        e.getAction(), e.getNote(), e.isResolved(), e.getRecordedByUserId(), e.getRecordedByName(),
                        e.getCreatedAt()))
                .toList();
    }
}
