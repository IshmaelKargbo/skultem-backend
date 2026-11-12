package com.moriba.skultem.application.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.repository.SchoolDocumentFieldLinkRepository;
import com.moriba.skultem.domain.vo.DocumentKind;
import com.moriba.skultem.infrastructure.compose.MComposeClient;
import com.moriba.skultem.infrastructure.compose.MComposeClient.DesignField;
import com.moriba.skultem.infrastructure.persistence.mapper.JsonMapper;

/**
 * A school links each placeholder of a design to Skultem's data: "{{advisor}} is the class teacher's name",
 * "{{session_total}} is the attendance", "the subjects table's final_grade column is the grade", or fixed text. Links
 * are kept per school, kind of document and design, and applied to the data just before it is printed. A
 * placeholder the school hasn't linked gets the data of the same name (or another name for it), as before.
 */
@Service
public class DocumentFieldLinkService {

    /** One link: a Skultem data key, or fixed text; for a list, also its columns (row key or "text:fixed"). */
    public record Link(String source, String text, Map<String, String> columns) {
    }

    /**
     * A placeholder as the page shows it, with its saved link and what it gets on its own. automaticColumns: for a
     * list, by each Skultem list it could take its rows from, which row field fills each of its columns on its own.
     */
    public record Placeholder(String name, String type, List<String> columns, Link link, String automatic,
            Map<String, Map<String, String>> automaticColumns) {
    }

    /**
     * A group of the design as the page shows it. choice: the school's (true show, false hide, null automatic).
     * follows / followsLabel: the report card setting it follows when automatic (null: the design's own default).
     */
    public record GroupChoice(String id, String name, boolean designShows, String field, Boolean choice,
            String follows, String followsLabel) {
    }

    /**
     * level: the level these links are for (null = all levels). inherited: the level has no links of its own yet,
     * so what is shown (and what prints) is the all-levels links - saving makes them the level's own.
     */
    public record Editor(String kind, String template, String templateName, boolean readyMade,
            List<Placeholder> placeholders, List<DocumentFieldCatalog.Source> sources, boolean saved,
            List<GroupChoice> groups, String level, boolean inherited) {
    }

    // A group follows no school setting any more: Automatic is what the design does. (Kept as a hook - follows()
    // returns null - so the page and the stored choices keep their shape.)
    private record Follow(String key, String label) {
    }

    static Follow follows(DocumentKind kind, MComposeClient.DesignGroup g) {
        return null;
    }

    private static final String TEXT = "text:";
    // Row placeholders that mean the same as a Skultem row field.
    private static final Map<String, List<String>> ROW_ALIASES = Map.of(
            "final_grade", List.of("grade"),
            "comment", List.of("remarks", "remark"),
            "remarks", List.of("remark"),
            "remark", List.of("remarks"),
            "mark", List.of("score"),
            "percent", List.of("score_percent"));

    private final SchoolDocumentFieldLinkRepository repo;
    private final MComposeClient compose;

    public DocumentFieldLinkService(SchoolDocumentFieldLinkRepository repo, MComposeClient compose) {
        this.repo = repo;
        this.compose = compose;
    }

    // A level's own links are kept beside the all-levels ones, under "<design>#<LEVEL>" (a design id has no "#").
    private static String stored(String template, com.moriba.skultem.domain.vo.Level level) {
        return level == null ? template : template + "#" + level.name();
    }

    private boolean hasOwn(String schoolId, DocumentKind kind, String template, com.moriba.skultem.domain.vo.Level level) {
        String key = stored(template, level);
        return repo.find(schoolId, kind, key).isPresent() || repo.findGroups(schoolId, kind, key).isPresent();
    }

    // Where a level's links come from: its own when it has them, else the all-levels ones.
    private String source(String schoolId, DocumentKind kind, String template, com.moriba.skultem.domain.vo.Level level) {
        return level != null && hasOwn(schoolId, kind, template, level) ? stored(template, level) : template;
    }

    /** Everything the linking page needs for one design. */
    public Editor editor(String schoolId, DocumentKind kind, String template) {
        return editor(schoolId, kind, template, null);
    }

