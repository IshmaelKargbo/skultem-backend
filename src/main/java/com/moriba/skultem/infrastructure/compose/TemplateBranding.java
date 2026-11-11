package com.moriba.skultem.infrastructure.compose;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Gives a design brand colours, so it prints in a school's own colours - the same rules as M-Compose's
 * tools/branding.py. A design made without brand colours (most designs saved in the designer) would otherwise ignore
 * the school colours Skultem sends.
 *
 * It finds the two strong colours used most on the design's shapes, lines and table headers (of clearly different
 * hues); the darker is "primary", the other "accent". Every colour setting that uses them, or a pale shade of them,
 * then follows the brand (@primary, @primary/12). Greys, black and white stay as they are, and a shade is only used
 * when it is within a few steps of the colour that was there - so with the design's own colours it looks the same.
 * A design that already has brand colours is left alone (the engine applies the school's colours to it itself).
 */
public final class TemplateBranding {

    /** The design to render, and whether anything changed (false = render the saved design as it is). */
    public record Result(ObjectNode template, boolean changed) {
    }

    private static final Set<String> SHAPES = Set.of("rectangle", "ellipse", "line", "table");
    private static final List<String> SHAPE_FIELDS = List.of("fill", "stroke", "header_fill");
    // Every colour setting the engine lets follow a brand colour (app/core/brand_colors.py COLOR_FIELDS).
    private static final List<String> COLOR_FIELDS = List.of("fill", "stroke", "color", "text_color", "header_fill",
            "header_color", "grid_color", "foreground", "background");
    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final int TOLERANCE = 6;

    private TemplateBranding() {
    }

    public static Result brand(ObjectNode original) {
        JsonNode existing = original.get("colors");
        if (existing != null && existing.isObject() && existing.size() > 0) {
            return new Result(original, false);
        }
        ObjectNode template = original.deepCopy();
        Map<String, String> colors = choose(template);
        if (colors.isEmpty()) {
            return new Result(original, false);
        }
        int changed = 0;
        for (ObjectNode element : elements(template.path("elements"))) {
            if (element.get("properties") instanceof ObjectNode props) {
                changed += convert(props, colors);
            }
        }
        if (template.get("styles") instanceof ObjectNode styles) {
            var it = styles.fields();
            while (it.hasNext()) {
                if (it.next().getValue() instanceof ObjectNode style) {
                    changed += convert(style, colors);
                }
            }
        }
        if (changed == 0) {
            return new Result(original, false);
        }
        ObjectNode roles = template.putObject("colors");
        colors.forEach(roles::put);
        return new Result(template, true);
    }

    // ---- Choosing the colours

    static Map<String, String> choose(ObjectNode template) {
        Map<String, Integer> used = new LinkedHashMap<>();
        for (ObjectNode element : elements(template.path("elements"))) {
            if (!SHAPES.contains(element.path("type").asText())) {
                continue;
            }
            JsonNode props = element.path("properties");
            for (String name : SHAPE_FIELDS) {
                String value = props.path(name).asText(null);
                if (isHex(value) && isStrong(value.toLowerCase())) {
                    used.merge(value.toLowerCase(), 1, Integer::sum);
                }
            }
        }
        if (used.isEmpty()) {
            return Map.of();
        }
        List<String> ranked = new ArrayList<>(used.keySet());
        ranked.sort(Comparator.comparing((String c) -> used.get(c)).reversed());
        String first = ranked.get(0);
        String second = ranked.stream().skip(1).filter(c -> hueGap(c, first) > 25).findFirst().orElse(null);
        Map<String, String> colors = new LinkedHashMap<>();
        if (second == null) {
            colors.put("primary", first);
            return colors;
        }
        // Of two colours the darker is the main one (navy, not its gold trim).
        boolean firstDarker = hls(first)[1] <= hls(second)[1];
        colors.put("primary", firstDarker ? first : second);
        colors.put("accent", firstDarker ? second : first);
        return colors;
    }

    // ---- Following the brand

    private static int convert(ObjectNode settings, Map<String, String> colors) {
        int changed = 0;
        for (String name : COLOR_FIELDS) {
            JsonNode value = settings.get(name);
            if (value != null && value.isTextual()) {
                String next = asBrand(value.asText(), colors);
                if (!next.equals(value.asText())) {
                    settings.put(name, next);
                    changed++;
                }
            }
        }
        if (settings.get("colors") instanceof ArrayNode list) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).isTextual()) {
                    String next = asBrand(list.get(i).asText(), colors);
                    if (!next.equals(list.get(i).asText())) {
                        list.set(i, settings.textNode(next));
                        changed++;
                    }
                }
            }
        }
        return changed;
    }

    /** The brand reference that draws this colour (the colour itself, or a shade within tolerance), else as it is. */
    static String asBrand(String value, Map<String, String> colors) {
        if (!isHex(value)) {
            return value;
        }
        String color = value.toLowerCase();
        for (var e : colors.entrySet()) {
            if (color.equals(e.getValue())) {
                return "@" + e.getKey();
            }
        }
        int[] target = rgb(color);
        for (var e : colors.entrySet()) {
            for (int percent = 1; percent < 100; percent++) {
                int[] near = rgb(shade(e.getValue(), percent));
                if (Math.abs(near[0] - target[0]) <= TOLERANCE && Math.abs(near[1] - target[1]) <= TOLERANCE
                        && Math.abs(near[2] - target[2]) <= TOLERANCE) {
                    return "@" + e.getKey() + "/" + percent;
                }
            }
        }
        return value;
    }

    // ---- Colour helpers (as the engine computes them)

    /** percent% of the colour over white: 100 is the colour itself, 10 a pale tint. */
    static String shade(String color, int percent) {
        int[] c = rgb(color);
        StringBuilder out = new StringBuilder("#");
        for (int channel : c) {
            out.append(String.format("%02x", (int) Math.round(255 - (255 - channel) * percent / 100.0)));
        }
        return out.toString();
    }

    // A colour with real colour in it: not a grey, not near black or white.
    static boolean isStrong(String color) {
        double[] hls = hls(color);
        return hls[2] >= 0.35 && hls[1] >= 0.12 && hls[1] <= 0.8;
    }

    private static double hueGap(String a, String b) {
        double gap = Math.abs(hls(a)[0] - hls(b)[0]) * 360;
        return Math.min(gap, 360 - gap);
    }

    // Python's colorsys.rgb_to_hls: {hue 0-1, lightness 0-1, saturation 0-1}.
    private static double[] hls(String color) {
        int[] c = rgb(color);
        double r = c[0] / 255.0, g = c[1] / 255.0, b = c[2] / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        double l = (max + min) / 2.0;
        if (min == max) {
            return new double[] { 0, l, 0 };
        }
        double s = l <= 0.5 ? (max - min) / (max + min) : (max - min) / (2.0 - max - min);
        double rc = (max - r) / (max - min), gc = (max - g) / (max - min), bc = (max - b) / (max - min);
        double h = r == max ? bc - gc : g == max ? 2.0 + rc - bc : 4.0 + gc - rc;
        h = (h / 6.0) % 1.0;
        if (h < 0) {
            h += 1.0;
        }
        return new double[] { h, l, s };
    }

    private static int[] rgb(String color) {
        return new int[] { Integer.parseInt(color.substring(1, 3), 16), Integer.parseInt(color.substring(3, 5), 16),
                Integer.parseInt(color.substring(5, 7), 16) };
    }

    private static boolean isHex(String value) {
        return value != null && HEX.matcher(value).matches();
    }

    // Every element, children of repeats included.
    private static List<ObjectNode> elements(JsonNode list) {
        List<ObjectNode> out = new ArrayList<>();
        if (list != null && list.isArray()) {
            for (JsonNode e : list) {
                if (e instanceof ObjectNode o) {
                    out.add(o);
                    out.addAll(elements(o.path("children")));
                }
            }
        }
        return out;
    }
}
