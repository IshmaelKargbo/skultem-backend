package com.moriba.skultem.domain.repository;

import java.util.List;

import com.moriba.skultem.domain.model.StudentFollowUp;

public interface StudentFollowUpRepository {
    void save(StudentFollowUp followUp);

    // Newest first.
    List<StudentFollowUp> findAllByClass(String schoolId, String classId);
}
