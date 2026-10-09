# 03 - Automation generation

```
Use the automation-engineer agent. Follow CLAUDE.md strictly.

Implement src/test/resources/features/04_sort.feature (already reviewed - no @draft).
Use the verified locators in ai/outputs/locators/search-results.md and ai/outputs/locators/sort-sheet.md.

Requirements:
- the sort assertion must verify the business rule on a harvested sample of results
  (numeric ordering, unpriced/unrated items last), not just that an option was tapped
- report the first out-of-order pair in the assertion message
- sort labels come from testdata/sorting.json
- run mvn -q test -Punit and fix all violations
Finish with the self-review checklist from your instructions.
```

**Human gate:** read every assertion and ask "which real bug would make this fail?". If the answer is
"none", the test is decoration. Then run the new scenarios 3 times on a simulator.
