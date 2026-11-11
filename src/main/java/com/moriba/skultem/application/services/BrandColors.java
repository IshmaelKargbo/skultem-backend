package com.moriba.skultem.application.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.moriba.skultem.application.dto.SchoolDTO;

/**
 * A school's brand colours as M-Compose takes them: {"colors": {"primary": "#f66102", "secondary": "#820013",
 * "accent": "#820013"}} with the data. Every colour in a template that follows a brand colour (@primary, @accent/12)
 * then takes the school's, for that document only; a role the template doesn't have is ignored by the engine, so
 * the same template prints in each school's own colours. Secondary is sent under both names templates use for it.
 */
public final class BrandColors {
    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private BrandColors() {
    }

    public static Map<String, String> of(SchoolDTO school) {
        return of(school == null ? null : school.primaryColor(), school == null ? null : school.secondaryColor());
    }

    public static Map<String, String> of(String primary, String secondary) {
        Map<String, String> colors = new LinkedHashMap<>();
        if (valid(primary)) {
            colors.put("primary", primary.trim().toLowerCase());
        }
        if (valid(secondary)) {
            String s = secondary.trim().toLowerCase();
            colors.put("secondary", s);
            colors.put("accent", s);
        }
        return colors;
    }

    private static boolean valid(String hex) {
        return hex != null && HEX.matcher(hex.trim()).matches();
    }
}
