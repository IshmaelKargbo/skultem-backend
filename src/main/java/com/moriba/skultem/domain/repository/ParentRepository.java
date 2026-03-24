package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.vo.Filter;

public interface ParentRepository {
    void save(Parent domain);

    Optional<Parent> findById(String id);

    Optional<Parent> findByIdAndSchoolId(String id, String school);

    Optional<Parent> findByUserIdAndSchoolId(String userId, String schoolId);

    boolean existsByPhoneAndSchool(String phone, String schoolId);

    Page<Parent> findBySchool(String schoolId, Pageable pageable);

    long countAll();
    
    Page<Parent> runReport(String schoolId, List<Filter> filters, Pageable pageable);
}
