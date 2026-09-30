ALTER TABLE public.teachers DROP CONSTRAINT teachers_title_check;

ALTER TABLE public.teachers
ADD CONSTRAINT teachers_title_check CHECK (
    title IN (
        'MR',
        'MRS',
        'MISS',
        'MS',
        'DR',
        'PROF',
        'PST',
        'REV',
        'HON',
        'ENG',
        'SIR',
        'MADAM',
        'SHEIKH',
        'IMAM'
    )
);