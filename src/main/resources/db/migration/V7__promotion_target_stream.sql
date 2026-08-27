-- Streams (Science/Arts/Commercial) only exist at SSS level in the Sierra Leone system - a JSS or
-- Primary class session never has one (enforced by CreateClassSessionUseCase). So promoting a JSS3
-- student into SSS1 can't just carry the source session's stream forward like every other
-- promotion does - there isn't one. The class master picks a stream per student at that boundary.

ALTER TABLE public.promotion_request_items
    ADD COLUMN target_stream_id character varying(255),
    ADD CONSTRAINT fk_promotion_request_items_target_stream FOREIGN KEY (target_stream_id) REFERENCES public.streams(id);

CREATE INDEX idx_promotion_request_items_target_stream_id ON public.promotion_request_items (target_stream_id);
