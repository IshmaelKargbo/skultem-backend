package com.moriba.skultem.application.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.moriba.skultem.application.dto.LibrarySessionDTO;
import com.moriba.skultem.application.dto.LibrarySessionDetailDTO;
import com.moriba.skultem.application.dto.LibraryVisitDTO;
import com.moriba.skultem.domain.model.LibraryAttendanceCorrection;
import com.moriba.skultem.domain.model.LibraryRosterChange;
import com.moriba.skultem.domain.model.LibrarySession;
import com.moriba.skultem.domain.model.LibrarySession.Slot;
import com.moriba.skultem.domain.model.LibrarySessionRecord;
import com.moriba.skultem.domain.model.LibrarySessionRecord.Status;
import com.moriba.skultem.domain.model.LibraryVisit;

public class LibraryAttendanceDtoMapper {

    public static final String UPCOMING = "UPCOMING";

    public static LibraryVisitDTO toDTO(LibraryVisit v, Instant now) {
        return new LibraryVisitDTO(v.getId(), v.getStudentId(), v.getStudentName(), v.getStudentClass(),
                v.getStatus().name(), v.isUnresolved(now), v.getCheckedInAt(), v.getCheckedInByName(),
                v.getSchoolDayEndsAt(), v.getCheckedOutAt(), v.getCheckedOutByName(), v.durationMinutes(),
                v.getResolvedAt(), v.getResolvedByName(), v.getResolutionReason());
    }

    /** A session nobody has opened yet. */
    public static LibrarySessionDTO upcoming(Slot s, LocalDateTime now, LocalDate today) {
        boolean ended = LocalDateTime.of(s.date(), s.endTime()).isBefore(now);
        boolean editable = !today.isAfter(s.termEndDate());
        return new LibrarySessionDTO(null, s.timetableEntryId(), s.date(), s.classSessionId(), s.className(),
                s.subjectName(), s.periodName(), s.startTime(), s.endTime(), s.roomName(), UPCOMING, ended,
                editable, null, 0, 0, 0, 0, 0);
    }

    public static LibrarySessionDTO toDTO(LibrarySession s, List<LibrarySessionRecord> records, LocalDateTime now,
            LocalDate today) {
        var counted = records.stream().filter(LibrarySessionRecord::isCounted).toList();
        boolean editable = s.isInProgress() && !today.isAfter(s.getTermEndDate());
        return new LibrarySessionDTO(s.getId(), s.getTimetableEntryId(), s.getDate(), s.getClassSessionId(),
                s.getClassName(), s.getSubjectName(), s.getPeriodName(), s.getStartTime(), s.getEndTime(),
                s.getRoomName(), s.getStatus().name(), s.isIncomplete(now), editable, s.getCancelReason(),
                counted.size(), count(counted, Status.PRESENT), count(counted, Status.ABSENT),
                count(counted, Status.EXCUSED), count(counted, Status.UNMARKED));
    }

    public static LibrarySessionDetailDTO toDetail(LibrarySession s, List<LibrarySessionRecord> records,
            List<LibraryRosterChange> changes, List<LibraryAttendanceCorrection> corrections, LocalDateTime now,
            LocalDate today) {
        return new LibrarySessionDetailDTO(toDTO(s, records, now, today), s.getOpenedAt(), s.getOpenedByName(),
                s.getFinalizedAt(), s.getFinalizedByName(), s.getCancelledAt(), s.getCancelledByName(),
                records.stream().map(r -> new LibrarySessionDetailDTO.Record(r.getId(), r.getStudentId(),
                        r.getStudentName(), r.getStudentClass(), r.getStatus().name(), r.getExcuseReason(),
                        r.isRemoved(), r.isAddedManually(), r.isCorrected())).toList(),
                changes.stream().map(c -> new LibrarySessionDetailDTO.RosterChange(c.getStudentId(),
                        c.getStudentName(), c.getChange().name(), c.getReason(), c.getChangedAt(),
                        c.getChangedByName())).toList(),
                corrections.stream().map(c -> new LibrarySessionDetailDTO.Correction(c.getRecordId(),
                        c.getStudentId(), c.getStudentName(), c.getOriginalStatus().name(),
                        c.getCorrectedStatus().name(), c.getReason(), c.getCorrectedAt(), c.getCorrectedByName()))
                        .toList());
    }

    private static int count(List<LibrarySessionRecord> records, Status status) {
        return (int) records.stream().filter(r -> r.getStatus() == status).count();
    }
}
