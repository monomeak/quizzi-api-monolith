-- DEMO DATA - dev profile only.
-- This lives in db/seed, a location the prod profile does not load, so production
-- can never accidentally get demo accounts. Flyway still versions it (V900), so it
-- runs once per database rather than on every startup.
--
-- All demo passwords are 'password123' except admin, which is 'admin123'.

INSERT INTO app_user (username, email, display_name, password_hash, role, created_at) VALUES
('admin',  'admin@example.com',  'Site Admin',   '$2b$10$QB3nsAFYzBk/AJVnETtd6O75JStD8lXrj8e6bO04LJXnK2PNyqA16', 'ADMIN', NOW()),
('sokha',  'sokha@example.com',  'Sokha Chan',   '$2b$10$o3kxt0me/0R0pIOzydxOpOk7KX..ELNUMvisyrW6uaWHyyshNdzHC', 'USER',  NOW()),
('dara',   'dara@example.com',   'Dara Kim',     '$2b$10$o3kxt0me/0R0pIOzydxOpOk7KX..ELNUMvisyrW6uaWHyyshNdzHC', 'USER',  NOW()),
('vichet', 'vichet@example.com', 'Vichet Sok',   '$2b$10$o3kxt0me/0R0pIOzydxOpOk7KX..ELNUMvisyrW6uaWHyyshNdzHC', 'USER',  NOW());

-- Sokha owns a published quiz that dara and vichet can join with code JAVA01.
INSERT INTO quiz (title, description, category, join_code, status, owner_id, created_at)
SELECT 'Java Basics', 'A short warm-up on Java fundamentals', 'Java', 'JAVA01', 'PUBLISHED', id, NOW()
FROM app_user WHERE username = 'sokha';

INSERT INTO question (quiz_id, question_text, option_a, option_b, option_c, option_d, correct_option, difficulty, position)
SELECT q.id, v.question_text, v.a, v.b, v.c, v.d, v.correct, v.difficulty, v.position
FROM quiz q
CROSS JOIN (VALUES
    ('Which Java keyword is used to create a subclass?', 'class', 'interface', 'extends', 'implements', 'C', 'EASY', 1),
    ('What is the default value of an uninitialized boolean field?', 'true', 'false', '0', 'null', 'B', 'EASY', 2),
    ('Which keyword explicitly throws an exception?', 'try', 'throw', 'catch', 'finally', 'B', 'EASY', 3),
    ('Which loop always executes its body at least once?', 'for', 'while', 'do-while', 'switch', 'C', 'EASY', 4),
    ('Which collection is a resizable array?', 'HashMap', 'ArrayList', 'HashSet', 'TreeMap', 'B', 'EASY', 5)
) AS v(question_text, a, b, c, d, correct, difficulty, position)
WHERE q.join_code = 'JAVA01';

-- Dara owns a second quiz, still a DRAFT, to show the publish step.
INSERT INTO quiz (title, description, category, join_code, status, owner_id, created_at)
SELECT 'Spring Boot Starter', 'Draft quiz - publish it to get a join code working', 'Spring', 'SPRNG1', 'DRAFT', id, NOW()
FROM app_user WHERE username = 'dara';

INSERT INTO question (quiz_id, question_text, option_a, option_b, option_c, option_d, correct_option, difficulty, position)
SELECT q.id, v.question_text, v.a, v.b, v.c, v.d, v.correct, v.difficulty, v.position
FROM quiz q
CROSS JOIN (VALUES
    ('Which annotation marks a service class?', '@Component', '@Service', '@Repository', '@Bean', 'B', 'EASY', 1),
    ('Which file holds Flyway migrations by default?', 'src/main/db', 'classpath:db/migration', 'src/sql', 'classpath:flyway', 'B', 'MEDIUM', 2)
) AS v(question_text, a, b, c, d, correct, difficulty, position)
WHERE q.join_code = 'SPRNG1';
