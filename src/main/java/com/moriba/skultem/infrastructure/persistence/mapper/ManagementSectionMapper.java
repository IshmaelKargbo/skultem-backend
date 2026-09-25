package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.infrastructure.persistence.entity.ManagementSectionEntity;

public class ManagementSectionMapper {
    public static ManagementSection toDomain(ManagementSectionEntity param) {
        return new ManagementSection(param.getId(), param.getSchoolId(), param.getName(), param.getDisplayOrder(),
                param.getLogo(), param.getPrincipalName(), param.getPrincipalSignature(),
                JsonMapper.fromJson(param.getAddress(), Address.class), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ManagementSectionEntity toEntity(ManagementSection args) {
        return ManagementSectionEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .name(args.getName())
                .displayOrder(args.getDisplayOrder())
                .logo(args.getLogo())
                .principalName(args.getPrincipalName())
                .principalSignature(args.getPrincipalSignature())
                .address(args.getAddress() == null ? null : JsonMapper.toJson(args.getAddress()))
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
