# AI failure analyzer

Turns a failed run into a triaged, actionable report in seconds.

```bash
# deterministic triage only (no network, no secrets) - safe for every PR
python3 ai/failure-analyzer/analyze_failures.py --results target/allure-results

# + Claude root-cause hypotheses
export ANTHROPIC_API_KEY=...            # CI secret
export AI_MODEL=claude-sonnet-5-5       # optional override
python3 ai/failure-analyzer/analyze_failures.py --results target/allure-results --llm
```

Outputs `target/ai-failure-analysis.md` (also appended to the GitHub Actions job summary) and
`target/ai-failure-analysis.json` (for Slack / Jira automation).

## Design choices

| Choice | Why |
|---|---|
| Rules first, LLM second | The rule-based layer is reproducible, explainable and free; the LLM adds hypotheses on top and can never remove facts. |
| Cluster by normalised signature | 20 failures with one root cause become one item to fix, not 20. |
| Flaky = failed **and** passed in the same run | Evidence-based; a single failure is never labelled flaky by opinion. |
| Locator drift joined in | Turns "it still passes" into an early warning before the fallback breaks too. |
| Only structured text is sent to the model | No screenshots, page sources or credentials leave CI. Messages are truncated. |
| Output says "requires human review" | The model proposes; a person decides what is filed as a bug or merged. |

Same categories as `src/test/resources/categories.json`, so the Allure *Categories* tab and this report agree.
