# Design Diagrams: Cookie Monster Cookie-Intake Tracker & AI Diet Coach

Five design views for the feature, as Mermaid sources. Rendered to
`docs/architecture.html` (inline SVG, offline) following the `/akka:demo` diagram rules.

## 1. User Journey

```mermaid
journey
    title Caregiver keeps Cookie Monster on track
    section Log intake
      Open the app: 5: Caregiver
      Add cookies eaten: 4: Caregiver
      See today's total update: 5: Caregiver
      Remove a mistaken entry: 3: Caregiver
    section Review
      Scan the recent-days trend: 4: Caregiver
    section Coach
      Request a diet recommendation: 5: Caregiver
      Read allowance and tips: 5: Caregiver
```

## 2. Actor-Goal

```mermaid
flowchart LR
    actor(["Caregiver / Kid"])
    actor --> g1["Goal: Log daily cookie intake"]
    actor --> g2["Goal: See progress at a glance"]
    actor --> g3["Goal: Get an AI diet plan"]
    g1 --> api["CookieDietEndpoint"]
    g2 --> api
    g3 --> api
    api --> ent["CookieLogEntity"]
    api --> agent["DietCoachAgent"]
    agent --> mock[("Mock LLM<br/>no API key")]
```

## 3. Entity Map

```mermaid
erDiagram
    COOKIE_MONSTER_PROFILE ||--|| COOKIE_LOG : "profile of"
    COOKIE_LOG ||--o{ COOKIE_ENTRY : contains
    COOKIE_LOG ||--o| DIET_RECOMMENDATION : "latest"
    COOKIE_MONSTER_PROFILE {
        string name
        int heightInches
        string concern
    }
    COOKIE_ENTRY {
        string entryId
        int count
        string type "optional"
        date day
    }
    DIET_RECOMMENDATION {
        int dailyCookieAllowance
        list balancingSuggestions
        string rationale
        date generatedOn
    }
    COOKIE_LOG {
        CookieMonsterProfile profile
        list entries
        Optional latestRecommendation
    }
```

## 4. Component Graph

```mermaid
flowchart TB
    subgraph EXTERNAL["External"]
      spa["React SPA<br/>static-resources/index.html"]
    end
    subgraph APIL["API layer"]
      cde["CookieDietEndpoint<br/>@HttpEndpoint /api"]
      sre["StaticResourcesEndpoint<br/>@HttpEndpoint /"]
    end
    subgraph APPL["Application layer"]
      ent["CookieLogEntity<br/>EventSourcedEntity"]
      agent["DietCoachAgent<br/>Agent"]
      mock["MockDietModelProvider<br/>ModelProvider.Custom"]
    end
    subgraph DOM["Domain layer"]
      dom["CookieLog · CookieLogEvent<br/>DietRecommender · records"]
    end
    spa -->|"fetch /api/*"| cde
    spa -->|"GET /"| sre
    cde -->|"commands & reads"| ent
    cde -->|"recommend()"| agent
    agent -->|".model()"| mock
    ent --> dom
    agent --> dom
    mock --> dom
```

## 5. Sequence — Get a diet recommendation

```mermaid
sequenceDiagram
    participant U as React SPA
    participant E as CookieDietEndpoint
    participant EN as CookieLogEntity
    participant A as DietCoachAgent
    participant M as MockDietModelProvider
    U->>E: POST /api/recommendation
    E->>EN: getState()
    EN-->>E: CookieLog (intake summary)
    E->>A: recommend(CoachRequest)
    A->>M: chat(request) — mock LLM, no key
    M-->>A: CoachAdvice (JSON)
    A-->>E: CoachAdvice
    E->>EN: recordRecommendation(DietRecommendation)
    EN-->>E: Done
    E-->>U: 201 RecommendationView
```
