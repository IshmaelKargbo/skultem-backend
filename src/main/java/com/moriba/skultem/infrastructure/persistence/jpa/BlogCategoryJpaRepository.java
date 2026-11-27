package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.moriba.skultem.infrastructure.persistence.entity.BlogCategoryEntity;

public interface BlogCategoryJpaRepository extends JpaRepository<BlogCategoryEntity, UUID> {
    List<BlogCategoryEntity> findAllByOrderByDisplayOrderAscNameAsc();

    Optional<BlogCategoryEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    // Rows of [categoryId, all posts, published posts].
    @Query("""
            select p.categoryId, count(p), sum(case when p.published = true then 1 else 0 end)
            from BlogPostEntity p
            where p.categoryId is not null
            group by p.categoryId
            """)
    List<Object[]> postCounts();
}
