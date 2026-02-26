package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.StreamSubject;

public interface StreamSubjectRepository {
    void save(StreamSubject domain);
    void delete(StreamSubject domain);

    Optional<StreamSubject> findById(String id);

    Optional<StreamSubject> findByStreamIdAndSubjectIdAndSchoolId(String streamId, String subjectId, String schoolId);

    Page<StreamSubject> findBySchoolId(String school, Pageable pageable);

    Page<StreamSubject> findAllByStreamIdAndSchoolId(String streamId, String schoolId, Pageable pageable);

    boolean existsByStreamAndSubject(String streamId, String subjectId);

    boolean existsByStreamIdAndSubjectIdAndSchoolId(String streamId, String subjectId, String schoolId);

    Page<StreamSubject> findByStream(String streamId, Pageable pageable);
}
