package com.moriba.skultem.domain.repository;

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

    Optional<Clazz> findBySchoolAndLevelAndTerminal(String school, Level level);

    int countBySchoolAndLevel(String school, Level level);

    Optional<Clazz> findBySchoolAndLevelOrder(String school, int levelOrder);

    Page<Clazz> findBySchool(String school, Pageable pageable);

    void delete(Clazz domain);
}