    /** As above, for one level's links (a Primary report card's) - the all-levels ones until it has its own. */
    public Editor editor(String schoolId, DocumentKind kind, String template, com.moriba.skultem.domain.vo.Level level) {
        var design = compose.design(template);
        if (design == null) {
            throw new NotFoundException("That design is not on the document service");
        }
        var structure = compose.structureOf(design);
        if (structure == null) {
            throw new RuleException("The design could not be read from the document service. Try again.");
        }
        String source = source(schoolId, kind, template, level);
        boolean inherited = level != null && source.equals(template);
        var links = links(schoolId, kind, source);
        List<Placeholder> placeholders = new ArrayList<>();
        for (var f : structure) {
            var auto = DocumentFieldCatalog.automatic(kind, f.name());
            String automatic = auto != null && auto.type().equals(f.type()) ? auto.key() : null;
            Map<String, Map<String, String>> autoCols = new LinkedHashMap<>();
            if (DesignField.LIST.equals(f.type())) {
                for (var list : DocumentFieldCatalog.sources(kind)) {
                    if ("LIST".equals(list.type())) {
                        autoCols.put(list.key(), autoColumns(list, f.columns()));
                    }
                }
            }
            placeholders.add(new Placeholder(f.name(), f.type(), f.columns(), links.get(f.name()), automatic, autoCols));
        }
        var chosen = groupChoices(schoolId, kind, source);
        List<GroupChoice> groups = new ArrayList<>();
        for (var g : compose.groupsOf(design)) {
            var f = follows(kind, g);
            groups.add(new GroupChoice(g.id(), g.name(), g.show(), g.field(), chosen.get(g.id()),
                    f == null ? null : f.key(), f == null ? null : f.label()));
        }
        return new Editor(kind.key(), design.ref(), design.name(), design.readyMade(), placeholders,
                DocumentFieldCatalog.sources(kind), !links.isEmpty() || !chosen.isEmpty(), groups,
                level == null ? null : level.name(), inherited);
    }

    /** Saves the links for a design; links left empty go back to automatic. */
    public Editor save(String schoolId, DocumentKind kind, String template, Map<String, Link> links, String userId) {
        return save(schoolId, kind, template, links, null, userId);
    }

    /** Saves the links and which groups show (true), hide (false) - a group left out is automatic. */
    public Editor save(String schoolId, DocumentKind kind, String template, Map<String, Link> links,
            Map<String, Boolean> groups, String userId) {
        return save(schoolId, kind, template, null, links, groups, userId);
    }

    /** Saves the links for one level (null = all levels); a level's save makes the links its own. */
    public Editor save(String schoolId, DocumentKind kind, String template, com.moriba.skultem.domain.vo.Level level, Map<String, Link> links,
            Map<String, Boolean> groups, String userId) {
        var design = compose.design(template);
        var structure = design == null ? null : compose.structureOf(design);
        if (structure == null) {
            throw new NotFoundException("That design is not on the document service");
        }
        Map<String, DesignField> byName = new LinkedHashMap<>();
        structure.forEach(f -> byName.put(f.name(), f));
        Map<String, Link> clean = new LinkedHashMap<>();
        if (links != null) {
            for (var e : links.entrySet()) {
                var field = byName.get(e.getKey());
                Link link = e.getValue();
                if (field == null || link == null) {
                    continue; // a placeholder the design no longer has
                }
                Link checked = check(kind, field, link);
                if (checked != null) {
                    clean.put(field.name(), checked);
                }
            }
        }
        Map<String, Boolean> cleanGroups = new LinkedHashMap<>();
        if (groups != null) {
            var known = compose.groupsOf(design).stream().map(MComposeClient.DesignGroup::id).toList();
            groups.forEach((id, show) -> {
                if (show != null && known.contains(id)) {
                    cleanGroups.put(id, show);
                }
            });
        } else {
            cleanGroups.putAll(groupChoices(schoolId, kind, source(schoolId, kind, template, level)));
        }
        String key = stored(template, level);
        // All automatic for all levels: nothing to keep. A level keeps even an empty set, as its own.
        if (level == null && clean.isEmpty() && cleanGroups.isEmpty()) {
            repo.delete(schoolId, kind, key);
        } else {
            repo.saveAll(schoolId, kind, key, JsonMapper.toJson(clean), JsonMapper.toJson(cleanGroups), userId);
        }
        return editor(schoolId, kind, template, level);
    }

    /** The level goes back to the all-levels links. */
    public Editor useAllLevels(String schoolId, DocumentKind kind, String template, com.moriba.skultem.domain.vo.Level level) {
        if (level != null) {
            repo.delete(schoolId, kind, stored(template, level));
        }
        return editor(schoolId, kind, template, level);
    }

