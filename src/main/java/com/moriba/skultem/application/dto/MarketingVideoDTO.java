package com.moriba.skultem.application.dto;

import java.time.Instant;

// A marketing video. watchUrl / embedUrl / thumbnailUrl are built from youtubeId so the site can drop them straight
// into a link, an <iframe> and an <img>. embedUrl is youtube-nocookie.com: no YouTube cookies until the visitor
// presses play.
public record MarketingVideoDTO(String id, String title, String description, String youtubeId, String watchUrl,
        String embedUrl, String thumbnailUrl, boolean featured, boolean published, int displayOrder,
        Instant createdAt, Instant updatedAt) {
}
