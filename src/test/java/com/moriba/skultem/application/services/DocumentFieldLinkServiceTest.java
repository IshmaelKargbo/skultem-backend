package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.DocumentFieldLinkService.Link;
import com.moriba.skultem.domain.repository.SchoolDocumentFieldLinkRepository;
import com.moriba.skultem.domain.vo.DocumentKind;
import com.moriba.skultem.infrastructure.compose.MComposeClient;
import com.moriba.skultem.infrastructure.compose.MComposeClient.Design;
import com.moriba.skultem.infrastructure.compose.MComposeClient.DesignField;

class DocumentFieldLinkServiceTest {

    private final Map<String, String> stored = new HashMap<>();
    private final Map<String, String> groups = new HashMap<>();
    private final MComposeClient compose = mock(MComposeClient.class);
    private DocumentFieldLinkService service;

    // Like the navy sidebar report card: its own names for the school, the class teacher and the subjects table.
    private static final Design NAVY = new Design("navy-1", "navy-1", "Report card: navy sidebar", "", "Report card",
            false, false, 1);

    @BeforeEach
    void setUp() {
        SchoolDocumentFieldLinkRepository repo = new SchoolDocumentFieldLinkRepository() {
            public Optional<String> find(String s, DocumentKind k, String t) {
                return Optional.ofNullable(stored.get(s + k + t));
            }

            public void save(String s, DocumentKind k, String t, String json, String by) {
                stored.put(s + k + t, json);
            }

            public void delete(String s, DocumentKind k, String t) {
                stored.remove(s + k + t);
                groups.remove(s + k + t);
            }

            public Optional<String> findGroups(String s, DocumentKind k, String t) {
                return Optional.ofNullable(groups.get(s + k + t));
            }

            public void saveAll(String s, DocumentKind k, String t, String links, String g, String by) {
                if ("{}".equals(links)) {
                    stored.remove(s + k + t);
                } else {
                    stored.put(s + k + t, links);
                }
                groups.put(s + k + t, g);
            }
        };
        when(compose.groupsOf(any())).thenReturn(List.of(
                new MComposeClient.DesignGroup("position", "Position in class", true, "show_position"),
                new MComposeClient.DesignGroup("promot-to", "promot_to", true, "show_promot_to")));
        when(compose.design("navy-1")).thenReturn(NAVY);
        when(compose.structureOf(any())).thenReturn(List.of(
                new DesignField("school", DesignField.TEXT, List.of()),
                new DesignField("advisor", DesignField.TEXT, List.of()),
                new DesignField("motto_line", DesignField.TEXT, List.of()),
                new DesignField("photo", DesignField.IMAGE, List.of()),
                new DesignField("subjects", DesignField.LIST, List.of("subject", "final_grade", "comment", "effort"))));
        service = new DocumentFieldLinkService(repo, compose);
    }