    /** What to do to each record's data before printing it with this design (nothing when nothing is linked). */
    public UnaryOperator<Map<String, Object>> linker(String schoolId, DocumentKind kind, String template) {
        return linker(schoolId, kind, template, null);
    }

    /** As above, with the level's own links when it has them (a Primary card's "Head Teacher"). */
    public UnaryOperator<Map<String, Object>> linker(String schoolId, DocumentKind kind, String template,
            com.moriba.skultem.domain.vo.Level level) {
        String source = template == null ? null : source(schoolId, kind, template, level);
        var links = links(schoolId, kind, source);
        // Lists the school hasn't linked, and that Skultem doesn't already send under their own name, still get
        // their columns filled from the matching row fields.
        Map<String, Link> automatic = new LinkedHashMap<>();
        // Text placeholders with no data at all: sent as "" so they print blank, not as "{{name}}".
        java.util.Set<String> textPlaceholders = new java.util.HashSet<>();
        var design = template == null || template.isBlank() ? null : compose.design(template);
        var structure = design == null ? null : compose.structureOf(design);
        if (structure != null) {
            // Every placeholder the school hasn't linked gets what the page shows as "Automatic" (same type only).
            for (var f : structure) {
                if (DesignField.TEXT.equals(f.type())) {
                    textPlaceholders.add(f.name());
                }
                if (links.containsKey(f.name())) {
                    continue;
                }
                var auto = DocumentFieldCatalog.automatic(kind, f.name());
                if (auto == null || !auto.type().equals(f.type())) {
                    continue;
                }
                if (DesignField.LIST.equals(f.type())) {
                    var cols = autoColumns(auto, f.columns());
                    if (!cols.isEmpty()) {
                        automatic.put(f.name(), new Link(auto.key(), null, cols));
                    }
                } else if (!auto.key().equals(f.name())) {
                    automatic.put(f.name(), new Link(auto.key(), null, null));
                }
            }
        }
        // The design's groups: the school's choice, else the report card setting the group follows (in the data).
        var chosenGroups = groupChoices(schoolId, kind, source);
        List<MComposeClient.DesignGroup> designGroups = design == null ? List.of() : compose.groupsOf(design);
        if (links.isEmpty() && automatic.isEmpty() && textPlaceholders.isEmpty() && designGroups.isEmpty()) {
            return UnaryOperator.identity();
        }
        return input -> {
            Map<String, Object> withGroups = new LinkedHashMap<>(input);
            Map<String, Object> shown = new LinkedHashMap<>();
            if (input.get("groups") instanceof Map<?, ?> given) {
                given.forEach((k, v) -> shown.put(String.valueOf(k), v));
            }
            for (var g : designGroups) {
                Boolean choice = chosenGroups.get(g.id());
                if (choice == null) {
                    var f = follows(kind, g);
                    if (f != null && input.get(f.key()) instanceof Boolean setting) {
                        choice = setting;
                    }
                }
                if (choice != null) {
                    shown.put(g.id(), choice);
                }
            }
            if (!shown.isEmpty()) {
                withGroups.put("groups", shown);
            }
            final Map<String, Object> data = withGroups;
            Map<String, Link> all = new LinkedHashMap<>();
            automatic.forEach((name, link) -> {
                if (!data.containsKey(name) || name.equals(link.source())) {
                    all.put(name, link);
                }
            });
            all.putAll(links);
            Map<String, Object> out = apply(data, all);
            for (String name : textPlaceholders) {
                out.putIfAbsent(name, "");
            }
            return out;
        };
    }

    static Map<String, Object> apply(Map<String, Object> data, Map<String, Link> links) {
        Map<String, Object> out = new LinkedHashMap<>(data);
        for (var e : links.entrySet()) {
            Link link = e.getValue();
            if (link.text() != null) {
                out.put(e.getKey(), link.text());
                continue;
            }
            if (link.source() == null) {
                continue;
            }
            if (link.source().isEmpty()) {
                out.put(e.getKey(), "");
                continue;
            }
            Object value = data.get(link.source());
            if (value instanceof List<?> rows && link.columns() != null && !link.columns().isEmpty()) {
                List<Map<String, Object>> mapped = new ArrayList<>();
                for (Object r : rows) {
                    if (!(r instanceof Map<?, ?> row)) {
                        continue;
                    }
                    Map<String, Object> m = new LinkedHashMap<>();
                    row.forEach((k, v) -> m.put(String.valueOf(k), v));
                    for (var c : link.columns().entrySet()) {
                        String src = c.getValue();
                        m.put(c.getKey(), src == null || src.isEmpty() ? ""
                                : src.startsWith(TEXT) ? src.substring(TEXT.length()) : row.get(src));
                    }
                    mapped.add(m);
                }
                value = mapped;
            }
            out.put(e.getKey(), value == null ? "" : value);
        }
        return out;
    }

