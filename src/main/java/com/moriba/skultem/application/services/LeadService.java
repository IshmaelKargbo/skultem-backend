package com.moriba.skultem.application.services;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LeadDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.infrastructure.persistence.entity.LeadEntity;
import com.moriba.skultem.infrastructure.persistence.entity.LeadEntity.DemoStatus;
import com.moriba.skultem.infrastructure.persistence.entity.LeadEntity.Source;
import com.moriba.skultem.infrastructure.persistence.entity.LeadEntity.Status;
import com.moriba.skultem.infrastructure.persistence.entity.LeadNoteEntity;
import com.moriba.skultem.infrastructure.persistence.entity.LeadNoteEntity.Kind;
import com.moriba.skultem.infrastructure.persistence.jpa.LeadJpaRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.LeadNoteJpaRepository;
import com.moriba.skultem.infrastructure.rest.dto.SaveLeadDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The CRM side of marketing: every enquiry becomes one lead that staff work until it is converted into a school
 * or lost. Writes go through the entities; the lists and the dashboard counts are read with SQL because they
 * join in people and schools.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class LeadService {

    static final List<Status> CLOSED = List.of(Status.CONVERTED, Status.LOST);

    // Where a lead may be moved by hand. ONBOARDING and CONVERTED are reached through their own actions
    // (starting onboarding, converting to a school), so they are not offered here.
    private static final Map<Status, List<Status>> MOVES = new EnumMap<>(Map.of(
            Status.NEW, List.of(Status.CONTACTED, Status.DEMO_SCHEDULED, Status.LOST),
            Status.CONTACTED, List.of(Status.DEMO_SCHEDULED, Status.INTERESTED, Status.LOST),
            Status.DEMO_SCHEDULED, List.of(Status.CONTACTED, Status.DEMO_COMPLETED, Status.LOST),
            Status.DEMO_COMPLETED, List.of(Status.INTERESTED, Status.LOST),
            Status.INTERESTED, List.of(Status.CONTACTED, Status.DEMO_SCHEDULED, Status.LOST),
            Status.ONBOARDING, List.of(Status.LOST),
            Status.CONVERTED, List.of(),
            Status.LOST, List.of(Status.CONTACTED)));

    // A lead in one of these is waiting on us when its follow-up is due (or it has never been touched).
    private static final String AWAITING = "(l.status IN ('NEW','CONTACTED','DEMO_COMPLETED','INTERESTED') AND "
            + "(l.next_follow_up_at <= clock_timestamp() OR (l.next_follow_up_at IS NULL AND l.status = 'NEW')))";

    private final LeadJpaRepository leads;
    private final LeadNoteJpaRepository notes;
    private final JdbcTemplate jdbc;

    private static List<String> nextStatuses(Status from) {
        return MOVES.getOrDefault(from, List.of()).stream().map(Status::name).toList();
    }

    public static boolean canMove(Status from, Status to) {
        return MOVES.getOrDefault(from, List.of()).contains(to);
    }

    // ------------------------------------------------------------------ capturing enquiries

    /** A demo request just came in: make it a lead, unless that address already has one being worked. */
    public void captureDemoRequest(UUID demoRequestId, String name, String school, String email, String phone,
            String city, String priority, String message) {
        if (leads.findByDemoRequestId(demoRequestId).isPresent()) {
            return;
        }
        String summary = joinNonBlank(" · ", priority, message);
        var open = openLeadFor(email);
        if (open != null) {
            if (open.getDemoRequestId() == null) {
                open.setDemoRequestId(demoRequestId);
                open.setUpdatedAt(Instant.now());
                leads.save(open);
            }
            system(open.getId(), "Another demo request arrived from this address" + (summary.isBlank() ? "." : ": " + summary));
            return;
        }
        leads.save(newLead(name, school, email, phone, city, Source.DEMO_REQUEST, summary, demoRequestId, null));
    }

    /** A contact form message just came in: same idea. */
    public void captureContactMessage(UUID messageId, String name, String school, String email, String phone,
            String challenge) {
        if (leads.findByContactMessageId(messageId).isPresent()) {
            return;
        }
        var open = openLeadFor(email);
        if (open != null) {
            if (open.getContactMessageId() == null) {
                open.setContactMessageId(messageId);
                open.setUpdatedAt(Instant.now());
                leads.save(open);
            }
            system(open.getId(), "Another message arrived from this address: " + challenge);
            return;
        }
        leads.save(newLead(name, school, email, phone, null, Source.CONTACT_MESSAGE, challenge, null, messageId));
    }

    private LeadEntity openLeadFor(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return leads.findByEmailIgnoreCaseAndStatusNotInOrderByCreatedAtDesc(email.trim(), CLOSED).stream()
                .findFirst().orElse(null);
    }

    private LeadEntity newLead(String name, String school, String email, String phone, String city, Source source,
            String summary, UUID demoRequestId, UUID contactMessageId) {
        var now = Instant.now();
        return LeadEntity.builder().id(UUID.randomUUID()).name(name.trim()).schoolName(blankToNull(school))
                .email(email == null ? null : blankToNull(email.toLowerCase())).phone(blankToNull(phone))
                .city(blankToNull(city)).source(source).demoRequestId(demoRequestId)
                .contactMessageId(contactMessageId).status(Status.NEW).demoStatus(DemoStatus.NOT_SCHEDULED)
                // A new enquiry should be answered straight away.
                .nextFollowUpAt(now).summary(blankToNull(summary)).createdAt(now).updatedAt(now).build();
    }

    // ------------------------------------------------------------------ staff actions

    @AuditLogAnnotation(action = "LEAD_CREATED")
    public LeadDTO create(SaveLeadDTO p, String actorId) {
        var open = openLeadFor(p.email());
        if (open != null) {
            throw new AlreadyExistsException("A lead for " + p.email() + " is already open: " + open.getName());
        }
        var source = p.source() == null || p.source().isBlank() ? Source.MANUAL : Source.valueOf(p.source());
        var lead = newLead(p.name(), p.schoolName(), p.email(), p.phone(), p.city(), source, p.summary(), null, null);
        assertStaff(p.assignedToId());
        lead.setAssignedTo(blankToNull(p.assignedToId()));
        if (p.nextFollowUpAt() != null) {
            lead.setNextFollowUpAt(p.nextFollowUpAt());
        }
        leads.save(lead);
        note(lead.getId(), Kind.SYSTEM, "Lead added by hand.", actorId);
        return reload(lead.getId().toString());
    }

    @AuditLogAnnotation(action = "LEAD_UPDATED")
    public LeadDTO update(String id, SaveLeadDTO p, String actorId) {
        var lead = find(id);
        assertStaff(p.assignedToId());

        String newAssignee = blankToNull(p.assignedToId());
        boolean reassigned = !java.util.Objects.equals(lead.getAssignedTo(), newAssignee);

        lead.setName(p.name().trim());
        lead.setSchoolName(blankToNull(p.schoolName()));
        lead.setEmail(p.email() == null ? null : blankToNull(p.email().toLowerCase()));
        lead.setPhone(blankToNull(p.phone()));
        lead.setCity(blankToNull(p.city()));
        lead.setSummary(blankToNull(p.summary()));
        lead.setAssignedTo(newAssignee);
        lead.setNextFollowUpAt(p.nextFollowUpAt());
        lead.setUpdatedAt(Instant.now());
        leads.save(lead);

        if (reassigned) {
            note(lead.getId(), Kind.SYSTEM, newAssignee == null ? "Unassigned."
                    : "Assigned to " + nameOf(newAssignee) + ".", actorId);
        }
        return reload(id);
    }

    @AuditLogAnnotation(action = "LEAD_STATUS_CHANGED")
    public LeadDTO changeStatus(String id, String status, String reason, String actorId) {
        var lead = find(id);
        var to = Status.valueOf(status);

        if (!canMove(lead.getStatus(), to)) {
            throw new RuleException("A lead that is " + label(lead.getStatus()) + " can't be moved to " + label(to));
        }
        if (to == Status.LOST && (reason == null || reason.isBlank())) {
            throw new RuleException("Say why this lead was lost");
        }

        var from = lead.getStatus();
        lead.setStatus(to);
        lead.setLostReason(to == Status.LOST ? reason.trim() : null);
        lead.setUpdatedAt(Instant.now());
        leads.save(lead);

        note(lead.getId(), Kind.STATUS_CHANGE, label(from) + " → " + label(to)
                + (to == Status.LOST ? ": " + reason.trim() : ""), actorId);
        return reload(id);
    }

    @AuditLogAnnotation(action = "LEAD_DEMO_UPDATED")
    public LeadDTO demo(String id, String status, Instant at, String actorId) {
        var lead = find(id);
        if (CLOSED.contains(lead.getStatus())) {
            throw new RuleException("This lead is " + label(lead.getStatus()) + " - reopen it first");
        }

        var demo = DemoStatus.valueOf(status);
        if (demo == DemoStatus.SCHEDULED && at == null) {
            throw new RuleException("Pick the date and time of the demo");
        }
        var from = lead.getStatus();

        lead.setDemoStatus(demo);
        if (demo == DemoStatus.SCHEDULED) {
            lead.setDemoAt(at);
            if (canMove(from, Status.DEMO_SCHEDULED)) {
                lead.setStatus(Status.DEMO_SCHEDULED);
            }
        } else if (demo == DemoStatus.COMPLETED) {
            if (canMove(from, Status.DEMO_COMPLETED)) {
                lead.setStatus(Status.DEMO_COMPLETED);
            } else if (from == Status.NEW || from == Status.CONTACTED || from == Status.INTERESTED) {
                lead.setStatus(Status.DEMO_COMPLETED);
            }
        } else if (from == Status.DEMO_SCHEDULED) { // CANCELLED
            lead.setStatus(Status.CONTACTED);
        }
        lead.setUpdatedAt(Instant.now());
        leads.save(lead);

        system(lead.getId(), switch (demo) {
            case SCHEDULED -> "Demo scheduled for " + at + ".";
            case COMPLETED -> "Demo completed.";
            case CANCELLED -> "Demo cancelled.";
            default -> "Demo updated.";
        });
        return reload(id);
    }

    public LeadDTO addNote(String id, String kind, String body, String actorId) {
        var lead = find(id);
        note(lead.getId(), Kind.valueOf(kind), body.trim(), actorId);
        lead.setUpdatedAt(Instant.now());
        leads.save(lead);
        return reload(id);
    }

    /** The lead became a school on the platform: link the two and close the lead as converted. */
    @AuditLogAnnotation(action = "LEAD_CONVERTED")
    public LeadDTO convert(String id, String schoolId, String actorId) {
        var lead = find(id);
        if (CLOSED.contains(lead.getStatus())) {
            throw new RuleException("This lead is already " + label(lead.getStatus()));
        }
        Long exists = jdbc.queryForObject("SELECT count(*) FROM schools WHERE id = ? AND status <> 'DELETED'", Long.class, schoolId);
        if (exists == null || exists == 0) {
            throw new NotFoundException("School not found");
        }
        Long taken = jdbc.queryForObject("SELECT count(*) FROM leads WHERE school_id = ? AND id <> ?", Long.class, schoolId, lead.getId());
        if (taken != null && taken > 0) {
            throw new AlreadyExistsException("Another lead was already converted into that school");
        }

        var now = Instant.now();
        lead.setStatus(Status.CONVERTED);
        lead.setSchoolId(schoolId);
        lead.setConvertedAt(now);
        lead.setNextFollowUpAt(null);
        lead.setUpdatedAt(now);
        leads.save(lead);
        // The onboarding (if there is one) now knows which school it is for.
        jdbc.update("UPDATE onboardings SET school_id = ?, updated_at = now() WHERE lead_id = ?", schoolId, lead.getId());

        note(lead.getId(), Kind.STATUS_CHANGE, "Converted into a school.", actorId);
        return reload(id);
    }

    /** Whether convert() would succeed, so a caller that only wants to try can ask first instead of catching. */
    public boolean canConvertPublic(String id, String schoolId) {
        return canConvert(id, schoolId);
    }

    boolean canConvert(String id, String schoolId) {
        var lead = leads.findById(parse(id)).orElse(null);
        if (lead == null || CLOSED.contains(lead.getStatus())) {
            return false;
        }
        Long exists = jdbc.queryForObject("SELECT count(*) FROM schools WHERE id = ? AND status <> 'DELETED'", Long.class, schoolId);
        Long taken = jdbc.queryForObject("SELECT count(*) FROM leads WHERE school_id = ? AND id <> ?", Long.class, schoolId, lead.getId());
        return exists != null && exists > 0 && (taken == null || taken == 0);
    }

    /** Used by onboarding: moves the lead to ONBOARDING without going through the manual moves. */
    LeadEntity markOnboarding(String id, String actorId) {
        var lead = find(id);
        if (CLOSED.contains(lead.getStatus())) {
            throw new RuleException("This lead is " + label(lead.getStatus()) + " - reopen it before onboarding");
        }
        if (lead.getStatus() != Status.ONBOARDING) {
            var from = lead.getStatus();
            lead.setStatus(Status.ONBOARDING);
            lead.setUpdatedAt(Instant.now());
            leads.save(lead);
            note(lead.getId(), Kind.STATUS_CHANGE, label(from) + " → " + label(Status.ONBOARDING), actorId);
        }
        return lead;
    }

    // ------------------------------------------------------------------ reads

    public record Filter(String status, String assignedTo, String search, String view) {
    }

    private static final String SELECT = """
            SELECT l.*, u.given_name AS a_given, u.family_name AS a_family, s.name AS converted_school_name,
                   o.id AS onboarding_id, %s AS awaiting
            FROM leads l
            LEFT JOIN users u ON u.id = l.assigned_to
            LEFT JOIN schools s ON s.id = l.school_id
            LEFT JOIN onboardings o ON o.lead_id = l.id
            """.formatted(AWAITING);

    public Page<LeadDTO> list(Filter f, int page, int size) {
        var where = new StringBuilder(" WHERE 1=1");
        var args = new ArrayList<Object>();

        if (f.status() != null && !f.status().isBlank()) {
            where.append(" AND l.status = ?");
            args.add(Status.valueOf(f.status()).name());
        }
        if (f.assignedTo() != null && !f.assignedTo().isBlank()) {
            where.append(" AND l.assigned_to = ?");
            args.add(f.assignedTo());
        }
        if (f.search() != null && !f.search().isBlank()) {
            where.append(" AND (lower(l.name) LIKE ? OR lower(coalesce(l.school_name,'')) LIKE ? OR lower(coalesce(l.email,'')) LIKE ? OR coalesce(l.phone,'') LIKE ?)");
            String like = "%" + f.search().trim().toLowerCase() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add("%" + f.search().trim() + "%");
        }
        if ("awaiting".equals(f.view())) {
            where.append(" AND ").append(AWAITING);
        } else if ("unassigned".equals(f.view())) {
            where.append(" AND l.assigned_to IS NULL AND l.status NOT IN ('CONVERTED','LOST')");
        } else if ("open".equals(f.view())) {
            where.append(" AND l.status NOT IN ('CONVERTED','LOST')");
        }

        Long total = jdbc.queryForObject("SELECT count(*) FROM leads l" + where, Long.class, args.toArray());

        var pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) Math.max(page - 1, 0) * size);
        var rows = jdbc.query(SELECT + where + """
                 ORDER BY (l.status IN ('CONVERTED','LOST')), l.next_follow_up_at ASC NULLS LAST, l.created_at DESC
                 LIMIT ? OFFSET ?
                """, (rs, i) -> map(rs), pageArgs.toArray());

        return new PageImpl<>(rows, PageRequest.of(Math.max(page - 1, 0), size), total == null ? 0 : total);
    }

    public LeadDTO.Detail get(String id) {
        var uuid = parse(id);
        var rows = jdbc.query(SELECT + " WHERE l.id = ?", (rs, i) -> map(rs), uuid);
        if (rows.isEmpty()) {
            throw new NotFoundException("Lead not found");
        }
        var history = jdbc.query("""
                SELECT n.*, trim(concat_ws(' ', u.given_name, u.family_name)) AS author
                FROM lead_notes n LEFT JOIN users u ON u.id = n.created_by
                WHERE n.lead_id = ? ORDER BY n.created_at DESC
                """, (rs, i) -> new LeadDTO.Note(rs.getString("id"), rs.getString("kind"), rs.getString("body"),
                rs.getString("created_by"), blankToNull(rs.getString("author")), instant(rs, "created_at")), uuid);
        return new LeadDTO.Detail(rows.get(0), history);
    }

    public LeadDTO.Summary summary() {
        var byStatus = new LinkedHashMap<String, Long>();
        for (Status s : Status.values()) {
            byStatus.put(s.name(), 0L);
        }
        jdbc.query("SELECT status, count(*) AS n FROM leads GROUP BY status",
                rs -> { byStatus.put(rs.getString("status"), rs.getLong("n")); });

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new LeadDTO.Summary(total, byStatus,
                count("SELECT count(*) FROM leads l WHERE " + AWAITING),
                count("SELECT count(*) FROM leads l WHERE l.assigned_to IS NULL AND l.status NOT IN ('CONVERTED','LOST')"),
                count("SELECT count(*) FROM leads l WHERE l.status NOT IN ('CONVERTED','LOST') AND l.next_follow_up_at < clock_timestamp() - interval '1 day'"));
    }

    // The lead (if any) each demo request / contact message was made into, for the lists that show the enquiries.
    public Map<String, LeadDTO> byDemoRequests(Collection<String> ids) {
        return byColumn("demo_request_id", ids);
    }

    public Map<String, LeadDTO> byContactMessages(Collection<String> ids) {
        return byColumn("contact_message_id", ids);
    }

    private Map<String, LeadDTO> byColumn(String column, Collection<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        var uuids = ids.stream().map(LeadService::parse).toList();
        String marks = uuids.stream().map(x -> "?").collect(Collectors.joining(","));
        // column is one of two fixed names above, never user input
        var rows = jdbc.query(SELECT + " WHERE l." + column + " IN (" + marks + ")", (rs, i) -> map(rs), uuids.toArray());
        var out = new LinkedHashMap<String, LeadDTO>();
        for (var row : rows) {
            String key = "demo_request_id".equals(column) ? row.demoRequestId() : row.contactMessageId();
            out.put(key, row);
        }
        return out;
    }

    public List<LeadDTO.Staff> staff() {
        return jdbc.query("""
                SELECT DISTINCT u.id, trim(concat_ws(' ', u.given_name, u.family_name)) AS name, u.email
                FROM users u JOIN school_users su ON su.user_id = u.id
                WHERE su.role = 'SYSTEM_ADMIN' AND su.status = 'ACTIVE' AND u.status <> 'DELETED'
                ORDER BY name
                """, (rs, i) -> new LeadDTO.Staff(rs.getString("id"), rs.getString("name"), rs.getString("email")));
    }

    // ------------------------------------------------------------------ helpers

    // The reads below are SQL, so what the entities just saved has to reach the database first.
    public void flush() {
        leads.flush();
        notes.flush();
    }

    private LeadDTO reload(String id) {
        flush();
        return get(id).lead();
    }

    private LeadDTO map(ResultSet rs) throws SQLException {
        String assignee = blankToNull(joinNonBlank(" ", rs.getString("a_given"), rs.getString("a_family")));
        return new LeadDTO(rs.getString("id"), rs.getString("name"), rs.getString("school_name"), rs.getString("email"),
                rs.getString("phone"), rs.getString("city"), rs.getString("source"), rs.getString("demo_request_id"),
                rs.getString("contact_message_id"), rs.getString("status"), rs.getString("demo_status"),
                instant(rs, "demo_at"), rs.getString("assigned_to"), assignee, instant(rs, "next_follow_up_at"),
                rs.getString("summary"), rs.getString("school_id"), rs.getString("converted_school_name"),
                instant(rs, "converted_at"), rs.getString("lost_reason"), rs.getBoolean("awaiting"),
                rs.getString("onboarding_id"), nextStatuses(Status.valueOf(rs.getString("status"))),
                instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var t = rs.getTimestamp(column);
        return t == null ? null : Instant.ofEpochMilli(t.getTime());
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }

    LeadEntity find(String id) {
        return leads.findById(parse(id)).orElseThrow(() -> new NotFoundException("Lead not found"));
    }

    static UUID parse(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Not found");
        }
    }

    private void assertStaff(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        if (staff().stream().noneMatch(s -> s.id().equals(userId))) {
            throw new RuleException("Leads can only be assigned to an active system admin");
        }
    }

    private String nameOf(String userId) {
        return staff().stream().filter(s -> s.id().equals(userId)).map(LeadDTO.Staff::name).findFirst().orElse("someone");
    }

    private void note(UUID leadId, Kind kind, String body, String actorId) {
        notes.save(LeadNoteEntity.builder().id(UUID.randomUUID()).leadId(leadId).kind(kind).body(body)
                .createdBy(actorId).createdAt(Instant.now()).build());
    }

    private void system(UUID leadId, String body) {
        note(leadId, Kind.SYSTEM, body, null);
    }

    static String label(Status s) {
        String text = s.name().toLowerCase().replace('_', ' ');
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String joinNonBlank(String sep, String... parts) {
        return java.util.Arrays.stream(parts).filter(p -> p != null && !p.isBlank()).collect(Collectors.joining(sep));
    }
}
