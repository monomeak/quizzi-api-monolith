-- Hibernate maps an enum column of length 1 to CHAR(1); we want VARCHAR so the
-- enum can gain longer names later without an ALTER on a live table.
ALTER TABLE question       ALTER COLUMN correct_option  TYPE VARCHAR(10);
ALTER TABLE attempt_answer ALTER COLUMN selected_option TYPE VARCHAR(10);