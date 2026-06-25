package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.dto.RoomDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.RoomMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Room;
import com.moriba.skultem.domain.repository.RoomRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ManageRoomUseCase {

    private final RoomRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "ROOM_CREATED")
    public RoomDTO execute(String schoolId, String name, String no, String description) {

        String id = UUID.randomUUID().toString();
        var domain = Room.create(id, schoolId, name, no, description);
        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Room created",
                domain.getName() + " (" + domain.getName() + ")",
                null,
                domain.getId());

        return RoomMapper.toDTO(domain);
    }

    @AuditLogAnnotation(action = "ROOM_UPDATED")
    public RoomDTO executeUpdate(String id, String schoolId, String name, String no, String description) {

        var domain = repo.findByIdAndSchoolId(id, schoolId).orElseThrow(() -> new NotFoundException("room not found"));
        domain.update(name, no, description);
        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Room updated",
                domain.getName() + " (" + domain.getName() + ")",
                null,
                domain.getId());

        return RoomMapper.toDTO(domain);
    }

    @AuditLogAnnotation(action = "ROOM_DELETED")
    public RoomDTO executeDelete(String id, String schoolId) {
        var domain = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("room not found"));
        try {
            repo.delete(domain);
        } catch (Exception e) {
            domain.delete();
            repo.save(domain);
        }

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Room deleted",
                domain.getName() + " (" + domain.getName() + ")",
                null,
                domain.getId());
        return RoomMapper.toDTO(domain);
    }
}
