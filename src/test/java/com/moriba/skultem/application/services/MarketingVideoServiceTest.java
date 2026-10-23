package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;

class MarketingVideoServiceTest {

    private static final String ID = "dQw4w9WgXcQ";

    @Test
    void readsTheIdFromEveryYoutubeLinkShape() {
        assertThat(MarketingVideoService.youtubeId("https://www.youtube.com/watch?v=" + ID)).isEqualTo(ID);
        assertThat(MarketingVideoService.youtubeId("https://www.youtube.com/watch?feature=share&v=" + ID + "&t=42s"))
                .isEqualTo(ID);
        assertThat(MarketingVideoService.youtubeId("https://m.youtube.com/watch?v=" + ID)).isEqualTo(ID);
        assertThat(MarketingVideoService.youtubeId("https://youtu.be/" + ID + "?si=abc")).isEqualTo(ID);
        assertThat(MarketingVideoService.youtubeId("https://www.youtube.com/shorts/" + ID)).isEqualTo(ID);
        assertThat(MarketingVideoService.youtubeId("https://www.youtube.com/embed/" + ID)).isEqualTo(ID);
        assertThat(MarketingVideoService.youtubeId("https://www.youtube-nocookie.com/embed/" + ID)).isEqualTo(ID);
        assertThat(MarketingVideoService.youtubeId("https://www.youtube.com/live/" + ID)).isEqualTo(ID);
        assertThat(MarketingVideoService.youtubeId("youtube.com/watch?v=" + ID)).isEqualTo(ID);
        assertThat(MarketingVideoService.youtubeId("  " + ID + "  ")).isEqualTo(ID);
    }

    @Test
    void refusesAnythingThatIsNotAYoutubeVideo() {
        for (String bad : new String[] { "https://vimeo.com/123456", "https://www.youtube.com/@skultem",
                "https://www.youtube.com/playlist?list=PL123", "https://evil.example/watch?v=" + ID,
                "https://youtube.com.evil.example/watch?v=" + ID, "not a link", "", "javascript:alert(1)" }) {
            assertThatThrownBy(() -> MarketingVideoService.youtubeId(bad)).as(bad).isInstanceOf(RuleException.class);
        }
    }
}
