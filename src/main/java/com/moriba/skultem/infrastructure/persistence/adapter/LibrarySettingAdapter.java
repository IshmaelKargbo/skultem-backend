package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.LibrarySetting;
import com.moriba.skultem.domain.repository.LibrarySettingRepository;
import com.moriba.skultem.infrastructure.persistence.entity.LibrarySettingEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.LibrarySettingJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LibrarySettingAdapter implements LibrarySettingRepository {
    private final LibrarySettingJpaRepository repo;

    @Override
    public void save(LibrarySetting d) {
        repo.save(LibrarySettingEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .managementSectionId(d.getSectionId())
                .maxStudentLoans(d.getMaxStudentLoans())
                .maxTeacherLoans(d.getMaxTeacherLoans())
                .blockWhenOverdue(d.isBlockWhenOverdue())
                .defaultLoanDays(d.getDefaultLoanDays())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build());
    }

    @Override
    public Optional<LibrarySetting> findBySchool(String schoolId) {
        return repo.findBySchoolIdAndManagementSectionIdIsNull(schoolId).map(LibrarySettingAdapter::toDomain);
    }

    @Override
    public Optional<LibrarySetting> findBySchoolAndSection(String schoolId, String sectionId) {
        return repo.findBySchoolIdAndManagementSectionId(schoolId, sectionId).map(LibrarySettingAdapter::toDomain);
    }

    @Override
    public void delete(LibrarySetting d) {
        repo.deleteById(d.getId());
    }

    private static LibrarySetting toDomain(LibrarySettingEntity e) {
        return new LibrarySetting(e.getId(), e.getSchoolId(), e.getManagementSectionId(), e.getMaxStudentLoans(),
                e.getMaxTeacherLoans(), e.isBlockWhenOverdue(), e.getDefaultLoanDays(), e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
