-- Which terms a test runs in, as term numbers ("1,2"). NULL = every term, which is how every existing test behaves.
-- Lets a school sit Test 1 and Test 2 in terms 1 and 2 but only an Exam in term 3.
ALTER TABLE assessments ADD COLUMN terms varchar(50);
