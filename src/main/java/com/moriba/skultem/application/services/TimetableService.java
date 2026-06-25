package com.moriba.skultem.application.services;

import com.moriba.skultem.application.dto.*;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.*;
import com.moriba.skultem.application.usecase.CreatePeriodUseCase;
import com.moriba.skultem.application.usecase.CreateTimetableUseCase;
import com.moriba.skultem.application.usecase.DeletePeriodUseCase;
import com.moriba.skultem.application.usecase.ManageRoomUseCase;
import com.moriba.skultem.application.usecase.SetTimingUseCase;
import com.moriba.skultem.application.usecase.SetWorkingDayUseCase;
import com.moriba.skultem.domain.model.WorkingDay.Day;
import com.moriba.skultem.domain.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final RoomRepository roomRepo;
    private final TimingRepository timingRepo;
    private final TimetableRepository timetableRepo;
    private final PeriodRepository periodRepo;
    private final WorkingDayRepository workingDayRepo;
    private final SetTimingUseCase setTimingUseCase;
    private final ManageRoomUseCase manageRoomUsecase;
    private final CreateTimetableUseCase timetableUseCase;
    private final CreatePeriodUseCase createPeriodUseCase;
    private final SetWorkingDayUseCase setWorkingDayUseCase;
    private final DeletePeriodUseCase deletePeriodUseCase;

    public Page<RoomDTO> searchRoom(String schoolId, String value, int page, int size) {
        Pageable pageable = PageableMapper.toPageable(page, size);

        return roomRepo.search(schoolId, value, pageable).map(RoomMapper::toDTO);
    }

    public RoomDTO createRoom(String schoolId, String name, String no, String description) {
        return manageRoomUsecase.execute(schoolId, name, no, description);
    }

    public RoomDTO updateRoom(String id, String schoolId, String name, String no, String description) {
        return manageRoomUsecase.executeUpdate(id, schoolId, name, no, description);
    }
    
    public RoomDTO deleteRoom(String schoolId, String id) {
        return manageRoomUsecase.executeDelete(id, schoolId);
    }

    public PeriodDTO createPeriod(String schoolId, String session) {
        var domain = createPeriodUseCase.execute(schoolId, session);
        var subjects = getSubjects(domain.getSchoolId(), domain.getId());
        return PeriodMapper.toDTO(domain, subjects);
    }

    public PeriodDTO createBreak(String schoolId, String session) {
        var domain = createPeriodUseCase.executeBreak(schoolId, session);
        List<TimetableDTO> subjects = new ArrayList<>();

        if (!domain.isBreak() && !domain.isLunch()) {
            subjects = getSubjects(domain.getSchoolId(), domain.getId());
        }

        return PeriodMapper.toDTO(domain, subjects);
    }

    public PeriodDTO createLunch(String schoolId, String session) {
        var domain = createPeriodUseCase.executeLunch(schoolId, session);
        List<TimetableDTO> subjects = new ArrayList<>();
        if (!domain.isBreak() && !domain.isLunch()) {
            subjects = getSubjects(domain.getSchoolId(), domain.getId());
        }

        return PeriodMapper.toDTO(domain, subjects);
    }

    public TimetableDTO createTimetable(String schoolId, String periodId, String subject, Day day, String roomId,
            String color) {
        var domain = timetableUseCase.execut(schoolId, periodId, subject, day, roomId, color);
        return TimetableMapper.toDTO(domain);
    }

    public List<PeriodDTO> getTimeTable(String session) {
        return periodRepo.findAllBySessionId(session).stream().map((e) -> {
            List<TimetableDTO> subjects = new ArrayList<>();

            if (!e.isBreak() && !e.isLunch()) {
                subjects = getSubjects(e.getSchoolId(), e.getId());
            }

            return PeriodMapper.toDTO(e, subjects);
        }).toList();
    }

    // getTiming
    public TimingDTO getTiming(String schoolId) {
        var domain = timingRepo.findBySchoolId(schoolId).orElseThrow(() -> new NotFoundException("timing not found"));
        return TimingMapper.toDTO(domain);
    }

    // setTiming
    public TimingDTO setTiming(String schoolId, LocalTime startTime, LocalTime endTime, int periodDuration,
            int breakDuration, int lunchDuration) {
        var domain = setTimingUseCase.execute(schoolId, startTime, endTime, periodDuration, breakDuration,
                lunchDuration);
        return TimingMapper.toDTO(domain);
    }

    // deletePeriod
    public PeriodDTO deletePeriod(String id) {
        var domain = deletePeriodUseCase.execute(id);
        return PeriodMapper.toDTO(domain, null);
    }

    public List<WorkingDayDTO> listWorkingDays(String schoolId) {
        return workingDayRepo.findAllBySchoolId(schoolId).stream()
                .sorted(Comparator.comparing(day -> day.getDay().ordinal()))
                .map(WorkingDayMapper::toDTO)
                .toList();
    }

    public List<WorkingDayDTO> setWorkingDays(String schoolId, List<SetWorkingDayUseCase.WorkingDayRecord> days) {
        return setWorkingDayUseCase.execute(schoolId, days).stream().map(WorkingDayMapper::toDTO).toList();
    }

    private List<TimetableDTO> getSubjects(String school, String period) {
        var days = listWorkingDays(school);
        List<TimetableDTO> list = new ArrayList<>();

        for (WorkingDayDTO day : days) {

            if (!day.state()) {
                continue;
            }

            var timetable = timetableRepo
                    .findByPeriodIdAndDayIdAndSchoolId(period, day.id(), school)
                    .orElse(null);

            if (timetable == null) {
                list.add(null);
                continue;
            }

            list.add(TimetableMapper.toDTO(timetable));
        }

        return list;
    }
}
