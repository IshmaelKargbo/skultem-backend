package com.moriba.skultem.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class DataEtagFilterTest {

    private final DataEtagFilter filter = new DataEtagFilter();

    private static HttpServlet json(String body, int status, String type) {
        return new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {
                res.setStatus(status);
                res.setContentType(type);
                res.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    private MockHttpServletResponse run(String method, String uri, String ifNoneMatch, HttpServlet servlet)
            throws Exception {
        var req = new MockHttpServletRequest(method, uri);
        if (ifNoneMatch != null) {
            req.addHeader("If-None-Match", ifNoneMatch);
        }
        var res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain(servlet));
        return res;
    }

    @Test
    void sameDataAtADifferentTimeHasTheSameEtag() {
        String a = "{\"status\":\"success\",\"data\":[1,2],\"timestamp\":\"2026-11-18T10:00:00Z\"}";
        String b = "{\"status\":\"success\",\"data\":[1,2],\"timestamp\":\"2026-11-18T11:30:00Z\"}";
        String c = "{\"status\":\"success\",\"data\":[1,3],\"timestamp\":\"2026-11-18T10:00:00Z\"}";
        assertThat(DataEtagFilter.etagOf(a.getBytes())).isEqualTo(DataEtagFilter.etagOf(b.getBytes()));
        assertThat(DataEtagFilter.etagOf(a.getBytes())).isNotEqualTo(DataEtagFilter.etagOf(c.getBytes()));
    }

    @Test
    void unchangedDataIs304WithNoBody() throws Exception {
        var servlet = json("{\"data\":{\"fee\":100},\"timestamp\":\"t1\"}", 200, "application/json");
        var first = run("GET", "/api/v1/student/fee/s1", null, servlet);
        String etag = first.getHeader("ETag");
        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(etag).startsWith("W/\"");
        assertThat(first.getHeader("Cache-Control")).isEqualTo("private, no-cache");
        assertThat(first.getContentAsString()).contains("\"fee\":100");

        var again = run("GET", "/api/v1/student/fee/s1", etag,
                json("{\"data\":{\"fee\":100},\"timestamp\":\"t2\"}", 200, "application/json"));
        assertThat(again.getStatus()).isEqualTo(304);
        assertThat(again.getContentAsByteArray()).isEmpty();
    }

    @Test
    void changedDataIsSentInFull() throws Exception {
        var first = run("GET", "/api/v1/notice", null, json("{\"data\":[1]}", 200, "application/json"));
        var changed = run("GET", "/api/v1/notice", first.getHeader("ETag"),
                json("{\"data\":[1,2]}", 200, "application/json"));
        assertThat(changed.getStatus()).isEqualTo(200);
        assertThat(changed.getContentAsString()).isEqualTo("{\"data\":[1,2]}");
        assertThat(changed.getHeader("ETag")).isNotEqualTo(first.getHeader("ETag"));
    }

    @Test
    void leavesWritesErrorsAndFilesAlone() throws Exception {
        var post = run("POST", "/api/v1/widget/run", null, json("{\"data\":1}", 200, "application/json"));
        assertThat(post.getHeader("ETag")).isNull();

        var error = run("GET", "/api/v1/notice", null, json("{\"message\":\"no\"}", 403, "application/json"));
        assertThat(error.getHeader("ETag")).isNull();
        assertThat(error.getStatus()).isEqualTo(403);
        assertThat(error.getContentAsString()).contains("no");

        var pdf = run("GET", "/api/v1/report-card/x/pdf", null, json("%PDF", 200, "application/pdf"));
        assertThat(pdf.getHeader("ETag")).isNull();
        assertThat(pdf.getContentAsString()).isEqualTo("%PDF");
    }
}
