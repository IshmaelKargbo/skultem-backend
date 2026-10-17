package com.moriba.skultem.domain.vo;

import java.util.ArrayList;
import java.util.List;

// One named, weighted part of a continuous-assessment ("Attendance 20%", "Assignment 20%"). The weight is the
// part's share of the WHOLE assessment, so a configuration's components plus its formal test add up to 100.
// Component n is recorded as CA recording no. n.
public record CaComponent(String name, int weight, String source) {

    // Recorded by the teacher.
    public static final String MANUAL = "MANUAL";
    // Filled in from the attendance register: (days present + late) over the term's school days. Read-only.
    public static final String ATTENDANCE = "ATTENDANCE";

    public CaComponent {
        source = ATTENDANCE.equals(source) ? ATTENDANCE : MANUAL;
    }

    public CaComponent(String name, int weight) {
        this(name, weight, MANUAL);
    }

    public boolean fromAttendance() {
        return ATTENDANCE.equals(source);
    }

    // Storage form: one "name|weight" line per component, null when there are none.
    public static String encode(List<CaComponent> components) {
        return components == null || components.isEmpty() ? null
                : components.stream().map(c -> c.name() + "|" + c.weight() + (c.fromAttendance() ? "|A" : ""))
                        .collect(java.util.stream.Collectors.joining("\n"));
    }

    // Inline form for one line of the plan: "Attendance:20;Assignment:20". Names can't contain : ; | or a line break.
    public static String encodeInline(List<CaComponent> components) {
        return components == null ? "" : components.stream().map(c -> c.name() + ":" + c.weight() + (c.fromAttendance() ? ":A" : ""))
                .collect(java.util.stream.Collectors.joining(";"));
    }

    public static List<CaComponent> decodeInline(String text) {
        var out = new ArrayList<CaComponent>();
        if (text == null || text.isBlank()) {
            return out;
        }
        for (var part : text.split(";")) {
            var p = part.split(":");
            if (p.length == 2 || p.length == 3) {
                out.add(new CaComponent(p[0], Integer.parseInt(p[1]), p.length == 3 && "A".equals(p[2]) ? ATTENDANCE : MANUAL));
            }
        }
        return out;
    }

    // Checks and trims a list of components; returns the cleaned list and their total weight is left to the caller.
    public static List<CaComponent> clean(List<CaComponent> list, int maxCount) {
        var names = new java.util.HashSet<String>();
        var clean = new ArrayList<CaComponent>();
        if (list == null) {
            return clean;
        }
        if (list.size() > maxCount) {
            throw new com.moriba.skultem.application.error.RuleException("At most " + maxCount + " CA components are allowed");
        }
        for (var c : list) {
            var name = c.name() == null ? "" : c.name().trim();
            if (name.isEmpty()) {
                throw new com.moriba.skultem.application.error.RuleException("Every CA component needs a name");
            }
            if (name.contains("|") || name.contains(":") || name.contains(";") || name.contains("\n")) {
                throw new com.moriba.skultem.application.error.RuleException(
                        "A CA component name can't contain | : ; or a line break");
            }
            if (c.weight() < 1 || c.weight() > 99) {
                throw new com.moriba.skultem.application.error.RuleException(name + " must be worth between 1% and 99%");
            }
            if (!names.add(name.toLowerCase())) {
                throw new com.moriba.skultem.application.error.RuleException(name + " is listed twice");
            }
            if (c.fromAttendance() && clean.stream().anyMatch(CaComponent::fromAttendance)) {
                throw new com.moriba.skultem.application.error.RuleException(
                        "Only one CA part can be taken from attendance");
            }
            clean.add(new CaComponent(name, c.weight(), c.source()));
        }
        return clean;
    }

    public static int total(List<CaComponent> list) {
        return list.stream().mapToInt(CaComponent::weight).sum();
    }

    public static List<CaComponent> decode(String text) {
        var out = new ArrayList<CaComponent>();
        if (text == null || text.isBlank()) {
            return out;
        }
        for (var line : text.split("\n")) {
            var p = line.split("\\|");
            if (p.length == 2 || p.length == 3) {
                out.add(new CaComponent(p[0], Integer.parseInt(p[1]), p.length == 3 && "A".equals(p[2]) ? ATTENDANCE : MANUAL));
            }
        }
        return out;
    }
}
