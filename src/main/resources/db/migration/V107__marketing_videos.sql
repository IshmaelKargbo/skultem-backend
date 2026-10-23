-- YouTube videos the marketing site shows (were hardcoded there). Added by platform staff with MARKETING
-- permission; the site reads the published ones from GET /api/v1/public/videos. See MarketingVideoService.
CREATE TABLE marketing_videos (
    id uuid PRIMARY KEY,
    title character varying(255) NOT NULL,
    description text,
    youtube_id character varying(11) NOT NULL UNIQUE,
    featured boolean NOT NULL DEFAULT false,
    published boolean NOT NULL DEFAULT false,
    display_order integer NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone
);
