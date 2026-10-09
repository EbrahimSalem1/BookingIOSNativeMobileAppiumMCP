# Exploratory testing - Booking iOS app

> **Session-based exploratory testing (SBTM)** performed before automation, to learn the product, find risks,
> and decide *what* is worth automating.
>
> **How to read this document:** charters and the risk model were prepared up front. The *Observation* column
> records what was actually seen in the session on the delivered build; rows still marked `To verify` are risk
> hypotheses that guided the charters and must be confirmed against the app.

| | |
|---|---|
| Tester | Ibrahim Mohamed Abd ElMaksoud |
| Build | `<app version (build)>` - fill in |
| Devices | iPhone 15 simulator iOS 17.5 (primary), iPhone SE 3rd gen simulator (compact layout) |
| Tools | Appium Inspector / Appium MCP (element tree), QuickTime / `simctl io recordVideo`, Network Link Conditioner |
| Time box | 5 charters x 20-25 min |

---

## 1. Charters

| # | Charter (Explore ... with ... to discover ...) | Why (risk) | How (heuristics / techniques) |
|---|---|---|---|
| C1 | Explore **login** with valid, invalid and malformed credentials, keyboard and app interruptions, to discover authentication and usability risks | Gateway to every journey; account security | Boundary values (empty, whitespace, 256 chars, emoji, leading/trailing spaces), error message wording, double-tap on submit, background the app during login, rotate, password field masking / paste, keyboard covering the button on iPhone SE, VoiceOver labels |
| C2 | Explore **search** with different destinations, spellings and dates, to discover relevance and input-handling risks | Core value; wrong results = lost booking | Autocomplete vs free text, accents (`Zürich`), case, partial names, same-name cities (Alexandria EG / US), no-results, past dates, long stays, 0/many guests, search while offline (Network Link Conditioner), cancel during loading |
| C3 | Explore **filtering** with single and combined filters and state changes, to discover rule and persistence risks | Filters silently leaking non-matching results erodes trust | Boundary on price (exactly 200), combination filters, reset, count on "Show N results" vs actual, filter + back navigation, filter + new search, empty result after filtering |
| C4 | Explore **sorting** with each option, filters and scrolling, to discover ordering risks | Price order drives the booking decision | Numeric vs lexical order (`$1,200` vs `$300`), ties, unpriced/unrated items, order on later pages after scroll, sort + filter, sort persistence, which price is sorted (per night vs total) |
| C5 | Explore **result selection and details** across different cards, to discover list-to-details consistency risks | Showing a different property/price than tapped is a severe trust & legal issue | Compare name/price/rating/location card vs details, tap sponsored / sold-out cards, image gallery, back navigation keeps scroll position, deep link to details, rotate, Dynamic Type XXL |

## 2. Observations

Severity: **S1** blocker · **S2** major · **S3** minor · **S4** cosmetic. Type: Bug · Risk · Ambiguity · Usability · Automation challenge.

