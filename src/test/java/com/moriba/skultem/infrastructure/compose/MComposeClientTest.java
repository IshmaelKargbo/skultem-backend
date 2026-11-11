package com.moriba.skultem.infrastructure.compose;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moriba.skultem.application.error.DocumentServiceException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Against a tiny stand-in for the engine on a local port.
class MComposeClientTest {

    private static final byte[] PDF = "%PDF-1.4 fake".getBytes(StandardCharsets.US_ASCII);
    private final ObjectMapper json = new ObjectMapper();
    private final List<String> calls = new ArrayList<>();
    private final List<JsonNode> bodies = new ArrayList<>();
    private HttpServer server;
    private int status = 200;
    private String errorBody = "";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange ex) throws IOException {
        calls.add(ex.getRequestMethod() + " " + ex.getRequestURI().getPath() + " "
                + ex.getRequestHeaders().getFirst("Authorization"));
        byte[] in = ex.getRequestBody().readAllBytes();
        if (in.length > 0) {
            bodies.add(json.readTree(in));
        }
        if (status >= 400) {
            send(ex, status, "application/json", errorBody.getBytes(StandardCharsets.UTF_8));
        } else if (ex.getRequestURI().getPath().startsWith("/samples/")) {
            send(ex, 200, "application/json",
                    "{\"id\":\"report-card-classic\",\"template\":{\"name\":\"Report card: classic\"}}"
                            .getBytes(StandardCharsets.UTF_8));
        } else {
            send(ex, 200, "application/pdf", PDF);
        }
    }

    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().add("Content-Type", type);
        ex.sendResponseHeaders(code, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private MComposeClient client() {
        return new MComposeClient("http://127.0.0.1:" + server.getAddress().getPort() + "/", "mck_key", 10);
    }

    @Test
    void rendersASavedTemplateWithTheAccountKey() {
        byte[] pdf = client().render("tpl-1", Map.of("student", "Jordan"));

        assertThat(pdf).isEqualTo(PDF);
        assertThat(calls).containsExactly("POST /templates/tpl-1/render Bearer mck_key");
        assertThat(bodies.get(0).get("data").get("student").asText()).isEqualTo("Jordan");
    }

    @Test
    void rendersAReadyMadeDesignBySendingItsTemplate() {
        client().renderBatch("sample:report-card-classic", Map.of(), List.of(Map.of("student", "A"), Map.of("student", "B")));

        assertThat(calls).containsExactly(
                "GET /samples/report-card-classic Bearer mck_key",
                "POST /render/batch Bearer mck_key");
        JsonNode body = bodies.get(0);
        assertThat(body.get("template").get("name").asText()).isEqualTo("Report card: classic");
        assertThat(body.get("records")).hasSize(2);
    }

    @Test
    void passesOnTheEnginesReason() {
        status = 422;
        errorBody = "{\"error\":\"asset_error\",\"message\":\"Image 'logo' could not be loaded\"}";

        assertThatThrownBy(() -> client().render("tpl-1", Map.of()))
                .isInstanceOf(DocumentServiceException.class)
                .hasMessageContaining("Image 'logo' could not be loaded");
    }

    @Test
    void aRefusedKeyIsReportedAsASetupProblemWithoutTheKey() {
        status = 401;
        errorBody = "{\"error\":\"unauthorized\"}";

        assertThatThrownBy(() -> client().render("tpl-1", Map.of()))
                .isInstanceOf(DocumentServiceException.class)
                .hasMessageContaining("did not accept this server's key")
                .hasMessageNotContaining("mck_key");
    }

    @Test
    void refusesATemplateNameThatCouldChangeThePath() {
        assertThatThrownBy(() -> client().render("../accounts", Map.of()))
                .isInstanceOf(DocumentServiceException.class);
        assertThatThrownBy(() -> client().render("sample:a/b", Map.of()))
                .isInstanceOf(DocumentServiceException.class);
        assertThat(calls).isEmpty();
    }

    @Test
    void offWithoutUrlOrKey() {
        var off = new MComposeClient("", "", 10);
        assertThat(off.isEnabled()).isFalse();
        assertThatThrownBy(() -> off.render("tpl-1", Map.of()))
                .isInstanceOf(DocumentServiceException.class)
                .hasMessageContaining("not set up");
    }

    @Test
    void anUnreachableEngineSaysSo() {
        server.stop(0);
        assertThatThrownBy(() -> client().render("tpl-1", Map.of()))
                .isInstanceOf(DocumentServiceException.class)
                .hasMessageContaining("can't be reached");
    }
}
