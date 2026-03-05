package com.moriba.skultem.infrastructure.persistence.mapper;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.vo.Address;
import com.moriba.skultem.domain.model.vo.GradeBand;
import com.moriba.skultem.domain.model.vo.Owner;
import com.moriba.skultem.infrastructure.persistence.entity.SchoolEntity;

public class SchoolMapper {
    public static School toDomain(SchoolEntity param) {
        Owner owner = JsonMapper.fromJson(param.getOwner(), Owner.class);
        Address address = JsonMapper.fromJson(param.getAddress(), Address.class);
        List<GradeBand> gradingScale = JsonMapper.fromJson(param.getGradingScale(), new TypeReference<List<GradeBand>>() {
        });

        return new School(param.getId(), param.getName(), param.getDomain(), address, owner,
                param.getStatus(), gradingScale, param.getCreatedAt(), param.getUpdatedAt());
    }

    public static SchoolEntity toEntity(School args) {
        String owner = JsonMapper.toJson(args.getOwner());
        String address = JsonMapper.toJson(args.getAddress());
        String gradingScale = JsonMapper.toJson(args.getGradingScale());

        return SchoolEntity.builder()
                .id(args.getId())
                .name(args.getName())
                .owner(owner)
                .address(address)
                .gradingScale(gradingScale)
                .domain(args.getDomain())
                .status(args.getStatus())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
