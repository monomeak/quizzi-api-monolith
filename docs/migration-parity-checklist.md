# Parity Checklist — what must stay true after the split

**Read this with:** [current-microservices.md](current-microservices.md) (the target design) and
[migration-service-blueprint.md](migration-service-blueprint.md) (where the files go).

The target design was written from an *earlier* version of the monolith. Since then the app grew a
review endpoint, quiz update/delete/close, and different auth paths. This doc is the diff: every
place where **the monolith does something the microservice design does not yet account for**, plus
the rules that keep behaviour identical once it is split.

Legend: ❌ missing from the design · ⚠️ named differently · 🔍 needs a decision

---

## 1. Endpoint parity

Everything the monolith actually serves today, checked against
[current-microservices.md §2](current-microservices.md).

### Auth — `AuthController`

| Monolith (real) | Design says | Status |
| --- | --- | --- |
| `POST /api/v1/auth/signup` → `UserResponse` | `POST /api/v1/users` → `TokenResponse` | ⚠️ **different path _and_ different response.** The monolith registers you but does **not** log you in |
| `POST /api/v1/auth/signin` → `LoginResponse` | `POST /api/v1/auth/login` | ⚠️ path |
| *(does not exist)* | `GET /api/v1/users/me` | 🔍 the design invents this. Fine to add, but it is **new work**, not migration |

`LoginResponse` is `{ accessToken, tokenType: "Bearer", expiresInSeconds, user }` — keep those exact
field names or the frontend breaks.

> **Pick one spelling now.** Either update the design to `signup`/`signin`, or change the monolith
> and the frontend first. Deciding during the split means debugging the gateway and the frontend at
> the same time.

There is also a dead rule in `SecurityConfig`: `POST /api/v1/users` is `permitAll()` but no
controller maps it. Delete it, or implement it — do not carry a half-fact into the gateway config.

### Quizzes — `QuizController` → quiz-service

| Monolith (real) | Auth | In the design? |
| --- | --- | --- |
| `POST /api/v1/quizzes` | 🔒 | ✅ |
| `PUT /api/v1/quizzes/{id}` | 🔒 owner | ❌ **missing** |
| `DELETE /api/v1/quizzes/{id}` | 🔒 owner | ❌ **missing** — and it is the one that needs attempt data |
| `GET /api/v1/quizzes/mine` | 🔒 | ✅ |
| `GET /api/v1/quizzes/published` | 🔒 | ✅ |
| `GET /api/v1/quizzes/{id}` | 🔒 owner | ✅ |
| `POST /api/v1/quizzes/{id}/publish` | 🔒 owner | ✅ (plus: must now emit `QuizPublished`) |
| `POST /api/v1/quizzes/{id}/close` | 🔒 owner | ❌ **missing** |

Rules that must survive with the endpoints:

- **PUT is full-replace.** An omitted `description` clears it — that is deliberate PUT semantics.
- **Questions may only be replaced while `DRAFT`.** Rewriting a live quiz's questions would rewrite
  history, because every attempt stores one answer per question id.
- **Delete is refused once anybody has played.** This is the cross-service call — see §4.
- **`publish` does not currently check that the quiz has questions.** Publishing an empty quiz gives
  a `scorePercent` of 0 for everyone. Worth fixing before it becomes an event other services consume.

### Attempts — `AttemptController` → attempt-service

| Monolith (real) | Auth | In the design? |
| --- | --- | --- |
| `POST /api/v1/attempts/join` | 🔒 | ✅ |
| `GET /api/v1/attempts/{id}` | 🔒 owner | ✅ |
| `POST /api/v1/attempts/{id}/submit` | 🔒 owner | ✅ (plus: must now emit `QuizSubmitted`) |
| `GET /api/v1/attempts/me` | 🔒 | ✅ |
| `GET /api/v1/attempts/{id}/review` | 🔒 owner | ❌ **missing — and it is the newest, richest feature** |

`GET /attempts/{id}/review` is the endpoint most at risk in this migration, because it reads question
text and correct answers **after** the fact. It returns:

