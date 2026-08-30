# Migration Blueprint — from the monolith to the 5 services

**Read this with:** [current-microservices.md](current-microservices.md) (the target design) and
[migration-parity-checklist.md](migration-parity-checklist.md) (the gaps you must close).

This doc answers two questions:

1. Where does every folder of today's monolith end up?
2. What does each new service own, expose, and depend on?

Nothing here is code. It is the map you follow while moving files.

---

## 1. The monolith today

One Spring Boot app, one Postgres database (`quizdb`), one JVM.

```
quiz-api/
└── src/main/java/com/sokmeak/quizapp/
    ├── QuizappApplication.java
    ├── config/              SecurityConfig, OpenApiConfig
    ├── common/
    │   ├── exception/       ApiError, GlobalExceptionHandler, NotFound/BadRequest/Conflict/Forbidden
    │   └── logging/         HttpRequestLoggingFilter
    ├── constant/            TokenType
    ├── utils/               OptionKey, QuizStatus, AttemptStatus, DifficultyLevel,
    │                        AnswerVerdict, JoinCodeGenerator
    └── modules/
        ├── auth/            AuthController, AuthServiceImpl, jwt/{JwtService, Filter, Properties},
        │                    DatabaseUserDetailsService, RestAuthenticationEntryPoint
        ├── user/            User, Role, UserRepository, UserServiceImpl, UserMapper
        ├── quiz/            Quiz, QuizRepository, QuizServiceImpl, QuizMapper, QuizController
        ├── question/        Question, QuestionRepository, QuestionService, QuestionController
        └── attempt/         QuizAttempt, AttemptAnswer, AttemptRepository, AttemptServiceImpl,
                             AttemptController, LeadeboardController, AttemptMapper
```

The `modules/` shape is already service-shaped. **That is the good news** — the split is mostly a
copy, not a rewrite. What breaks is everything that crosses a module boundary *by object reference*
(`quiz.getOwner().getUsername()`, `attempt.getQuiz().getQuestions()`), because after the split those
references become network calls or they stop existing.

---

## 2. Target repo layout

Keep one repository, Maven multi-module. Easier to learn than 5 repos, and you can still deploy the
services independently.

```
quiz-platform/
├── pom.xml                        <- packaging: pom, lists every module below
│
├── common-web/                    shared library (NOT deployable)
├── common-events/                 Avro .avsc schemas (NOT deployable)
│
├── gateway/                       :8080
├── user-service/                  :8081  -> userdb
├── quiz-service/                  :8082  -> quizdb
├── attempt-service/               :8083  -> attemptdb
├── stats-service/                 :8084  -> statsdb
│
├── infra/
│   ├── postgres/init-databases.sql       creates userdb, quizdb, attemptdb, statsdb
│   └── docker-compose.yml                postgres, kafka, schema-registry, kafka-ui
│
├── docs/                          these files
└── .env.example
```

### Inside every service — the same 6 folders

Use the *same* layout everywhere. When each service looks like the others, you stop thinking about
folders and think about the domain instead.

```
<service>/
├── pom.xml
└── src/main/
    ├── java/com/sokmeak/quiz/<service>/
    │   ├── <Service>Application.java
    │   ├── config/          SecurityConfig, OpenApiConfig, KafkaConfig, RestClientConfig
    │   ├── controller/      public HTTP  (/api/v1/...)
    │   ├── internal/        service-to-service HTTP (/api/v1/internal/...)   <- new folder
    │   ├── domain/          entity/ + repository/ + the enums this service owns
    │   ├── service/         interface + impl/   (business rules live here)
    │   ├── dto/             request/ + response/
    │   ├── mapper/          MapStruct
    │   ├── event/           publisher/ or consumer/                          <- new folder
    │   └── client/          outbound calls to other services                 <- new folder
    └── resources/
        ├── application.yml + application-dev.yml + application-prod.yml
        └── db/migration/    V1__... (each service numbers from V1 again)
```

Three folders are new compared to the monolith, and each one marks a boundary you did not have
before: `internal/` (what only another service may call), `event/` (what you tell the world),
`client/` (what you ask of others). Keeping them separate from `controller/` is what stops an
internal endpoint from quietly becoming public.

---

## 3. Where every monolith file goes

