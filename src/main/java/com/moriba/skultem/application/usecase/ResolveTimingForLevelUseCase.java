package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.repository.TimingLevelRepository;
import com.moriba.skultem.domain.repository.TimingRepository;
import com.moriba.skultem.domain.vo.Level;

import lombok.RequiredArgsConstructor;

// Single source of truth for "which Timing template applies to this Level" - a Level explicitly
// assigned a template (TimingLevel) uses that one; anything else (including a null level, e.g. a
// class session with no Clazz) falls back to the school's default template. Shared by
// CreatePeriodUseCase (deciding a new period's start/end time) and TimetableService (deciding
// which set of working days a session's timetable grid should use).
@Service
@RequiredArgsConstructor
public class ResolveTimingForLevelUseCase {
    private final TimingRepository timingRepo;
    private final TimingLevelRepository timingLevelRepo;

    public Timing execute(String schoolId, Level level) {
        if (level != null) {
            var assigned = timingLevelRepo.findBySchoolIdAndLevel(schoolId, level);
            if (assigned.isPresent()) {
                return assigned.get().getTiming();
            }
        }

        return timingRepo.findDefaultBySchoolId(schoolId)
                .orElseThrow(() -> new NotFoundException("No timing has been configured for this school yet"));
    }
}
