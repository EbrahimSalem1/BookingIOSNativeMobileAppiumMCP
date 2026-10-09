# Prompt library

Reusable prompts for each stage of the AI-assisted workflow. They are written for Claude Code
(where the matching sub-agents in `.claude/agents/` are available) but work in any chat model.

| Stage | Prompt | Agent | Human gate |
|---|---|---|---|
| 1. Requirement analysis | [01-requirement-analysis.md](01-requirement-analysis.md) | `requirement-analyst` | PO/QA review of questions + risk ranking |
| 2. Locator discovery (MCP) | [02-locator-discovery-mcp.md](02-locator-discovery-mcp.md) | `locator-scout` | Locators verified on device |
| 3. Automation generation | [03-automation-generation.md](03-automation-generation.md) | `automation-engineer` | Code review + 3 green runs |
| 4. Code review | [04-code-review.md](04-code-review.md) | (any) | Reviewer owns the merge |
| 5. Failure analysis | [05-failure-analysis.md](05-failure-analysis.md) | `failure-analyst` | Human files the bug / merges the fix |
