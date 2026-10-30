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
        return repo.findBySchoolId(schoolId).map(e -> new LibrarySetting(e.getId(), e.getSchoolId(),
                e.getMaxStudentLoans(), e.getMaxTeacherLoans(), e.isBlockWhenOverdue(), e.getDefaultLoanDays(),
                e.getCreatedAt(), e.getUpdatedAt()));
    }
}
