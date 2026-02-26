package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.model.Clazz.Status;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ClassJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ClassMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClassAdapter implements ClassRepository {
    private final ClassJpaRepository repo;

    @Override
    public void save(Clazz domain) {
        var entity = ClassMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public boolean existsByNameAndSchool(String name, String school) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, school);
    }

    @Override
    public boolean existsByLevelOrderAndSchool(int levelOrder, String school) {
        return repo.existsByLevelOrderAndSchoolId(levelOrder, school);
    }

    @Override
    public Page<Clazz> findBySchool(String school, Pageable pageable) {
       return repo.findAllBySchoolIdAndStatusOrderByLevelOrderAsc(school, Status.ACTIVE, pageable).map(ClassMapper::toDomain);
    }

    @Override
    public void delete(Clazz domain) {
        var entity = ClassMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public int countBySchoolAndLevel(String school, Level level) {
        return repo.countBySchoolIdAndLevel(school, level);
    }

    @Override
    public Optional<Clazz> findBySchoolAndLevelAndTerminal(String school, Level level) {
        return repo.findBySchoolIdAndLevelAndTerminalTrue(school, level).map(ClassMapper::toDomain);
    }

    @Override
    public Optional<Clazz> findBySchoolAndLevelOrder(String school, int levelOrder) {
        return repo.findBySchoolIdAndLevelOrder(school, levelOrder).map(ClassMapper::toDomain);
    }

    @Override
    public Optional<Clazz> findByIdAndSchool(String id, String school) {
        return repo.findByIdAndSchoolId(id, school).map(ClassMapper::toDomain);
    }

}
