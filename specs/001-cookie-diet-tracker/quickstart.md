# Quickstart: Cookie Monster Cookie-Intake Tracker & AI Diet Coach

## Prerequisites
- Java 21+, Maven 3.9+ (already set up in this project)
- Node.js 18+ (for building the React frontend)

## Build the React frontend into the service
The SPA lives in `src/main/frontend` and builds into `src/main/resources/static`, which the Akka service serves.

```bash
cd src/main/frontend
npm install
npm run build        # Vite outputs to ../resources/static
```

(During UI development you can run `npm run dev` for a hot-reload dev server that proxies `/api` to the running service.)

## Run the service locally
From the project root:

```bash
mvn compile exec:java
```

The app is then available at `http://localhost:9000/` (React UI) with the API under `http://localhost:9000/api`.

## Exercise the API (no LLM cost — recommendations are deterministic)

```bash
# Log cookies
curl -i -XPOST localhost:9000/api/cookies \
  -H "Content-Type: application/json" \
  -d '{"count": 3, "type": "chocolate chip"}'

# Dashboard summary (today total, recent days, latest recommendation)
curl -s localhost:9000/api/summary

# Generate an AI (mocked) diet recommendation — allowance is always > 0
curl -i -XPOST localhost:9000/api/recommendation

# Latest recommendation
curl -s localhost:9000/api/recommendation

# Remove an entry
curl -i -XDELETE localhost:9000/api/cookies/<entryId>
```

## Run tests

```bash
mvn test          # domain + entity unit tests
mvn verify        # includes endpoint integration tests
```

## Later: swap the mock for a real LLM
Add `DietCoachAgent extends Agent` returning `DietRecommendation` via `responseConformsTo(...)`, point the recommendation route at the agent, and configure a model provider (e.g. Gemini) in `application.conf`. The HTTP contract and the React app do not change.
