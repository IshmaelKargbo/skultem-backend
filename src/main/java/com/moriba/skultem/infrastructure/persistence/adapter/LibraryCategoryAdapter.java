package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.LibraryCategory;
import com.moriba.skultem.domain.repository.LibraryCategoryRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.LibraryCategoryJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.LibraryCategoryMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LibraryCategoryAdapter implements LibraryCategoryRepository {
    private final LibraryCategoryJpaRepository repo;

    @Override
    public void save(LibraryCategory domain) {
        repo.save(LibraryCategoryMapper.toEntity(domain));
    }

    @Override
    public void deleteById(String id) {
        repo.deleteById(id);
    }

    @Override
    public boolean existsByNameAndSchool(String name, String schoolId) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, schoolId);
    }

    @Override
    public Optional<LibraryCategory> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(LibraryCategoryMapper::toDomain);
    }

    @Override
    public Page<LibraryCategory> search(String schoolId, String query, Pageable pageable) {
        return repo.search(schoolId, query == null ? "" : query, pageable).map(LibraryCategoryMapper::toDomain);
    }

    @Override
    public long countBySchool(String schoolId) {
        return repo.countBySchoolId(schoolId);
    }
}
