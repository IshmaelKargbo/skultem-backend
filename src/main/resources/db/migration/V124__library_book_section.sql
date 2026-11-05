-- Each library book belongs to a management section (e.g. Primary, Secondary). Staff limited to sections see and lend
-- only their sections' books. Null = not assigned yet: only whole-school staff see it until someone picks a section.
ALTER TABLE public.library_books ADD COLUMN management_section_id character varying(255);

ALTER TABLE public.library_books
    ADD CONSTRAINT library_books_management_section_fkey FOREIGN KEY (management_section_id)
    REFERENCES public.management_sections(id) ON DELETE SET NULL;

CREATE INDEX idx_library_books_school_section ON public.library_books (school_id, management_section_id);

-- A school with exactly one section: every existing book is obviously that section's.
UPDATE public.library_books b
SET management_section_id = s.id
FROM public.management_sections s
WHERE s.school_id = b.school_id
  AND b.management_section_id IS NULL
  AND (SELECT count(*) FROM public.management_sections x WHERE x.school_id = b.school_id) = 1;
