package com.moriba.skultem.application.usecase;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;

/**
 * Reconciles a school's platform fee: makes sure a platform fee setting row exists (see
 * {@link EnsurePlatformFeeSettingUseCase}), then re-runs {@link SeedPlatformFeeForAcademicYearUseCase}
 * for it (a no-op if the school has no active term). That use case is idempotent and
 * self-contained (finds-or-creates the fee structure, assigns only students still missing it), so
 * calling it here just catches up whatever a school's own term-activation path missed - a setting
 * that was only just configured, or enrollments that slipped through without the fee.
 * <p>
 * Two ways this runs:
 * <ul>
 * <li>{@link #execute()} - a platform-wide sweep over every school, used by the startup listener
 * (see infrastructure/listener) so this self-heals on every deploy even with no traffic.</li>
 * <li>{@link #executeForSchoolAsync(String)} - fired off whenever a school's own info is fetched
 * (see SchoolService.get()), so a school also gets reconciled the moment anyone from it is active,
 * not just at the last restart. Runs off the request thread (fire-and-forget) so it never slows
 * down that request, and is throttled per school (see {@link #COOLDOWN}) so a page that fetches
 * school info on every load doesn't re-run the full per-student sweep on every single request.</li>
 * </ul>
 * Deliberately not one big transaction across schools/calls - each school's work runs in its own
 * transaction (via the two use cases above), and a failure on one school is logged and skipped
 * rather than aborting the rest.
 */
@Service
@RequiredArgsConstructor
public class BackfillPlatformFeesUseCase {

    private static final Logger log = LoggerFactory.getLogger(BackfillPlatformFeesUseCase.class);

    // How long a school is left alone after a request-triggered reconcile before the next request
    // is allowed to trigger another one. Only guards the async, per-request path - the startup
    // sweep always runs for every school regardless of this.
    private static final Duration COOLDOWN = Duration.ofMinutes(30);

    private final Map<String, Instant> lastCheckedAt = new ConcurrentHashMap<>();

    private final SchoolRepository schoolRepo;
    private final EnsurePlatformFeeSettingUseCase ensurePlatformFeeSettingUseCase;
    private final SeedPlatformFeeForAcademicYearUseCase seedPlatformFeeForAcademicYearUseCase;

    public void execute() {
        var schools = schoolRepo.findAll(Pageable.unpaged()).getContent();

        for (var school : schools) {
            executeForSchool(school.getId());
        }
    }

    // Fired from SchoolService.get() - deliberately async and swallowing its own errors, since
    // nothing is waiting on this and a school info request must never fail or slow down because
    // of it. Named executor (see AsyncConfig) so this is bounded and can't compete with normal
    // request traffic for connections out of the shared DB pool.
    @Async("platformFeeExecutor")
    public void executeForSchoolAsync(String schoolId) {
        var lastRun = lastCheckedAt.get(schoolId);
        if (lastRun != null && lastRun.isAfter(Instant.now().minus(COOLDOWN))) {
            return;
        }

        lastCheckedAt.put(schoolId, Instant.now());
        executeForSchool(schoolId);
    }

    public void executeForSchool(String schoolId) {
        try {
            ensurePlatformFeeSettingUseCase.execute(schoolId);
            // Resolves the school's own active term/academic year internally, all within its own
            // transaction - see the javadoc on that overload for why this can't be done here
            // instead (a lazy-loaded entity from a since-closed transaction can't be dereferenced
            // in this, non-transactional, orchestrator).
            seedPlatformFeeForAcademicYearUseCase.execute(schoolId);
        } catch (Exception e) {
            log.warn("Could not reconcile platform fee for school {}: {}", schoolId, e.getMessage());
        }
    }
}
