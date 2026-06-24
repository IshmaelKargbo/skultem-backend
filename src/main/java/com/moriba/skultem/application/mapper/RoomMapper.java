package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.RoomDTO;
import com.moriba.skultem.domain.model.Room;

public class RoomMapper {
    public static RoomDTO toDTO(Room param) {
        if (param == null)
            return null;

        return new RoomDTO(param.getId(), param.getName(), param.getNo(), param.getDescription(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
