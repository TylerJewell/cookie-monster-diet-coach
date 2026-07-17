# Phase 0 Research: Cookie Monster Cookie-Intake Tracker & AI Diet Coach

All Technical Context unknowns are resolved below. No `NEEDS CLARIFICATION` remain.

## R1. Agent + mocked-LLM strategy for the first prototype

- **Decision**: Recommendations are produced by a real Akka `Agent` (`DietCoachAgent`) using the structured-response flow (`responseConformsTo(CoachAdvice.class)`). Its LLM is mocked with a `ModelProvider.Custom` implementation (`MockDietModelProvider`) wired via the agent's `.model(...)`. The mock is a LangChain4j `ChatModel` that parses the intake JSON from the user message and returns deterministic advice — no real provider, **no API key read**, no network, $0.
- **Rationale**: The user asked to "leverage an agent and mock out any LLM calls" and to not touch their `GOOGLE_AI_GEMINI_API_KEY`. Mocking at the *model layer* keeps the genuine agent pipeline (system/user messages, structured JSON schema, session, interaction logging) while guaranteeing zero cost and zero key usage. The mock reuses `DietRecommender` so advice stays intake-aware. The agent also declares an `.onFailure(...)` fallback to `DietRecommender` for resilience.
- **Swap path to real AI**: Remove the `.model(new MockDietModelProvider())` line in `DietCoachAgent.recommend(...)` and configure a real provider in `application.conf` (e.g. `model-provider = anthropic`). The endpoint, entity, HTTP contract, and React app are unchanged. Tests can then use `TestModelProvider.fixedResponse(...)`.
- **Alternatives considered**:
  - *Deterministic domain function, no agent* — rejected: does not "leverage an agent" as requested.
  - *Real `DietCoachAgent` with a live Gemini key* — rejected: spends money and uses the user's key, both explicitly disallowed.
  - *Mock only in tests via `TestModelProvider`* — insufficient on its own: `TestModelProvider` is test-only, so a live `mvn exec:java` run would still hit a real model. The `ModelProvider.Custom` mock covers the running service too.

## R2. Serving a React SPA from an Akka service

- **Decision**: Build the React app with Vite, output to `src/main/resources/static/`, and serve it from `CookieDietEndpoint` using Akka's static-content support, with an SPA fallback route returning `index.html` for non-API paths.
- **Rationale**: Keeps everything in one deployable ("hosted within Akka's services"). Static files packaged in resources are served directly by the service; no separate web host or CORS configuration is required because the SPA and API share an origin.
- **Reference**: "Serving static content" in `akka-context/sdk/http-endpoints.html.md`.
- **Alternatives considered**:
  - *Separate frontend host (e.g., static CDN)* — rejected: adds a second deployment and CORS handling; contradicts the "simple, single web app hosted in Akka" request.
  - *Server-rendered HTML from the endpoint* — rejected: the user specifically asked for a React app.

## R3. Read model for the dashboard (View vs entity state)

- **Decision**: For the single-subject prototype, the dashboard (today's total, recent-days trend, latest recommendation) is read directly from `CookieLogEntity` state via a read-only command. No `View` component in this iteration.
- **Rationale**: There is exactly one tracked subject and small data volume, so a single entity read fully satisfies the dashboard. Adding a View now is speculative infrastructure (Simplicity/YAGNI).
- **Follow-up trigger**: Introduce a `CookieIntakeByDayView` if the app grows to many subjects or needs cross-subject/large-history queries.
- **Alternatives considered**:
  - *Build a `View` now* — rejected as premature for a one-subject prototype.

## R4. Entity type: Event Sourced vs Key Value

- **Decision**: Use an **Event Sourced Entity** (`CookieLogEntity`) with events `CookieLogged`, `EntryRemoved`, `RecommendationGenerated`.
- **Rationale**: Intake tracking is naturally a sequence of add/remove actions; event sourcing gives a built-in audit trail of what was logged and removed and cleanly supports per-day reconstruction. It aligns with the entity examples in `AGENTS.md`/`akka-context`.
- **Alternatives considered**:
  - *Key Value Entity* — simpler state mechanics, but loses the natural audit trail of individual logging actions that the intake-history requirements benefit from. Marginal simplicity gain not worth the lost history semantics.

## R5. Profile handling (height, cholesterol concern)

- **Decision**: Model `CookieMonsterProfile` as a fixed record (name = "Cookie Monster", heightInches = 60, concern = "cholesterol") seeded into the entity's initial state. No profile-editing feature.
- **Rationale**: The spec treats height and the cholesterol concern as fixed context for recommendations, not tracked metrics. Keeping it a constant honors scope and Simplicity.
- **Alternatives considered**:
  - *Editable profile / weight & lab tracking* — out of scope per the spec's Assumptions and Out of Scope sections.