```
attemptId, quizId, quizTitle, category, status,
correctCount, incorrectCount, skippedCount, totalQuestions,
scorePercent, passed, summary,
startedAt, submittedAt, durationSeconds,
answers[ questionId, position, questionText, optionA..D,
         selectedOption, selectedOptionText,
         correctOption, correctOptionText,
         correct, verdict, feedback ]
```

Business rules living inside it, all of which must move to attempt-service **whole**:

- `PASS_MARK = 50.0` decides `passed`.
- `verdict` is `CORRECT` / `INCORRECT` / `SKIPPED` — skipped scores the same as incorrect and exists
  only so the screen can say "you left this blank".
- `feedback` and `summary` are fixed wording tiers (100 / 80 / 50 / 30). Copy the strings exactly;
  the frontend shows them verbatim.
- `scorePercent` is **read back from the stored column**, never recomputed, so the review always
  matches the leaderboard.
- Reviewing an unfinished attempt is a `409`, not a `404`.

### Leaderboard → stats-service

| Monolith (real) | Auth | In the design? |
| --- | --- | --- |
| `GET /api/v1/quizzes/{quizId}/leaderboard` | 🔓 public | ✅ |

Entry shape: `{ rank, username, displayName, correctCount, totalQuestions, scorePercent, submittedAt }`.
`rank` is computed 1..n by the server — stats-service must produce it too, not leave it to the client.
Order: `scorePercent DESC`, then `submittedAt ASC`. Top 20 only. `SUBMITTED` attempts only.

### Questions — `QuestionController` 🔍 **decide, do not port blindly**

| Monolith (real) | Reality |
| --- | --- |
| `GET /questions/all` | no `/api/v1` prefix, so the gateway would not route it anyway |
| `GET /questions/{id}` | returns the raw `Question` **entity** |

Both are authenticated only by `anyRequest().authenticated()`, and the entity includes
**`correctOption`**. Any logged-in user can read every correct answer for every quiz in the system,
including quizzes they are about to play.

This looks like leftover scaffolding from early learning. Three options, in order of preference:

1. **Delete it.** Nothing in the app calls it.
2. Move it to `/api/v1/internal/questions/**` — reachable by services, never by the gateway.
3. Keep it public but scope it to the owner and strip `correctOption` from the response.

Whatever you choose, do not carry option 0 (port it as-is) across the boundary. The split is a good
moment to close it.

---

## 2. The JWT contract — decide this before you move a single file

Today, every module resolves the caller with `userService.requireByUsername(username)` — a database
lookup. After the split **only user-service has that table**, so whatever the other services need must
be *inside the token*.

| Claim | Monolith issues | Design expects | Verdict |
| --- | --- | --- | --- |
| `sub` | username | username | ✅ same |
| `role` | single string, `"USER"` | — | ⚠️ |
| — | | `roles`, array `["USER"]` | ⚠️ rename + retype |
| — | *(absent)* | `uid` — user id | ❌ **must be added** |
| — | *(absent)* | `name` — display name | ❌ **must be added** |
| `iss` | `quiz-simple-app` | (unstated) | 🔍 must be identical everywhere |

Why `uid` and `name` are not optional:

- **`uid`** — quiz-service sets `owner_id` and attempt-service sets `user_id`. Without it, both would
  have to call user-service on *every single write*, which turns a trivial local read into a network
  hop and a new failure mode.
- **`name`** — the leaderboard shows `displayName`. Without it in the token, attempt-service cannot
  put it on the `QuizSubmitted` event, and stats-service has no way to get it at all.

Two more things that will bite:

- `JwtService.toAuthentication()` calls `requireIssuer(...)`. **Every service must be configured with
  the same issuer string and the same `JWT_SECRET`**, or every request 401s with a message that does
  not mention the issuer.
- Claim names belong in `common-web` as constants (`JwtClaims`), not as string literals in five
  services. They are a contract exactly like the Avro schemas.

**Do this change in the monolith first**, while there is one place to change it and one test suite to
run.

---

## 3. Data ownership — the FKs that cannot survive

Today, one database enforces all of this with foreign keys. After the split, four databases cannot
reference each other, and the constraints simply disappear.

