# Services, Endpoints & Events

A single-page inventory of what runs, what it exposes over HTTP, and what it puts on Kafka.
Companion docs: [ARCHITECTURE.md](ARCHITECTURE.md), [API.md](API.md), [EVENTS.md](EVENTS.md), [OPERATIONS.md](OPERATIONS.md).

---

## 1. Services at a glance

| Service             | Port | Database    | Kafka role | Responsibility                                                                                                                       |
| ------------------- | ---- | ----------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| **gateway**         | 8080 | —           | —          | Single front door. Routes`/api/v1/**` to the right service, validates the JWT, applies CORS. Refuses to route `/api/v1/internal/**`. |
| **user-service**    | 8081 | `userdb`    | —          | Registration, login, profile. The only service that**signs** JWTs.                                                                   |
| **quiz-service**    | 8082 | `quizdb`    | producer   | Owns quizzes and questions (including correct answers). Publishes a quiz for joining.                                                |
| **attempt-service** | 8083 | `attemptdb` | producer   | Player joins a quiz, snapshots its questions, scores the submission locally.                                                         |
| **stats-service**   | 8084 | `statsdb`   | consumer   | Read model. Builds the leaderboard purely from events — never queries another service.                                               |

### Shared modules (not deployables)

| Module            | Contains                                                                                  |
| ----------------- | ----------------------------------------------------------------------------------------- |
| **common-events** | The Avro schemas (`.avsc`) → generated event classes shared by producers and consumers.   |
| **common-web**    | JWT resource-server config,`CurrentUser` resolver, `ApiError` / `GlobalExceptionHandler`. |

### Infrastructure (docker-compose)

| Component       | Port | Note                                                                         |
| --------------- | ---- | ---------------------------------------------------------------------------- |
| PostgreSQL 16   | 5432 | One database**per service**; created by `infra/postgres/init-databases.sql`. |
| Kafka (KRaft)   | 9092 | No ZooKeeper. Containers use`kafka:29092`, host uses `localhost:9092`.       |
| Schema Registry | 8085 | Compatibility level**FULL**.                                                 |
| Kafka UI        | 8090 | Browse topics, messages and schemas.                                         |

---

## 2. Public API (through the gateway, `http://localhost:8080`)

Every path below is prefixed with `/api/v1`. 🔓 = no token needed, 🔒 = `Authorization: Bearer <jwt>`.

### user-service — Users & auth

| Method | Path          | Auth | Request                                                      | Response                                                                                               |
| ------ | ------------- | ---- | ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ |
| POST   | `/users`      | 🔓   | `RegisterRequest { username, email, displayName, password }` | `201` `TokenResponse` — registers **and** signs you in                                                 |
| POST   | `/auth/login` | 🔓   | `LoginRequest { username, password }`                        | `200` `TokenResponse { accessToken, tokenType, expiresInSeconds, user }`                               |
| GET    | `/users/me`   | 🔒   | —                                                            | `UserResponse { id, username, email, displayName, role, createdAt }` — read from the JWT, no DB lookup |

### quiz-service — Quizzes

| Method | Path                    | Auth     | Request                                                           | Response                                                              |
| ------ | ----------------------- | -------- | ----------------------------------------------------------------- | --------------------------------------------------------------------- |
| POST   | `/quizzes`              | 🔒       | `CreateQuizRequest { title, description, category, questions[] }` | `201` `QuizDetailResponse` — created as **DRAFT**                     |
| GET    | `/quizzes/mine`         | 🔒       | —                                                                 | `QuizSummaryResponse[]`                                               |
| GET    | `/quizzes/published`    | 🔒       | —                                                                 | `QuizSummaryResponse[]`                                               |
| GET    | `/quizzes/{id}`         | 🔒 owner | —                                                                 | `QuizDetailResponse` — **owner view, includes `correctOption`**       |
| POST   | `/quizzes/{id}/publish` | 🔒 owner | —                                                                 | `QuizSummaryResponse` — status → **PUBLISHED**, emits `QuizPublished` |

### attempt-service — Attempts

| Method | Path                    | Auth     | Request                                                            | Response                                                                       |
| ------ | ----------------------- | -------- | ------------------------------------------------------------------ | ------------------------------------------------------------------------------ |
| POST   | `/attempts/join`        | 🔒       | `JoinQuizRequest { joinCode }`                                     | `201` `AttemptResponse` — questions **without** answers                        |
| GET    | `/attempts/{id}`        | 🔒 owner | —                                                                  | `AttemptResponse`                                                              |
| POST   | `/attempts/{id}/submit` | 🔒 owner | `SubmitAttemptRequest { answers[{ questionId, selectedOption }] }` | `AttemptResultResponse` — score + per-question feedback; emits `QuizSubmitted` |
| GET    | `/attempts/me`          | 🔒       | —                                                                  | `AttemptHistoryResponse[]`                                                     |

### stats-service — Leaderboard

| Method | Path                            | Auth | Response                                                                                      |
| ------ | ------------------------------- | ---- | --------------------------------------------------------------------------------------------- |
| GET    | `/quizzes/{quizId}/leaderboard` | 🔓   | `LeaderboardEntryResponse[]` — top 20, ordered by `scorePercent DESC`, then `submittedAt ASC` |

> The leaderboard keeps the monolith's URL even though a **different service** answers it now.
> In [gateway/src/main/resources/application.yml](../gateway/src/main/resources/application.yml) the narrow
> `/api/v1/quizzes/*/leaderboard` route is declared **before** the broad `/api/v1/quizzes/**` route —
> Spring Cloud Gateway takes the first match, so the order is load-bearing.

