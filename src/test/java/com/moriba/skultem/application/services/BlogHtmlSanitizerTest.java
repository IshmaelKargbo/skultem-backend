package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BlogHtmlSanitizerTest {

    @Test
    void keepsWhatTheEditorProduces() {
        String html = "<h2>Title</h2><p style=\"text-align: center\"><strong>Bold</strong> <em>it</em> <u>u</u> "
                + "<s>strike</s></p><ul><li>one</li></ul><ol><li>two</li></ol><blockquote><p>quote</p></blockquote>"
                + "<hr><p><a href=\"https://skultem.com\" target=\"_blank\">link</a></p>";

        String clean = BlogHtmlSanitizer.clean(html);

        assertThat(clean).contains("<h2>Title</h2>", "<strong>Bold</strong>", "<em>it</em>", "<u>u</u>",
                "<s>strike</s>", "<ul><li>one</li></ul>", "<ol><li>two</li></ol>", "<blockquote>", "<hr>",
                "style=\"text-align: center\"", "href=\"https://skultem.com\"", "target=\"_blank\"",
                "rel=\"noopener noreferrer nofollow\"");
    }

    @Test
    void removesScriptsAndEventHandlers() {
        String clean = BlogHtmlSanitizer.clean("<p onclick=\"steal()\">Hi</p><script>alert(1)</script>"
                + "<img src=x onerror=\"alert(1)\"><iframe src=\"https://evil.example\"></iframe>"
                + "<svg onload=alert(1)></svg>");

        assertThat(clean).doesNotContain("script", "onclick", "onerror", "onload", "iframe", "svg", "alert");
        assertThat(clean).contains("<p>Hi</p>");
    }

    @Test
    void removesJavascriptLinks() {
        String clean = BlogHtmlSanitizer.clean("<a href=\"javascript:alert(1)\">x</a>"
                + "<a href=\"JaVaScRiPt:alert(1)\">y</a><a href=\"data:text/html,<script>alert(1)</script>\">z</a>");

        assertThat(clean).doesNotContain("javascript", "JaVaScRiPt", "data:", "alert");
    }

    @Test
    void keepsOnlyTextAlignInStyles() {
        String clean = BlogHtmlSanitizer.clean("<p style=\"position:fixed;top:0;background:url(x)\">a</p>"
                + "<p style=\"color:red; text-align: RIGHT\">b</p><p style=\"text-align: expression(alert(1))\">c</p>");

        assertThat(clean).isEqualTo("<p>a</p><p style=\"text-align: right\">b</p><p>c</p>");
    }

    @Test
    void dropsUnexpectedLinkTargets() {
        assertThat(BlogHtmlSanitizer.clean("<a href=\"https://a.b\" target=\"_top\">x</a>")).doesNotContain("target");
    }

    @Test
    void contentWithNothingVisibleIsBlank() {
        assertThat(BlogHtmlSanitizer.isBlank(BlogHtmlSanitizer.clean("<script>alert(1)</script><p> </p>"))).isTrue();
        assertThat(BlogHtmlSanitizer.isBlank("<p>Hello</p>")).isFalse();
        assertThat(BlogHtmlSanitizer.isBlank("<hr>")).isFalse();
    }
}
