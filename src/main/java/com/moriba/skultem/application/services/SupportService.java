package com.moriba.skultem.application.services;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SupportDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.infrastructure.rest.dto.SupportRequests;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Support tickets worked by Skultem staff. Replies are recorded on the ticket; nothing is sent from here, so the
 * service never claims a school was told anything - staff reply by email or phone and note it.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SupportService {

    public enum Status {
        OPEN, IN_PROGRESS, WAITING, RESOLVED, CLOSED
    }

    // Where a ticket can be moved. A resolved ticket can be reopened (the school may say it is not fixed); a closed one is final.
    private static final Map<Status, List<Status>> MOVES = new EnumMap<>(Map.of(
            Status.OPEN, List.of(Status.IN_PROGRESS, Status.WAITING, Status.RESOLVED, Status.CLOSED),
            Status.IN_PROGRESS, List.of(Status.WAITING, Status.RESOLVED, Status.CLOSED),
            Status.WAITING, List.of(Status.IN_PROGRESS, Status.RESOLVED, Status.CLOSED),
            Status.RESOLVED, List.of(Status.OPEN, Status.CLOSED),
            Status.CLOSED, List.of()));

    private static final List<String> FINISHED = List.of("RESOLVED", "CLOSED");

    private final JdbcTemplate jdbc;
    private final LeadService leadService;

    public static boolean canMove(Status from, Status to) {
        return MOVES.getOrDefault(from, List.of()).contains(to);
    }

    // ------------------------------------------------------------------ changing

    @AuditLogAnnotation(action = "SUPPORT_TICKET_CREATED")
    public SupportDTO.Detail create(SupportRequests.Create p, String actorId) {
        assertSchool(p.schoolId());
        assertStaff(p.assignedToId());

        var id = UUID.randomUUID();
        Long n = jdbc.queryForObject("SELECT nextval('support_ticket_number_seq')", Long.class);
        String number = "TKT-%d-%05d".formatted(LocalDate.now().getYear(), n);
        var now = java.sql.Timestamp.from(Instant.now());

        jdbc.update("""
                INSERT INTO support_tickets (id, number, school_id, requester_name, requester_email, requester_phone, category,
                       priority, status, subject, description, assigned_to, created_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, ?, ?, ?, ?, ?)
                """, id, number, blank(p.schoolId()), p.requesterName().trim(), blank(p.requesterEmail()),
                blank(p.requesterPhone()), p.category(), p.priority(), p.subject().trim(), p.description().trim(),
                blank(p.assignedToId()), actorId, now, now);
        message(id, "SYSTEM", "Ticket opened.", actorId);
        return get(id.toString());
    }

    @AuditLogAnnotation(action = "SUPPORT_TICKET_UPDATED")
    public SupportDTO.Detail update(String id, SupportRequests.Update p, String actorId) {
        var uuid = LeadService.parse(id);
        var current = ticketRow(uuid);
        assertOpen(current);
        assertStaff(p.assignedToId());

        var notes = new ArrayList<String>();
        if (p.category() != null && !p.category().equals(current.category())) {
            jdbc.update("UPDATE support_tickets SET category = ? WHERE id = ?", p.category(), uuid);
            notes.add("Category set to " + pretty(p.category()) + ".");
        }
        if (p.priority() != null && !p.priority().equals(current.priority())) {
            jdbc.update("UPDATE support_tickets SET priority = ? WHERE id = ?", p.priority(), uuid);
            notes.add("Priority set to " + pretty(p.priority()) + ".");
        }
        if (blank(p.assignedToId()) != null && !p.assignedToId().equals(current.assignedToId())) {
            jdbc.update("UPDATE support_tickets SET assigned_to = ? WHERE id = ?", p.assignedToId(), uuid);
            notes.add("Assigned to " + nameOf(p.assignedToId()) + ".");
        } else if (Boolean.TRUE.equals(p.clearAssignee()) && current.assignedToId() != null) {
            jdbc.update("UPDATE support_tickets SET assigned_to = NULL WHERE id = ?", uuid);
            notes.add("Unassigned.");
        }
        if (!notes.isEmpty()) {
            jdbc.update("UPDATE support_tickets SET updated_at = now() WHERE id = ?", uuid);
            message(uuid, "SYSTEM", String.join(" ", notes), actorId);
        }
        return get(id);
    }

    @AuditLogAnnotation(action = "SUPPORT_TICKET_STATUS_CHANGED")
    public SupportDTO.Detail changeStatus(String id, String status, String resolutionNote, String actorId) {
        var uuid = LeadService.parse(id);
        var current = ticketRow(uuid);
        var from = Status.valueOf(current.status());
        var to = Status.valueOf(status);

        if (!canMove(from, to)) {
            throw new RuleException("A ticket that is " + pretty(from.name()) + " can't be moved to " + pretty(to.name()));
        }
        if (to == Status.RESOLVED && blank(resolutionNote) == null) {
            throw new RuleException("Say how it was resolved");
        }

        boolean finishing = to == Status.RESOLVED || to == Status.CLOSED;
        jdbc.update("""
                UPDATE support_tickets SET status = ?, updated_at = now(),
                       resolved_at = CASE WHEN ?::boolean THEN coalesce(resolved_at, now()) ELSE NULL END,
                       resolution_note = coalesce(?::text, resolution_note)
                WHERE id = ?
                """, to.name(), finishing, blank(resolutionNote), uuid);
        message(uuid, "SYSTEM", pretty(from.name()) + " → " + pretty(to.name())
                + (blank(resolutionNote) != null ? ": " + resolutionNote.trim() : ""), actorId);
        return get(id);
    }

    public SupportDTO.Detail addMessage(String id, String kind, String body, String actorId) {
        var uuid = LeadService.parse(id);
        assertOpen(ticketRow(uuid));
        message(uuid, kind, body.trim(), actorId);
        jdbc.update("UPDATE support_tickets SET updated_at = now() WHERE id = ?", uuid);
        // The school answering puts the ball back with us.
        if ("FROM_SCHOOL".equals(kind)) {
            jdbc.update("UPDATE support_tickets SET status = 'IN_PROGRESS' WHERE id = ? AND status IN ('WAITING', 'RESOLVED')", uuid);
        }
        return get(id);
    }

    private void message(UUID ticketId, String kind, String body, String actorId) {
        jdbc.update("INSERT INTO support_ticket_messages (id, ticket_id, kind, body, created_by, created_at) VALUES (?, ?, ?, ?, ?, now())",
                UUID.randomUUID(), ticketId, kind, body, actorId);
    }

    // ------------------------------------------------------------------ reading

    private static final String SELECT = """
            SELECT t.*, s.name AS school_name,
                   trim(concat_ws(' ', a.given_name, a.family_name)) AS assignee,
                   trim(concat_ws(' ', c.given_name, c.family_name)) AS creator
            FROM support_tickets t
            LEFT JOIN schools s ON s.id = t.school_id
            LEFT JOIN users a ON a.id = t.assigned_to
            LEFT JOIN users c ON c.id = t.created_by
            """;

    public record Filter(String status, String priority, String assignedTo, String schoolId, String search, String view) {
    }

    public Page<SupportDTO.Ticket> list(Filter f, int page, int size) {
        var where = new StringBuilder(" WHERE 1=1");
        var args = new ArrayList<Object>();
        if (f.status() != null && !f.status().isBlank()) {
            where.append(" AND t.status = ?");
            args.add(Status.valueOf(f.status()).name());
        }
        if (f.priority() != null && !f.priority().isBlank()) {
            where.append(" AND t.priority = ?");
            args.add(f.priority());
        }
        if (f.assignedTo() != null && !f.assignedTo().isBlank()) {
            where.append(" AND t.assigned_to = ?");
            args.add(f.assignedTo());
        }
        if (f.schoolId() != null && !f.schoolId().isBlank()) {
            where.append(" AND t.school_id = ?");
            args.add(f.schoolId());
        }
        if (f.search() != null && !f.search().isBlank()) {
            where.append(" AND (lower(t.number) LIKE ? OR lower(t.subject) LIKE ? OR lower(t.requester_name) LIKE ? OR lower(coalesce(s.name,'')) LIKE ?)");
            String like = "%" + f.search().trim().toLowerCase() + "%";
            for (int i = 0; i < 4; i++) {
                args.add(like);
            }
        }
        if ("open".equals(f.view())) {
            where.append(" AND t.status NOT IN ('RESOLVED', 'CLOSED')");
        } else if ("unassigned".equals(f.view())) {
            where.append(" AND t.assigned_to IS NULL AND t.status NOT IN ('RESOLVED', 'CLOSED')");
        }

        Long total = jdbc.queryForObject("SELECT count(*) FROM support_tickets t LEFT JOIN schools s ON s.id = t.school_id" + where,
                Long.class, args.toArray());
        var pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) Math.max(page - 1, 0) * size);
        // Unfinished first, then the most urgent, then the oldest (it has waited longest).
        var rows = jdbc.query(SELECT + where + """
                 ORDER BY (t.status IN ('RESOLVED', 'CLOSED')),
                          CASE t.priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'NORMAL' THEN 2 ELSE 3 END,
                          t.created_at
                 LIMIT ? OFFSET ?
                """, (rs, i) -> map(rs), pageArgs.toArray());
        return new PageImpl<>(rows, PageRequest.of(Math.max(page - 1, 0), size), total == null ? 0 : total);
    }

    public SupportDTO.Detail get(String id) {
        var uuid = LeadService.parse(id);
        var ticket = ticketRow(uuid);
        var messages = jdbc.query("""
                SELECT m.*, trim(concat_ws(' ', u.given_name, u.family_name)) AS author
                FROM support_ticket_messages m LEFT JOIN users u ON u.id = m.created_by
                WHERE m.ticket_id = ? ORDER BY m.created_at
                """, (rs, i) -> new SupportDTO.Message(rs.getString("id"), rs.getString("kind"), rs.getString("body"),
                blank(rs.getString("author")), instant(rs, "created_at")), uuid);
        return new SupportDTO.Detail(ticket, messages);
    }

    public SupportDTO.Summary summary() {
        var byStatus = new LinkedHashMap<String, Long>();
        for (Status s : Status.values()) {
            byStatus.put(s.name(), 0L);
        }
        jdbc.query("SELECT status, count(*) AS n FROM support_tickets GROUP BY status", rs -> { byStatus.put(rs.getString("status"), rs.getLong("n")); });
        return new SupportDTO.Summary(byStatus.get("OPEN"), byStatus.get("IN_PROGRESS"), byStatus.get("WAITING"),
                byStatus.get("RESOLVED"),
                count("SELECT count(*) FROM support_tickets WHERE assigned_to IS NULL AND status NOT IN ('RESOLVED', 'CLOSED')"),
                count("SELECT count(*) FROM support_tickets WHERE priority = 'URGENT' AND status NOT IN ('RESOLVED', 'CLOSED')"),
                byStatus);
    }

    // ------------------------------------------------------------------ helpers

    private SupportDTO.Ticket ticketRow(UUID id) {
        var rows = jdbc.query(SELECT + " WHERE t.id = ?", (rs, i) -> map(rs), id);
        if (rows.isEmpty()) {
            throw new NotFoundException("Ticket not found");
        }
        return rows.get(0);
    }

    private SupportDTO.Ticket map(ResultSet rs) throws SQLException {
        String status = rs.getString("status");
        boolean finished = FINISHED.contains(status);
        return new SupportDTO.Ticket(rs.getString("id"), rs.getString("number"), rs.getString("school_id"),
                rs.getString("school_name"), rs.getString("requester_name"), rs.getString("requester_email"),
                rs.getString("requester_phone"), rs.getString("category"), rs.getString("priority"), status,
                rs.getString("subject"), rs.getString("description"), rs.getString("assigned_to"),
                blank(rs.getString("assignee")), rs.getString("resolution_note"), blank(rs.getString("creator")),
                instant(rs, "created_at"), instant(rs, "updated_at"), instant(rs, "resolved_at"),
                !finished && rs.getString("assigned_to") == null,
                MOVES.getOrDefault(Status.valueOf(status), List.of()).stream().map(Status::name).toList());
    }

    private void assertOpen(SupportDTO.Ticket t) {
        if ("CLOSED".equals(t.status())) {
            throw new RuleException("This ticket is closed");
        }
    }

    private void assertSchool(String schoolId) {
        if (blank(schoolId) == null) {
            return;
        }
        Long n = jdbc.queryForObject("SELECT count(*) FROM schools WHERE id = ? AND status <> 'DELETED'", Long.class, schoolId);
        if (n == null || n == 0) {
            throw new NotFoundException("School not found");
        }
    }

    private void assertStaff(String userId) {
        if (blank(userId) != null && leadService.staff().stream().noneMatch(s -> s.id().equals(userId))) {
            throw new RuleException("Tickets can only be assigned to an active system admin");
        }
    }

    private String nameOf(String userId) {
        return leadService.staff().stream().filter(s -> s.id().equals(userId)).map(com.moriba.skultem.application.dto.LeadDTO.Staff::name).findFirst().orElse("someone");
    }

    private static String pretty(String code) {
        String text = code.toLowerCase().replace('_', ' ');
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var t = rs.getTimestamp(column);
        return t == null ? null : Instant.ofEpochMilli(t.getTime());
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
