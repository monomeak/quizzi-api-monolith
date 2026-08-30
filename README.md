# quizzi-api-monolith

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![Status](https://img.shields.io/badge/status-learning%20project-blueviolet)

> 🎓 **A learning project.** Built to actually understand Spring Boot — layered architecture,
> JWT security, Flyway migrations, MapStruct, and the road from a monolith to microservices.
> Read it, fork it, break it, steal the bits you like.

`quizzi-api-monolith` is the primary RESTful backend for **Quizzi**, built with Java and Spring Boot.
It handles core business logic, authentication, quiz operations, scoring, and data persistence.

## Tech Stack

| Area       | Choice                                                                |
| ---------- | --------------------------------------------------------------------- |
| Language   | Java 21                                                               |
| Framework  | Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation, Actuator) |
| Database   | PostgreSQL 16                                                         |
| Migrations | Flyway                                                                |
| Auth       | JWT (jjwt 0.12.6)                                                     |
| Mapping    | MapStruct + Lombok                                                    |
| Docs       | springdoc-openapi (Swagger UI)                                        |
| Build      | Maven (`./mvnw`)                                                      |

## Getting Started

### Prerequisites

- JDK 21
- Docker (for the local PostgreSQL instance)

### 1. Start the database

```bash
docker compose up -d
```

This runs PostgreSQL on host port `5430` with database `quizdb` (user `postgres`).

### 2. Configure environment

```bash
cp .env.example .env.dev
```

Edit `.env.dev` and make sure `DB_URL` points at the port Docker exposed:

```
DB_URL=jdbc:postgresql://localhost:5430/quizdb
DB_USERNAME=postgres
DB_PASSWORD=superpassword
JWT_SECRET=<openssl rand -base64 48>
```

### 3. Run the app

```bash
./mvnw spring-boot:run
```

Flyway applies the schema and (in the `dev` profile) the demo seed data on startup.

The API is available at `http://localhost:8080`.

- Swagger UI — http://localhost:8080/swagger-ui.html
- OpenAPI JSON — http://localhost:8080/v3/api-docs
- Health — http://localhost:8080/actuator/health

## Project Structure

```
src/main/java/com/sokmeak/quizapp/
├── common/       # global exception handling, request logging
├── config/       # security, CORS, OpenAPI configuration
├── constant/     # shared constants
├── modules/
│   ├── auth/     # signup, signin, JWT issuing and filtering
│   ├── user/     # user entity, roles (ADMIN, USER)
│   ├── quiz/     # quiz + question authoring, publish/close lifecycle
│   ├── question/ # question lookup
│   └── attempt/  # joining, submitting, scoring, review, leaderboard
└── utils/
src/main/resources/db/
├── migration/    # V1..V4 — real schema, applied in every profile
└── seed/         # V5 — demo data, dev profile only
```

## API Overview

All endpoints are under `/api/v1`. Everything requires a `Bearer` token except where noted.

### Auth

| Method | Path           | Notes                  |
| ------ | -------------- | ---------------------- |
| `POST` | `/auth/signup` | public                 |
| `POST` | `/auth/signin` | public — returns a JWT |

### Quizzes

| Method   | Path                        | Description                 |
| -------- | --------------------------- | --------------------------- |
| `POST`   | `/quizzes`                  | Create a quiz               |
| `GET`    | `/quizzes/mine`             | Quizzes owned by the caller |
| `GET`    | `/quizzes/published`        | Published quizzes           |
| `GET`    | `/quizzes/{id}`             | Quiz detail                 |
| `PUT`    | `/quizzes/{id}`             | Update a quiz               |
| `DELETE` | `/quizzes/{id}`             | Delete a quiz               |
| `POST`   | `/quizzes/{id}/publish`     | Open the quiz for attempts  |
| `POST`   | `/quizzes/{id}/close`       | Stop accepting attempts     |
| `GET`    | `/quizzes/{id}/leaderboard` | public                      |

### Attempts

| Method | Path                    | Description                              |
| ------ | ----------------------- | ---------------------------------------- |
| `POST` | `/attempts/join`        | Join a quiz by code and start an attempt |
| `GET`  | `/attempts/{id}`        | Re-open an in-progress attempt           |
| `POST` | `/attempts/{id}/submit` | Submit answers and get the score         |
| `GET`  | `/attempts/me`          | The caller's attempt history             |
| `GET`  | `/attempts/{id}/review` | Per-question review with feedback        |

## Profiles

| Profile         | Behaviour                                                                                     |
| --------------- | --------------------------------------------------------------------------------------------- |
| `dev` (default) | Reads `.env.dev`, seeds demo data, SQL logging on, Swagger on, `flyway.clean` allowed         |
| `prod`          | Reads `.env.prod`, no seed data, no SQL logging, Swagger off by default, `ddl-auto: validate` |

Switch with `SPRING_PROFILES_ACTIVE=prod`.

> `.env*` files are gitignored except `.env.example`. Never commit real secrets — `prod` has no fallback `JWT_SECRET`, so it must be supplied.

## Build & Test

```bash
./mvnw test            # run tests
./mvnw clean package   # build the executable jar
java -jar target/quizapp-0.0.1-SNAPSHOT.jar
```

## Docs

## Roadmap

- [x] Auth with JWT (signup / signin, `USER` and `ADMIN` roles)
- [x] Quiz authoring with a publish / close lifecycle
- [x] Attempts: join by code, submit, auto-scoring
- [x] Per-question review with feedback and verdict flags
- [x] Public leaderboard per quiz
- [ ] Refresh tokens
- [ ] Rate limiting on attempt submission
- [ ] Split into microservices — see [docs/migration-service-blueprint.md](docs/migration-service-blueprint.md)

## Contributing

PRs and issues are welcome — especially the "you're doing this wrong, here's why" kind.
That feedback is the whole point of the project.

1. Fork it and branch off `development`
2. Keep the module layout (`controller → service → repository`, DTOs at the edges)
3. Add a Flyway migration for any schema change — never edit an applied one
4. Run `./mvnw test` before opening the PR

## License

[MIT](LICENSE) © monomeak

Free to use, copy, learn from, and rebuild. No warranty — this is coursework that grew legs.

## Acknowledgements

Built while learning Java and Spring Boot, with thanks to the Spring, Flyway, MapStruct,
and jjwt teams for documentation that is genuinely good to read.
