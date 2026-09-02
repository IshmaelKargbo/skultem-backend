ALTER TABLE public.class_subjects
ADD COLUMN stream_id VARCHAR NULL;

ALTER TABLE public.class_subjects
ADD CONSTRAINT fk_class_subjects_stream
FOREIGN KEY (stream_id)
REFERENCES public.streams(id);