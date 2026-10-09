# 05 - Failure analysis

```
Use the failure-analyst agent.

The nightly run failed. Start from:
  python3 ai/failure-analyzer/analyze_failures.py --results target/allure-results
then inspect attachments for one test per cluster, and target/locator-drift-report.json.

For every LOCATOR_CHANGE cluster, reproduce on the simulator through appium-mcp and propose a verified locator.
For every PRODUCT_BUG cluster, draft a Jira bug: title, environment (device / iOS / build), steps,
expected vs actual (quote the assertion message), evidence, severity and your confidence.
```

**Human gate:** a person confirms the classification before a bug is filed or a locator change is merged.
