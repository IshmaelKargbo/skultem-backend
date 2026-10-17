package com.moriba.skultem.application.services;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.OnboardingDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.infrastructure.persistence.entity.LeadEntity.DemoStatus;
import com.moriba.skultem.infrastructure.persistence.entity.OnboardingEntity;
import com.moriba.skultem.infrastructure.persistence.entity.OnboardingEntity.Status;
import com.moriba.skultem.infrastructure.persistence.entity.OnboardingStepEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.OnboardingJpaRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.OnboardingStepJpaRepository;
import com.moriba.skultem.infrastructure.rest.dto.OnboardingActionDTOs;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * A new school's journey from first demo to launch. Every onboarding starts from the standard steps; steps that
 * do not apply to a particular school are switched off, and custom ones can be added, so no school is forced
 * through irrelevant work.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OnboardingService {

    public static final String CUSTOM = "CUSTOM";

    public record StepTemplate(String key, String label) {
    }

    public static final List<StepTemplate> STANDARD_STEPS = List.of(
            new StepTemplate("LEAD_RECEIVED", "Lead received"),
            new StepTemplate("DEMO_SCHEDULED", "Demo scheduled"),
            new StepTemplate("DEMO_COMPLETED", "Demo completed"),
            new StepTemplate("ONBOARDING_APPROVED", "Onboarding approved"),
            new StepTemplate("SCHOOL_CONFIGURED", "School details configured"),
            new StepTemplate("ADMIN_CREATED", "Administrator account created"),
            new StepTemplate("DATA_IMPORTED", "Data import or initial setup completed"),
            new StepTemplate("TRAINING_COMPLETED", "Training completed"),
            new StepTemplate("SCHOOL_LAUNCHED", "School launched"));

    private final OnboardingJpaRepository onboardings;
    private final OnboardingStepJpaRepository steps;
    private final LeadService leadService;
    private final JdbcTemplate jdbc;

    /** Starts the onboarding for a lead (or returns the one it already has) and moves the lead to ONBOARDING. */
    @AuditLogAnnotation(action = "ONBOARDING_STARTED")
    public OnboardingDTO start(String leadId, String actorId) {
        var lead = leadService.markOnboarding(leadId, actorId);

        var existing = onboardings.findByLeadId(lead.getId());
        if (existing.isPresent()) {
            return reload(existing.get().getId().toString());
        }

        var now = Instant.now();
        var onboarding = onboardings.save(OnboardingEntity.builder().id(UUID.randomUUID()).leadId(lead.getId())
                .assignedTo(lead.getAssignedTo()).status(Status.IN_PROGRESS).startedAt(now).createdAt(now)
                .updatedAt(now).build());

        int position = 1;
        for (var template : STANDARD_STEPS) {
            // What already happened with the lead is recorded as done, so the checklist starts from the truth.
            boolean done = switch (template.key()) {
                case "LEAD_RECEIVED" -> true;
                case "DEMO_SCHEDULED" -> lead.getDemoStatus() == DemoStatus.SCHEDULED
                        || lead.getDemoStatus() == DemoStatus.COMPLETED;
                case "DEMO_COMPLETED" -> lead.getDemoStatus() == DemoStatus.COMPLETED;
                default -> false;
            };
            steps.save(OnboardingStepEntity.builder().id(UUID.randomUUID()).onboardingId(onboarding.getId())
                    .stepKey(template.key()).label(template.label()).position(position++).applicable(true)
                    .status(done ? OnboardingStepEntity.Status.DONE : OnboardingStepEntity.Status.PENDING)
                    .completedAt(done ? now : null).completedBy(null).build());
        }
        return reload(onboarding.getId().toString());
    }

    @AuditLogAnnotation(action = "ONBOARDING_UPDATED")
    public OnboardingDTO update(String id, OnboardingActionDTOs.Update p) {
        var onboarding = find(id);
        if (p.assignedToId() != null && !p.assignedToId().isBlank()
                && leadService.staff().stream().noneMatch(s -> s.id().equals(p.assignedToId()))) {
            throw new RuleException("Onboarding can only be assigned to an active system admin");
        }
        if (p.schoolId() != null && !p.schoolId().isBlank()) {
            Long exists = jdbc.queryForObject("SELECT count(*) FROM schools WHERE id = ? AND status <> 'DELETED'", Long.class, p.schoolId());
            if (exists == null || exists == 0) {
                throw new NotFoundException("School not found");
            }
        }
        onboarding.setAssignedTo(blank(p.assignedToId()));
        onboarding.setSchoolId(blank(p.schoolId()));
        onboarding.setUpdatedAt(Instant.now());
        onboardings.save(onboarding);
        return reload(id);
    }

    @AuditLogAnnotation(action = "ONBOARDING_STEP_UPDATED")
    public OnboardingDTO updateStep(String id, String stepId, OnboardingActionDTOs.StepUpdate p, String actorId) {
        var onboarding = find(id);
        var step = findStep(onboarding, stepId);
        var now = Instant.now();

        if (p.applicable() != null) {
            step.setApplicable(p.applicable());
        }
        if (p.status() != null) {
            var status = OnboardingStepEntity.Status.valueOf(p.status());
            if (!step.isApplicable() && status != OnboardingStepEntity.Status.PENDING) {
                throw new RuleException("This step is switched off for this school - switch it on first");
            }
            step.setStatus(status);
            step.setCompletedAt(status == OnboardingStepEntity.Status.DONE ? now : null);
            step.setCompletedBy(status == OnboardingStepEntity.Status.DONE ? actorId : null);
        }
        if (p.assignedToId() != null && !p.assignedToId().isBlank()) {
            if (leadService.staff().stream().noneMatch(s -> s.id().equals(p.assignedToId()))) {
                throw new RuleException("Steps can only be assigned to an active system admin");
            }
            step.setAssignedTo(p.assignedToId());
        }
        if (Boolean.TRUE.equals(p.clearAssignee())) {
            step.setAssignedTo(null);
        }
        if (p.dueDate() != null) {
            step.setDueDate(p.dueDate());
        }
        if (Boolean.TRUE.equals(p.clearDueDate())) {
            step.setDueDate(null);
        }
        if (p.note() != null) {
            step.setNote(blank(p.note()));
        }
        steps.save(step);

        recompute(onboarding, actorId);
        return reload(id);
    }

    @AuditLogAnnotation(action = "ONBOARDING_STEP_ADDED")
    public OnboardingDTO addStep(String id, String label, String actorId) {
        var onboarding = find(id);
        int next = steps.findByOnboardingIdOrderByPosition(onboarding.getId()).stream()
                .mapToInt(OnboardingStepEntity::getPosition).max().orElse(0) + 1;
        steps.save(OnboardingStepEntity.builder().id(UUID.randomUUID()).onboardingId(onboarding.getId())
                .stepKey(CUSTOM).label(label.trim()).position(next).applicable(true)
                .status(OnboardingStepEntity.Status.PENDING).build());
        recompute(onboarding, actorId);
        return reload(id);
    }

    @AuditLogAnnotation(action = "ONBOARDING_STEP_REMOVED")
    public OnboardingDTO removeStep(String id, String stepId, String actorId) {
        var onboarding = find(id);
        var step = findStep(onboarding, stepId);
        if (!CUSTOM.equals(step.getStepKey())) {
            throw new RuleException("The standard steps can't be removed - switch the step off if it doesn't apply");
        }
        steps.delete(step);
        recompute(onboarding, actorId);
        return reload(id);
    }

    /** True when every step that applies is done (and at least one applies). */
    public static boolean isComplete(List<OnboardingStepEntity> all) {
        var applicable = all.stream().filter(OnboardingStepEntity::isApplicable).toList();
        return !applicable.isEmpty()
                && applicable.stream().allMatch(s -> s.getStatus() == OnboardingStepEntity.Status.DONE);
    }

    // Finishing the last applicable step completes the onboarding; reopening one puts it back in progress.
    private void recompute(OnboardingEntity onboarding, String actorId) {
        var all = steps.findByOnboardingIdOrderByPosition(onboarding.getId());
        boolean complete = isComplete(all);
        var now = Instant.now();

        if (complete && onboarding.getStatus() == Status.IN_PROGRESS) {
            onboarding.setStatus(Status.COMPLETED);
            onboarding.setCompletedAt(now);
            // A finished onboarding for a school that exists closes the lead as converted.
            // (Asked first rather than caught: a failed call would mark this whole transaction for rollback.)
            if (onboarding.getSchoolId() != null
                    && leadService.canConvert(onboarding.getLeadId().toString(), onboarding.getSchoolId())) {
                leadService.convert(onboarding.getLeadId().toString(), onboarding.getSchoolId(), actorId);
            }
        } else if (!complete && onboarding.getStatus() == Status.COMPLETED) {
            onboarding.setStatus(Status.IN_PROGRESS);
            onboarding.setCompletedAt(null);
        }
        onboarding.setUpdatedAt(now);
        onboardings.save(onboarding);
    }

    // The reads are SQL, so what was just saved has to reach the database first.
    private OnboardingDTO reload(String id) {
        leadService.flush();
        onboardings.flush();
        steps.flush();
        return get(id);
    }

    // ------------------------------------------------------------------ reads

    private static final String SELECT = """
            SELECT o.*, l.name AS lead_name, l.school_name AS lead_school, s.name AS linked_school,
                   trim(concat_ws(' ', u.given_name, u.family_name)) AS assignee,
                   (SELECT count(*) FROM onboarding_steps x WHERE x.onboarding_id = o.id AND x.applicable AND x.status = 'DONE') AS done_steps,
                   (SELECT count(*) FROM onboarding_steps x WHERE x.onboarding_id = o.id AND x.applicable) AS total_steps,
                   (SELECT x.label FROM onboarding_steps x WHERE x.onboarding_id = o.id AND x.applicable AND x.status <> 'DONE' ORDER BY x.position LIMIT 1) AS current_step,
                   (SELECT min(x.due_date) FROM onboarding_steps x WHERE x.onboarding_id = o.id AND x.applicable AND x.status <> 'DONE') AS next_due,
                   (SELECT count(*) FROM onboarding_steps x WHERE x.onboarding_id = o.id AND x.applicable AND x.status <> 'DONE' AND x.due_date < current_date) AS overdue_steps
            FROM onboardings o
            JOIN leads l ON l.id = o.lead_id
            LEFT JOIN schools s ON s.id = o.school_id
            LEFT JOIN users u ON u.id = o.assigned_to
            """;

    public Page<OnboardingDTO> list(String status, String search, int page, int size) {
        var where = new StringBuilder(" WHERE 1=1");
        var args = new ArrayList<Object>();
        if (status != null && !status.isBlank()) {
            where.append(" AND o.status = ?");
            args.add(Status.valueOf(status).name());
        }
        if (search != null && !search.isBlank()) {
            where.append(" AND (lower(l.name) LIKE ? OR lower(coalesce(l.school_name,'')) LIKE ?)");
            String like = "%" + search.trim().toLowerCase() + "%";
            args.add(like);
            args.add(like);
        }

        Long total = jdbc.queryForObject("SELECT count(*) FROM onboardings o JOIN leads l ON l.id = o.lead_id" + where,
                Long.class, args.toArray());

        var pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) Math.max(page - 1, 0) * size);
        var rows = jdbc.query(SELECT + where + " ORDER BY (o.status <> 'IN_PROGRESS'), o.started_at DESC LIMIT ? OFFSET ?",
                (rs, i) -> map(rs, List.of()), pageArgs.toArray());
        return new PageImpl<>(rows, PageRequest.of(Math.max(page - 1, 0), size), total == null ? 0 : total);
    }

    public OnboardingDTO get(String id) {
        var uuid = LeadService.parse(id);
        var step = jdbc.query("""
                SELECT x.*, trim(concat_ws(' ', a.given_name, a.family_name)) AS assignee,
                       trim(concat_ws(' ', c.given_name, c.family_name)) AS completer
                FROM onboarding_steps x
                LEFT JOIN users a ON a.id = x.assigned_to
                LEFT JOIN users c ON c.id = x.completed_by
                WHERE x.onboarding_id = ? ORDER BY x.position
                """, (rs, i) -> step(rs), uuid);
        var rows = jdbc.query(SELECT + " WHERE o.id = ?", (rs, i) -> map(rs, step), uuid);
        if (rows.isEmpty()) {
            throw new NotFoundException("Onboarding not found");
        }
        return rows.get(0);
    }

    /** What the dashboard counts. */
    public Map<String, Long> overview() {
        var out = new LinkedHashMap<String, Long>();
        out.put("inProgress", count("SELECT count(*) FROM onboardings WHERE status = 'IN_PROGRESS'"));
        out.put("completed", count("SELECT count(*) FROM onboardings WHERE status = 'COMPLETED'"));
        out.put("overdueSteps", count("""
                SELECT count(*) FROM onboarding_steps x JOIN onboardings o ON o.id = x.onboarding_id
                WHERE o.status = 'IN_PROGRESS' AND x.applicable AND x.status <> 'DONE' AND x.due_date < current_date"""));
        return out;
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }

    private OnboardingDTO map(ResultSet rs, List<OnboardingDTO.Step> stepList) throws SQLException {
        int done = rs.getInt("done_steps");
        int total = rs.getInt("total_steps");
        var due = rs.getDate("next_due");
        return new OnboardingDTO(rs.getString("id"), rs.getString("lead_id"), rs.getString("lead_name"),
                rs.getString("lead_school"), rs.getString("school_id"), rs.getString("linked_school"),
                rs.getString("assigned_to"), blank(rs.getString("assignee")), rs.getString("status"),
                instant(rs, "started_at"), instant(rs, "completed_at"), done, total,
                total == 0 ? 0 : Math.round(done * 100f / total), rs.getString("current_step"),
                due == null ? null : due.toLocalDate(), rs.getInt("overdue_steps"), stepList);
    }

    private OnboardingDTO.Step step(ResultSet rs) throws SQLException {
        var due = rs.getDate("due_date");
        LocalDate dueDate = due == null ? null : due.toLocalDate();
        boolean done = "DONE".equals(rs.getString("status"));
        return new OnboardingDTO.Step(rs.getString("id"), rs.getString("step_key"), rs.getString("label"),
                rs.getInt("position"), rs.getBoolean("applicable"), rs.getString("status"), rs.getString("assigned_to"),
                blank(rs.getString("assignee")), dueDate, rs.getString("note"), instant(rs, "completed_at"),
                blank(rs.getString("completer")),
                !done && rs.getBoolean("applicable") && dueDate != null && dueDate.isBefore(LocalDate.now()));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var t = rs.getTimestamp(column);
        return t == null ? null : Instant.ofEpochMilli(t.getTime());
    }

    private OnboardingEntity find(String id) {
        return onboardings.findById(LeadService.parse(id)).orElseThrow(() -> new NotFoundException("Onboarding not found"));
    }

    private OnboardingStepEntity findStep(OnboardingEntity onboarding, String stepId) {
        var step = steps.findById(LeadService.parse(stepId)).orElseThrow(() -> new NotFoundException("Step not found"));
        if (!step.getOnboardingId().equals(onboarding.getId())) {
            throw new NotFoundException("Step not found");
        }
        return step;
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
