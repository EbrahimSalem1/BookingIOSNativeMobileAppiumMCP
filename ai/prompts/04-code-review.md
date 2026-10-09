# 04 - AI-assisted code review

```
Review the diff of this branch against main as a senior mobile SDET.
Focus, in this order:
1. Flakiness: waits without conditions, timing assumptions, animations, keyboard covering elements,
   reliance on list position, shared state between scenarios or threads.
2. Assertion quality: does each Then step verify a business rule? Could it pass while the feature is broken?
3. Locators: strategy order (accessibility id > predicate/class chain > XPath), localisation-sensitive labels,
   dynamic values in locators.
4. Design: page objects without assertions, thin steps, data in testdata/, no duplication with existing code.
5. Security: no secrets, no credentials in logs or attachments.
Output: a table (file:line, severity blocker/major/minor, issue, suggested fix). Do not rewrite the code.
```

The model is a second reviewer, not the reviewer: it is good at spotting sleeps, duplication and weak
assertions; it cannot know whether a locator is stable on the real app or whether a business rule is right.
