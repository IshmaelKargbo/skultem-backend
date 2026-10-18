package com.moriba.skultem.application.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /** A role as the app sees it: one of the built-in ones, or one an owner created. */
    public record RoleDef(String key, String label, String description, Set<Permission> permissions, boolean custom) {
        static RoleDef of(Role r) {
            return new RoleDef(r.name(), r.label, r.description, r.permissions, false);
        }

        public boolean isOwner() {
            return key.equals(Role.OWNER.name());
        }
    }

    public record Staff(String userId, String name, String email, String status, RoleDef role, boolean explicit,
            Instant lastLoginAt) {
    }

    private static final String CUSTOM_PREFIX = "CUSTOM_";
    /** Only an owner may hold these, so a custom role can never be used to hand out control of the platform. */
    private static final Set<Permission> OWNER_ONLY = EnumSet.of(Permission.ROLES, Permission.SETTINGS, Permission.ADMINS);

    private final JdbcTemplate jdbc;

    // ------------------------------------------------------------------ checks

    public RoleDef roleOf(String userId) {
        var rows = jdbc.queryForList("SELECT role FROM platform_staff_roles WHERE user_id = ?", String.class, userId);
        return rows.isEmpty() ? RoleDef.of(Role.OWNER) : resolve(rows.get(0));
    }

    public Set<Permission> permissionsOf(String userId) {
        return roleOf(userId).permissions();
    }

    public boolean has(String userId, Permission permission) {
        return permissionsOf(userId).contains(permission);
    }

    // ------------------------------------------------------------------ the roles themselves

    /** The built-in roles followed by the ones owners created, oldest first. */
    public List<RoleDef> roles() {
        var all = new ArrayList<RoleDef>(Arrays.stream(Role.values()).map(RoleDef::of).toList());
        all.addAll(jdbc.query("SELECT id, label, description, permissions FROM platform_custom_roles ORDER BY created_at",
                (rs, i) -> custom(rs.getString("id"), rs.getString("label"), rs.getString("description"), rs.getString("permissions"))));
        return all;
    }

    private static RoleDef custom(String id, String label, String description, String permissions) {
        var set = EnumSet.noneOf(Permission.class);
        for (String p : permissions.split(",")) {
            if (!p.isBlank()) {
                set.add(Permission.valueOf(p.trim()));
            }
        }
        return new RoleDef(CUSTOM_PREFIX + id, label, description, set, true);
    }

    private RoleDef resolve(String key) {
        if (key.startsWith(CUSTOM_PREFIX)) {
            var rows = jdbc.query("SELECT id, label, description, permissions FROM platform_custom_roles WHERE id = ?",
                    (rs, i) -> custom(rs.getString("id"), rs.getString("label"), rs.getString("description"), rs.getString("permissions")),
                    key.substring(CUSTOM_PREFIX.length()));
            if (rows.isEmpty()) {
                throw new NotFoundException("That role no longer exists");
            }
            return rows.get(0);
        }
        try {
            return RoleDef.of(Role.valueOf(key));
        } catch (IllegalArgumentException e) {
            throw new RuleException("Unknown role");
        }
    }

    /** Creates a role an owner can hand out. It always sees the dashboard and can never include owner-only areas. */
    @AuditLogAnnotation(action = "PLATFORM_ROLE_CREATED")
    public RoleDef createRole(String label, String description, List<String> requested, String actorId) {
        var name = label == null ? "" : label.trim();
        if (name.isEmpty() || name.length() > 60) {
            throw new RuleException("Give the role a name of up to 60 characters");
        }
        if (roles().stream().anyMatch(r -> r.label().equalsIgnoreCase(name))) {
            throw new RuleException("A role called \"" + name + "\" already exists");
        }
        var granted = EnumSet.of(Permission.DASHBOARD);
        for (String p : requested == null ? List.<String>of() : requested) {
            Permission perm;
            try {
                perm = Permission.valueOf(p);
            } catch (IllegalArgumentException e) {
                throw new RuleException("Unknown permission " + p);
            }
            if (OWNER_ONLY.contains(perm)) {
                throw new RuleException("Only an owner can have access to " + perm.name().toLowerCase());
            }
            granted.add(perm);
        }
        if (granted.size() < 2) {
            throw new RuleException("Choose at least one area for this role");
        }
        var id = UUID.randomUUID().toString();
        var stored = granted.stream().map(Enum::name).collect(Collectors.joining(","));
        jdbc.update("INSERT INTO platform_custom_roles (id, label, description, permissions, created_by) VALUES (?, ?, ?, ?, ?)",
                id, name, description == null ? "" : description.trim(), stored, actorId);
        return custom(id, name, description == null ? "" : description.trim(), stored);
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
                            role == null ? RoleDef.of(Role.OWNER) : resolve(role), role != null,
                            login == null ? null : Instant.ofEpochMilli(login.getTime()));
                });
    }

    /** Gives a system admin a role. Owner-only (the endpoint asks for ROLES); never leaves the platform without an owner. */
    @AuditLogAnnotation(action = "PLATFORM_ROLE_CHANGED")
    public Staff assign(String userId, String roleName, String actorId) {
        var role = resolve(roleName);

        var all = staff();
        var target = all.stream().filter(s -> s.userId().equals(userId)).findFirst()
                .orElseThrow(() -> new NotFoundException("That person is not a system admin"));

        if (target.role().isOwner() && !role.isOwner()) {
            long owners = all.stream().filter(s -> s.role().isOwner() && "ACTIVE".equals(s.status())).count();
            if (owners <= 1) {
                throw new RuleException("The platform needs at least one owner - make someone else an owner first");
            }
        }

        if (role.isOwner()) {
            // Owner is what everyone without a role already is, so there is nothing to store.
            jdbc.update("DELETE FROM platform_staff_roles WHERE user_id = ?", userId);
        } else {
            jdbc.update("""
                    INSERT INTO platform_staff_roles (user_id, role, updated_by, updated_at) VALUES (?, ?, ?, now())
                    ON CONFLICT (user_id) DO UPDATE SET role = EXCLUDED.role, updated_by = EXCLUDED.updated_by, updated_at = now()
                    """, userId, role.key(), actorId);
        }
        return staff().stream().filter(s -> s.userId().equals(userId)).findFirst().orElseThrow();
    }
}
