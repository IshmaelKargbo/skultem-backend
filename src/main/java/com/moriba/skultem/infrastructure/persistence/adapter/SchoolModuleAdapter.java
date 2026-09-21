package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.repository.SchoolModuleRepository;
import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.persistence.entity.SchoolModuleEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.SchoolModuleJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SchoolModuleAdapter implements SchoolModuleRepository {

    private final SchoolModuleJpaRepository repo;

    @Override
    public Set<FeatureModule> findEnabledBySchool(String schoolId) {
        Set<FeatureModule> enabled = EnumSet.noneOf(FeatureModule.class);
        for (var row : repo.findAllBySchoolIdAndEnabledTrue(schoolId)) {
            // A key this version doesn't know (a module removed in code) is ignored, not an error.
            try {
                enabled.add(FeatureModule.valueOf(row.getModuleKey()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return enabled;
    }

    @Override
    public void setEnabled(String schoolId, FeatureModule module, boolean enabled, String changedByUserId) {
        var now = Instant.now();
        var row = repo.findBySchoolIdAndModuleKey(schoolId, module.name()).orElse(null);

        if (row == null) {
            row = SchoolModuleEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .schoolId(schoolId)
                    .moduleKey(module.name())
                    .installedBy(changedByUserId)
                    .installedAt(now)
                    .build();
        } else if (enabled && !row.isEnabled()) {
            row.setInstalledBy(changedByUserId);
            row.setInstalledAt(now);
        }

        row.setEnabled(enabled);
        row.setUpdatedAt(now);
        repo.save(row);
    }
}
