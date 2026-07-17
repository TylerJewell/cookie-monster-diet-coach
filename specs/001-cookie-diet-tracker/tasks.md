# Tasks: Cookie Monster Cookie-Intake Tracker & AI Diet Coach

**Feature**: `001-cookie-diet-tracker` | **Input**: plan.md, data-model.md, contracts/http-api.md, research.md
`[P]` = can run in parallel (independent files). Base package `com.example`, Akka SDK 3.6.0.

## Phase 1: Domain layer (`com.example.domain`)

- [X] T001 [P] `CookieMonsterProfile.java` — fixed profile record + `defaultProfile()`
- [X] T002 [P] `CookieEntry.java` — (entryId, count, Optional<type>, day)
- [X] T003 [P] `DailyTotal.java` — (day, totalCount)
- [X] T004 [P] `DietRecommendation.java` — (allowance>0, tips, rationale, generatedOn)
- [X] T005 [P] `IntakeSummary.java` — (averageDailyCookies, daysTracked, peakDailyCount)
- [X] T006 `CookieLogEvent.java` — sealed interface + `@TypeName` events (CookieLogged, EntryRemoved, RecommendationGenerated)
- [X] T007 `CookieLog.java` — state record + business logic (addEntry, removeEntry, totals, summarize, withRecommendation)
- [X] T008 `DietRecommender.java` — deterministic mock "AI" (allowance always > 0; addresses cholesterol)

## Phase 2: Application layer (`com.example.application`)

- [X] T009 `CookieLogEntity.java` — Event Sourced Entity (logCookies, removeEntry, recordRecommendation, getState) + `LogCookies` command record
- [X] T009a `DietCoachAgent.java` — real Akka `Agent` (structured `responseConformsTo(CoachAdvice)`) for diet recommendations
- [X] T009b `MockDietModelProvider.java` — `ModelProvider.Custom` mock LLM (offline, no API key); agent uses it via `.model(...)`

## Phase 3: API layer (`com.example.api`)

- [X] T010 `CookieDietEndpoint.java` — `/api` routes + request/response records + `toApi` converters
- [X] T011 [P] `StaticResourcesEndpoint.java` — serve the React SPA (`GET /` → index.html)
- [X] T012 [P] `src/main/resources/static-resources/index.html` — self-contained React SPA (log cookies, list, recent days, recommendation)

## Phase 4: Tests

- [X] T013 [P] `DietRecommenderTest.java` — allowance never zero, starter plan, cholesterol rationale (mock-LLM logic)
- [X] T014 [P] `CookieLogEntityTest.java` — log/total, reject count≤0, remove updates total, records recommendation
- [X] T014a [P] `DietCoachAgentTest.java` — agent against offline mock model returns non-zero allowance, no API key
- [X] T015 `CookieDietEndpointIntegrationTest.java` — log+summary (delta), generate recommendation (via agent), reject zero

## Phase 5: Build & verify

- [X] T016 `mvn compile`
- [X] T017 `mvn test`

## Notes
- Frontend is a CDN-based single-file React app for the prototype (no npm/Vite build); migration path documented in plan/quickstart.
- SummaryView additionally carries `todayEntries` (so the UI can list + remove today's cookies) — a small superset of contracts/http-api.md.
- Diet recommendations run through a real Akka `Agent` whose LLM is mocked by a `ModelProvider.Custom` (`MockDietModelProvider`) — fully offline, no API key touched, $0. Going live = remove the `.model(...)` override in `DietCoachAgent` and configure a real provider. `DietRecommender` is the deterministic logic the mock (and the agent's `onFailure` fallback) reuse.
