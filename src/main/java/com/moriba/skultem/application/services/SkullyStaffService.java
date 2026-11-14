package com.moriba.skultem.application.services;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.security.AuthUser;

import lombok.RequiredArgsConstructor;

/**
 * School staff on Skully. A user's WhatsApp number (users.phone, saved as digits with the country code) lets Skully
 * recognise them, and they can ask about their school - only what their role can see in Skultem itself:
 * <ul>
 * <li>fees: owner, proprietor, principal, super admin, accountant (as the fee reports)</li>
 * <li>academics (students and classes needing attention) and attendance: owner, proprietor, principal, super admin,
 * admin, exam officer (as the attention reports)</li>
 * </ul>
 * A section-limited admin or principal only sees their own sections: the reports run as that user, exactly as in
 * the app. Read-only, and only at schools with Skully switched on.
 */
@Service
@RequiredArgsConstructor
public class SkullyStaffService {

    // Most senior first: the role a request runs as (and so its section scope) is the first one the user has.
    static final List<Role> STAFF_ROLES = List.of(Role.OWNER, Role.PROPRIETOR, Role.SUPER_ADMIN, Role.PRINCIPAL,
            Role.ADMIN, Role.EXAM_OFFICER, Role.ACCOUNTANT);
    static final Set<Role> FEES = EnumSet.of(Role.OWNER, Role.PROPRIETOR, Role.PRINCIPAL, Role.SUPER_ADMIN, Role.ACCOUNTANT);
    static final Set<Role> ACADEMICS = EnumSet.of(Role.OWNER, Role.PROPRIETOR, Role.PRINCIPAL, Role.SUPER_ADMIN,
            Role.ADMIN, Role.EXAM_OFFICER);

    private final JdbcTemplate jdbc;
    private final ModuleAccessService modules;
    private final SkullyReminderService numbers;
    private final SkullyInsightsService insights;

    /** One school where this number belongs to staff, and what they may ask about there. */
    public record StaffAccess(String schoolId, String schoolName, String name, List<String> roles, List<String> canSee) {
    }

    private record Membership(String userId, String name, String schoolId, String schoolName, Role role) {
    }

    // ---------------------------------------------------------------------------------------------------------
    // The number itself (profile and user management)

    public String phone(String userId) {
        return jdbc.queryForList("SELECT phone FROM users WHERE id = ?", String.class, userId).stream()
                .findFirst().orElse(null);
    }

    /** Saves (or with a blank one, removes) a user's WhatsApp number. One account per number. */
    @Transactional
    public String savePhone(String userId, String phone) {
        String number = null;
        if (phone != null && !phone.isBlank()) {
            number = numbers.whatsappNumber(phone);
            if (number == null) throw new RuleException("That doesn't look like a phone number");
            Integer taken = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE phone = ? AND id <> ?",
                    Integer.class, number, userId);
            if (taken != null && taken > 0) {
                throw new AlreadyExistsException("Another account already uses this WhatsApp number");
            }
        }
        jdbc.update("UPDATE users SET phone = ?, updated_at = now() WHERE id = ?", number, userId);
        return number;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Skully

    public List<StaffAccess> staff(String phone) {
        Map<String, List<Membership>> bySchool = new LinkedHashMap<>();
        for (Membership m : memberships(phone)) bySchool.computeIfAbsent(m.schoolId(), k -> new ArrayList<>()).add(m);
        List<StaffAccess> out = new ArrayList<>();
        for (var entry : bySchool.entrySet()) {
            var first = entry.getValue().get(0);
            Set<Role> roles = EnumSet.noneOf(Role.class);
            entry.getValue().forEach(m -> roles.add(m.role()));
            out.add(new StaffAccess(first.schoolId(), first.schoolName(), first.name(),
                    STAFF_ROLES.stream().filter(roles::contains).map(Role::name).toList(), parts(roles).stream().toList()));
        }
        return out;
    }

    /** What this number's staff member may see at that school, as the reports show it to them. Empty if not theirs. */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> overview(String phone, String schoolId) {
        List<Membership> mine = memberships(phone).stream().filter(m -> m.schoolId().equals(schoolId)).toList();
        if (mine.isEmpty()) return Optional.empty();
        Set<Role> roles = EnumSet.noneOf(Role.class);
        mine.forEach(m -> roles.add(m.role()));
        Role actingAs = STAFF_ROLES.stream().filter(roles::contains).findFirst().orElseThrow();
        Set<String> parts = parts(roles);

        Map<String, Object> facts = as(new AuthUser(mine.get(0).userId(), schoolId, actingAs),
                () -> insights.facts(schoolId, parts));
        Map<String, Object> out = new LinkedHashMap<>(facts);
        out.put("yourRoles", STAFF_ROLES.stream().filter(roles::contains).map(Role::name).toList());
        out.put("youCanSee", parts);
        return Optional.of(out);
    }

    static Set<String> parts(Set<Role> roles) {
        Set<String> parts = new LinkedHashSet<>();
        if (roles.stream().anyMatch(ACADEMICS::contains)) {
            parts.add(SkullyInsightsService.ATTENDANCE);
            parts.add(SkullyInsightsService.ACADEMICS);
        }
        if (roles.stream().anyMatch(FEES::contains)) parts.add(SkullyInsightsService.FEES);
        return parts;
    }

    // Active staff memberships for this number, at schools with Skully on. Matched on the whole number (with its
    // country code), never a part of it: this is school data, not a reminder.
    private List<Membership> memberships(String phone) {
        String number = numbers.whatsappNumber(phone);
        if (number == null) return List.of();
        List<Membership> out = new ArrayList<>();
        for (var r : jdbc.queryForList("""
                SELECT u.id, u.given_name, u.family_name, su.school_id, su.role, s.name AS school_name
                FROM users u
                JOIN school_users su ON su.user_id = u.id AND su.status = 'ACTIVE'
                JOIN schools s ON s.id = su.school_id
                WHERE u.phone = ? AND u.status IN ('ACTIVE', 'RESET_PASSWORD')
                ORDER BY s.name
                """, number)) {
            Role role;
            try {
                role = Role.valueOf((String) r.get("role"));
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (!STAFF_ROLES.contains(role)) continue;
            String schoolId = (String) r.get("school_id");
            if (!modules.isEnabled(schoolId, FeatureModule.WHATSAPP_ASSISTANT)) continue;
            String name = (Optional.ofNullable((String) r.get("given_name")).orElse("") + " "
                    + Optional.ofNullable((String) r.get("family_name")).orElse("")).trim();
            out.add(new Membership((String) r.get("id"), name, schoolId, (String) r.get("school_name"), role));
        }
        return out;
    }

    // Runs the reports as this staff member, so the same section scope applies as when they sign in.
    private static <T> T as(AuthUser user, Supplier<T> work) {
        var context = SecurityContextHolder.getContext();
        var previous = context.getAuthentication();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        try {
            return work.get();
        } finally {
            context.setAuthentication(previous);
        }
    }
}
