package com.moriba.skultem.infrastructure.compose;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

// The same results as M-Compose's tools/branding.py gives for this design (checked side by side).
class TemplateBrandingTest {

    private final ObjectMapper json = new ObjectMapper();

    private static final String DESIGN = """
            {"name":"Report card: test","elements":[
             {"type":"rectangle","properties":{"fill":"#f3f6fd","stroke":"#c9d3ee"}},
             {"type":"line","properties":{"stroke":"#1f3a8a"}},
             {"type":"rectangle","properties":{"fill":"#1f3a8a"}},
             {"type":"rectangle","properties":{"fill":"#c9a227"}},
             {"type":"table","properties":{"header_fill":"#1f3a8a","grid_color":"#cccccc"}},
             {"type":"text","style":"h","properties":{"text":"x","color":"#333333"}}],
             "styles":{"h":{"color":"#1f3a8a"},"b":{"color":"#1f2937"}}}
            """;

    @Test
    void theMainColoursFollowTheSchoolsBrand() throws Exception {
        var result = TemplateBranding.brand((ObjectNode) json.readTree(DESIGN));
        var t = result.template();

        assertThat(result.changed()).isTrue();
        // Navy is darker than the gold, so it is the primary colour.
        assertThat(t.path("colors").path("primary").asText()).isEqualTo("#1f3a8a");
        assertThat(t.path("colors").path("accent").asText()).isEqualTo("#c9a227");
        assertThat(t.at("/elements/1/properties/stroke").asText()).isEqualTo("@primary");
        assertThat(t.at("/elements/3/properties/fill").asText()).isEqualTo("@accent");
        assertThat(t.at("/elements/4/properties/header_fill").asText()).isEqualTo("@primary");
        assertThat(t.at("/styles/h/color").asText()).isEqualTo("@primary");
        // A pale tint of the navy follows it as a shade.
        assertThat(t.at("/elements/0/properties/fill").asText()).isEqualTo("@primary/3");
        // Greys and body text stay as they are.
        assertThat(t.at("/elements/4/properties/grid_color").asText()).isEqualTo("#cccccc");
        assertThat(t.at("/elements/5/properties/color").asText()).isEqualTo("#333333");
        assertThat(t.at("/styles/b/color").asText()).isEqualTo("#1f2937");
    }

    @Test
    void aDesignThatAlreadyHasBrandColoursIsLeftAlone() throws Exception {
        var design = (ObjectNode) json.readTree(DESIGN);
        design.putObject("colors").put("primary", "#000000");
        assertThat(TemplateBranding.brand(design).changed()).isFalse();
    }

    @Test
    void aDesignWithNoStrongColoursIsLeftAlone() throws Exception {
        var design = (ObjectNode) json.readTree("""
                {"elements":[{"type":"rectangle","properties":{"fill":"#eeeeee","stroke":"#333333"}}]}""");
        assertThat(TemplateBranding.brand(design).changed()).isFalse();
    }
}
