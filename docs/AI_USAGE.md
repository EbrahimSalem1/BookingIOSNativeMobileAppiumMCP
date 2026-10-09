# AI usage

AI was used as an **engineering accelerator** with a human gate after every stage. This document records which
tools were used, for what, with which prompts, what came out, what a human changed, and where AI falls short.

## 1. Tools and agents

| Tool | Role | Where |
|---|---|---|
| **Claude (Anthropic)** - chat + Claude Code | Pair programmer for framework scaffolding, documentation drafts, code review | whole repo |
| **Claude Code sub-agents** | Specialised, reusable roles with fixed instructions and tool permissions | `.claude/agents/*.md` |
| ├ `requirement-analyst` | Story -> risk-ranked test design, ambiguities, Gherkin drafts | `ai/outputs/requirements/` |
| ├ `locator-scout` | Explores the live app via **Appium MCP**; verified locators; failure reproduction | `ai/outputs/locators/` |
| ├ `automation-engineer` | Implements reviewed scenarios following `CLAUDE.md` | `src/` |
| └ `failure-analyst` | Triage of failed runs, bug drafts, verified locator fixes | CI artefacts |
| **Appium MCP server** (`appium-mcp`) | Gives agents device access (session, tree, tap/type, screenshot) | `.mcp.json`, [APPIUM_MCP.md](APPIUM_MCP.md) |
| **Claude API** | Root-cause hypotheses on top of deterministic triage | `ai/failure-analyzer/analyze_failures.py` |

## 2. The workflow and its human gates

| # | Step | Automated by | Human validation (gate) |
|---|---|---|---|
| 1 | User story -> test conditions, risks, questions | `requirement-analyst` | QA/PO accept risk ranking, answer ambiguities - **AI never decides expected behaviour** |
| 2 | Test design review | - | Human selects UI vs API vs exploratory scope |
| 3 | Gherkin from accepted conditions | agent drafts (`@draft`) | Human edits wording, removes weak scenarios, drops `@draft` |
| 4 | Locator discovery | `locator-scout` via MCP | Human re-checks on a second device size; ids requested from iOS team |
| 5 | Step definitions + page objects | `automation-engineer` | `CodeConventionsTest` (machine gate) + code review (human gate) + 3 green runs |
| 6 | Code review | AI first pass (`04-code-review.md`) | Reviewer owns the merge |
| 7 | Execution | CI | - |
| 8 | Failure analysis | rules + Claude | Human confirms classification, files bug or merges fix |

## 3. Example prompts

Full prompt library: [`ai/prompts/`](../ai/prompts). Two representative examples:

**Requirement analysis** (`01-requirement-analysis.md`)
> Use the requirement-analyst agent. Story BOOK-SORT-1: As a traveller comparing stays, I want to sort search
> results by price, rating, or recommended ... Produce the analysis described in your instructions. Be explicit
> about ambiguities - do not resolve them yourself.

**Locator discovery through MCP** (`02-locator-discovery-mcp.md`)
> Use the locator-scout agent with the appium-mcp server. Start an iOS session with ai/mcp/capabilities.json, log in,
> search for "Paris", open the Sort sheet. For each element report primary + fallback locator and verify each returns
> exactly one displayed element. List elements with no accessibility identifier as a request for the iOS team.

**Framework scaffolding** (chat, initial)
> Build a Native iOS automation framework for a booking app: Appium Java client (W3C, XCUITestOptions), Cucumber,
> TestNG, POM, Allure, explicit waits only, externalised test data, parallel-ready... [challenge brief attached]

## 4. Generated output (examples)

| Output | Location |
|---|---|
| Requirement analysis with risk ranking and reviewer decisions | [`ai/outputs/requirements/BOOK-SORT-1.md`](../ai/outputs/requirements/BOOK-SORT-1.md) |
| Locator discovery table format (filled per screen once the app is available) | [`ai/outputs/locators/_TEMPLATE.md`](../ai/outputs/locators/_TEMPLATE.md) |
| Framework code, features, CI, docs (first drafts) | this repository |
| Failure triage report | `target/ai-failure-analysis.md` (generated per run; sample in [`ai/failure-analyzer/README.md`](../ai/failure-analyzer/README.md)) |

## 5. Review performed and improvements made

Every AI draft was reviewed against one question: *would this test fail if the feature were broken, and only then?*

### 5.1 Defects found in AI-generated code during review (and fixed)

These came from review iterations while the framework was generated. Add your own findings from the review of
the final code and from the first runs against the real app here - that log is the evidence of human ownership.

| # | Area | AI draft | Problem | Fix |
|---|---|---|---|---|
| 1 | Rating parser | regex with optional scale | `"4.5 120 reviews"` parsed as 4.5 out of 120 | Scale only after `/` or `out of`; covered by `TextParsersTest` |
| 2 | Cloud capabilities | flat `"bstack:options.userName"` keys | Sent as a literal dotted capability name; cloud ignores it | `CapabilityMapper` nests vendor blocks and types `"true"`/numbers |
| 3 | Sort indicator assertion | fell back to the Sort button text | Fails on apps that do not display the active sort (false failure) | Asserted only when a sort summary is present |
| 4 | Filter persistence | "button label contains a digit" | Too loose (matched a non-zero digit anywhere, e.g. inside a word) | Non-zero count token or matching chip |
| 5 | Details title locator | predicate on `traits` | Not a reliably queryable attribute in XCUITest predicates | Class chain fallback |
| 6 | Real-device signing | `xcodeOrgId` mandatory when `device.real` | Breaks device clouds that sign WDA themselves | Optional; applied only when configured |
| 7 | Parallel thread count | passed as a system property | TestNG ignores it there | Surefire `<properties>` `dataproviderthreadcount` |

### 5.2 Structural decisions owned by the engineer (not delegated to AI)
- Pages never assert; steps assert business rules.
- Oracles verify rules over a harvested sample (adjacent pairs for sorting, every item for filters) - a check on
  the first result alone would pass while items 2..n are wrong.
- Selected-card-vs-details comparison instead of comparing details to static test data.
- One rerun labelled flaky, never "retry until green".
- `CodeConventionsTest` so AI output is checked mechanically before human review.

### 5.3 Reviewer checklist used
- [ ] Assertion names a business rule and would catch a named bug
- [ ] No wait without a condition, no locator not observed on device
- [ ] No literal that belongs in test data; no secret in code, logs or attachments
- [ ] Failure message understandable without reading code
- [ ] Runs green 3x on simulator; fails for the right reason when the rule is broken (mutation check: invert the oracle once)

## 6. Limitations of the AI approach

| Limitation | Mitigation |
|---|---|
| **Hallucinated APIs / locators** - plausible but non-existent methods or accessibility ids | Compile + unit tests; locators only from MCP observation; `CLAUDE.md` forbids invented locators |
| **Oracle problem** - AI infers "expected" from current behaviour | Expected behaviour comes from requirements/PO; ambiguities are listed, not resolved |
| **Weak assertions that look strong** | Mutation check + reviewer question "which bug does this catch?" |
| **Non-determinism** - same prompt, different output | Agents produce reviewable artefacts (Markdown, PRs), never act directly on main |
| **Context limits** - misses conventions spread across a large repo | `CLAUDE.md` + convention tests make rules explicit and local |
| **Confidentiality** - prompts may contain app data | Only structured failure text is sent to the API; no screenshots/page sources/credentials; enterprise data controls |
| **Over-trust** - "the AI said it's flaky" | Flaky only with pass+fail evidence; AI reports labelled "requires human review" |
| **Cost/latency** of LLM calls in CI | Deterministic triage always; LLM layer optional and capped |
