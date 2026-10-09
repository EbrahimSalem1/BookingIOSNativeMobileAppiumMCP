# 01 - Requirement analysis

```
Use the requirement-analyst agent.

Story BOOK-SORT-1:
As a traveller comparing stays, I want to sort search results by price (low to high,
high to low), rating, or recommended, so that the best options for me appear first.
Acceptance criteria:
- Sort options are available from the results screen.
- Selecting an option re-orders the current results.
- The active sort option is visible.

Context: native iOS app, results are an infinite-scroll list, results may be filtered.
Produce the analysis described in your instructions. Save to ai/outputs/requirements/BOOK-SORT-1.md.
Be explicit about ambiguities - do not resolve them yourself.
```

**What to check in the output (human gate):**
- Are the implicit rules real (ties, unpriced items, sort + filter interaction, sort persistence after back navigation)?
- Is the risk ranking defensible? Price sorting directly affects conversion; "Recommended" cannot be asserted.
- Are the Gherkin drafts business-level, and do they reuse existing steps?
