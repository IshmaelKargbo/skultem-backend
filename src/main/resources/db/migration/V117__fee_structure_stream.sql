-- A class fee can be narrowed to one stream of a streamed class (e.g. SSS 1 Science vs SSS 1 Art,
-- priced differently). Null keeps the old meaning: every student in the class, whatever stream.
ALTER TABLE public.fee_structures ADD COLUMN stream_id character varying(255);

ALTER TABLE public.fee_structures
    ADD CONSTRAINT fk_fee_structures_stream FOREIGN KEY (stream_id) REFERENCES public.streams(id);

CREATE INDEX IF NOT EXISTS idx_fee_structures_stream_id ON public.fee_structures (stream_id);
