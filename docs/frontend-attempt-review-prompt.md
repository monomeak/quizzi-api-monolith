# Attempt Review - API contract & frontend prompt

## What was added

`GET /api/v1/attempts/{attemptId}/review` - the "how did I do?" screen for one
finished attempt. Requires the JWT of the player who owns the attempt.

The rule it encodes:

| The player | `verdict`  | `feedback` text                                                 |
|------------|------------|-----------------------------------------------------------------|
| answered correctly | `CORRECT`   | `Well done! That is the right answer.`                  |
| answered wrongly   | `INCORRECT` | `Not quite. The correct answer was A. 0`                |
| left it blank      | `SKIPPED`   | `You skipped this one. The correct answer was D. java.util` |

The backend already writes the sentence, so the client can render `feedback`
straight onto the card and use `verdict` only to pick the colour/icon.

### Where it fits in the existing flow

1. `POST /api/v1/attempts/join` - `{ "joinCode": "K4P2QX" }` -> questions, no answers.
2. `POST /api/v1/attempts/{id}/submit` -> score.
3. `GET  /api/v1/attempts/me` -> list of past attempts (each has `attemptId`).
4. **`GET  /api/v1/attempts/{id}/review`** -> full breakdown. *(new)*

Correct answers are never exposed before submission: the review returns **409**
while the attempt is still `IN_PROGRESS`.

### Example response - 200

```json
{
  "attemptId": 12,
  "quizId": 3,
  "quizTitle": "Java Basics",
  "category": "Programming",
  "status": "SUBMITTED",
  "correctCount": 3,
  "incorrectCount": 1,
  "skippedCount": 1,
  "totalQuestions": 5,
  "scorePercent": 60.0,
  "passed": true,
  "summary": "You passed - 3 out of 5 correct.",
  "startedAt": "2026-08-28T10:02:11",
  "submittedAt": "2026-08-28T10:07:45",
  "durationSeconds": 334,
  "answers": [
    {
      "questionId": 21,
      "position": 1,
      "questionText": "Which keyword makes a field constant in Java?",
      "optionA": "const",
      "optionB": "final",
      "optionC": "static",
      "optionD": "sealed",
      "selectedOption": "B",
      "selectedOptionText": "final",
      "correctOption": "B",
      "correctOptionText": "final",
      "correct": true,
      "verdict": "CORRECT",
      "feedback": "Well done! That is the right answer."
    },
    {
      "questionId": 22,
      "position": 2,
      "questionText": "What is the default value of an int field?",
      "optionA": "0",
      "optionB": "1",
      "optionC": "null",
      "optionD": "undefined",
      "selectedOption": "C",
      "selectedOptionText": "null",
      "correctOption": "A",
      "correctOptionText": "0",
      "correct": false,
      "verdict": "INCORRECT",
      "feedback": "Not quite. The correct answer was A. 0"
    },
    {
      "questionId": 23,
      "position": 3,
      "questionText": "Which package holds ArrayList?",
      "optionA": "java.io",
      "optionB": "java.lang",
      "optionC": "java.nio",
      "optionD": "java.util",
      "selectedOption": null,
      "selectedOptionText": null,
      "correctOption": "D",
      "correctOptionText": "java.util",
      "correct": false,
      "verdict": "SKIPPED",
      "feedback": "You skipped this one. The correct answer was D. java.util"
    }
  ]
}
```

`optionC` / `optionD` are nullable - a question may have only two options.
`answers` comes back sorted by `position`.

### Errors

| Status | When                                        | Body                                                |
|--------|---------------------------------------------|-----------------------------------------------------|
| 401    | missing / expired token                     | `{ "message": "..." }`                              |
| 403    | the attempt belongs to somebody else        | `{ "message": "You do not have permission to do that" }` |
| 404    | no such attempt                             | `{ "message": "Attempt not found: 99" }`            |
| 409    | attempt still `IN_PROGRESS`                 | `{ "message": "Submit this attempt before reviewing it" }` |

---

## Prompt to hand to the frontend

> Build a **Quiz Review** screen.
>
> **Data:** `GET /api/v1/attempts/{attemptId}/review` with header
> `Authorization: Bearer <token>`. Reach it from the history list
> (`GET /api/v1/attempts/me`) - each row has `attemptId` - and from the
> submit-result screen via a "Review answers" button.
>
> **Header section** - use the top-level fields as they are, do not recompute:
> `quizTitle`, `category`, `summary` as the headline, a big
> `scorePercent`% with `correctCount` / `totalQuestions` under it, three small
> counters for `correctCount`, `incorrectCount`, `skippedCount`, and the time
> taken from `durationSeconds` (format as `m:ss`). Show a pass/fail badge from
> the boolean `passed`.
>
> **Answer list** - one card per item in `answers`, already in display order:
> - Show `position` and `questionText`.
> - List the four options (`optionA`..`optionD`; skip the null ones), labelled
>   A/B/C/D.
> - Style each option by comparing its letter with `selectedOption` and
>   `correctOption`: green for the correct one, red for the player's pick when
>   it was wrong, neutral for the rest. Mark the player's pick with "Your answer"
>   and the right one with "Correct answer".
> - Render `feedback` verbatim as the note under the options - the backend
>   already writes the sentence. Use `verdict` (`CORRECT` / `INCORRECT` /
>   `SKIPPED`) only to choose the colour and icon: green check, red cross,
>   grey dash.
> - When `selectedOption` is null the player skipped it: show "Not answered"
>   instead of a highlighted pick.
>
> **Filter** - a small toggle above the list: All / Wrong only / Skipped only,
> filtering on `verdict` client-side.
>
> **Error states:** 409 means the attempt is not submitted yet - send the user
> back to the quiz instead of rendering the review. 403 and 404 both mean
> "this review is not yours" - show a friendly empty state with a link back to
> the history page. 401 - redirect to login.
>
> Keep it responsive: cards stack on mobile, and the header sticks to the top
> while the answer list scrolls.
