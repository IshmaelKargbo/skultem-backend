package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassMasterRecord;
import com.moriba.skultem.application.dto.ClassOverviewDTO;
import com.moriba.skultem.application.dto.ClassStreamOverviewDTO;
import com.moriba.skultem.application.error.NotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetClassOverviewUseCase {
    private final GetClassUseCase getClassUseCase;
    private final GetClassSessionUseCase getClassSessionUseCase;
    private final ListClassSectionByClassUseCase listClassSectionByClassUseCase;
    private final ListClassStreamByIdUseCase listClassStreamByIdUseCase;
    private final GetCurrentClassMasterUseCase getCurrentClassMasterUseCase;

    // academicYearId: the year the page is viewing (null = the active one) - masters are per year's session, so
    // asking for the active year while viewing another showed no master at all.
    public ClassOverviewDTO execute(String schoolId, String classId, String academicYearId) {
        var clazz = getClassUseCase.execute(schoolId, classId);
        var sections = listClassSectionByClassUseCase.execute(schoolId, classId);
        var streams = listClassStreamByIdUseCase.execute(schoolId, classId);
        // No session that year just means no master yet - not a reason to fail the whole overview.
        List<ClassMasterRecord> masters;
        try {
            masters = getCurrentClassMasterUseCase.executeRecord(schoolId, classId, academicYearId);
        } catch (NotFoundException e) {
            masters = List.of();
        }

        return new ClassOverviewDTO(
                clazz,
                sections.size(),
                streams.size(),
                masters.size(),
                sections,
                streams,
                masters);
    }

    public ClassStreamOverviewDTO execute(String schoolId, String academicYear, String classId, String streamId,
            String sessionId) {
        var clazz = getClassUseCase.execute(schoolId, classId);
        var sections = listClassSectionByClassUseCase.execute(schoolId, classId);
        var session = sessionId != null && !sessionId.isBlank()
                ? getClassSessionUseCase.executeBySession(sessionId, schoolId)
                : getClassSessionUseCase.executeByClassAndStream(classId, streamId, schoolId, academicYear);
        var masters = getCurrentClassMasterUseCase.executeRecord(schoolId, session.id());

        return new ClassStreamOverviewDTO(
                clazz,
                sections.size(),
                masters.size(),
                sections,
                masters);
    }
}