    private static Map<String, Object> data() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("organisation", "Moriba International School");
        d.put("teacher", "Ms. A. Conteh");
        d.put("school_motto", "Learn and serve");
        d.put("results", List.of(Map.of("subject", "Mathematics", "grade", "A", "remarks", "Excellent", "score", "88")));
        return d;
    }

    @Test
    void showsEachPlaceholderWithTheDataItGetsOnItsOwn() {
        var editor = service.editor("s1", DocumentKind.REPORT_CARD, "navy-1");

        var byName = new HashMap<String, DocumentFieldLinkService.Placeholder>();
        editor.placeholders().forEach(p -> byName.put(p.name(), p));
        assertThat(byName.get("school").automatic()).isEqualTo("organisation");
        assertThat(byName.get("advisor").automatic()).isEqualTo("teacher");
        assertThat(byName.get("motto_line").automatic()).isNull(); // the school links this one
        assertThat(byName.get("subjects").automatic()).isEqualTo("results");
        assertThat(byName.get("subjects").automaticColumns().get("results"))
                .containsEntry("subject", "subject").containsEntry("final_grade", "grade")
                // Report card rows carry no remark, so a "comment" column isn't filled on its own.
                .doesNotContainKey("comment").doesNotContainKey("effort");
        assertThat(editor.sources()).isNotEmpty();
        assertThat(editor.saved()).isFalse();
    }

    @Test
    void linksChangeWhatThePlaceholdersPrint() {
        Map<String, Link> links = new LinkedHashMap<>();
        links.put("motto_line", new Link("school_motto", null, null));
        links.put("advisor", new Link(null, "Mr. Head of Year", null));
        links.put("subjects", new Link("results", null, Map.of("final_grade", "score", "effort", "text:Good")));
        service.save("s1", DocumentKind.REPORT_CARD, "navy-1", links, "u1");

        var out = service.linker("s1", DocumentKind.REPORT_CARD, "navy-1").apply(data());

        assertThat(out.get("motto_line")).isEqualTo("Learn and serve");
        assertThat(out.get("advisor")).isEqualTo("Mr. Head of Year");
        @SuppressWarnings("unchecked")
        var rows = (List<Map<String, Object>>) out.get("subjects");
        assertThat(rows.get(0)).containsEntry("subject", "Mathematics").containsEntry("final_grade", "88")
                .containsEntry("effort", "Good");
        // Everything Skultem sends is still there for placeholders that weren't linked.
        assertThat(out.get("organisation")).isEqualTo("Moriba International School");
    }

    @Test
    void aPlaceholderWithNoDataPrintsBlankNotItsName() {
        // motto_line has no data and no link: it must not print as "{{motto_line}}".
        var out = service.linker("s1", DocumentKind.REPORT_CARD, "navy-1").apply(data());
        assertThat(out.get("motto_line")).isEqualTo("");
    }

    @Test
    void aGroupDoesWhatTheDesignDoesUntilTheSchoolChooses() {
        var editor = service.editor("s1", DocumentKind.REPORT_CARD, "navy-1");
        // No group follows a setting any more.
        assertThat(editor.groups()).extracting(DocumentFieldLinkService.GroupChoice::follows).containsOnlyNulls();

        Map<String, Object> data = new LinkedHashMap<>(data());
        // Nothing chosen: no group is switched, so each keeps the design's default.
        assertThat(service.linker("s1", DocumentKind.REPORT_CARD, "navy-1").apply(data)).doesNotContainKey("groups");

        // The school's own choice wins.
        service.save("s1", DocumentKind.REPORT_CARD, "navy-1", Map.of(), Map.of("position", true, "promot-to", false), "u1");
        @SuppressWarnings("unchecked")
        var chosen = (Map<String, Object>) service.linker("s1", DocumentKind.REPORT_CARD, "navy-1").apply(data).get("groups");
        assertThat(chosen).containsEntry("position", true).containsEntry("promot-to", false);
    }

    @Test
    void primaryCanSayHeadTeacherWhileJssSaysPrincipal() {
        var primary = com.moriba.skultem.domain.vo.Level.PRIMARY;
        var jss = com.moriba.skultem.domain.vo.Level.JSS;
        // All levels: "Principal".
        service.save("s1", DocumentKind.REPORT_CARD, "navy-1", Map.of("motto_line", new Link(null, "Principal", null)), "u1");
        // Primary has its own: "Head Teacher".
        var before = service.editor("s1", DocumentKind.REPORT_CARD, "navy-1", primary);
        assertThat(before.inherited()).isTrue();
        service.save("s1", DocumentKind.REPORT_CARD, "navy-1", primary,
                Map.of("motto_line", new Link(null, "Head Teacher", null)), Map.of(), "u1");

        assertThat(service.linker("s1", DocumentKind.REPORT_CARD, "navy-1", primary).apply(data()).get("motto_line"))
                .isEqualTo("Head Teacher");
        assertThat(service.linker("s1", DocumentKind.REPORT_CARD, "navy-1", jss).apply(data()).get("motto_line"))
                .isEqualTo("Principal");
        assertThat(service.editor("s1", DocumentKind.REPORT_CARD, "navy-1", primary).inherited()).isFalse();

        // Primary goes back to the all-levels links.
        service.useAllLevels("s1", DocumentKind.REPORT_CARD, "navy-1", primary);
        assertThat(service.linker("s1", DocumentKind.REPORT_CARD, "navy-1", primary).apply(data()).get("motto_line"))
                .isEqualTo("Principal");
    }

    @Test
    void anotherSchoolOrDesignIsNotAffected() {
        service.save("s1", DocumentKind.REPORT_CARD, "navy-1", Map.of("advisor", new Link(null, "X", null)), "u1");
        // Another school, or another kind of document, gets the automatic data (the class teacher), not "X".
        assertThat(service.linker("s2", DocumentKind.REPORT_CARD, "navy-1").apply(data()).get("advisor"))
                .isEqualTo("Ms. A. Conteh");
        assertThat(service.linker("s1", DocumentKind.EXAM_RESULT_SLIP, "navy-1").apply(data()).get("advisor"))
                .isNotEqualTo("X");
    }

    @Test
    void refusesLinksThatCantWork() {
        assertThatThrownBy(() -> service.save("s1", DocumentKind.REPORT_CARD, "navy-1",
                Map.of("photo", new Link(null, "text", null)), "u1")).isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> service.save("s1", DocumentKind.REPORT_CARD, "navy-1",
                Map.of("advisor", new Link("results", null, null)), "u1"))
                .isInstanceOf(RuleException.class).hasMessageContaining("needs text, not a list");
        assertThatThrownBy(() -> service.save("s1", DocumentKind.REPORT_CARD, "navy-1",
                Map.of("advisor", new Link("nope", null, null)), "u1")).isInstanceOf(RuleException.class);
        assertThat(stored).isEmpty();
    }

    @Test
    void clearingEveryLinkGoesBackToAutomatic() {
        service.save("s1", DocumentKind.REPORT_CARD, "navy-1", Map.of("advisor", new Link(null, "X", null)), "u1");
        service.save("s1", DocumentKind.REPORT_CARD, "navy-1", Map.of(), "u1");
        assertThat(stored).isEmpty();
    }
}