| Table | Column | Today | After |
| --- | --- | --- | --- |
| `quiz` | `owner_id` | `REFERENCES app_user(id)` | plain `BIGINT`, **plus** a copied `owner_username` |
| `quiz_attempt` | `quiz_id` | `REFERENCES quiz(id) ON DELETE CASCADE` | plain `BIGINT`, **plus** copied `quiz_title`, `category` |
| `quiz_attempt` | `user_id` | `REFERENCES app_user(id)` | plain `BIGINT`, **plus** copied `username`, `display_name` |
| `attempt_answer` | `question_id` | `REFERENCES question(id)` | plain `BIGINT` pointing at the **snapshot**, not at quiz-service |
| `quiz_attempt` | `(quiz_id, user_id)` | `UNIQUE` | ✅ survives — both columns stay inside `attemptdb` |

### The snapshot table — the one piece of new schema

attempt-service needs its own copy of the questions, written once at join time:

> `attempt_question` — `attempt_id`, `source_question_id`, `position`, `question_text`,
> `option_a`, `option_b`, `option_c`, `option_d`, `correct_option`

Everything after join reads *this*, never quiz-service:

- **submit** scores against the snapshot;
- **re-open** (`GET /attempts/{id}`) serves questions from the snapshot **with `correct_option`
  stripped out** — exactly what `AttemptQuestionResponse` does today;
- **review** reads option text and the correct answer from the snapshot.

Without it, `review()` re-reads a question that quiz-service may have changed, and a player is shown a
different question than the one they were scored on. The monolith is protected from this by the
DRAFT-only edit rule and the delete guard — protections you no longer control from attempt-service.

The snapshot also means **only one network call per attempt**, at join. Submit, re-open and review
stay purely local, which is what keeps scoring fast and available when quiz-service is down.

---

## 4. Every cross-module call, and its replacement

Grep the monolith for these; each one is a boundary.

| # | In the monolith | Becomes | Cost |
| --- | --- | --- | --- |
| 1 | `QuizServiceImpl` → `userService.requireByUsername()` | read `uid` + `sub` from the JWT | free |
| 2 | `QuizServiceImpl.delete()` → `attemptRepository.countByQuizId()` | 🔍 internal call, or an event-fed counter, or forbid delete unless `DRAFT` | see below |
| 3 | `AttemptServiceImpl.join()` → `quizRepository.findByJoinCode()` | `GET /internal/quizzes/by-code/{code}` + snapshot | 1 HTTP call per attempt |
| 4 | `AttemptServiceImpl.join()/myHistory()` → `userService.requireByUsername()` | JWT claims | free |
| 5 | `AttemptServiceImpl.submit()` → `quiz.getQuestions()` | the snapshot | free |
| 6 | `AttemptServiceImpl.review()` → `answer.getQuestion().getOptionA()` | the snapshot | free |
| 7 | `AttemptServiceImpl.leaderboard()` → `attempt.getUser()` | denormalised columns + event fields | free |
| 8 | `AttemptServiceImpl.leaderboard()` → `quizRepository.existsById()` | stats-service's own `quiz_ref`, filled from `quiz.published.v1` | free, but see §5 |
| 9 | `findByIdWithAnswers` `join fetch a.user, a.quiz` | plain columns on `quiz_attempt` | free — and one fewer join |

**On #2 (the delete guard):** the monolith's message is *"3 people have already played this quiz —
close it instead of deleting it"*. To keep it word-for-word you need a synchronous count from
attempt-service. If you would rather not add that dependency, forbidding delete on anything past
`DRAFT` gives the same protection with a different message. Either is defensible — just choose
deliberately, because the design doc does not mention `DELETE` at all.

---

## 5. Behaviour that genuinely changes

Not everything can stay identical. These are the ones to accept knowingly and tell the frontend about.

