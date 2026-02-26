package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ReferenceSequence;
import com.moriba.skultem.infrastructure.persistence.entity.ReferenceSequenceEntity;

public class ReferenceSequenceMapper {
    public static ReferenceSequence toDomain(ReferenceSequenceEntity param) {
        var domain = new ReferenceSequence();
        domain.setLastNumber(param.getLastNumber());
        domain.setReferenceType(param.getReferenceType());
        domain.setYear(param.getYear());
        return domain;
    }

    public static ReferenceSequenceEntity toEntity(ReferenceSequence args) {
        return ReferenceSequenceEntity.builder()
                .lastNumber(args.getLastNumber())
                .year(args.getYear())
                .referenceType(args.getReferenceType())
                .build();
    }
}
