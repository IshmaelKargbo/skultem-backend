package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.StaffManagementSection;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.persistence.entity.StaffManagementSectionEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.StaffManagementSectionJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StaffManagementSectionAdapter implements StaffManagementSectionRepository {
    private final StaffManagementSectionJpaRepository repo;

    @Override
    public List<StaffManagementSection> findBySchoolAndUserAndRole(String schoolId, String userId, Role role) {
        return repo.findAllBySchoolIdAndUserIdAndRole(schoolId, userId, role).stream().map(this::toDomain).toList();
    }

    @Override
    public List<StaffManagementSection> findBySchoolId(String schoolId) {
        return repo.findAllBySchoolId(schoolId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsBySectionId(String managementSectionId) {
        return repo.existsByManagementSectionId(managementSectionId);
    }

    @Override
    public void replace(String schoolId, String userId, Role role, List<String> managementSectionIds) {
        repo.deleteAllBySchoolIdAndUserIdAndRole(schoolId, userId, role);
        for (var sectionId : new LinkedHashSet<>(managementSectionIds)) {
            repo.save(toEntity(StaffManagementSection.create(schoolId, userId, role, sectionId)));
        }
    }

    private StaffManagementSection toDomain(StaffManagementSectionEntity e) {
        return new StaffManagementSection(e.getId(), e.getSchoolId(), e.getUserId(), e.getRole(),
                e.getManagementSectionId(), e.getCreatedAt());
    }

    private StaffManagementSectionEntity toEntity(StaffManagementSection d) {
        return StaffManagementSectionEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .userId(d.getUserId())
                .role(d.getRole())
                .managementSectionId(d.getManagementSectionId())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
