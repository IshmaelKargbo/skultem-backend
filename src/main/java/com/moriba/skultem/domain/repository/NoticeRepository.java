package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Notice;

public interface NoticeRepository {
    void save(Notice domain);

    void delete(Notice domain);

    Optional<Notice> findByIdAndSchool(String id, String schoolId);

    Page<Notice> findAllBySchoolId(String schoolId, Pageable pageable);
}
