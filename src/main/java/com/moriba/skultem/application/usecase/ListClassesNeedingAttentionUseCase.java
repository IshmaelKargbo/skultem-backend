package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassAttentionSummaryDTO;
import com.moriba.skultem.application.dto.FlaggedClassDTO;
import com.moriba.skultem.domain.repository.ClassSessionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// School-wide rollup of ComputeClassAttentionUseCase, for the admin dashboard's "Needs Attention"
// widget. Two (or more) class sessions can share the same underlying Clazz (different
// streams/sections of the same class), and ComputeClassAttentionUseCase already aggregates across
// every stream for a Clazz, so each Clazz is only computed once here even if it has multiple
// sessions - repeating it per session would double-count the same flagged students.
@Service
@Transactional
@RequiredArgsConstructor
public class ListClassesNeedingAttentionUseCase {

    private static final int TOP_N = 5;

    private final ClassSessionRepository classSessionRepo;
    private final ComputeClassAttentionUseCase computeClassAttentionUseCase;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public ClassAttentionSummaryDTO execute(String schoolId, String academicYearId) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        var sessions = classSessionRepo
                .findBySchoolIdAndAcademicYearId(schoolId, academicYear.getId(), Pageable.unpaged())
                .getContent();

        // First session seen for each Clazz wins the display fields (name/section/stream) - which
        // one doesn't matter for a single-stream class, and for a multi-stream one it's simply
        // showing one representative stream's label alongside the class-wide flagged count.
        Map<String, FlaggedClassDTO> byClass = new LinkedHashMap<>();
        int flaggedStudents = 0;

        for (var session : sessions) {
            var clazz = session.getClazz();

            if (byClass.containsKey(clazz.getId())) {
                continue;
            }

            var result = computeClassAttentionUseCase.execute(schoolId, clazz.getId(), academicYear.getId());
            if (result.flaggedCount() == 0) {
                continue;
            }

            flaggedStudents += result.flaggedCount();

            var section = session.getSection();
            var stream = session.getStream();

            byClass.put(clazz.getId(), new FlaggedClassDTO(
                    clazz.getId(),
                    clazz.getName(),
                    section != null ? section.getName() : "",
                    stream != null ? stream.getName() : "N/A",
                    result.flaggedCount(),
                    result.totalStudents()));
        }

        var flagged = new ArrayList<>(byClass.values());
        flagged.sort(Comparator.comparingInt(FlaggedClassDTO::flaggedCount).reversed());

        int totalClasses = (int) sessions.stream().map(s -> s.getClazz().getId()).distinct().count();

        List<FlaggedClassDTO> top = flagged.size() > TOP_N ? flagged.subList(0, TOP_N) : flagged;

        return new ClassAttentionSummaryDTO(totalClasses, flagged.size(), flaggedStudents, top);
    }
}
