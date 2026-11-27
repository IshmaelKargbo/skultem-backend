package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.BlogPostEntity;

public interface BlogPostJpaRepository extends JpaRepository<BlogPostEntity, UUID> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<BlogPostEntity> findBySlugAndPublishedTrue(String slug);

    // tag is matched as a whole comma separated item, not a substring ("fee" must not match "fees").
    // categoryId: null = any category.
    @Query("""
            select p from BlogPostEntity p
            where p.published = true
              and (:tag = '' or concat(',', lower(coalesce(p.tags, '')), ',') like concat('%,', lower(:tag), ',%'))
              and (:categoryId is null or p.categoryId = :categoryId)
            order by p.publishedAt desc
            """)
    Page<BlogPostEntity> findPublished(@Param("tag") String tag, @Param("categoryId") UUID categoryId,
            Pageable pageable);

    // Admin list, drafts included. uncategorised: only posts with no category; else categoryId null = any.
    @Query("""
            select p from BlogPostEntity p
            where (:uncategorised = false or p.categoryId is null)
              and (:categoryId is null or p.categoryId = :categoryId)
            """)
    Page<BlogPostEntity> findForAdmin(@Param("categoryId") UUID categoryId,
            @Param("uncategorised") boolean uncategorised, Pageable pageable);
}