| Monolith path | Destination | Note |
| --- | --- | --- |
| `modules/user/**` | **user-service** | moves as-is |
| `modules/auth/**` | **user-service** | but see the split below |
| `modules/auth/jwt/JwtService#issue()` | **user-service** only | only one service may *sign* |
| `modules/auth/jwt/` verify half + `JwtAuthenticationFilter` + `JwtProperties` | **common-web** | every service *verifies* |
| `modules/quiz/**` | **quiz-service** | moves as-is |
| `modules/question/**` | **quiz-service** | folded into the quiz module — a question has no life without its quiz |
| `modules/attempt/**` minus the leaderboard | **attempt-service** | moves, but the entities change (§5) |
| `LeadeboardController` + `AttemptServiceImpl#leaderboard()` | **stats-service** | rebuilt from Kafka, not from a table join |
| `common/exception/**` | **common-web** | one `ApiError` JSON shape across all 5 services |
| `common/logging/HttpRequestLoggingFilter` | **common-web** | add a request/correlation id while you are here |
| `config/SecurityConfig` | **split 3 ways** | CORS -> gateway; JWT verify + rules -> common-web; `AuthenticationManager`, `PasswordEncoder`, `DaoAuthenticationProvider`, `DatabaseUserDetailsService` -> user-service |
| `config/OpenApiConfig` | **common-web** | each service keeps its own Swagger UI |
| `utils/JoinCodeGenerator` | **quiz-service** | it queries `quizRepository` — stays with the table |
| `utils/QuizStatus` | **quiz-service** | |
| `utils/AttemptStatus`, `utils/AnswerVerdict` | **attempt-service** | |
| `utils/OptionKey`, `utils/DifficultyLevel` | **quiz-service _and_ attempt-service** | duplicated on purpose — see below |
| `modules/user/Role`, `constant/TokenType` | **user-service** | claim *names* go to common-web |
| `db/migration/V1__create_users.sql` | **user-service** `V1__` | |
| `V2__create_quiz_and_question.sql` + the `question` half of `V4__` | **quiz-service** `V1__` | drop `REFERENCES app_user(id)` |
| `V3__create_attempt.sql` + the `attempt_answer` half of `V4__` | **attempt-service** `V1__` | drop both FKs, add the snapshot table |
| `db/seed/V5__demo_data.sql` | **split per service** | ids must line up by hand now |
| — | **stats-service** `V1__` | new: `leaderboard_entry`, `quiz_ref`, `processed_event` |

### Why duplicate `OptionKey` and `DifficultyLevel` instead of sharing them?

A shared "common-domain" jar looks tidy and then quietly re-couples the services: change one enum and
every service must be rebuilt and redeployed together, which is the exact thing you gave up a
monolith to avoid. Two four-line enums are cheaper than that coupling.

Share only what is a **contract**: the Avro schemas (`common-events`) and the JWT claim names
(`common-web`). Those *must* match, so they belong in one place.

---

## 4. The five services

### gateway — :8080

| | |
| --- | --- |
| **Owns** | nothing. No database, no business logic |
| **Job** | one public URL; route `/api/v1/**` to the right service; verify the JWT once; apply CORS |
| **Must refuse** | `/api/v1/internal/**` — no route exists at all, so it 404s at the front door |
| **Load-bearing detail** | the `/api/v1/quizzes/*/leaderboard` route must be declared **before** `/api/v1/quizzes/**`. Spring Cloud Gateway takes the first match, so route order decides whether stats-service or quiz-service answers |
| **From the monolith** | the CORS bean out of `SecurityConfig` |

### user-service — :8081, `userdb`

