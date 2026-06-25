package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Room;
import com.moriba.skultem.infrastructure.persistence.entity.RoomEntity;

public class RoomMapper {
    public static Room toDomain(RoomEntity param) {
        return new Room(param.getId(), param.getSchoolId(), param.getName(), param.getNo(), param.getDescription(), param.isSoftDelete(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static RoomEntity toEntity(Room param) {
        return RoomEntity.builder()
                .id(param.getId())
                .name(param.getName())
                .softDelete(param.isSoftDelete())
                .no(param.getNo())
                .description(param.getDescription())
                .schoolId(param.getSchoolId())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