| Case | Monolith | After the split |
| --- | --- | --- |
| Leaderboard right after submit | the score is there immediately — same transaction | **eventually consistent**: durable in `attemptdb` at once, visible on the board after stats-service consumes the event |
| Leaderboard for a `DRAFT` quiz | `200 []` — `existsById` passes, no attempts match | **`404`** — stats-service has never heard of an unpublished quiz. 🔍 Decide: 404, or return `200 []` for any unknown id |
| Leaderboard for a nonexistent quiz | `404` | `404` ✅ |
| quiz-service is down | impossible | **join fails**; submit / review / history keep working, because they only read the snapshot |
| Process dies between commit and event send | impossible | **the event is lost** — publishers are `@TransactionalEventListener(AFTER_COMMIT)`. The fix is the outbox pattern; the design deliberately leaves it out |
| Publishing the same quiz twice | just re-sets the status | emits a second `QuizPublished`. Consumers dedupe on `eventId`, so make the handler an upsert |
| Player's answers | in one database with everything else | in `attemptdb` **only** — deliberately *not* on the `QuizSubmitted` event. Anyone who can read the topic can read every field, so the payload is an access-control decision |

---

## 6. Config parity

Small things that silently change behaviour if they are not carried over.

| Setting | Monolith | After |
| --- | --- | --- |
| `spring.jpa.open-in-view` | `false` | keep `false` in all four data services |
| `server.error.include-message` | `never` | keep — `GlobalExceptionHandler` formats errors, not Spring |
| CORS | in every app, `allowed-origins: http://localhost:3000`, `/api/**` | **gateway only.** Two layers of CORS produces duplicate headers, which browsers reject |
| Public paths | listed in `SecurityConfig` | same list, but split across services + gateway. Do not let one drift |
| Swagger | `/swagger-ui.html` per app | one per service; optionally aggregated at the gateway |
| Actuator | `health,info` | same, and now it is what tells you *which* of the five is unhealthy |
| Flyway | `db/migration` + `db/seed`, `clean-disabled: false` | per service, numbered from `V1` again. **Never** enable clean outside dev |
| `ddl-auto` | `validate` | keep `validate` — migrations own the schema, Hibernate only checks it |
| Ports | one, `8080` | 8080 gateway, 8081–8084 services. Postgres in compose is on `5430`, but the design assumes `5432` — 🔍 pick one and fix the other |

---

## 7. Done-when: the acceptance walk

The migration is finished when this sequence behaves the same through the gateway as it does against
the monolith today. Run it end to end **before** you split, record the responses, then run it again
after.

1. `POST /api/v1/auth/signup` — a fresh user → `201`, `UserResponse`, no token.
2. `POST /api/v1/auth/signin` → `200`, a token that the other four services accept.
3. `POST /api/v1/quizzes` with 3 questions → `201`, status `DRAFT`, a 6-character join code.
4. `PUT /api/v1/quizzes/{id}` replacing the questions → `200`, positions renumbered from 1.
5. `POST /api/v1/quizzes/{id}/publish` → `200`, status `PUBLISHED`.
6. Second user: `POST /api/v1/attempts/join` with that code → `201`, questions **without**
   `correctOption`.
7. **Owner** tries to join their own quiz → `400`.
8. `POST /api/v1/attempts/{id}/submit`, one answer omitted → correct score, the omitted one comes back
   `correct: false`.
9. Submit the same attempt again → `409`.
10. `GET /api/v1/attempts/{id}/review` → verdicts `CORRECT` / `INCORRECT` / `SKIPPED`, the exact
    feedback strings, `passed` matching the 50% mark, and `scorePercent` **identical** to step 8.
11. `GET /api/v1/attempts/me` → the attempt, with the quiz title and category on it.
12. `GET /api/v1/quizzes/{id}/leaderboard` with no token → the entry appears, `rank: 1`.
    *(After the split, allow a moment — this is the one eventually-consistent step.)*
13. `DELETE /api/v1/quizzes/{id}` as the owner → refused, because somebody has played it.
14. `POST /api/v1/quizzes/{id}/close` → `200`, and a new join with that code is refused.
15. Any `/api/v1/internal/**` path through the gateway → `404`.
16. Stop stats-service, wipe `statsdb`, start it again → the leaderboard rebuilds itself from the
    topic. *(This one has no monolith equivalent — it is the payoff.)*

Steps 10 and 16 are the two worth writing down first. Step 10 is the feature most likely to be
silently broken by the split; step 16 is the reason you did it.
