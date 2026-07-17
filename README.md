# Cookie Monster Diet Coach 🍪

A small [Akka SDK](https://doc.akka.io/) service that tracks Cookie Monster's daily
cookie intake and gives him a friendly AI diet plan — one that keeps cookies on the
menu while watching his cholesterol.

- **`CookieLogEntity`** (Event Sourced Entity) — durable daily cookie-intake log
- **`DietCoachAgent`** (Akka Agent) — structured diet recommendations. For this prototype
  the LLM is **mocked** by an offline `ModelProvider.Custom` (`MockDietModelProvider`), so it
  runs with **no API key and no cost**. Going live is a one-line change (see the docs).
- **`CookieDietEndpoint`** + **`StaticResourcesEndpoint`** — REST API and a React single-page UI

See [`docs/architecture.html`](docs/architecture.html) for a rendered architecture doc
(component inventory + five design diagrams), and [`specs/`](specs/) for the spec-driven
design artifacts.

## Clone and build with Claude Code (Akka plugin)

This app was built with the Akka spec-driven workflow in Claude Code
(`/akka:specify` → `/akka:plan` → `/akka:tasks` → `/akka:implement`), and you can build and
run it the same way — no manual `mvn` commands needed.

**Prerequisites:** [Claude Code](https://claude.com/claude-code) with the Akka plugin
installed (it provides the `/akka:*` commands and the Akka MCP server).

```shell
git clone https://github.com/TylerJewell/cookie-monster-diet-coach.git
cd cookie-monster-diet-coach
claude
```

Then, inside Claude Code:

1. **`/akka:setup`** — one-time environment check. Verifies/installs Java 21+, Maven, the
   Akka CLI, and the download token. It is idempotent, so it's safe to re-run. If it creates
   or updates `.mcp.json`, restart Claude Code (`claude --resume`) so the Akka MCP tools load.

2. **`/akka:build`** — the local development loop and the Claude-native equivalent of
   `mvn compile exec:java`. It compiles, runs the tests, starts the local Akka runtime, and
   runs the service. When it's up, open the UI at **http://localhost:9000/**.

To explore or extend the design, the same spec-driven commands are available:
`/akka:specify` (write a feature spec), `/akka:plan`, `/akka:tasks`, `/akka:implement`,
and `/akka:deploy` (ship it to the Akka platform).

## Build and run with Maven directly

If you prefer the raw toolchain instead of Claude Code:

```shell
mvn compile              # compile
mvn verify               # compile + run all tests (12)
mvn compile exec:java    # run locally → http://localhost:9000/
```

## Try the API

```shell
# Log cookies
curl -i -XPOST localhost:9000/api/cookies \
  -H "Content-Type: application/json" \
  -d '{"count": 3, "type": "chocolate chip"}'

# Dashboard summary (today's total, recent days, latest recommendation)
curl -s localhost:9000/api/summary

# Generate an AI (mocked) diet recommendation — allowance is always > 0
curl -i -XPOST localhost:9000/api/recommendation
```

## Deploy

Build the container image and deploy to the [Akka platform](https://console.akka.io) —
either via **`/akka:deploy`** in Claude Code, or manually:

```shell
mvn clean install -DskipTests
akka service deploy cookie-monster-diet-coach cookie-monster-diet-coach:tag-name --push
```

See [Deploy and manage services](https://doc.akka.io/operations/services/deploy-service.html)
for details.
