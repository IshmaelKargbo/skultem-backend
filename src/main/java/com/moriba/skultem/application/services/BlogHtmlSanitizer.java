package com.moriba.skultem.application.services;

import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

// Cleans a blog post's HTML before it is stored. The marketing site renders the content as HTML (v-html), so
// anything not on this list - <script>, <iframe>, event handlers (onclick, onerror...), javascript: links, forms,
// arbitrary CSS - is removed. A taken-over marketing account can then only publish text, not code that runs in
// visitors' browsers.
//
// The list matches what the admin editor (components/marketing/BlogForm.vue) produces: paragraphs, h2/h3 headings,
// bold/italic/underline/strike, lists, quotes, links, horizontal rules, code, and text alignment - which arrives
// as style="text-align: ...", so style is allowed but only a valid text-align survives.
public final class BlogHtmlSanitizer {

    private static final Pattern TEXT_ALIGN = Pattern.compile(
            "(?i)(?:^|;)\\s*text-align\\s*:\\s*(left|right|center|justify)\\s*(?:;|$)");

    private static final Safelist SAFELIST = new Safelist()
            .addTags("p", "br", "hr", "h1", "h2", "h3", "h4", "h5", "h6", "strong", "b", "em", "i", "u", "s",
                    "strike", "del", "mark", "blockquote", "ul", "ol", "li", "code", "pre", "span", "a", "img")
            .addAttributes("a", "href", "title", "target")
            .addAttributes("img", "src", "alt", "title")
            .addAttributes(":all", "style")
            .addProtocols("a", "href", "http", "https", "mailto", "tel")
            .addProtocols("img", "src", "https", "http")
            // Links leave the site safely: the opened page can't reach back into ours, and search engines don't
            // treat them as endorsements.
            .addEnforcedAttribute("a", "rel", "noopener noreferrer nofollow");

    private BlogHtmlSanitizer() {
    }

    public static String clean(String html) {
        if (html == null) {
            return null;
        }
        Document dirty = Jsoup.parseBodyFragment(html);
        Document clean = new Cleaner(SAFELIST).clean(dirty);

        for (Element el : clean.body().select("[style]")) {
            var match = TEXT_ALIGN.matcher(el.attr("style"));
            if (match.find()) {
                el.attr("style", "text-align: " + match.group(1).toLowerCase());
            } else {
                el.removeAttr("style");
            }
        }
        for (Element a : clean.body().select("a[target]")) {
            if (!"_blank".equals(a.attr("target"))) {
                a.removeAttr("target");
            }
        }

        clean.outputSettings().prettyPrint(false);
        return clean.body().html();
    }

    // Nothing a reader would see: no text, and no image or rule.
    public static boolean isBlank(String html) {
        if (html == null) {
            return true;
        }
        var body = Jsoup.parseBodyFragment(html).body();
        return body.text().isBlank() && body.select("img, hr").isEmpty();
    }
}
