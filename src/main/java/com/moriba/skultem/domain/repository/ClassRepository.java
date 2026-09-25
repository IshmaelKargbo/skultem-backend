package com.moriba.skultem.domain.repository;

import java.util.Collection;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.vo.Level;

public interface ClassRepository {
    void save(Clazz domain);
    
    Optional<Clazz> findByIdAndSchool(String id, String school);

    boolean existsByNameAndSchool(String name, String school);

    boolean existsByLevelOrderAndSchool(int levelOrder, String school);

    // Highest level order in the school across every class, including soft-deleted ones (so a new
    // class never lands on a deleted class's slot); 0 when the school has none yet.
    int maxLevelOrderBySchool(String school);

    Optional<Clazz> findBySchoolAndLevelAndTerminal(String school, Level level);

    int countBySchoolAndLevel(String school, Level level);

    // Non-deleted classes only - what decides whether a school can stop offering a level.
    int countActiveBySchoolAndLevel(String school, Level level);

    Optional<Clazz> findBySchoolAndLevelOrder(String school, int levelOrder);

    Page<Clazz> findBySchool(String school, Pageable pageable);

    // Only classes at these levels - a management-section caller's view (see SectionScope).
    Page<Clazz> findBySchool(String school, Collection<Level> levels, Pageable pageable);

    void delete(Clazz domain);
}
