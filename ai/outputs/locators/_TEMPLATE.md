# <Screen name> - locator discovery (Appium MCP)

> Produced by the `locator-scout` agent via `appium-mcp` on: `<device> / iOS <version> / app build <x.y.z (n)>`
> Evidence: `evidence/<screen>-screenshot.png`, `evidence/<screen>-source.xml`

| Element | Type | Primary (verified) | Fallback (verified) | Matches | Notes |
|---|---|---|---|---|---|
| Results list | XCUIElementTypeTable | `accessibilityId: search_results_list` | `classChain: **/XCUIElementTypeTable` | 1 | |
| Result card | XCUIElementTypeCell | `classChain: **/XCUIElementTypeCell[\`name BEGINSWITH 'result_card'\`]` | `classChain: **/XCUIElementTypeTable/XCUIElementTypeCell` | n | Only visible cells are in the tree |
| Card price | XCUIElementTypeStaticText | `accessibilityId: result_card_price` | predicate on currency symbol | 1 per card | Dynamic text - never put the value in a locator |

## Missing accessibility identifiers (request to iOS team)
| Element | Suggested id | Why it matters |
|---|---|---|
| | | |

## Ready-to-paste
```java
private static final Locator RESULTS_LIST = Locator.named("results.list")
        .accessibilityId("search_results_list")
        .classChain("**/XCUIElementTypeTable")
        .build();
```
