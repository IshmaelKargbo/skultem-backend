package com.moriba.skultem.infrastructure.compose;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moriba.skultem.application.error.DocumentServiceException;

/**
 * Turns Skultem data into PDFs with M-Compose, the document engine. Skultem has its own account there and calls it
 * with that account's key (a "renderer" key is enough), so the key never reaches a browser.
 *
 * A template is named one of two ways:
 * - a saved template's id ("8f3c..."): POST /templates/{id}/render, the normal case once staff have designed one;
 * - "sample:<id>" for one of the engine's ready-made designs ("sample:report-card-classic"): fetched with
 *   GET /samples/{id} and rendered as it is. Handy to get started before anyone has designed one.
 *
 * Off (isEnabled() false) until m-compose.url and m-compose.api-key are set.
 */
@Component
public class MComposeClient {
    private static final Logger log = LoggerFactory.getLogger(MComposeClient.class);
    private static final String SAMPLE_PREFIX = "sample:";
    // The engine's ids are letters, digits, dashes and underscores; anything else must not reach a URL path.
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,128}");

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http;
    private final String baseUrl;
    private final String apiKey;
    private final Duration timeout;

    public MComposeClient(
            @Value("${m-compose.url:}") String baseUrl,
            @Value("${m-compose.api-key:}") String apiKey,
            @Value("${m-compose.timeout-seconds:60}") long timeoutSeconds) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public boolean isEnabled() {
        return !baseUrl.isEmpty() && !apiKey.isEmpty();
    }

    /** What a connection check found, in words for platform staff. */
    public record Check(boolean configured, boolean reachable, boolean keyAccepted, String message) {
    }

    /** Is the engine there, and does it accept Skultem's key? Never throws. */
    public Check check() {
        if (!isEnabled()) {
            return new Check(false, false, false,
                    "Not set up: MCOMPOSE_URL and MCOMPOSE_API_KEY are needed on the server.");
        }
        try {
            var health = http.send(HttpRequest.newBuilder(URI.create(baseUrl + "/health"))
                    .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.discarding());
            if (health.statusCode() >= 400) {
                return new Check(true, false, false, "The engine answered " + health.statusCode() + " at " + baseUrl);
            }
            var auth = http.send(request("/templates").timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            int status = auth.statusCode();
            if (status == 401 || status == 403) {
                return new Check(true, true, false,
                        "The engine is running but refused Skultem's key (revoked, wrong, or account switched off).");
            }
            if (status >= 400) {
                return new Check(true, true, false, "The engine answered " + status + " when checking the key.");
            }
            return new Check(true, true, true, "Connected to " + baseUrl);
        } catch (IOException e) {
            return new Check(true, false, false, "The engine can't be reached at " + baseUrl);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Check(true, false, false, "The check was interrupted");
        }
    }

    /** A design a school can print with: one of the engine's ready-made ones, or one saved in Skultem's account. */
    public record Design(String ref, String id, String name, String description, String category, boolean readyMade,
            boolean hasPreview, int revision) {
    }

    /** A group of a design: elements shown or hidden together. field: the data switch the design set for it. */
    public record DesignGroup(String id, String name, boolean show, String field) {
    }

    /** The design's groups (none when it has none, or it can't be read). */
    public List<DesignGroup> groupsOf(Design design) {
        try {
            JsonNode groups = designJson(design.ref()).path("groups");
            List<DesignGroup> out = new java.util.ArrayList<>();
            if (groups.isArray()) {
                for (JsonNode g : groups) {
                    String id = g.path("id").asText("");
                    if (!id.isBlank()) {
                        out.add(new DesignGroup(id, g.path("name").asText(id), g.path("show").asBoolean(true),
                                g.path("field").asText("")));
                    }
                }
            }
            return out;
        } catch (DocumentServiceException e) {
            return List.of();
        }
    }

    /** A placeholder in a design: text, a picture, or a list (a table or repeat) with its own row fields. */
    public record DesignField(String name, String type, List<String> columns) {
        public static final String TEXT = "TEXT", IMAGE = "IMAGE", LIST = "LIST";
    }

    // Each design's placeholders, by ref@revision (a saved design changes revision when it is edited).
    private final java.util.Map<String, List<DesignField>> structureCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Pattern BINDING = Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*)");
    private static final java.util.Set<String> RESERVED = java.util.Set.of("page", "pages");

    /** The data fields a design fills in ({{student}}, {{results}}, ...), or null if the design can't be read. */
    public java.util.Set<String> fieldsOf(Design design) {
        var structure = structureOf(design);
        if (structure == null) {
            return null;
        }
        java.util.Set<String> names = new java.util.TreeSet<>();
        structure.forEach(f -> names.add(f.name()));
        return names;
    }

    /** The design with this ref, or null. */
    public Design design(String ref) {
        return designs().stream().filter(d -> d.ref().equals(ref)).findFirst().orElse(null);
    }

    /**
     * A design's placeholders: pictures (an image's source, a shape's background picture), lists (a table's data
     * with its columns, a repeat's collection with the fields its rows use) and text (everything else).
     */
    public List<DesignField> structureOf(Design design) {
        String key = design.ref() + "@" + design.revision();
        var cached = structureCache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            String path = design.readyMade() ? "/samples/" + id(design.id()) : "/templates/" + id(design.id());
            JsonNode template = json.readTree(send(request(path).GET().build()).body()).get("template");
            if (template == null) {
                return null;
            }
            java.util.Map<String, DesignField> fields = new java.util.TreeMap<>();
            walk(template.path("elements"), fields);
            var result = List.copyOf(fields.values());
            structureCache.put(key, result);
            return result;
        } catch (IOException | DocumentServiceException e) {
            return null;
        }
    }

    private void walk(JsonNode elements, java.util.Map<String, DesignField> out) {
        if (elements == null || !elements.isArray()) {
            return;
        }
        for (JsonNode e : elements) {
            String type = e.path("type").asText();
            JsonNode props = e.path("properties");
            if ("table".equals(type) && bound(props.path("data").asText()) != null) {
                List<String> cols = new java.util.ArrayList<>();
                props.path("columns").forEach(c -> {
                    String f = c.path("field").asText("");
                    if (!f.isBlank() && !cols.contains(f)) {
                        cols.add(f);
                    }
                });
                out.put(bound(props.path("data").asText()), new DesignField(bound(props.path("data").asText()),
                        DesignField.LIST, cols));
                continue;
            }
            if ("repeat".equals(type) && bound(props.path("collection").asText()) != null) {
                java.util.Set<String> cols = new java.util.LinkedHashSet<>();
                collect(e.path("children"), cols);
                String name = bound(props.path("collection").asText());
                out.put(name, new DesignField(name, DesignField.LIST, List.copyOf(cols)));
                continue;
            }
            String picture = "image".equals(type) ? bound(props.path("source").asText())
                    : bound(props.path("background_image").asText());
            if (picture != null && !(out.get(picture) != null && DesignField.LIST.equals(out.get(picture).type()))) {
                out.put(picture, new DesignField(picture, DesignField.IMAGE, List.of()));
            }
            java.util.Set<String> texts = new java.util.LinkedHashSet<>();
            try {
                var m = BINDING.matcher(json.writeValueAsString(props));
                while (m.find()) {
                    texts.add(m.group(1));
                }
            } catch (IOException ignored) {
                // a node always serializes
            }
            for (String t : texts) {
                if (!RESERVED.contains(t) && !t.equals(picture)) {
                    out.putIfAbsent(t, new DesignField(t, DesignField.TEXT, List.of()));
                }
            }
            walk(e.path("children"), out);
        }
    }

    // Every binding inside a repeat's children (the fields of one row).
    private void collect(JsonNode children, java.util.Set<String> out) {
        try {
            var m = BINDING.matcher(json.writeValueAsString(children));
            while (m.find()) {
                if (!RESERVED.contains(m.group(1))) {
                    out.add(m.group(1));
                }
            }
        } catch (IOException ignored) {
            // a node always serializes
        }
    }

    // "{{results}}" -> "results"; anything else (a fixed source like "stock:seal-blue") -> null.
    private static String bound(String value) {
        if (value == null) {
            return null;
        }
        var m = Pattern.compile("^\\s*\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*(\\|[^}]*)?}}\\s*$").matcher(value);
        return m.matches() ? m.group(1) : null;
    }

    /** Every template saved in Skultem's account on the engine. */
    public List<Design> designs() {
        var cached = designsCache;
        if (cached != null && cached.at().plusSeconds(60).isAfter(java.time.Instant.now())) {
            return cached.list();
        }
        // Only the templates created in Skultem's own account: the engine's ready-made gallery is not offered to
        // schools ("sample:<id>" still renders when the server is set to one, but is never listed).
        List<Design> out = new java.util.ArrayList<>();
        for (JsonNode t : listOf(send(request("/templates").GET().build()).body(), "templates")) {
            String id = t.path("id").asText();
            String name = t.path("name").asText(id);
            out.add(new Design(id, id, name, t.path("description").asText(""), category(name), false, true,
                    t.path("revision").asInt(0)));
        }
        designsCache = new CachedDesigns(List.copyOf(out), java.time.Instant.now());
        return designsCache.list();
    }

    // The design list for a minute: a whole class's documents, or the picker, don't ask the engine each time.
    private record CachedDesigns(List<Design> list, java.time.Instant at) {
    }

    private volatile CachedDesigns designsCache;

    /** A saved template's picture (its thumbnail), or null when it has none. */
    public byte[] designPreview(String templateId) {
        try {
            return send(request("/templates/" + id(templateId) + "/thumbnail").GET().build()).body();
        } catch (DocumentServiceException e) {
            return null;
        }
    }

    // "Report card: classic" -> "Report card", as the engine names categories.
    private static String category(String name) {
        int colon = name == null ? -1 : name.indexOf(':');
        return colon > 0 ? name.substring(0, colon).trim() : "";
    }

    private List<JsonNode> listOf(byte[] body, String field) {
        try {
            JsonNode node = json.readTree(body).get(field);
            List<JsonNode> items = new java.util.ArrayList<>();
            if (node != null && node.isArray()) {
                node.forEach(items::add);
            }
            return items;
        } catch (IOException e) {
            throw new DocumentServiceException("The document service sent back something unexpected", e);
        }
    }

    /**
     * A design's preview: the data given, and placeholders for whatever is missing (the engine's own preview, which
     * never fails on a missing picture). For showing a design before a real document exists.
     */
    public byte[] preview(String template, Map<String, Object> data) {
        return preview(template, data, null);
    }

    /** As above, with the design changed for this print (e.g. the results table's columns); null = as it is. */
    public byte[] preview(String template, Map<String, Object> data, java.util.function.Consumer<ObjectNode> adjust) {
        var inline = prepared(template, wantsBrand(data), adjust);
        if (inline != null) {
            return post("/preview", body("template", inline, "data", data));
        }
        return post("/templates/" + id(template) + "/preview", body("data", data));
    }

    /** One document: the template filled with this data. */
    public byte[] render(String template, Map<String, Object> data) {
        return render(template, data, null);
    }

    /** As above, with the design changed for this print (e.g. the results table's columns); null = as it is. */
    public byte[] render(String template, Map<String, Object> data, java.util.function.Consumer<ObjectNode> adjust) {
        var inline = prepared(template, wantsBrand(data), adjust);
        if (inline != null) {
            return post("/render", body("template", inline, "data", data));
        }
        return post("/templates/" + id(template) + "/render", body("data", data));
    }

    /** One PDF holding the template once per record, in order (a class's report cards, say). */
    public byte[] renderBatch(String template, Map<String, Object> shared, List<Map<String, Object>> records) {
        return renderBatch(template, shared, records, null);
    }

    public byte[] renderBatch(String template, Map<String, Object> shared, List<Map<String, Object>> records,
            java.util.function.Consumer<ObjectNode> adjust) {
        if (records == null || records.isEmpty()) {
            throw new DocumentServiceException("There is nothing to put in the document");
        }
        Map<String, Object> data = shared == null ? Map.of() : shared;
        var inline = prepared(template, wantsBrand(data) || wantsBrand(records.get(0)), adjust);
        if (inline != null) {
            return post("/render/batch", body("template", inline, "data", data, "records", records));
        }
        return post("/templates/" + id(template) + "/render/batch", body("data", data, "records", records));
    }

    // ---- Changing a design for one print: the school's colours (TemplateBranding), and any adjustment

    // Designs as fetched, and branded, by ref@revision: a saved design changes revision when it is edited.
    private final java.util.Map<String, ObjectNode> designCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, TemplateBranding.Result> brandingCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean wantsBrand(Map<String, Object> data) {
        return data != null && data.get("colors") instanceof Map<?, ?> colors && !colors.isEmpty();
    }

    /**
     * The design to send with the data, or null to render the saved design as it is (by id - nothing to change).
     * A ready-made "sample:" design is always sent. If the design can't be read, it prints as it is.
     */
    private ObjectNode prepared(String template, boolean colours, java.util.function.Consumer<ObjectNode> adjust) {
        if (template == null || template.isBlank()) {
            throw new DocumentServiceException("No document design is chosen");
        }
        try {
            ObjectNode base = isSample(template) || colours || adjust != null ? designJson(template) : null;
            if (base == null) {
                return null;
            }
            boolean changed = isSample(template);
            if (colours) {
                final ObjectNode original = base;
                var branded = brandingCache.computeIfAbsent(cacheKey(template), k -> TemplateBranding.brand(original));
                if (branded.changed()) {
                    changed = true;
                }
                base = branded.template();
            }
            if (adjust != null) {
                ObjectNode copy = base.deepCopy();
                adjust.accept(copy);
                if (!copy.equals(base)) {
                    return copy;
                }
            }
            return changed ? base : null;
        } catch (DocumentServiceException e) {
            if (isSample(template)) {
                throw e;
            }
            log.warn("Could not prepare {} for printing, printing it as it is: {}", template, e.getMessage());
            return null;
        }
    }

    private ObjectNode designJson(String template) {
        return designCache.computeIfAbsent(cacheKey(template), k -> {
            JsonNode t = isSample(template) ? sampleTemplate(template) : storedTemplate(template);
            if (t instanceof ObjectNode o) {
                return o;
            }
            throw new DocumentServiceException("The document design could not be read");
        });
    }

    private String cacheKey(String template) {
        if (isSample(template)) {
            return template;
        }
        int revision = designs().stream().filter(d -> d.ref().equals(template)).mapToInt(Design::revision)
                .findFirst().orElse(-1);
        return template + "@" + revision;
    }

    private JsonNode storedTemplate(String template) {
        try {
            return json.readTree(send(request("/templates/" + id(template)).GET().build()).body()).get("template");
        } catch (IOException e) {
            throw new DocumentServiceException("The document service sent back something unexpected", e);
        }
    }

    private static boolean isSample(String template) {
        return template != null && template.startsWith(SAMPLE_PREFIX);
    }

    private JsonNode sampleTemplate(String template) {
        String sampleId = id(template.substring(SAMPLE_PREFIX.length()));
        HttpResponse<byte[]> res = send(request("/samples/" + sampleId).GET().build());
        try {
            JsonNode node = json.readTree(res.body()).get("template");
            if (node == null || !node.isObject()) {
                throw new DocumentServiceException("The document service has no design called " + sampleId);
            }
            return node;
        } catch (IOException e) {
            throw new DocumentServiceException("The document service sent back something unexpected", e);
        }
    }

    private byte[] post(String path, Map<String, Object> body) {
        byte[] payload;
        try {
            payload = json.writeValueAsBytes(body);
        } catch (IOException e) {
            throw new DocumentServiceException("Could not prepare the document data", e);
        }
        HttpResponse<byte[]> res = send(request(path)
                .header("Content-Type", "application/json")
                .header("Accept", "application/pdf")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build());
        String type = res.headers().firstValue("Content-Type").orElse("");
        if (!type.startsWith("application/pdf")) {
            throw new DocumentServiceException("The document service did not send back a PDF");
        }
        return res.body();
    }

    private HttpRequest.Builder request(String path) {
        if (!isEnabled()) {
            throw new DocumentServiceException("PDF documents are not set up on this server yet");
        }
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey);
    }

    private HttpResponse<byte[]> send(HttpRequest request) {
        HttpResponse<byte[]> res;
        try {
            res = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            log.warn("M-Compose at {} could not be reached: {}", baseUrl, e.getMessage());
            throw new DocumentServiceException("The document service can't be reached right now. Please try again.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DocumentServiceException("Making the document was interrupted", e);
        }
        int status = res.statusCode();
        if (status < 400) {
            return res;
        }
        String reason = errorMessage(res.body());
        log.warn("M-Compose refused {} {}: {} {}", request.method(), request.uri().getPath(), status, reason);
        if (status == 401 || status == 403) {
            // Skultem's own key is wrong, revoked, too weak, or its account is switched off - not the user's fault.
            throw new DocumentServiceException("The document service did not accept this server's key. "
                    + "Ask the platform team to check the M-Compose settings.");
        }
        if (status == 404) {
            throw new DocumentServiceException("The document design was not found on the document service");
        }
        throw new DocumentServiceException(reason.isEmpty()
                ? "The document service could not make this document (" + status + ")"
                : "The document service could not make this document: " + reason);
    }

    // The engine answers errors as {"error": "...", "message": "..."}.
    private String errorMessage(byte[] body) {
        try {
            JsonNode node = json.readTree(body);
            JsonNode message = node == null ? null : node.get("message");
            if (message != null && message.isTextual()) {
                return message.asText();
            }
            JsonNode error = node == null ? null : node.get("error");
            return error != null && error.isTextual() ? error.asText() : "";
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }

    private static String id(String value) {
        if (value == null || !ID.matcher(value).matches()) {
            throw new DocumentServiceException("The document design is not named correctly in the server settings");
        }
        return value;
    }

    private static Map<String, Object> body(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
