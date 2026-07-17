# Feature Specification: Cookie Monster Cookie-Intake Tracker & AI Diet Coach

**Feature Branch**: `001-cookie-diet-tracker`
**Created**: 2026-07-16
**Status**: Draft
**Input**: User description: "My kids love Cookie Monster. But they are worried about his cholesterol from too many cookies. We want to create a dieting app to help him without reducing his love of cookies. He's five feet tall. I want this as a simple web app that is hosted within Akka's services. I want to track his daily cookie intake and use AI to recommend a new diet."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Log daily cookie intake (Priority: P1)

A caregiver (one of the kids) opens the web app and records the cookies Cookie Monster ate today — how many, and optionally what kind — so the family can see how much he is actually eating over time.

**Why this priority**: Nothing else in the app works without intake data. Tracking is the single feature that delivers value on day one and is the foundation the AI recommendation depends on.

**Independent Test**: Open the app, add several cookie entries for today, and confirm the running daily total and history are shown correctly. Delivers standalone value even before any AI is involved.

**Acceptance Scenarios**:

1. **Given** the app is open on today's date, **When** the caregiver logs "3 chocolate chip cookies", **Then** today's cookie count increases by 3 and the entry appears in today's list.
2. **Given** cookies were logged earlier today, **When** the caregiver adds another entry, **Then** the daily total updates to include the new entry without losing prior entries.
3. **Given** an entry was logged by mistake, **When** the caregiver removes that entry, **Then** the daily total decreases accordingly.
4. **Given** cookies were logged on previous days, **When** the caregiver views history, **Then** each past day shows its own total.

### User Story 2 - Get an AI diet recommendation (Priority: P2)

Because the family worries about Cookie Monster's cholesterol, the caregiver asks the app for a diet plan. The app uses AI to produce a friendly, cookie-loving plan that reduces health risk without asking him to give up cookies entirely.

**Why this priority**: This is the emotional payoff of the app — the reason the kids want it — but it depends on intake data existing first, so it follows P1.

**Independent Test**: With some intake history present, request a recommendation and confirm the app returns a readable diet plan that references his actual cookie habits and still allows cookies.

**Acceptance Scenarios**:

1. **Given** there is recent cookie-intake history, **When** the caregiver requests a diet recommendation, **Then** the app returns a plan that includes a suggested daily cookie allowance greater than zero plus healthier balancing suggestions.
2. **Given** intake has been very high, **When** a recommendation is generated, **Then** the plan acknowledges the cholesterol concern and proposes moderation rather than elimination.
3. **Given** a recommendation was previously generated, **When** the caregiver requests a new one, **Then** the latest recommendation reflects the most recent intake data.

### User Story 3 - See progress at a glance (Priority: P3)

The caregiver opens the app and immediately sees today's cookie count, a short recent-days trend, and the most recent AI recommendation, so the family can tell whether Cookie Monster is trending healthier.

**Why this priority**: Improves engagement and makes the value visible, but the app is still useful without a polished dashboard.

**Independent Test**: Open the app with existing data and confirm the landing view summarizes today's intake, recent trend, and current recommendation without extra navigation.

**Acceptance Scenarios**:

1. **Given** intake and a recommendation exist, **When** the app loads, **Then** today's total, a recent-days summary, and the current recommendation are all visible on the main view.
2. **Given** no cookies have been logged today, **When** the app loads, **Then** today's total shows zero and the app invites the caregiver to add an entry.

### Edge Cases

