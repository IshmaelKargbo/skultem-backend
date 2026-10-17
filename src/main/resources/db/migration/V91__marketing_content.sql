-- Content for the public marketing site, managed by system admins and read through the public API.
CREATE TABLE marketing_testimonials (
    id uuid PRIMARY KEY,
    author_name character varying(255) NOT NULL,
    author_role character varying(255),
    school_name character varying(255),
    quote text NOT NULL,
    rating integer NOT NULL DEFAULT 5,
    photo_url character varying(1024),
    featured boolean NOT NULL DEFAULT false,
    published boolean NOT NULL DEFAULT false,
    display_order integer NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT marketing_testimonials_rating_check CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE marketing_blog_posts (
    id uuid PRIMARY KEY,
    title character varying(255) NOT NULL,
    slug character varying(255) NOT NULL UNIQUE,
    excerpt character varying(1000),
    content text NOT NULL,
    cover_image_url character varying(1024),
    author_name character varying(255),
    tags character varying(500),
    published boolean NOT NULL DEFAULT false,
    published_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone
);
CREATE INDEX idx_marketing_blog_published ON marketing_blog_posts (published, published_at DESC);

CREATE TABLE marketing_contact_messages (
    id uuid PRIMARY KEY,
    name character varying(150) NOT NULL,
    email character varying(255) NOT NULL,
    phone character varying(30),
    subject character varying(255) NOT NULL,
    message text NOT NULL,
    status character varying(20) NOT NULL DEFAULT 'NEW',
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT marketing_contact_status_check CHECK (status IN ('NEW', 'READ', 'REPLIED', 'ARCHIVED'))
);
CREATE INDEX idx_marketing_contact_status ON marketing_contact_messages (status, created_at DESC);
