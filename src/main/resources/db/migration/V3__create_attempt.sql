CREATE TABLE quiz_attempt (
    id              BIGSERIAL PRIMARY KEY,
    quiz_id         BIGINT      NOT NULL REFERENCES quiz (id) ON DELETE CASCADE,
    user_id         BIGINT      NOT NULL REFERENCES app_user (id),
    status          VARCHAR(20) NOT NULL,
    started_at      TIMESTAMP   NOT NULL,
    submitted_at    TIMESTAMP,
    correct_count   INTEGER,
    total_questions INTEGER,
    score_percent   DOUBLE PRECISION,
    -- One attempt per person per quiz. The service checks this too, for a friendly
    -- 409, but only the database can enforce it under concurrent requests.
    CONSTRAINT uq_attempt_quiz_user UNIQUE (quiz_id, user_id)
);

-- Exactly the order the leaderboard query asks for, so it can be read straight
-- from the index instead of sorting every time.
CREATE INDEX idx_attempt_leaderboard
    ON quiz_attempt (quiz_id, score_percent DESC, submitted_at ASC);

CREATE INDEX idx_attempt_user ON quiz_attempt (user_id);

CREATE TABLE attempt_answer (
    id              BIGSERIAL PRIMARY KEY,
    attempt_id      BIGINT     NOT NULL REFERENCES quiz_attempt (id) ON DELETE CASCADE,
    question_id     BIGINT     NOT NULL REFERENCES question (id),
    selected_option VARCHAR(1),
    correct         BOOLEAN    NOT NULL
);

CREATE INDEX idx_answer_attempt ON attempt_answer (attempt_id);