- What happens when a recommendation is requested with **no intake history**? The app should return a gentle starter plan rather than an error.
- How does the app handle a **very large single entry** (e.g., 100 cookies)? It should still record it and reflect it in the total.
- What happens across a **day boundary** — entries logged near midnight must count toward the correct calendar day.
- What if the **AI service is temporarily unavailable**? The app should show a clear "try again" message and keep all logged intake intact.
- What happens when the caregiver tries to log a **zero or negative** cookie count? The app should reject it with a friendly validation message.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let a caregiver record a cookie-intake entry consisting of a count and, optionally, a cookie type, associated with a calendar day.
- **FR-002**: The system MUST reject intake entries with a count of zero or less.
- **FR-003**: The system MUST maintain and display a running total of cookies for the current day.
- **FR-004**: The system MUST let a caregiver remove a previously logged entry, updating the affected day's total.
- **FR-005**: The system MUST retain intake history across days so past daily totals remain viewable.
- **FR-006**: The system MUST let a caregiver request an AI-generated diet recommendation based on Cookie Monster's recorded intake history and his fixed profile (height: five feet; concern: cholesterol).
- **FR-007**: Every AI diet recommendation MUST permit a non-zero daily cookie allowance — the plan must never require eliminating cookies.
- **FR-008**: The AI diet recommendation MUST acknowledge the cholesterol concern and offer healthier balancing suggestions alongside the cookie allowance.
- **FR-009**: The system MUST store the most recent AI recommendation so it can be displayed without regenerating it.
- **FR-010**: The system MUST present a main view summarizing today's cookie total, a recent-days trend, and the current recommendation.
- **FR-011**: The system MUST remain usable (intake logging and history viewing) when the AI recommendation cannot be generated, showing a clear, non-blocking error for the recommendation only.
- **FR-012**: The system MUST be delivered as a web application accessible from a browser.

### Key Entities *(include if feature involves data)*

- **Cookie Monster Profile**: The single subject being tracked. Fixed attributes: name, height (five feet), and the standing health concern (cholesterol). Used as context for recommendations.
- **Cookie Intake Entry**: A single logged event — cookie count, optional cookie type, and the calendar day it belongs to.
- **Daily Intake Summary**: The aggregate of all entries for a given day — the day and its total cookie count.
- **Diet Recommendation**: An AI-generated plan — a suggested daily cookie allowance (non-zero), healthier balancing suggestions, and the date it was generated.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A caregiver can log a cookie entry and see the updated daily total in under 5 seconds from opening the app.
- **SC-002**: 100% of generated diet recommendations include a daily cookie allowance greater than zero (cookies are never eliminated).
- **SC-003**: A caregiver can request and read a diet recommendation within 30 seconds of asking for it.
- **SC-004**: After logging intake for at least 7 days, the caregiver can view a per-day breakdown for each of those days with correct totals.
- **SC-005**: When the AI service is unavailable, previously logged intake and history remain 100% accessible and no intake data is lost.
- **SC-006**: A first-time caregiver can log their first cookie entry without instructions in under 1 minute.

## Assumptions

- **Single subject**: The app tracks one individual (Cookie Monster). No multi-user accounts, sign-up, or role management is required for this feature; anyone with access to the app acts as a caregiver.
- **Trusted family use**: Because this is a lighthearted single-subject family app, no authentication or per-user permissions are in scope. (Network-level access control is an operational concern, not a product requirement here.)
- **Cookies are the primary tracked food**: Tracking centers on cookie intake (count and optional type). General meal/calorie logging is out of scope for this feature.
- **Height is fixed profile context**: Cookie Monster's height (five feet) is a static profile value used to inform recommendations; the app does not track changes to it. Weight and lab cholesterol readings are not captured in this feature — the "cholesterol" concern is treated as a fixed qualitative goal that shapes the AI plan.
- **On-demand recommendations**: Diet recommendations are generated when the caregiver requests one, not automatically on a schedule.
- **"Day" means calendar day** in the app's operating time zone; entries are attributed to the calendar day on which they are logged.
- **Simple, single web app**: The deliverable is one small web application (UI plus its backing service), consistent with the request for a "simple web app hosted within Akka's services."

## Out of Scope

- Multi-user accounts, authentication, and sharing.
- Tracking foods other than cookies, or full nutritional/calorie accounting.
- Recording weight or clinical lab results over time.
- Automated/scheduled recommendation generation or reminders/notifications.
- Native mobile apps.
