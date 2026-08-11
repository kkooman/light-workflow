# Copilot instructions for light-workflow

## Repository summary
This repository is a Java 21, Gradle 8, Spring Boot 3.4.5 application that combines MyBatis persistence with a Lucene-based AML watchlist search layer. The database is the source of truth for watchlist rows, while the Lucene index is stored on disk in a configurable filesystem path so multiple servers can share a persisted index.

The codebase is organized around a `watchlist` feature package with search, API, repository, and config classes. It includes H2-backed local development defaults, file-backed Lucene indexing, fuzzy/typo search support, weighted field scoring, and operational endpoints for rebuild/sync/index status.

## Project profile
- Language: Java 21
- Build tool: Gradle 8 (wrapper included)
- Framework: Spring Boot 3.4.5
- Persistence: MyBatis + H2 for local/dev
- Search: Apache Lucene 9.12.1
- Primary domain: AML watchlist search and indexing

## Required runtime and environment
- Always use Java 21. The project sets the toolchain in `build.gradle`.
- Use the Gradle wrapper instead of a system Gradle install: `./gradlew ...`
- For app startup, the default server port is `8080`.
- `WATCHLIST_INDEX_PATH` can override the filesystem Lucene index location. Default: `./data/lucene/watchlist`
- For multi-server deployments, ensure the index path is on a shared or shared-access filesystem. Do not treat the Lucene index as in-memory.

## Bootstrap / build / test / run commands
Run these commands in order from the repository root:

```bash
./gradlew clean test --rerun-tasks
./gradlew bootRun
```

Validated commands:
- `./gradlew clean test --rerun-tasks` -> BUILD SUCCESSFUL
- `./gradlew bootRun` -> Spring Boot starts successfully on port 8080

Optional build/check commands:

```bash
./gradlew clean build
```

Use `./gradlew` for all build/test/run tasks. There is no custom lint task configured in this repository.

## Repo layout and architecture
Key files and directories:

- `README.md` – project overview, runtime requirements, validation flow, and watchlist behavior
- `build.gradle` – Java 21 toolchain, Spring Boot plugin, MyBatis, Lucene, and test dependencies
- `settings.gradle` – Gradle project settings
- `src/main/resources/application.yml` – app config, datasource, MyBatis, and watchlist scoring/index settings
- `src/main/java/com/kkooman/lightworkflow/watchlist/` – main feature package
  - `api/` – controller and request/response contracts
  - `service/` – core Lucene search, scoring, rebuild/sync, and risk classification logic
  - `repository/` – MyBatis mappers and persistence helpers
  - `config/` – watchlist search properties/config
- `src/main/resources/mapper/` – MyBatis XML mapper definitions
- `src/test/java/` – unit/integration tests for watchlist logic and API behavior

## Watchlist-specific facts important to changes
- The DB is the source of truth. Watchlist entries are loaded from DB and indexed into Lucene.
- Search scoring is configured under `watchlist.search.field-weights` in `application.yml`.
- Risk thresholds are configured under:
  - `watchlist.search.high-risk-threshold`
  - `watchlist.search.review-threshold`
- Search and audit behavior should avoid logging raw sensitive search terms; hashes are preferred.
- The mapper XML assumes DB table names/columns such as `watchlist_entry` and `watchlist_search_audit`. Match the real schema before production deployment.
- If the real AML schema differs from the mapper assumptions, update the mapper SQL rather than changing unrelated application logic.

## Validation expectations
Before finalizing changes, run the narrowest relevant validation:

```bash
./gradlew clean test --rerun-tasks
```

This repository does not define a separate lint pipeline. The project’s effective validation gate is the Gradle test task.

## Working rules for this repo
- Prefer the existing Gradle wrapper and project conventions over ad hoc scripts.
- Keep changes aligned with the Spring Boot + MyBatis + Lucene architecture already in use.
- Do not replace the file-backed Lucene index approach with in-memory storage for production behavior.
- Favor small, focused edits; this project is feature-oriented and grouped by domain package.
- If a required fact is missing from these instructions, search only the project’s README, `build.gradle`, `application.yml`, and relevant feature package files. Do not do broad repo exploration unless necessary.
- Trust these instructions. Only perform additional search when the provided project information is incomplete or clearly incorrect.
