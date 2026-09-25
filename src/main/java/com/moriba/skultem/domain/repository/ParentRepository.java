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

    // Phone is unique per school (see CreateParentUseCase) - how a bulk import links siblings to
    // the guardian already created for an earlier row.
    Optional<Parent> findByPhoneAndSchool(String phone, String schoolId);

    Page<Parent> findBySchool(String schoolId, Pageable pageable);

    // Matches on name, email or phone - backs the parents list search box.
    Page<Parent> search(String schoolId, String query, Pageable pageable);

    boolean existsByPhoneAndSchoolAndIdNot(String phone, String schoolId, String parentId);

    // A section-limited caller sees a parent only when one of their children is enrolled at one of these
    // levels, or when they have no children yet (so a just-added parent is still findable).
    Page<Parent> searchInLevels(String schoolId, String query, boolean wholeSchool,
            java.util.Collection<com.moriba.skultem.domain.vo.Level> levels, Pageable pageable);

    boolean visibleInLevels(String parentId, String schoolId,
            java.util.Collection<com.moriba.skultem.domain.vo.Level> levels);

    long countAll();
    
    Page<Parent> runReport(String schoolId, List<Filter> filters, Pageable pageable);
}