| | |
| --- | --- |
| **Owns** | `app_user`. The only service with a password hash |
| **Signs** | the JWT. No other service may hold the signing logic |
| **Public** | `POST /api/v1/auth/signup`, `POST /api/v1/auth/signin` |
| **Internal** | `GET /api/v1/internal/users/{id}` |
| **Depends on** | nothing |
| **Kafka** | none |
| **From the monolith** | `modules/user`, `modules/auth`, `DatabaseUserDetailsService`, `PasswordEncoder`, `AuthenticationManager` |
| **Decide first** | which claims the token carries — see [parity checklist §2](migration-parity-checklist.md#2-the-jwt-contract--decide-this-before-you-move-a-single-file) |

### quiz-service — :8082, `quizdb`

| | |
| --- | --- |
| **Owns** | `quiz`, `question` — **including `correct_option`**, the one secret in the system |
| **Public** | create / update / delete / publish / close a quiz, `mine`, `published`, owner detail |
| **Internal** | `GET /api/v1/internal/quizzes/by-code/{joinCode}` — returns the quiz **with answers**, so attempt-service can snapshot and score |
| **Depends on** | attempt-service, only for the "has anybody played this?" delete guard (§5, item 2) |
| **Kafka** | **produces** `quiz.published.v1` |
| **From the monolith** | `modules/quiz`, `modules/question`, `JoinCodeGenerator`, `QuizStatus` |
| **Changes** | `owner` stops being a `User` object and becomes `owner_id` + `owner_username` copied from the JWT |

### attempt-service — :8083, `attemptdb`

| | |
| --- | --- |
| **Owns** | `quiz_attempt`, `attempt_answer`, **and a new snapshot table** of the questions as they were at join time |
| **Public** | join, re-open, submit, my history, **review** |
| **Depends on** | quiz-service over HTTP at join time (wrap it in Resilience4j `@Retry` + `@CircuitBreaker`) |
| **Kafka** | **produces** `quiz.submitted.v1` |
| **From the monolith** | `modules/attempt` minus the leaderboard, `AttemptStatus`, `AnswerVerdict`, the `PASS_MARK`/feedback/summary wording |
| **Changes** | the biggest ones in the whole migration. Scoring and review must read the **snapshot**, never quiz-service. See §5 |

### stats-service — :8084, `statsdb`

| | |
| --- | --- |
| **Owns** | the leaderboard read model, and a `processed_event` table for idempotency |
| **Public** | `GET /api/v1/quizzes/{quizId}/leaderboard` — the monolith's URL, answered by a different service |
| **Depends on** | **nothing.** It never calls another service. If it needs a field, that field rides on the event |
| **Kafka** | **consumes** `quiz.published.v1` and `quiz.submitted.v1`, group `stats-service`, `auto-offset-reset: earliest` so a fresh instance rebuilds the whole board |
| **From the monolith** | `LeadeboardController` and `AttemptServiceImpl#leaderboard()` — the ranking rule, not the query |
| **Changes** | it cannot `join` to `app_user`, so `username` and `displayName` must arrive on `QuizSubmitted` |

### Shared modules

| Module | Contains | Rule |
| --- | --- | --- |
| **common-web** | JWT verification + filter, `JwtClaims` constant names, `ApiError`, `GlobalExceptionHandler`, `RestAuthenticationEntryPoint`, `@CurrentUser` resolver, request logging, OpenAPI defaults | plumbing only. **Never** put an entity or a business rule here — that is a monolith growing back inside a jar |
| **common-events** | the `.avsc` files that generate the event classes | the wire contract. Both producer and consumer compile against the same jar |

---

## 5. The five couplings that break — and what replaces them

These are the lines in today's code that stop compiling the moment the services are separate. Each
one needs a deliberate answer *before* you move files.

**1. `QuizServiceImpl` calls `userService.requireByUsername(username)` to set the quiz owner.**
quiz-service will have no `app_user` table. Replace with `owner_id` + `owner_username` read straight
from the JWT — no network call, provided the token carries `uid`.

**2. `QuizServiceImpl.delete()` calls `attemptRepository.countByQuizId(quizId)`.**
quiz-service reaching into attempt data. Three ways out:
- (a) attempt-service exposes an internal count endpoint — keeps today's behaviour exactly;
- (b) quiz-service keeps a play counter fed by `quiz.submitted.v1` — no sync call, but eventually consistent, so a delete can slip through in the gap;
- (c) simply forbid deleting anything that is not `DRAFT`.

(c) is the smallest and matches the intent ("deleting is for mistakes"). Add (a) later if you want the
exact old message.

**3. `AttemptServiceImpl.join()` reads the whole `Quiz` entity, questions and correct answers included.**
Becomes the internal HTTP call `GET /internal/quizzes/by-code/{joinCode}`. But the call alone is not
enough — see the next point.

**4. `AttemptAnswer` has a foreign key to `question.id`; `review()` reads `question.getOptionA()`, `getCorrectOption()` at *review* time.**
That FK cannot cross databases, and re-fetching the question later is worse: quiz-service may have
changed or deleted it, and the review would then show a different question than the one that was
scored. **attempt-service must snapshot every question into its own database at join time** — text,
all four options, the correct option, the position — and score, review, and re-open from that copy.
This is the single most important change in the migration.

**5. `leaderboard()` reads `attempt.getUser().getUsername()` and `getDisplayName()`, and `findByIdWithAnswers` join-fetches `a.user` and `a.quiz`.**
Cross-database joins. Every one of those fields must be **denormalised**: copied onto the attempt row
at join time (quiz title, category), and carried on the `QuizSubmitted` event (username, displayName,
quizTitle) so stats-service never has to ask anyone.

> The pattern behind all five: **a monolith joins, a distributed system copies.** Every `getX().getY()`
> that crosses a module is either a network call you must justify, or a field you must copy at the
> moment it was true.

---

## 6. Suggested order of work

Each step leaves the system running. Do not do them all at once.

| # | Step | Why this order |
| --- | --- | --- |
| 1 | Fix the JWT claims **in the monolith** (`uid`, `name`, `roles`) | every service depends on this; changing it later means re-issuing every token |
| 2 | Denormalise inside the monolith: copy `quizTitle`, `category`, `username`, `displayName` onto the attempt; add the question snapshot table | pure refactor, fully testable with one database, and it is the hard part |
| 3 | Stand up `infra/` (postgres + kafka + schema registry) and the empty multi-module skeleton | nothing depends on it yet |
| 4 | Extract **user-service** | zero inbound dependencies — the easiest first cut |
| 5 | Extract **quiz-service** | depends only on the JWT |
| 6 | Extract **attempt-service** + the internal quiz call | step 2 already did the hard work |
| 7 | Add **gateway** and re-point the frontend at :8080 | the frontend's URLs stop changing from here on |
| 8 | Add Kafka + **stats-service**, move the leaderboard last | the only endpoint whose consistency model actually changes |

Step 2 is where a migration is won or lost. If the monolith still passes every test after you have
denormalised and snapshotted, the split is mostly mechanical. If you split first and denormalise
after, you debug across five processes instead of one.
