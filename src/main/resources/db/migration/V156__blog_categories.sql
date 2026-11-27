-- Blog categories: each post can sit in one, and the marketing site lists and filters posts by them.
CREATE TABLE marketing_blog_categories (
    id uuid PRIMARY KEY,
    name character varying(80) NOT NULL,
    slug character varying(120) NOT NULL UNIQUE,
    description character varying(500),
    display_order integer NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone
);

-- Deleting a category leaves its posts uncategorised.
ALTER TABLE marketing_blog_posts ADD COLUMN category_id uuid
    REFERENCES marketing_blog_categories (id) ON DELETE SET NULL;
CREATE INDEX idx_marketing_blog_category ON marketing_blog_posts (category_id);
