package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.LibraryAttendanceCorrection;
import com.moriba.skultem.domain.model.LibraryRosterChange;
import com.moriba.skultem.domain.model.LibrarySession;
import com.moriba.skultem.domain.model.LibrarySessionRecord;
import com.moriba.skultem.domain.model.LibraryVisit;
import com.moriba.skultem.infrastructure.persistence.entity.LibraryAttendanceCorrectionEntity;
import com.moriba.skultem.infrastructure.persistence.entity.LibraryRosterChangeEntity;
import com.moriba.skultem.infrastructure.persistence.entity.LibrarySessionEntity;
import com.moriba.skultem.infrastructure.persistence.entity.LibrarySessionRecordEntity;
import com.moriba.skultem.infrastructure.persistence.entity.LibraryVisitEntity;

// Library Attendance rows <-> domain. Flat records with no relations, so one mapper covers all five.
public class LibraryAttendanceMapper {

    public static LibraryVisit toDomain(LibraryVisitEntity e) {
        return new LibraryVisit(e.getId(), e.getSchoolId(), e.getStudentId(), e.getStudentName(), e.getStudentClass(),
                e.getStatus(), e.getCheckedInAt(), e.getCheckedInBy(), e.getCheckedInByName(), e.getSchoolDayEndsAt(),
                e.getCheckedOutAt(), e.getCheckedOutBy(), e.getCheckedOutByName(), e.getResolvedAt(),
                e.getResolvedBy(), e.getResolvedByName(), e.getResolutionReason(), e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public static LibraryVisitEntity toEntity(LibraryVisit d) {
        return LibraryVisitEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .studentId(d.getStudentId())
                .studentName(d.getStudentName())
                .studentClass(d.getStudentClass())
                .status(d.getStatus())
                .checkedInAt(d.getCheckedInAt())
                .checkedInBy(d.getCheckedInBy())
                .checkedInByName(d.getCheckedInByName())
                .schoolDayEndsAt(d.getSchoolDayEndsAt())
                .checkedOutAt(d.getCheckedOutAt())
                .checkedOutBy(d.getCheckedOutBy())
                .checkedOutByName(d.getCheckedOutByName())
                .resolvedAt(d.getResolvedAt())
                .resolvedBy(d.getResolvedBy())
                .resolvedByName(d.getResolvedByName())
                .resolutionReason(d.getResolutionReason())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public static LibrarySession toDomain(LibrarySessionEntity e) {
        return new LibrarySession(e.getId(), e.getSchoolId(), e.getTimetableEntryId(), e.getDate(),
                e.getClassSessionId(), e.getClassName(), e.getSubjectId(), e.getSubjectName(), e.getPeriodName(),
                e.getStartTime(), e.getEndTime(), e.getRoomName(), e.getTermId(), e.getTermEndDate(), e.getStatus(),
                e.getOpenedAt(), e.getOpenedBy(), e.getOpenedByName(), e.getFinalizedAt(), e.getFinalizedBy(),
                e.getFinalizedByName(), e.getCancelledAt(), e.getCancelledBy(), e.getCancelledByName(),
                e.getCancelReason(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public static LibrarySessionEntity toEntity(LibrarySession d) {
        return LibrarySessionEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .timetableEntryId(d.getTimetableEntryId())
                .date(d.getDate())
                .classSessionId(d.getClassSessionId())
                .className(d.getClassName())
                .subjectId(d.getSubjectId())
                .subjectName(d.getSubjectName())
                .periodName(d.getPeriodName())
                .startTime(d.getStartTime())
                .endTime(d.getEndTime())
                .roomName(d.getRoomName())
                .termId(d.getTermId())
                .termEndDate(d.getTermEndDate())
                .status(d.getStatus())
                .openedAt(d.getOpenedAt())
                .openedBy(d.getOpenedBy())
                .openedByName(d.getOpenedByName())
                .finalizedAt(d.getFinalizedAt())
                .finalizedBy(d.getFinalizedBy())
                .finalizedByName(d.getFinalizedByName())
                .cancelledAt(d.getCancelledAt())
                .cancelledBy(d.getCancelledBy())
                .cancelledByName(d.getCancelledByName())
                .cancelReason(d.getCancelReason())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public static LibrarySessionRecord toDomain(LibrarySessionRecordEntity e) {
        return new LibrarySessionRecord(e.getId(), e.getSchoolId(), e.getSessionId(), e.getStudentId(),
                e.getStudentName(), e.getStudentClass(), e.getStatus(), e.getExcuseReason(), e.isRemoved(),
                e.isAddedManually(), e.isCorrected(), e.getMarkedAt(), e.getMarkedBy(), e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public static LibrarySessionRecordEntity toEntity(LibrarySessionRecord d) {
        return LibrarySessionRecordEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .sessionId(d.getSessionId())
                .studentId(d.getStudentId())
                .studentName(d.getStudentName())
                .studentClass(d.getStudentClass())
                .status(d.getStatus())
                .excuseReason(d.getExcuseReason())
                .removed(d.isRemoved())
                .addedManually(d.isAddedManually())
                .corrected(d.isCorrected())
                .markedAt(d.getMarkedAt())
                .markedBy(d.getMarkedBy())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public static LibraryRosterChange toDomain(LibraryRosterChangeEntity e) {
        return new LibraryRosterChange(e.getId(), e.getSchoolId(), e.getSessionId(), e.getStudentId(),
                e.getStudentName(), e.getChange(), e.getReason(), e.getChangedAt(), e.getChangedBy(),
                e.getChangedByName(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public static LibraryRosterChangeEntity toEntity(LibraryRosterChange d) {
        return LibraryRosterChangeEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .sessionId(d.getSessionId())
                .studentId(d.getStudentId())
                .studentName(d.getStudentName())
                .change(d.getChange())
                .reason(d.getReason())
                .changedAt(d.getChangedAt())
                .changedBy(d.getChangedBy())
                .changedByName(d.getChangedByName())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public static LibraryAttendanceCorrection toDomain(LibraryAttendanceCorrectionEntity e) {
        return new LibraryAttendanceCorrection(e.getId(), e.getSchoolId(), e.getSessionId(), e.getRecordId(),
                e.getStudentId(), e.getStudentName(), e.getOriginalStatus(), e.getCorrectedStatus(), e.getReason(),
                e.getCorrectedAt(), e.getCorrectedBy(), e.getCorrectedByName(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public static LibraryAttendanceCorrectionEntity toEntity(LibraryAttendanceCorrection d) {
        return LibraryAttendanceCorrectionEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .sessionId(d.getSessionId())
                .recordId(d.getRecordId())
                .studentId(d.getStudentId())
                .studentName(d.getStudentName())
                .originalStatus(d.getOriginalStatus())
                .correctedStatus(d.getCorrectedStatus())
                .reason(d.getReason())
                .correctedAt(d.getCorrectedAt())
                .correctedBy(d.getCorrectedBy())
                .correctedByName(d.getCorrectedByName())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
