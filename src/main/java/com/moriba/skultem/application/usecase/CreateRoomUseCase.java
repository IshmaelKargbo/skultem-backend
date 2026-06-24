package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.dto.RoomDTO;
import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.RoomMapper;
import com.moriba.skultem.application.mapper.TermMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Room;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.model.Term.Status;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.RoomRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateRoomUseCase {

    private final RoomRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action="ROOM_CREATED")
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
}
