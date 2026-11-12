package com.moriba.skultem.infrastructure.compose;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class ResultTableColumnsTest {

    private final ObjectMapper json = new ObjectMapper();

    private ObjectNode design(String table) throws Exception {
        return (ObjectNode) json.readTree("""
                {"elements":[%s,
                 {"type":"table","properties":{"data":"{{attitude}}","columns":[{"field":"trait"}]}}]}""".formatted(table));
    }

    @Test
    void anOlderResultsTableIsPointedAtTheDatasColumns() throws Exception {
        var t = design("""
                {"type":"table","properties":{"data":"{{results}}","columns":[{"field":"subject"},{"field":"score"}]}}""");
        assertThat(ResultTableColumns.apply(t)).isTrue();
        assertThat(t.at("/elements/0/properties/columns_from").asText()).isEqualTo("{{columns}}");
        // Its own columns stay as the fallback; another table is left alone.
        assertThat(t.at("/elements/0/properties/columns").size()).isEqualTo(2);
        assertThat(t.at("/elements/1/properties/columns_from").isMissingNode()).isTrue();
    }

    @Test
    void aTableThatAlreadyReadsColumnsIsLeftAlone() throws Exception {
        var t = design("""
                {"type":"table","properties":{"data":"{{subjects}}","columns_from":"{{my_columns}}"}}""");
        assertThat(ResultTableColumns.apply(t)).isFalse();
        assertThat(t.at("/elements/0/properties/columns_from").asText()).isEqualTo("{{my_columns}}");
    }
}
