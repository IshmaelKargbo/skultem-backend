package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.vo.Address;
import com.moriba.skultem.domain.model.vo.Owner;
import com.moriba.skultem.infrastructure.persistence.entity.SchoolEntity;

public class SchoolMapper {
    public static School toDomain(SchoolEntity param) {
        Owner owner = JsonMapper.fromJson(param.getOwner(), Owner.class);
        Address address = JsonMapper.fromJson(param.getAddress(), Address.class);

        return new School(param.getId(), param.getName(), param.getDomain(), address, owner,
                param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static SchoolEntity toEntity(School args) {
        String owner = JsonMapper.toJson(args.getOwner());
        String address = JsonMapper.toJson(args.getAddress());

        return SchoolEntity.builder()
                .id(args.getId())
                .name(args.getName())
                .owner(owner)
                .address(address)
                .domain(args.getDomain())
                .status(args.getStatus())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