---

## 3. Internal API (service-to-service only — never routed by the gateway)

`/api/v1/internal/**` has **no gateway route at all**, so an outside request gets a 404 at the front door.
These endpoints carry data the public API must never leak.

| Service      | Method | Path                                          | Called by       | Why it exists                                                                                                                                        |
| ------------ | ------ | --------------------------------------------- | --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| quiz-service | GET    | `/api/v1/internal/quizzes/by-code/{joinCode}` | attempt-service | Returns the full quiz**including `correctOption`**, so attempt-service can snapshot and score. Wrapped in Resilience4j `@Retry` + `@CircuitBreaker`. |
| user-service | GET    | `/api/v1/internal/users/{id}`                 | any service     | Lookup by id, for the cases a JWT claim cannot cover.                                                                                                |

---

## 4. Kafka events

Format **Avro**, serialized with `KafkaAvroSerializer` and registered in Schema Registry.
Topic names carry a `.v1` suffix: an incompatible change publishes to `.v2` so consumers migrate at their own pace.

### Topics

| Topic               | Producer        | Consumers                            | Message key |
| ------------------- | --------------- | ------------------------------------ | ----------- |
| `quiz.published.v1` | quiz-service    | stats-service (group`stats-service`) | `quizId`    |
| `quiz.submitted.v1` | attempt-service | stats-service (group`stats-service`) | `quizId`    |

Keying by `quizId` puts all events for one quiz on the same partition, so consumers see them in the order they happened.

### `QuizPublished` — `quiz.published.v1`

Emitted by **quiz-service** when an owner opens a quiz for joining (`POST /quizzes/{id}/publish`).

| Field           | Type             | Note                            |
| --------------- | ---------------- | ------------------------------- |
| `eventId`       | string           | UUID — consumers dedupe on this |
| `occurredAt`    | timestamp-millis |                                 |
| `quizId`        | long             |                                 |
| `title`         | string           |                                 |
| `category`      | string?          | nullable, default`null`         |
| `joinCode`      | string           |                                 |
| `ownerId`       | long             |                                 |
| `ownerUsername` | string           |                                 |
| `questionCount` | int              |                                 |

### `QuizSubmitted` — `quiz.submitted.v1`

Emitted by **attempt-service** when a player finishes a quiz (`POST /attempts/{id}/submit`).

| Field            | Type             | Note                                                                     |
| ---------------- | ---------------- | ------------------------------------------------------------------------ |
| `eventId`        | string           | UUID                                                                     |
| `occurredAt`     | timestamp-millis |                                                                          |
| `attemptId`      | long             |                                                                          |
| `quizId`         | long             |                                                                          |
| `quizTitle`      | string           | denormalised — saves stats-service a call to quiz-service                |
| `userId`         | long             |                                                                          |
| `username`       | string           |                                                                          |
| `displayName`    | string           | denormalised from the JWT — the cross-service join that no longer exists |
| `correctCount`   | int              |                                                                          |
| `totalQuestions` | int              |                                                                          |
| `scorePercent`   | double           |                                                                          |

**What is deliberately absent:** the player's answers and the correct options. Anyone who can read the topic can read every field, so the payload is an access-control decision as much as a data-shape one.

### Publishing rules in force

- **Publish after commit.** Both publishers are `@TransactionalEventListener(AFTER_COMMIT)`, so no event ever describes a row that got rolled back. _Known gap:_ if the process dies between commit and send the event is lost — the fix is the outbox pattern, left as an exercise.
- **Consume idempotently.** stats-service checks a `processed_event` table on `eventId`, plus a second guard on `attemptId` so a replay with a fresh event id still cannot double-post a score.
- **Replayable read model.** The consumer runs with `auto-offset-reset: earliest` — a fresh stats-service rebuilds the whole leaderboard from the topic.
- **Fan-out by group id.** All stats-service instances share group `stats-service`, so each message is handled once. A different service with a different group gets its own copy of every message.

---

## 5. Auth model

user-service signs an HS256 JWT; every other service verifies it with the same shared secret (`JWT_SECRET`) and never calls back to user-service to resolve the caller.

| Claim   | Meaning                   |
| ------- | ------------------------- |
| `sub`   | username                  |
| `uid`   | user id                   |
| `name`  | display name              |
| `roles` | `["USER"]` or `["ADMIN"]` |

Claim names live in `common-web` as constants (`JwtClaims`), not as string literals — they are a contract exactly like the Avro schemas.

Public (no token) endpoints: `POST /api/v1/users`, `POST /api/v1/auth/login`,
`GET /api/v1/quizzes/{id}/leaderboard`, plus `/actuator/health` and the Swagger UI on each service.

---

## 6. End-to-end flow

```
register / login ──► user-service ──► JWT
                                       │
create quiz ──► quiz-service (DRAFT) ──┘
publish     ──► quiz-service ──► emit QuizPublished ──► quiz.published.v1 ──► stats-service
join        ──► attempt-service ──HTTP──► quiz-service /internal/quizzes/by-code/{code}
                                          (snapshots questions + answers)
submit      ──► attempt-service (scores locally)
                     └──► emit QuizSubmitted ──► quiz.submitted.v1 ──► stats-service
                                                                          └──► leaderboard
```

The leaderboard is **eventually consistent**: the score is durable in `attemptdb` the moment submit returns, and appears on the leaderboard once stats-service has consumed the event.
