package com.moriba.skultem.application.services;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MarketingVideoDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.PageableMapper;
import com.moriba.skultem.infrastructure.persistence.entity.MarketingVideoEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.MarketingVideoJpaRepository;
import com.moriba.skultem.infrastructure.rest.dto.SaveMarketingVideoDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// YouTube videos for the marketing site - managed by platform staff (MarketingAdminController), read by the site
// (PublicMarketingController). Only the video id is stored; the links and thumbnail are built from it.
@Service
@Transactional
@RequiredArgsConstructor
public class MarketingVideoService {

    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    private final MarketingVideoJpaRepository repo;

    public Page<MarketingVideoDTO> list(int page, int size) {
        return repo.findAll(PageableMapper.toPage(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(MarketingVideoService::toDTO);
    }

    // What the marketing site shows: published only, featured first, then by display order.
    public List<MarketingVideoDTO> listPublished() {
        return repo.findAllByPublishedTrueOrderByFeaturedDescDisplayOrderAscCreatedAtDesc().stream()
                .map(MarketingVideoService::toDTO).toList();
    }

    public MarketingVideoDTO create(SaveMarketingVideoDTO param) {
        var now = Instant.now();
        var entity = MarketingVideoEntity.builder().id(UUID.randomUUID()).createdAt(now).build();
        apply(entity, param, now);
        return toDTO(repo.save(entity));
    }

    public MarketingVideoDTO update(String id, SaveMarketingVideoDTO param) {
        var entity = find(id);
        apply(entity, param, Instant.now());
        return toDTO(repo.save(entity));
    }

    public void delete(String id) {
        repo.delete(find(id));
    }

    private MarketingVideoEntity find(String id) {
        return repo.findById(TestimonialService.parse(id)).orElseThrow(() -> new NotFoundException("Video not found"));
    }

    private void apply(MarketingVideoEntity e, SaveMarketingVideoDTO p, Instant now) {
        String youtubeId = youtubeId(p.youtubeUrl());
        if (repo.existsByYoutubeIdAndIdNot(youtubeId, e.getId())) {
            throw new AlreadyExistsException("This video has already been added");
        }
        e.setTitle(p.title().trim());
        e.setDescription(p.description() == null || p.description().isBlank() ? null : p.description().trim());
        e.setYoutubeId(youtubeId);
        e.setFeatured(p.featured());
        e.setPublished(p.published());
        e.setDisplayOrder(p.displayOrder());
        e.setUpdatedAt(now);
    }

    // The video id from any YouTube link (watch?v=, youtu.be/, /shorts/, /embed/, /live/, /v/) or a bare id.
    // Anything else - another site, a playlist or channel link - is refused.
    static String youtubeId(String input) {
        String value = input == null ? "" : input.trim();
        if (VIDEO_ID.matcher(value).matches()) {
            return value;
        }
        URI uri;
        try {
            uri = URI.create(value.startsWith("http") ? value : "https://" + value);
        } catch (IllegalArgumentException e) {
            throw invalid();
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase().replaceFirst("^(www\\.|m\\.)", "");
        String path = uri.getPath() == null ? "" : uri.getPath();
        String candidate = null;
        if (host.equals("youtu.be")) {
            candidate = path.replaceFirst("^/", "").split("/")[0];
        } else if (host.equals("youtube.com") || host.equals("youtube-nocookie.com")) {
            if (path.equals("/watch") && uri.getRawQuery() != null) {
                for (String part : uri.getRawQuery().split("&")) {
                    if (part.startsWith("v=")) {
                        candidate = part.substring(2);
                    }
                }
            } else {
                var m = Pattern.compile("^/(?:shorts|embed|live|v)/([^/?#]+)").matcher(path);
                if (m.find()) {
                    candidate = m.group(1);
                }
            }
        }
        if (candidate == null || !VIDEO_ID.matcher(candidate).matches()) {
            throw invalid();
        }
        return candidate;
    }

    private static RuleException invalid() {
        return new RuleException("Enter a YouTube video link, e.g. https://www.youtube.com/watch?v=dQw4w9WgXcQ");
    }

    static MarketingVideoDTO toDTO(MarketingVideoEntity e) {
        String id = e.getYoutubeId();
        return new MarketingVideoDTO(e.getId().toString(), e.getTitle(), e.getDescription(), id,
                "https://www.youtube.com/watch?v=" + id,
                "https://www.youtube-nocookie.com/embed/" + id,
                "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg",
                e.isFeatured(), e.isPublished(), e.getDisplayOrder(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
