package com.moriba.skultem.infrastructure.compose;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Lets a report card design's results table take its columns from the data. Skultem sends the card's own columns
 * as "columns" (Subject, Test 1 (50), Test 2 (50), Total, Grade - see ReportCardDocumentData), and M-Compose
 * builds the table from them for a table whose columns_from is "{{columns}}" - as its school report card designs
 * have. A design drawn before that (fixed columns: Subject, Score, Grade, Remarks) gets columns_from on the copy that
 * is printed, so it shows the card's columns too. The saved design is not changed, and a table that already takes
 * its columns from the data is left as it is.
 */
public final class ResultTableColumns {

    public static final String BINDING = "{{columns}}";

    private ResultTableColumns() {
    }

    /** Points every results table ({{results}} or {{subjects}}) without its own columns_from at the data's columns. */
    public static boolean apply(ObjectNode template) {
        boolean changed = false;
        for (ObjectNode table : tables(template.path("elements"))) {
            if (!(table.get("properties") instanceof ObjectNode props)) {
                continue;
            }
            String data = props.path("data").asText("").replace(" ", "");
            boolean results = data.startsWith("{{results") || data.startsWith("{{subjects");
            if (!results || !props.path("columns_from").asText("").isBlank()) {
                continue;
            }
            props.put("columns_from", BINDING);
            changed = true;
        }
        return changed;
    }

    private static List<ObjectNode> tables(JsonNode elements) {
        List<ObjectNode> out = new ArrayList<>();
        if (elements != null && elements.isArray()) {
            for (JsonNode e : elements) {
                if (e instanceof ObjectNode o) {
                    if ("table".equals(o.path("type").asText())) {
                        out.add(o);
                    }
                    out.addAll(tables(o.path("children")));
                }
            }
        }
        return out;
    }
}
