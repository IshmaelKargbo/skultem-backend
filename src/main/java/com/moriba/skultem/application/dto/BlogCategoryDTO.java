package com.moriba.skultem.application.dto;

// postCount: every post in it, drafts included (admin); publishedCount: the ones the marketing site shows.
public record BlogCategoryDTO(String id, String name, String slug, String description, int displayOrder,
        long postCount, long publishedCount) {
}