| ID | Charter | Type | Observation / hypothesis | Sev | Observation on build | Follow-up |
|---|---|---|---|---|---|---|
| O1 | C1 | Usability | Keyboard can cover the Log in button on compact devices (iPhone SE) | S3 | To verify | Automation dismisses keyboard before submit; raise UX issue if covered |
| O2 | C1 | Risk | Error message may reveal whether the account exists ("user not found" vs "invalid credentials") - account enumeration | S2 | To verify | Security review; test asserts generic message (`messages.json`) |
| O3 | C1 | Ambiguity | Are leading/trailing spaces in email trimmed? | S3 | To verify | Ask PO |
| O4 | C1 | Automation challenge | iOS "Save Password?" sheet after login blocks the next screen | - | To verify | Handled in `LoginPage.loginAs` via `AlertComponent` |
| O5 | C2 | Risk | Free-text destination without picking a suggestion may produce ambiguous results (same-name cities) | S2 | To verify | Automation selects the first suggestion; regression on ambiguous names stays exploratory |
| O6 | C2 | Ambiguity | Relevance rule: must every result be *in* the city, or may nearby areas appear ("near Paris")? | - | To verify | PO; `relevanceKeyword` in data makes the rule configurable |
| O7 | C2 | Risk | Sponsored/"featured" cards may not match the search and are not marked | S2 | To verify | If confirmed, cards need a `sponsored` id so the oracle can exclude them |
| O8 | C3 | Bug candidate | "Show N results" count differs from results actually listed | S3 | To verify | Add assertion if reproducible |
| O9 | C3 | Risk | Price filter boundary (exactly 200) inclusive or exclusive? | S3 | To verify | Oracle assumes inclusive (`<=`); confirm with PO |
| O10 | C3 | Risk | Filter lost after opening details and coming back | S2 | To verify | Automated: "Applied filter survives viewing a stay" |
| O11 | C4 | Ambiguity | Sort by price uses nightly price while card shows total (or vice versa) | S2 | To verify | **Blocking question** - determines whether a sort failure is a bug |
| O12 | C4 | Risk | Lexical ordering of formatted prices | S1 | To verify | Automated: numeric oracle + `TextParsersTest` |
| O13 | C4 | Risk | Items loaded on later pages break the order (sorting applied per page) | S2 | To verify | API-level test recommended; UI harvests 10 items |
| O14 | C5 | Risk | Details price differs from card price (taxes added, different dates) | S2 | To verify | Automated comparison; expected rule documented with PO |
| O15 | C5 | Automation challenge | Only visible cells exist in the XCUITest tree; card index changes after scroll | - | To verify | Cards identified by name, not index; harvest with scroll |
| O16 | C5 | Usability | Back from details loses scroll position | S3 | To verify | Exploratory only |
| O17 | All | Automation challenge | Missing accessibility identifiers on cards/labels | - | To verify | Contract table in ARCHITECTURE.md sent to iOS team |
| O18 | All | Risk | Behaviour on slow / lossy network (spinner forever, no retry) | S2 | To verify | Explore with Network Link Conditioner "3G" / "100% loss" |

## 3. Prioritisation - what was automated first and why

Scoring: **Business risk x User impact x Frequency x Regression risk**, each 1-3, then weighed against
**technical complexity / stability** of UI automation.

| Area | Risk | Impact | Freq. | Regr. | Score | UI complexity | Decision | Order |
|---|---|---|---|---|---|---|---|---|
| Login (happy) | 3 | 3 | 3 | 2 | 54 | Low | Automate - gate for everything | 1 |
| Search relevance | 3 | 3 | 3 | 3 | 81 | Medium | Automate - core value | 2 |
| Result -> details consistency | 3 | 3 | 3 | 2 | 54 | Medium | Automate - trust/legal risk | 3 |
| Sort by price | 3 | 3 | 2 | 3 | 54 | Medium (harvest) | Automate with numeric oracle | 4 |
| Filter rules + persistence | 3 | 2 | 2 | 3 | 36 | Medium | Automate | 5 |
| Login negative | 2 | 2 | 2 | 2 | 16 | Low | Automate (cheap, security signal) | 6 |
| Keyboard behaviour | 1 | 2 | 3 | 1 | 6 | Low | Automate one scenario (UX regression) | 7 |
| Order across pages | 2 | 2 | 1 | 2 | 8 | High | **API level**, not UI | - |
| Network loss, interruptions, accessibility, localisation | 2 | 2 | 1 | 1 | 4 | High / judgement | **Keep exploratory** each release | - |

**Rationale.** The end-to-end journey (launch -> login -> search -> filter -> sort -> details) is automated as one
`@e2e @smoke` release-gate scenario *and* each step is automated in isolation, so a failure points straight at
the broken capability. High-complexity, low-frequency or judgement-heavy risks stay exploratory: automating them
at UI level would cost more in flakiness than it returns in confidence.

## 4. Questions raised to the Product Owner
1. Sort by price: nightly or total price? Tie-break?
2. Are sponsored results exempt from sorting/filtering, and how are they marked?
3. Price filter boundaries inclusive?
4. Relevance: is "nearby" acceptable for a city search?
5. Should sort/filter persist across a new search?

## 5. Debrief notes
_Fill after the session: what surprised you, what took longest, what you would explore next, and any bugs filed
(with ticket ids)._
