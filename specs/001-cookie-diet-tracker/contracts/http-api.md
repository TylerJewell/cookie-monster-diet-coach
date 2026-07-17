# Phase 1 Contract: HTTP API

Endpoint class: `com.example.api.CookieDietEndpoint` (`@HttpEndpoint("/api")`, `@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))`).
All request/response types are records **defined inside the endpoint** (API isolation — no domain types exposed). Synchronous style, `ComponentClient.method(...).invoke(...)`. The single subject uses a fixed entity id `"cookie-monster"`.

## Data shapes (API records)

```
LogCookieRequest   { int count, String type }          // type optional/nullable
EntryView          { String entryId, int count, String type, String day }
DailyTotalView     { String day, int total }
RecommendationView { int dailyCookieAllowance, List<String> balancingSuggestions,
                     String rationale, String generatedOn }
SummaryView        { String day, int todayTotal,
                     List<DailyTotalView> recentDays,
                     RecommendationView latestRecommendation }   // latestRecommendation nullable
```

## Routes

### POST /api/cookies — log an intake entry
- Body: `LogCookieRequest`
- 201 Created on success (returns updated `SummaryView` or the created `EntryView`).
- 400 Bad Request if `count <= 0`.
- Requirements: FR-001, FR-002, FR-003.

### DELETE /api/cookies/{entryId} — remove an entry
- 200 OK on success (returns updated `SummaryView`).
- 404 Not Found if `entryId` is unknown.
- Requirements: FR-004.

### GET /api/summary — dashboard payload
- 200 OK → `SummaryView` (today's total, recent days, latest recommendation).
- Requirements: FR-003, FR-005, FR-010.

### POST /api/recommendation — generate a new recommendation
- No body. Triggers `CookieLogEntity.generateRecommendation()`.
- 201 Created → `RecommendationView` (allowance always > 0).
- Requirements: FR-006, FR-007, FR-008, FR-009.

### GET /api/recommendation — latest recommendation
- 200 OK → `RecommendationView`.
- 404 Not Found if none has been generated yet.
- Requirements: FR-009.

## Static content (React SPA)
- `GET /` and non-`/api` paths → serve `index.html` / built assets from `src/main/resources/static/` (SPA fallback).
- Requirements: FR-012.

## Error handling
- Validation and not-found cases use `akka.javasdk.http.HttpResponses.badRequest(...)` / `notFound(...)` rather than throwing.
- When the recommendation cannot be produced, intake routes remain fully functional (FR-011). (In the prototype the deterministic recommender does not fail; this contract still isolates the recommendation route so a future LLM-backed agent failure cannot break intake logging.)
