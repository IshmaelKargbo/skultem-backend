package com.moriba.skultem.application.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Platform-wide settings. Only the settings listed here exist - each one is read by something real (billing,
 * the dashboard, the marketing site) - and a value is checked against its definition before it is saved. A setting
 * nobody has changed uses its built-in default.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PlatformSettingsService {

    public enum Type {
        INTEGER, TEXT, EMAIL, PHONE
    }

    /** What a setting is, how it is checked and what it falls back to. */
    public record Definition(String key, String group, String label, String description, Type type,
            String defaultValue, Integer min, Integer max, boolean isPublic) {
    }

    public static final String PAYMENT_TERMS_DAYS = "billing.paymentTermsDays";
    public static final String RENEWAL_WINDOW_DAYS = "billing.renewalWindowDays";
    public static final String STALE_PLAYGROUND_DAYS = "insights.stalePlaygroundDays";
    public static final String PLATFORM_NAME = "platform.name";
    public static final String SUPPORT_EMAIL = "support.email";
    public static final String SUPPORT_PHONE = "support.phone";

    public static final List<Definition> DEFINITIONS = List.of(
            new Definition(PAYMENT_TERMS_DAYS, "Billing", "Payment terms (days)",
                    "How many days a school has to pay a new subscription invoice, counted from the later of today and the period's start.",
                    Type.INTEGER, "14", 0, 90, false),
            new Definition(RENEWAL_WINDOW_DAYS, "Billing", "Renewal window (days)",
                    "How close to the end of a paid period a subscription can be renewed, and how far ahead the renewals list looks.",
                    Type.INTEGER, "30", 1, 120, false),
            new Definition(STALE_PLAYGROUND_DAYS, "Dashboard", "Playground flagged after (days)",
                    "A school that has been in playground mode this long shows under pending tasks on the dashboard.",
                    Type.INTEGER, "30", 7, 365, false),
            new Definition(PLATFORM_NAME, "Marketing site", "Platform name", "The name the marketing site shows.",
                    Type.TEXT, "Skultem", null, null, true),
            new Definition(SUPPORT_EMAIL, "Marketing site", "Support email", "Where schools should write for help. Leave empty to show none.",
                    Type.EMAIL, "", null, null, true),
            new Definition(SUPPORT_PHONE, "Marketing site", "Support phone", "Where schools should call for help. Leave empty to show none.",
                    Type.PHONE, "", null, null, true));

    public record Entry(Definition definition, String value, boolean changed, String updatedByName, Instant updatedAt) {
    }

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ]{7,20}$");

    private final JdbcTemplate jdbc;

    // ------------------------------------------------------------------ reading

    /** Every setting with its current value. */
    public List<Entry> all() {
        var stored = new LinkedHashMap<String, String[]>();
        jdbc.query("""
                SELECT s.key, s.value, trim(concat_ws(' ', u.given_name, u.family_name)) AS by_name, s.updated_at
                FROM platform_config s LEFT JOIN users u ON u.id = s.updated_by
                """, rs -> {
                    var at = rs.getTimestamp("updated_at");
                    stored.put(rs.getString("key"), new String[] { rs.getString("value"), rs.getString("by_name"),
                            at == null ? null : String.valueOf(at.getTime()) });
                });

        var out = new ArrayList<Entry>();
        for (var d : DEFINITIONS) {
            var row = stored.get(d.key());
            out.add(row == null
                    ? new Entry(d, d.defaultValue(), false, null, null)
                    : new Entry(d, row[0], true, row[1] == null || row[1].isBlank() ? null : row[1],
                            row[2] == null ? null : Instant.ofEpochMilli(Long.parseLong(row[2]))));
        }
        return out;
    }

    public String value(String key) {
        var d = definition(key);
        var rows = jdbc.queryForList("SELECT value FROM platform_config WHERE key = ?", String.class, key);
        return rows.isEmpty() ? d.defaultValue() : rows.get(0);
    }

    public int intValue(String key) {
        return Integer.parseInt(value(key));
    }

    /** What the public marketing site may read. */
    public Map<String, String> publicValues() {
        var out = new LinkedHashMap<String, String>();
        for (var e : all()) {
            if (e.definition().isPublic()) {
                out.put(e.definition().key(), e.value());
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ changing

    /** Checks a value against its setting's definition; returns it cleaned up, or throws saying what is wrong. */
    public static String validate(Definition d, String raw) {
        String v = raw == null ? "" : raw.trim();
        switch (d.type()) {
            case INTEGER -> {
                int n;
                try {
                    n = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new RuleException(d.label() + " must be a whole number");
                }
                if (d.min() != null && n < d.min() || d.max() != null && n > d.max()) {
                    throw new RuleException(d.label() + " must be between " + d.min() + " and " + d.max());
                }
                return String.valueOf(n);
            }
            case EMAIL -> {
                if (!v.isEmpty() && !EMAIL.matcher(v).matches()) {
                    throw new RuleException(d.label() + " must be a valid email address");
                }
                return v.toLowerCase();
            }
            case PHONE -> {
                if (!v.isEmpty() && !PHONE.matcher(v).matches()) {
                    throw new RuleException(d.label() + " may only contain digits, spaces and a leading +");
                }
                return v;
            }
            default -> {
                if (v.isEmpty()) {
                    throw new RuleException(d.label() + " can't be empty");
                }
                if (v.length() > 100) {
                    throw new RuleException(d.label() + " must be at most 100 characters");
                }
                return v;
            }
        }
    }

    /** Saves the settings that changed. All of them are checked first - one bad value saves nothing. */
    @AuditLogAnnotation(action = "PLATFORM_SETTINGS_CHANGED")
    public List<Entry> save(Map<String, String> values, String actorId) {
        var cleaned = new LinkedHashMap<String, String>();
        for (var e : values.entrySet()) {
            cleaned.put(e.getKey(), validate(definition(e.getKey()), e.getValue()));
        }
        var current = new LinkedHashMap<String, String>();
        all().forEach(entry -> current.put(entry.definition().key(), entry.value()));

        cleaned.forEach((key, value) -> {
            if (!value.equals(current.get(key))) {
                jdbc.update("""
                        INSERT INTO platform_config (key, value, updated_by, updated_at) VALUES (?, ?, ?, now())
                        ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, updated_by = EXCLUDED.updated_by, updated_at = now()
                        """, key, value, actorId);
            }
        });
        return all();
    }

    static Definition definition(String key) {
        return DEFINITIONS.stream().filter(d -> d.key().equals(key)).findFirst()
                .orElseThrow(() -> new RuleException("Unknown setting: " + key));
    }
}
