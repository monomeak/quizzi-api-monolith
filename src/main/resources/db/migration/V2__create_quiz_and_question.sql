CREATE TABLE quiz (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    category    VARCHAR(50),
    join_code   VARCHAR(10)  NOT NULL UNIQUE,
    status      VARCHAR(20)  NOT NULL,
    owner_id    BIGINT       NOT NULL REFERENCES app_user (id),
    created_at  TIMESTAMP    NOT NULL
);

CREATE INDEX idx_quiz_owner  ON quiz (owner_id);
CREATE INDEX idx_quiz_status ON quiz (status);

CREATE TABLE question (
    id             BIGSERIAL PRIMARY KEY,
    -- ON DELETE CASCADE mirrors orphanRemoval on the Java side: no question
    -- can outlive its quiz, in the database as well as in the code.
    quiz_id        BIGINT       NOT NULL REFERENCES quiz (id) ON DELETE CASCADE,
    question_text  VARCHAR(500) NOT NULL,
    option_a       VARCHAR(255) NOT NULL,
    option_b       VARCHAR(255) NOT NULL,
    option_c       VARCHAR(255),
    option_d       VARCHAR(255),
    correct_option VARCHAR(1)   NOT NULL,
    difficulty     VARCHAR(10),
    position       INTEGER      NOT NULL
);

CREATE INDEX idx_question_quiz ON question (quiz_id);
