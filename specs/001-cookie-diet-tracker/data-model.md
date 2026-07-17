# Phase 1 Data Model: Cookie Monster Cookie-Intake Tracker & AI Diet Coach

Package: `com.example.domain` (records + logic), `com.example.application` (entity).

## Domain records (`com.example.domain`)

### CookieMonsterProfile
Fixed context for the tracked subject.

| Field | Type | Notes |
|-------|------|-------|
| name | `String` | "Cookie Monster" |
| heightInches | `int` | 60 (five feet) |
| concern | `String` | "cholesterol" |

- Provide a `CookieMonsterProfile.defaultProfile()` factory. Immutable.

### CookieEntry
One logged intake action.

| Field | Type | Validation |
|-------|------|------------|
| entryId | `String` | non-blank; unique within the log (UUID) |
| count | `int` | **> 0** |
| type | `Optional<String>` | optional cookie type (e.g., "chocolate chip") |
| day | `LocalDate` | the calendar day the entry belongs to |

### DailyTotal
Aggregate for a single day (derived, used in summary responses).

| Field | Type | Notes |
|-------|------|-------|
| day | `LocalDate` | |
| totalCount | `int` | sum of counts for that day |

### DietRecommendation
The "AI" output (deterministic in the prototype). LLM-shaped for a future `DietCoachAgent` swap.

| Field | Type | Validation |
|-------|------|------------|
| dailyCookieAllowance | `int` | **> 0** — cookies are never eliminated (FR-007) |
| balancingSuggestions | `List<String>` | ≥ 1 healthier-balance tip (FR-008) |
| rationale | `String` | short friendly explanation acknowledging the cholesterol concern |
| generatedOn | `LocalDate` | date the recommendation was produced |

### CookieLog  (entity STATE)
Holds all entries + the latest recommendation + profile. Contains the business logic.

| Field | Type | Notes |
|-------|------|-------|
| profile | `CookieMonsterProfile` | seeded on first use |
| entries | `List<CookieEntry>` | immutable copy-on-write |
| latestRecommendation | `Optional<DietRecommendation>` | most recent (FR-009) |

**Behavior (pure methods, no Akka types):**
- `static CookieLog empty()` → profile = default, no entries, no recommendation.
- `CookieLog addEntry(CookieEntry e)` — precondition `e.count() > 0`; returns copy with entry appended.
- `CookieLog removeEntry(String entryId)` — returns copy without that entry (no-op semantics handled by entity).
- `boolean hasEntry(String entryId)`.
- `int totalForDay(LocalDate day)`.
- `List<DailyTotal> recentDays(int n)` — last `n` days that have entries, most recent first.
- `IntakeSummary summarize()` — compact input for the recommender (e.g., recent daily average, days tracked, peak day).
- `CookieLog withRecommendation(DietRecommendation r)` — returns copy with `latestRecommendation` set.

### IntakeSummary
Compact recommender input (derived from `CookieLog.summarize()`).

| Field | Type |
|-------|------|
| averageDailyCookies | `double` |
| daysTracked | `int` |
| peakDailyCount | `int` |

### DietRecommender  (domain function — the mocked "AI")
`static DietRecommendation recommend(IntakeSummary summary, CookieMonsterProfile profile, LocalDate today)`

Deterministic rules (prototype):
- Base allowance derived from recent average, **floored at a minimum ≥ 1** so it is always > 0.
- If `averageDailyCookies` is high, allowance = a moderated fraction of the average (never zero) and `rationale` explicitly mentions the cholesterol concern.
- `balancingSuggestions` always includes at least one non-cookie balance tip (e.g., "swap one afternoon cookie for fruit", "add a walk after cookie time").
- With no history (`daysTracked == 0`), return a gentle starter plan (small positive allowance + starter tips).

## Events (`com.example.domain` — `CookieLogEvent` sealed interface)

Each event is a record annotated with `@TypeName`.

| Event | Fields | Meaning |
|-------|--------|---------|
| `CookieLogged` | `entryId, count, Optional<String> type, LocalDate day` | an intake entry was recorded |
| `EntryRemoved` | `entryId` | a previously logged entry was removed |
| `RecommendationGenerated` | `DietRecommendation recommendation` | a new recommendation was produced/stored |

`applyEvent` is a pure state transition (append entry / remove entry / set latest recommendation). It never fails — all validation happens in the command handler before persisting.

## State transitions

```
(empty) --logCookies(count>0)--> CookieLogged           => entries += entry
        --removeEntry(existing id)--> EntryRemoved        => entries -= entry
        --generateRecommendation--> RecommendationGenerated => latestRecommendation = r
```

Invalid commands (count ≤ 0, remove unknown id) return `effects().error(...)` and persist **no** event.

## Entity (`com.example.application.CookieLogEntity`)

- `EventSourcedEntity<CookieLog, CookieLogEvent>`, `@Component(id = "cookie-log")`.
- Fixed instance id for the single subject: `"cookie-monster"`.
- Commands:
  - `logCookies(LogCookies cmd)` → validates `count > 0`; persists `CookieLogged`; replies `Done` (with generated entryId available via summary/read).
  - `removeEntry(String entryId)` → error if unknown; persists `EntryRemoved`; replies `Done`.
  - `generateRecommendation()` (no param) → computes `DietRecommender.recommend(state.summarize(), profile, today)`; persists `RecommendationGenerated`; replies the `DietRecommendation`.
  - `getSummary()` (read-only) → replies a summary view (today total, recent days, latest recommendation) built from state.
- `emptyState()` → `CookieLog.empty()`.

## Validation rules → requirements traceability

| Rule | Requirement |
|------|-------------|
| count > 0 rejected otherwise | FR-002 |
| running daily total maintained | FR-003 |
| remove updates total | FR-004 |
| history retained across days | FR-005 |
| allowance always > 0 | FR-007, SC-002 |
| rationale addresses cholesterol + balancing tips | FR-008 |
| latest recommendation stored | FR-009 |
