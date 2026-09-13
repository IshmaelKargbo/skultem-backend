package com.moriba.skultem.application.services;

import com.moriba.skultem.application.dto.*;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.*;
import com.moriba.skultem.application.usecase.AssignTimingLevelUseCase;
import com.moriba.skultem.application.usecase.CreatePeriodUseCase;
import com.moriba.skultem.application.usecase.CreateTimetableUseCase;
import com.moriba.skultem.application.usecase.DeletePeriodUseCase;
import com.moriba.skultem.application.usecase.DeleteTimingUseCase;
import com.moriba.skultem.application.usecase.ManageRoomUseCase;
import com.moriba.skultem.application.usecase.ResolveTimingForLevelUseCase;
import com.moriba.skultem.application.usecase.SaveTimingUseCase;
import com.moriba.skultem.application.usecase.SetDefaultTimingUseCase;
import com.moriba.skultem.application.usecase.SetWorkingDayUseCase;
import com.moriba.skultem.domain.model.WorkingDay.Day;
import com.moriba.skultem.domain.repository.*;
import com.moriba.skultem.domain.vo.Level;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final RoomRepository roomRepo;
    private final TimingRepository timingRepo;
    private final TimingLevelRepository timingLevelRepo;
    private final TimetableRepository timetableRepo;
    private final PeriodRepository periodRepo;
    private final WorkingDayRepository workingDayRepo;
    private final ClassSessionRepository classSessionRepo;
    private final ResolveTimingForLevelUseCase resolveTimingForLevelUseCase;
    private final SaveTimingUseCase saveTimingUseCase;
    private final DeleteTimingUseCase deleteTimingUseCase;
    private final SetDefaultTimingUseCase setDefaultTimingUseCase;
    private final AssignTimingLevelUseCase assignTimingLevelUseCase;
    private final ManageRoomUseCase manageRoomUsecase;
    private final CreateTimetableUseCase timetableUseCase;
    private final CreatePeriodUseCase createPeriodUseCase;
    private final SetWorkingDayUseCase setWorkingDayUseCase;
    private final DeletePeriodUseCase deletePeriodUseCase;

    public Page<RoomDTO> searchRoom(String schoolId, String value, int page, int size) {
        Pageable pageable = PageableMapper.toPage(page, size);

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
        var subjects = getSubjects(domain.getSchoolId(), domain.getId(), domain.getSession().getId());
        return PeriodMapper.toDTO(domain, subjects);
    }

    public PeriodDTO createBreak(String schoolId, String session) {
        var domain = createPeriodUseCase.executeBreak(schoolId, session);
        List<TimetableDTO> subjects = new ArrayList<>();

        if (!domain.isBreak() && !domain.isLunch()) {
            subjects = getSubjects(domain.getSchoolId(), domain.getId(), domain.getSession().getId());
        }

        return PeriodMapper.toDTO(domain, subjects);
    }

    public PeriodDTO createLunch(String schoolId, String session) {
        var domain = createPeriodUseCase.executeLunch(schoolId, session);
        List<TimetableDTO> subjects = new ArrayList<>();
        if (!domain.isBreak() && !domain.isLunch()) {
            subjects = getSubjects(domain.getSchoolId(), domain.getId(), domain.getSession().getId());
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
                subjects = getSubjects(e.getSchoolId(), e.getId(), session);
            }

            return PeriodMapper.toDTO(e, subjects);
        }).toList();
    }

    // Every Timing template for the school - the Settings page's template manager. A school that
    // hasn't configured any timing yet gets one unsaved, editable "Default" placeholder rather
    // than an empty list, same reasoning as the old singleton behaviour: show something sensible
    // to fill in, and saving it is what actually creates the first (default) template.
    public List<TimingDTO> listTimings(String schoolId) {
        var timings = timingRepo.findAllBySchoolId(schoolId);

        if (timings.isEmpty()) {
            return List.of(defaultTimingPlaceholder(schoolId));
        }

        var levelsByTiming = groupLevelsByTiming(schoolId);

        return timings.stream()
                .map(t -> TimingMapper.toDTO(t, levelsByTiming.getOrDefault(t.getId(), List.of())))
                .toList();
    }

    private Map<String, List<Level>> groupLevelsByTiming(String schoolId) {
        Map<String, List<Level>> result = new HashMap<>();

        for (var assignment : timingLevelRepo.findAllBySchoolId(schoolId)) {
            result.computeIfAbsent(assignment.getTiming().getId(), k -> new ArrayList<>()).add(assignment.getLevel());
        }

        return result;
    }

    private TimingDTO defaultTimingPlaceholder(String schoolId) {
        return new TimingDTO(null, schoolId, "Default", true, LocalTime.of(8, 0), LocalTime.of(15, 0), 40, 15, 45,
                List.of(), null, null);
    }

    public TimingDTO saveTiming(String schoolId, String id, String name, LocalTime startTime, LocalTime endTime,
            int periodDuration, int breakDuration, int lunchDuration) {
        var domain = saveTimingUseCase.execute(schoolId, id, name, startTime, endTime, periodDuration, breakDuration,
                lunchDuration);
        var levels = timingLevelRepo.findAllBySchoolId(schoolId).stream()
                .filter(a -> a.getTiming().getId().equals(domain.getId()))
                .map(a -> a.getLevel())
                .toList();
        return TimingMapper.toDTO(domain, levels);
    }

    public TimingDTO deleteTiming(String schoolId, String id) {
        var domain = deleteTimingUseCase.execute(schoolId, id);
        return TimingMapper.toDTO(domain, List.of());
    }

    public TimingDTO setDefaultTiming(String schoolId, String id) {
        var domain = setDefaultTimingUseCase.execute(schoolId, id);
        return TimingMapper.toDTO(domain, List.of());
    }

    public List<TimingLevelDTO> listTimingLevels(String schoolId) {
        return timingLevelRepo.findAllBySchoolId(schoolId).stream().map(TimingLevelMapper::toDTO).toList();
    }

    public TimingLevelDTO assignTimingLevel(String schoolId, Level level, String timingId) {
        var domain = assignTimingLevelUseCase.execute(schoolId, level, timingId);
        return TimingLevelMapper.toDTO(domain);
    }

    // deletePeriod
    public PeriodDTO deletePeriod(String id) {
        var domain = deletePeriodUseCase.execute(id);
        return PeriodMapper.toDTO(domain, null);
    }

    // The working days that apply to a specific class session's timetable grid - resolved via
    // that session's Level, same rule CreatePeriodUseCase uses to pick the session's Timing.
    public List<WorkingDayDTO> listWorkingDaysBySession(String schoolId, String sessionId) {
        var session = classSessionRepo.findByIdAndSchoolId(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("No class found"));

        var level = session.getClazz() != null ? session.getClazz().getLevel() : null;
        var timing = resolveTimingForLevelUseCase.execute(schoolId, level);

        return listWorkingDaysByTiming(schoolId, timing.getId());
    }

    // A specific Timing template's working days directly - the Settings page's template editor.
    public List<WorkingDayDTO> listWorkingDaysByTiming(String schoolId, String timingId) {
        var days = workingDayRepo.findAllByTimingId(timingId);

        if (days.isEmpty()) {
            return defaultWorkingDays();
        }

        return days.stream()
                .sorted(Comparator.comparing(day -> day.getDay().ordinal()))
                .map(WorkingDayMapper::toDTO)
                .toList();
    }

    // A school that hasn't saved working days yet gets the usual school week (Mon-Fri on,
    // weekend off) rather than an empty list - same reasoning as defaultTimingPlaceholder(): show
    // something sensible and editable instead of nothing. Every day, weekend included, is still
    // freely toggleable once Edit is clicked - this is only the starting point.
    private static final Set<Day> DEFAULT_WORKING_DAYS = EnumSet.of(
            Day.MONDAY, Day.TUESDAY, Day.WEDNESDAY, Day.THURSDAY, Day.FRIDAY);

    private List<WorkingDayDTO> defaultWorkingDays() {
        return Arrays.stream(Day.values())
                .map(day -> new WorkingDayDTO(null, day, null, DEFAULT_WORKING_DAYS.contains(day), null, null))
                .toList();
    }

    public List<WorkingDayDTO> setWorkingDays(String schoolId, String timingId,
            List<SetWorkingDayUseCase.WorkingDayRecord> days) {
        return setWorkingDayUseCase.execute(schoolId, timingId, days).stream().map(WorkingDayMapper::toDTO).toList();
    }

    private List<TimetableDTO> getSubjects(String school, String period, String sessionId) {
        var days = listWorkingDaysBySession(school, sessionId);
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
