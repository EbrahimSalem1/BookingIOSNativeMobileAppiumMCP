# 02 - Locator discovery through Appium MCP

Prerequisites: Appium running (`appium`), simulator booted, app installed, `.mcp.json` loaded (`/mcp` shows `appium-mcp`).

```
Use the locator-scout agent with the appium-mcp server.

Start an iOS session with ai/mcp/capabilities.json.
Log in with the credentials in BOOKING_USERNAME / BOOKING_PASSWORD, search for "Paris",
then open the Sort sheet.

For the Search Results screen and the Sort sheet:
- capture screenshot + page source
- for: results list, one result card and its name/location/price/rating labels, filter button,
  sort button, each sort option, apply button
  report primary + fallback locator, and verify each returns exactly one displayed element
- list elements with no accessibility identifier as a request for the iOS team

Output the table and a Locator.named(...) block in the style of SearchResultsPage.java.
Do not book anything and close the session when done.
```

**Human gate:** run the proposed locators once more yourself in Appium Inspector or via MCP on a
*second* device size (e.g. iPhone SE) - labels and cell structure often differ on compact layouts.
