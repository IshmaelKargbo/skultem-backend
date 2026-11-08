-- Report cards kept the student's photo from when they were generated, so photos added afterwards never showed.
-- Bring every existing card up to the student's current photo (new uploads now update the cards themselves).
UPDATE report_cards rc
SET photo = s.photo
FROM students s
WHERE s.id = rc.student_id
  AND s.photo IS NOT NULL
  AND rc.photo IS DISTINCT FROM s.photo;