    // ---- Helpers

    private Map<String, Boolean> groupChoices(String schoolId, DocumentKind kind, String template) {
        if (schoolId == null || template == null || template.isBlank()) {
            return Map.of();
        }
        return repo.findGroups(schoolId, kind, template)
                .map(json -> JsonMapper.fromJson(json, new TypeReference<LinkedHashMap<String, Boolean>>() {
                }))
                .map(m -> (Map<String, Boolean>) m)
                .orElse(Map.of());
    }

    private Map<String, Link> links(String schoolId, DocumentKind kind, String template) {
        if (schoolId == null || template == null || template.isBlank()) {
            return Map.of();
        }
        return repo.find(schoolId, kind, template)
                .map(json -> JsonMapper.fromJson(json, new TypeReference<LinkedHashMap<String, Link>>() {
                }))
                .map(m -> (Map<String, Link>) m)
                .orElse(Map.of());
    }

    // Only links that make sense: data of the same type (text to text, picture to picture, list to list), real
    // columns. A picture can't be fixed text; a blank picture prints as nothing.
    private static Link check(DocumentKind kind, DesignField field, Link link) {
        if (link.text() != null) {
            if (!DesignField.TEXT.equals(field.type())) {
                throw new RuleException("{{" + field.name() + "}} can't be fixed text");
            }
            if (link.text().length() > 500) {
                throw new RuleException("The text for {{" + field.name() + "}} is too long");
            }
            return new Link(null, link.text(), null);
        }
        if (link.source() == null) {
            return null;
        }
        if (link.source().isEmpty()) {
            if (DesignField.IMAGE.equals(field.type())) {
                return null; // pictures stay as Skultem sends them
            }
            return new Link("", null, null);
        }
        var source = DocumentFieldCatalog.find(kind, link.source());
        if (source == null) {
            throw new RuleException("Unknown data for {{" + field.name() + "}}");
        }
        if (!source.type().equals(field.type())) {
            throw new RuleException("{{" + field.name() + "}} needs " + describe(field.type()) + ", not "
                    + describe(source.type()));
        }
        Map<String, String> columns = new LinkedHashMap<>();
        if (DesignField.LIST.equals(field.type())) {
            // Columns left automatic get the matching row field of the chosen list, so they print too.
            columns.putAll(autoColumns(source, field.columns()));
        }
        if (link.columns() != null && DesignField.LIST.equals(field.type())) {
            for (var c : link.columns().entrySet()) {
                if (!field.columns().contains(c.getKey()) || c.getValue() == null) {
                    continue;
                }
                String v = c.getValue();
                boolean known = v.isEmpty() || v.startsWith(TEXT)
                        || source.columns().stream().anyMatch(col -> col.key().equals(v));
                if (!known) {
                    throw new RuleException("Unknown column data for " + c.getKey());
                }
                columns.put(c.getKey(), v);
            }
        }
        return new Link(source.key(), null, columns.isEmpty() ? null : columns);
    }

    private static Map<String, String> autoColumns(DocumentFieldCatalog.Source list, List<String> designColumns) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String col : designColumns) {
            String match = rowMatch(list, col);
            if (match != null) {
                out.put(col, match);
            }
        }
        return out;
    }

    private static String rowMatch(DocumentFieldCatalog.Source list, String column) {
        if (list.columns().stream().anyMatch(c -> c.key().equals(column))) {
            return column;
        }
        for (String alias : ROW_ALIASES.getOrDefault(column, List.of())) {
            if (list.columns().stream().anyMatch(c -> c.key().equals(alias))) {
                return alias;
            }
        }
        return null;
    }

    private static String describe(String type) {
        return switch (type) {
            case "IMAGE" -> "a picture";
            case "LIST" -> "a list";
            default -> "text";
        };
    }
}
