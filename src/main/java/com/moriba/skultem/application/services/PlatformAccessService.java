package com.moriba.skultem.application.services;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * What each system admin may do on the platform.
 *
 * Every platform endpoint asks for one permission (see PermissionService#hasPlatformPermission) and this answers
 * from the system admin's role. A system admin with no role recorded is an OWNER, so introducing roles never
 * takes access away from anybody: access only narrows when an owner gives someone a narrower role. The rule that
 * there is always at least one owner keeps the platform from locking itself out.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PlatformAccessService {

    public enum Permission {
        DASHBOARD, SCHOOLS, ADMINS, BILLING, MARKETING, COMMUNICATION, SUPPORT, SETTINGS, AUDIT, ROLES, HEALTH
    }

    public enum Role {
        OWNER("Owner", "Everything on the platform, including who else can do what.",
                EnumSet.allOf(Permission.class)),
        BILLING("Billing manager", "Subscriptions, invoices and payments.",
                EnumSet.of(Permission.DASHBOARD, Permission.BILLING)),
        MARKETING("Marketing manager", "Leads, onboarding, demo requests, messages, the blog, testimonials and announcements.",
                EnumSet.of(Permission.DASHBOARD, Permission.MARKETING, Permission.COMMUNICATION)),
        SUPPORT("Support agent", "Helping schools: look at schools, handle their support tickets and check platform health.",
                EnumSet.of(Permission.DASHBOARD, Permission.SCHOOLS, Permission.SUPPORT, Permission.HEALTH)),
        AUDITOR("Auditor", "Read the audit log and the platform's health. Cannot change anything.",
                EnumSet.of(Permission.DASHBOARD, Permission.AUDIT, Permission.HEALTH));

        public final String label;
        public final String description;
        public final Set<Permission> permissions;

        Role(String label, String description, Set<Permission> permissions) {
            this.label = label;
            this.description = description;
            this.permissions = permissions;
        }
    }

    public record Staff(String userId, String name, String email, String status, Role role, boolean explicit,
            Instant lastLoginAt) {
    }

    private final JdbcTemplate jdbc;

    // ------------------------------------------------------------------ checks

    public Role roleOf(String userId) {
        var rows = jdbc.queryForList("SELECT role FROM platform_staff_roles WHERE user_id = ?", String.class, userId);
        return rows.isEmpty() ? Role.OWNER : Role.valueOf(rows.get(0));
    }

    public Set<Permission> permissionsOf(String userId) {
        return roleOf(userId).permissions;
    }

    public boolean has(String userId, Permission permission) {
        return permissionsOf(userId).contains(permission);
    }

    // ------------------------------------------------------------------ managing roles

    /** The people who can administer the platform, with the role each has and when they last signed in. */
    public List<Staff> staff() {
        return jdbc.query("""
                SELECT u.id, trim(concat_ws(' ', u.given_name, u.family_name)) AS name, u.email, u.status,
                       r.role,
                       (SELECT max(a.created_at) FROM audit_logs a
                         WHERE a.user_id = u.id AND a.status = 'SUCCESS'
                           AND a.action IN ('SYSTEM_ADMIN_LOGIN_SUCCESS', 'LOGIN_SUCCESS')) AS last_login
                FROM users u
                LEFT JOIN platform_staff_roles r ON r.user_id = u.id
                WHERE u.status <> 'DELETED'
                  AND EXISTS (SELECT 1 FROM school_users su WHERE su.user_id = u.id AND su.role = 'SYSTEM_ADMIN' AND su.status = 'ACTIVE')
                ORDER BY name
                """, (rs, i) -> {
                    String role = rs.getString("role");
                    var login = rs.getTimestamp("last_login");
                    return new Staff(rs.getString("id"), rs.getString("name"), rs.getString("email"), rs.getString("status"),
                            role == null ? Role.OWNER : Role.valueOf(role), role != null,
                            login == null ? null : Instant.ofEpochMilli(login.getTime()));
                });
    }

    /** Gives a system admin a role. Owner-only (the endpoint asks for ROLES); never leaves the platform without an owner. */
    @AuditLogAnnotation(action = "PLATFORM_ROLE_CHANGED")
    public Staff assign(String userId, String roleName, String actorId) {
        Role role;
        try {
            role = Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new RuleException("Unknown role");
        }

        var all = staff();
        var target = all.stream().filter(s -> s.userId().equals(userId)).findFirst()
                .orElseThrow(() -> new NotFoundException("That person is not a system admin"));

        if (target.role() == Role.OWNER && role != Role.OWNER) {
            long owners = all.stream().filter(s -> s.role() == Role.OWNER && "ACTIVE".equals(s.status())).count();
            if (owners <= 1) {
                throw new RuleException("The platform needs at least one owner - make someone else an owner first");
            }
        }

        if (role == Role.OWNER) {
            // Owner is what everyone without a role already is, so there is nothing to store.
            jdbc.update("DELETE FROM platform_staff_roles WHERE user_id = ?", userId);
        } else {
            jdbc.update("""
                    INSERT INTO platform_staff_roles (user_id, role, updated_by, updated_at) VALUES (?, ?, ?, now())
                    ON CONFLICT (user_id) DO UPDATE SET role = EXCLUDED.role, updated_by = EXCLUDED.updated_by, updated_at = now()
                    """, userId, role.name(), actorId);
        }
        return staff().stream().filter(s -> s.userId().equals(userId)).findFirst().orElseThrow();
    }
}
