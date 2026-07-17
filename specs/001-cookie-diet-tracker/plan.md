# Implementation Plan: Cookie Monster Cookie-Intake Tracker & AI Diet Coach

**Branch**: `001-cookie-diet-tracker` | **Date**: 2026-07-16 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/001-cookie-diet-tracker/spec.md`

## Summary

A simple single-service Akka web app that tracks Cookie Monster's daily cookie intake and produces a friendly "diet coach" recommendation that never asks him to give up cookies. The backend is an Akka Event Sourced Entity (intake log + latest recommendation) exposed through one HTTP endpoint. The frontend is a small React (TypeScript/Vite) single-page app served as static content by the Akka service. Per the user's cost constraint, the first prototype **mocks the LLM**: recommendations are produced by a deterministic domain rule engine (`DietRecommender`), with a documented drop-in path to a real Akka `DietCoachAgent` later. No external LLM calls, $0 to run.

## Technical Context

**Language/Version**: Java 21+ (Akka SDK 3.4+) backend; TypeScript + React (Vite) frontend
**Primary Dependencies**: Akka SDK (Event Sourced Entity, HTTP Endpoint, static content); React + Vite (dev/build only, not a runtime service dependency)
**Storage**: Akka Event Sourced Entity journal (built-in, replicated) — no external database
**AI / LLM**: **Mocked for prototype.** Deterministic `DietRecommender` domain function produces the recommendation; no model provider is configured and no network calls are made. Contract (`DietRecommendation`) is kept LLM-shaped so a real `DietCoachAgent` is a later swap.
**Testing**: JUnit 5, `EventSourcedTestKit` (entity), `TestKitSupport` + `httpClient` (endpoint integration), AssertJ. Frontend: manual/browser verification for the prototype.
**Target Platform**: Akka service (single deployable), local dev via `mvn compile exec:java`; deploy to Akka platform later.
**Project Type**: Web application (Akka backend service + React SPA served as static content)
**Performance Goals**: Interactive prototype latencies — log/summary responses well under the 5s / 30s user-facing targets in the spec's Success Criteria. No high-throughput requirement.
**Constraints**: $0 runtime cost (no LLM spend); single subject; simple/minimal component surface.
**Scale/Scope**: One tracked subject (Cookie Monster), small data volume, family use.

## Constitution Check

*GATE: Checked before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Akka SDK First (NON-NEGOTIABLE) | ✅ Pass | All state, events, API, and AI use Akka SDK primitives: Event Sourced Entity + HTTP Endpoint + static content + a real Akka `Agent` (`DietCoachAgent`) for recommendations. The agent's LLM is mocked with a `ModelProvider.Custom` (`MockDietModelProvider`) so the prototype runs fully offline with no API key — honoring "mock any LLMs … don't want to spend on Gemini." React/Vite are build-time-only tooling, not runtime dependencies. |
| II. Design Principles | ✅ Pass | Domain logic (`CookieLog`, `DietRecommender`) is framework-independent and unit-testable; endpoint defines its own request/response records (API isolation); components are single-purpose; names are domain-aligned (`CookieLogEntity`, `DietRecommendation`, no generic `Manager`/`Service`). |
| III. Test Coverage | ✅ Pass | Plan includes domain unit tests (`DietRecommender`), entity unit tests (`EventSourcedTestKit`), and endpoint integration tests (`TestKitSupport`). |
| IV. Simplicity (YAGNI) | ✅ Pass | One entity, no premature View (single subject → dashboard reads come from entity state), deterministic recommender instead of speculative AI infra. |

**Result**: PASS (one justified deviation, tracked below). No unresolved gate violations.

## Project Structure

### Documentation (this feature)

```
specs/001-cookie-diet-tracker/
├── spec.md
├── plan.md              # this file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── http-api.md      # Phase 1 output — HTTP contract
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```
src/main/java/com/example/
├── domain/
│   ├── CookieMonsterProfile.java     # fixed profile (name, height, concern)
│   ├── CookieEntry.java              # one logged intake event's data
│   ├── DailyTotal.java               # (day, totalCount) summary row
│   ├── DietRecommendation.java       # allowance (>0), tips, rationale, date
│   ├── CookieLog.java                # entity STATE + business logic
│   ├── CookieLogEvent.java           # sealed interface + @TypeName events
│   └── DietRecommender.java          # deterministic mock "AI" recommendation
├── application/
│   └── CookieLogEntity.java          # Event Sourced Entity (log/remove/recommend/read)
└── api/
    └── CookieDietEndpoint.java       # HTTP endpoint + request/response records + static content

src/main/resources/
├── application.conf
└── static/                           # built React SPA (index.html + assets) served by endpoint

src/main/frontend/                    # React + Vite source (builds into ../resources/static)
├── index.html
├── package.json
├── vite.config.ts
└── src/
    ├── main.tsx
    ├── App.tsx
    └── api.ts                        # typed fetch client for the endpoint

src/test/java/com/example/
├── domain/
│   └── DietRecommenderTest.java
├── application/
│   └── CookieLogEntityTest.java
└── api/
    └── CookieDietEndpointIntegrationTest.java
```

**Structure Decision**: Single Akka service with the React SPA served as static content from the same service (matches "simple web app hosted within Akka's services"). Backend follows the scaffold's existing flat `com.example.{domain,application,api}` layout.

## Complexity Tracking

*Deviations from the constitution requiring justification.*

| Deviation | Why it's needed | Simpler alternative rejected because |
|-----------|-----------------|--------------------------------------|
| No View component for intake history | Single subject + small data volume; the dashboard reads today's total and recent days directly from entity state | Adding a `View` now would be speculative infrastructure (YAGNI). Add one if the app later tracks many subjects or large history. |

No constitution deviations remain. The AI recommendation uses a real Akka `Agent`; the LLM is mocked at the model layer (`ModelProvider.Custom`) rather than by skipping the agent, so both "leverage an agent" and "mock any LLM calls (no key/no spend)" hold. Going live is removing the `.model(new MockDietModelProvider())` override in `DietCoachAgent` and configuring a provider in `application.conf`.

## Phase 0: Outline & Research

See [research.md](./research.md). Resolved decisions:
1. **Mock-LLM strategy** → deterministic `DietRecommender` domain function; documented Agent swap path.
2. **Serving a React SPA from an Akka service** → Vite builds into `src/main/resources/static`; endpoint serves static content + SPA fallback.
3. **Read model for the dashboard** → entity-state reads (no View) for the single-subject prototype.
4. **Entity type** → Event Sourced Entity (natural audit trail for log/remove intake actions).

## Phase 1: Design & Contracts

- [data-model.md](./data-model.md) — entities, fields, validation rules, events, state transitions.
- [contracts/http-api.md](./contracts/http-api.md) — HTTP endpoint contract (routes, request/response shapes, status codes).
- [quickstart.md](./quickstart.md) — how to build the frontend, run the service, and exercise the app.

**Post-Design Constitution Re-check**: PASS. Design keeps domain logic framework-independent, the endpoint isolates API types, component count is minimal, and the single justified deviation (mock recommender) is contained behind a stable contract.

## Next Command

Run `/akka:tasks` to generate the dependency-ordered `tasks.md` from these artifacts.
