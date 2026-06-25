package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Period;
import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.PeriodRepository;
import com.moriba.skultem.domain.repository.TimingRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatePeriodUseCase {

    private final TimingRepository timingRepo;
    private final ClassSessionRepository sessionRepo;
    private final PeriodRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "PERIOD_CREATED")
    public Period execute(String schoolId, String sessionId) {
        return create(schoolId, sessionId, Period.Type.PERIOD);
    }

    @AuditLogAnnotation(action = "BREAK_CREATED")
    public Period executeBreak(String schoolId, String sessionId) {
        return create(schoolId, sessionId, Period.Type.BREAK);
    }

    @AuditLogAnnotation(action = "LUNCH_CREATED")
    public Period executeLunch(String schoolId, String sessionId) {
        return create(schoolId, sessionId, Period.Type.LUNCH);
    }

    private Period create(String schoolId, String sessionId, Period.Type type) {

        var timing = timingRepo.findBySchoolId(schoolId)
                .orElseThrow(() -> new NotFoundException("No timing found"));

        var session = sessionRepo.findByIdAndSchoolId(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("No class found"));

        String id = UUID.randomUUID().toString();

        var duration = getDuration(session.getId(), timing, type);

        var domain = Period.create(
                id,
                schoolId,
                session,
                type,
                duration.name(),
                duration.startTime(),
                duration.endTime()
        );

        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                type.name() + " created",
                domain.getName(),
                null,
                domain.getId()
        );

        return domain;
    }

    private PeriodDuration getDuration(
            String sessionId,
            Timing timing,
            Period.Type type
    ) {

        var periods = repo.findAllBySessionId(sessionId);

        LocalTime startTime;

        if (periods.isEmpty()) {
            startTime = timing.getStartTime();
        } else {
            var latest = periods.stream()
                    .max(Comparator.comparing(Period::getEndTime))
                    .orElseThrow();

            startTime = latest.getEndTime();
        }

        int durationMinutes;
        String name;

        switch (type) {
            case PERIOD -> {
                durationMinutes = timing.getPeriodDuration();
                long count = periods.stream()
                        .filter(p -> p.getType() == Period.Type.PERIOD)
                        .count();
                name = "Period " + (count + 1);
            }

            case BREAK -> {
                durationMinutes = timing.getBreakDuration();
                long count = periods.stream()
                        .filter(p -> p.getType() == Period.Type.BREAK)
                        .count();
                name = "Break " + (count + 1);
            }

            case LUNCH -> {
                durationMinutes = timing.getLunchDuration();
                long count = periods.stream()
                        .filter(p -> p.getType() == Period.Type.LUNCH)
                        .count();
                name = "Lunch " + (count + 1);
            }

            default -> throw new IllegalArgumentException("Unsupported period type");
        }

        LocalTime endTime = startTime.plusMinutes(durationMinutes);

        return new PeriodDuration(name, startTime, endTime);
    }

    private record PeriodDuration(
            String name,
            LocalTime startTime,
            LocalTime endTime
    ) {}
}
