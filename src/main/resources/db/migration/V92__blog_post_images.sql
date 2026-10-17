-- A post's photos, one URL per line. The first is the cover.
ALTER TABLE marketing_blog_posts ADD COLUMN images text;
UPDATE marketing_blog_posts SET images = cover_image_url WHERE cover_image_url IS NOT NULL;
