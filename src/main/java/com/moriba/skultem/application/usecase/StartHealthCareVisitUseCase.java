package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HealthCareReportDTOs.StartResult;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.HealthCareDtoMapper;
import com.moriba.skultem.application.services.HealthCareSupport;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.HealthCareSetting;
import com.moriba.skultem.domain.model.HealthCareTurnedAway;
import com.moriba.skultem.domain.model.HealthCareVisit;
import com.moriba.skultem.domain.model.HealthCareVisit.Complaint;
import com.moriba.skultem.domain.model.HealthCareVisit.OverLimit;
import com.moriba.skultem.domain.repository.HealthCareTurnedAwayRepository;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;
import com.moriba.skultem.domain.service.HealthCareLimits;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Starts a sick bay visit (docs/HEALTH_CARE.md §5.2, §6.2).
 * <p>
 * In order (L-6): the student must be enrolled and have no open visit (V-1); then the school's limit rules are
 * checked - unless this visit continues one from earlier today (V-9). A Refuse rule that isn't overridden by an
 * urgent complaint records a turned-away entry instead of a visit (L-5); "Ask for a reason" needs the reason; a
 * visit let past any rule keeps a copy of it (L-7). The audit row carries no medical details (H-2).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StartHealthCareVisitUseCase {

    private final HealthCareVisitRepository visitRepo;
    private final HealthCareTurnedAwayRepository turnedAwayRepo;
    private final HealthCareSupport support;
    private final LogActivityUseCase logActivityUseCase;

    public record Command(String studentId, Instant arrivedAt, List<String> complaints, String complaintOther,
            String broughtBy, BigDecimal temperature, String notes, String continuesVisitId, String overLimitReason) {
    }

    @AuditLogAnnotation(action = "HEALTH_CARE_VISIT_STARTED", includeDetails = false)
    public StartResult execute(String schoolId, Command c, String userId) {
        if (c.studentId() == null || c.studentId().isBlank()) {
            throw new BadRequestException("Choose a student");
        }
        var setting = support.settings(schoolId);
        var student = support.currentStudent(schoolId, c.studentId());
        var complaints = resolveComplaints(setting, c.complaints());
        requireSettingFields(setting, c.broughtBy(), c.temperature());

        visitRepo.findOpenByStudent(schoolId, c.studentId()).ifPresent(open -> {
            throw new RuleException(student.student().getName() + (open.needsFollowUp(support.now())
                    ? " has a visit from an earlier school day that was never closed. Follow it up first."
                    : " is already in the sick bay. Close that visit first."));
        });

        Instant now = support.now();
        var today = support.today();
        String userName = support.userName(userId);
        boolean urgent = complaints.stream().anyMatch(Complaint::urgent);

        String continues = null;
        OverLimit overLimit = null;
        String warning = null;
        List<Instant> counted = List.of();
        var term = support.termOn(schoolId, today);

        if (c.continuesVisitId() != null && !c.continuesVisitId().isBlank()) {
            var earlier = visitRepo.findByIdAndSchool(c.continuesVisitId(), schoolId)
                    .orElseThrow(() -> new NotFoundException("The earlier visit was not found"));
            if (!earlier.getStudentId().equals(c.studentId()) || !earlier.isClosed()
                    || !earlier.arrivalDate().equals(today)) {
                throw new RuleException("A visit can only continue one of the same student's closed visits from today");
            }
            continues = earlier.getId();
        } else {
            counted = support.countedArrivals(schoolId, c.studentId(), support.countingSince(today, term));
            var check = support.checkLimits(setting, student, urgent, counted, today, term);
            if (check.refused()) {
                var rule = check.rule();
                turnedAwayRepo.save(HealthCareTurnedAway.record(schoolId, c.studentId(), student.student().getName(),
                        student.className(), labels(complaints, c.complaintOther()), rule.name(), rule.maxVisits(),
                        rule.period().name(), (int) check.count(), userId, userName, now));
                logActivityUseCase.log(schoolId, ActivityType.HEALTH_CARE, "Health care visit turned away",
                        student.student().getName(), null, null);
                return new StartResult(null, true, check.message());
            }
            if (check.needsReason() && (c.overLimitReason() == null || c.overLimitReason().isBlank())) {
                throw new RuleException(check.message());
            }
            if (check.reached()) {
                var rule = check.rule();
                boolean reasonGiven = check.result() == HealthCareLimits.Result.REASON && !urgent;
                overLimit = new OverLimit(rule.name(), rule.maxVisits(), rule.period().name(), rule.action().name(),
                        check.count(), urgent, reasonGiven ? c.overLimitReason().trim() : null, userId, userName);
                warning = check.message();
            }
        }

        Instant dayEnd = support.schoolDayEnd(schoolId, student.enrollment(), now);
        final String continuesId = continues;
        final OverLimit over = overLimit;
        var visit = LibraryAttendanceSupport.rule(() -> HealthCareVisit.start(schoolId, c.studentId(),
                student.student().getName(), student.className(), c.arrivedAt(), dayEnd, complaints,
                c.complaintOther(), c.broughtBy(), c.temperature(), c.notes(), continuesId, over, userId, userName,
                now));
        LibraryAttendanceSupport.saveUnique(() -> visitRepo.save(visit),
                student.student().getName() + " is already in the sick bay");

        logActivityUseCase.log(schoolId, ActivityType.HEALTH_CARE, "Health care visit", visit.getStudentName(), null,
                visit.getId());

        if (continues == null) {
            alertPrincipalsOnCount(setting, visit, counted, today, term);
        }

        return new StartResult(HealthCareDtoMapper.toDTO(visit, setting, now), false, warning);
    }

    /**
     * Tells principal-level users when this visit brings the student to exactly the alert count in the alert
     * period (§6.5) - once, as the count is reached, not on every visit after.
     */
    private void alertPrincipalsOnCount(HealthCareSetting setting, HealthCareVisit visit, List<Instant> counted,
            java.time.LocalDate today, HealthCareLimits.TermDates term) {
        if (setting.getPrincipalAlertVisits() == null || setting.getPrincipalAlertPeriod() == null) {
            return;
        }
        var window = HealthCareLimits.window(setting.getPrincipalAlertPeriod(), today, term);
        if (window == null) {
            return;
        }
        long total = HealthCareLimits.countBetween(counted, window[0], window[1]) + 1;
        if (total != setting.getPrincipalAlertVisits()) {
            return;
        }
        String phrase = HealthCareLimits.periodPhrase(setting.getPrincipalAlertPeriod());
        support.notifyPrincipals(visit.getSchoolId(),
                visit.getStudentName() + " has visited the sick bay " + total + " times " + phrase,
                visit.getStudentName() + " (" + (visit.getStudentClass() == null ? "" : visit.getStudentClass())
                        + ") has now visited the sick bay " + total + " times " + phrase + ".",
                Map.of("student_id", visit.getStudentId(), "visit_id", visit.getId(), "kind", "visit_count"));
    }

    /** Complaint codes -> the school's complaints as they are now. Hidden ones can't be chosen for a new visit. */
    static List<Complaint> resolveComplaints(HealthCareSetting setting, List<String> codes) {
        var distinct = new LinkedHashSet<>(codes == null ? List.<String>of() : codes);
        distinct.removeIf(code -> code == null || code.isBlank());
        if (distinct.isEmpty()) {
            throw new BadRequestException("Choose at least one complaint");
        }
        List<Complaint> list = new ArrayList<>();
        for (String code : distinct) {
            var complaint = setting.complaint(code)
                    .orElseThrow(() -> new BadRequestException("Unknown complaint: " + code));
            if (complaint.hidden()) {
                throw new RuleException("\"" + complaint.label() + "\" is no longer on the school's complaint list");
            }
            list.add(new Complaint(complaint.code(), complaint.label(), complaint.urgent()));
        }
        return list;
    }

    static void requireSettingFields(HealthCareSetting setting, String broughtBy, BigDecimal temperature) {
        if (setting.isRequireBroughtBy() && (broughtBy == null || broughtBy.isBlank())) {
            throw new RuleException("Record who brought the student");
        }
        if (setting.isRequireTemperature() && temperature == null) {
            throw new RuleException("Record the student's temperature");
        }
    }

    private static String labels(List<Complaint> complaints, String other) {
        String text = String.join(", ", complaints.stream().map(Complaint::label).toList());
        if (other != null && !other.isBlank()) {
            text = text + " (" + other.trim() + ")";
        }
        return text.length() > 1000 ? text.substring(0, 1000) : text;
    }
}
