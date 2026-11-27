package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;
import com.moriba.skultem.infrastructure.persistence.jpa.BlogPostJpaRepository;
import com.moriba.skultem.infrastructure.rest.dto.SaveBlogPostDTO;

class BlogPostServiceTest {

    @Test
    void slugifyMakesUrlSafeLowercaseSlugs() {
        assertEquals("5-ways-to-collect-school-fees", BlogPostService.slugify("5 Ways to Collect School Fees!"));
        assertEquals("cafe-au-lait", BlogPostService.slugify("  Café au lait  "));
        assertEquals("a-b", BlogPostService.slugify("a --- b"));
    }

    @Test
    void slugifyOfSymbolsOnlyIsEmpty() {
        assertTrue(BlogPostService.slugify("!!! ???").isEmpty());
    }

    @Test
    void slugifyCapsLengthWithoutTrailingDash() {
        String slug = BlogPostService.slugify("word ".repeat(60));
        assertTrue(slug.length() <= 120);
        assertTrue(!slug.endsWith("-"));
    }

    private static SaveBlogPostDTO post(boolean published, String... images) {
        return new SaveBlogPostDTO("A post", null, null, "<p>Body</p>", List.of(images), "Team", List.of(), published, null);
    }

    private BlogPostService service() {
        var repo = mock(BlogPostJpaRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        return new BlogPostService(repo, mock(R2StorageService.class), mock(BlogCategoryService.class));
    }

    @Test
    void aPostCanBePublishedWithAnyNumberOfPhotosUpToFour() {
        assertTrue(service().create(post(true)).published());
        assertEquals(List.of("a", "b"), service().create(post(true, "a", "b")).images());
    }

    @Test
    void aDraftMayHaveFewerPhotos() {
        var saved = service().create(post(false, "a"));
        assertEquals(List.of("a"), saved.images());
        assertTrue(!saved.published());
    }

    @Test
    void thePublishedPostKeepsItsPhotosInOrderAndUsesTheFirstAsCover() {
        var saved = service().create(post(true, "a", "b", "c", "d", "a"));
        assertEquals(List.of("a", "b", "c", "d"), saved.images());
        assertEquals("a", saved.coverImageUrl());
        assertTrue(saved.published());
    }

    @Test
    void aTypedSlugIsUsedAsWrittenOrRefused() {
        var typed = new SaveBlogPostDTO("A post", "my-slug-2", null, "<p>x</p>", List.of(), null, List.of(), false, null);
        assertEquals("my-slug-2", service().create(typed).slug());

        for (String bad : new String[] { "My Slug", "my--slug", "-slug", "slug-", "sl_ug", "é" }) {
            var dto = new SaveBlogPostDTO("A post", bad, null, "<p>x</p>", List.of(), null, List.of(), false, null);
            assertThatThrownBy(() -> service().create(dto)).isInstanceOf(RuleException.class);
        }
    }

    @Test
    void aTypedSlugAnotherPostUsesIsRefused() {
        var repo = mock(BlogPostJpaRepository.class);
        when(repo.existsBySlug("taken")).thenReturn(true);
        var svc = new BlogPostService(repo, mock(R2StorageService.class), mock(BlogCategoryService.class));
        var dto = new SaveBlogPostDTO("A post", "taken", null, "<p>x</p>", List.of(), null, List.of(), false, null);
        assertThatThrownBy(() -> svc.create(dto)).isInstanceOf(com.moriba.skultem.application.error.AlreadyExistsException.class);
    }

    @Test
    void aSlugMadeFromTheTitleGetsANumberWhenTaken() {
        var repo = mock(BlogPostJpaRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repo.existsBySlug("a-post")).thenReturn(true);
        var svc = new BlogPostService(repo, mock(R2StorageService.class), mock(BlogCategoryService.class));
        assertEquals("a-post-2", svc.create(post(false)).slug());
    }

    @Test
    void slugAvailabilityChecksFormatAndUse() {
        var repo = mock(BlogPostJpaRepository.class);
        when(repo.existsBySlug("used")).thenReturn(true);
        var svc = new BlogPostService(repo, mock(R2StorageService.class), mock(BlogCategoryService.class));
        assertTrue(svc.slugAvailable("free-one", null));
        assertTrue(!svc.slugAvailable("used", null));
        assertTrue(!svc.slugAvailable("Not Valid", null));
    }

    @Test
    void aPostCannotHaveMoreThanFourPhotos() {
        assertThatThrownBy(() -> service().create(post(false, "a", "b", "c", "d", "e")))
                .isInstanceOf(RuleException.class).hasMessageContaining("at most 4");
    }

    @Test
    void theCategoriesSlugIsReservedForTheSitesCategoryList() {
        var repo = mock(BlogPostJpaRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        var svc = new BlogPostService(repo, mock(R2StorageService.class), mock(BlogCategoryService.class));

        var typed = new SaveBlogPostDTO("A post", "categories", null, "<p>x</p>", List.of(), null, List.of(), false, null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> svc.create(typed))
                .isInstanceOf(com.moriba.skultem.application.error.AlreadyExistsException.class);
        // Made from the title, it just gets a number.
        var fromTitle = new SaveBlogPostDTO("Categories", null, null, "<p>x</p>", List.of(), null, List.of(), false, null);
        org.assertj.core.api.Assertions.assertThat(svc.create(fromTitle).slug()).isEqualTo("categories-2");
        org.assertj.core.api.Assertions.assertThat(svc.slugAvailable("categories", null)).isFalse();
    }

    @Test
    void aPostCarriesItsCategory() {
        var repo = mock(BlogPostJpaRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        var categories = mock(BlogCategoryService.class);
        var id = java.util.UUID.randomUUID();
        when(categories.resolve(id.toString())).thenReturn(id);
        when(categories.byId()).thenReturn(java.util.Map.of(id,
                com.moriba.skultem.infrastructure.persistence.entity.BlogCategoryEntity.builder()
                        .id(id).name("Fees").slug("fees").build()));
        var svc = new BlogPostService(repo, mock(R2StorageService.class), categories);

        var post = svc.create(new SaveBlogPostDTO("A post", null, null, "<p>x</p>", List.of(), null, List.of(), false,
                id.toString()));

        org.assertj.core.api.Assertions.assertThat(post.categoryName()).isEqualTo("Fees");
        org.assertj.core.api.Assertions.assertThat(post.categorySlug()).isEqualTo("fees");
    }
}
