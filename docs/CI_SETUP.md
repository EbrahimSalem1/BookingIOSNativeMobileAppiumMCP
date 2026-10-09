# CI setup (GitHub Actions)

The `framework-checks` job (device-free unit and convention tests) runs on every push with no setup.
The `ios-simulator` job is **skipped** until the configuration below exists, so the repository stays green
before the app build and test account are available.

Add these under **Settings → Secrets and variables → Actions**.

## Secrets (encrypted)

| Name | Value | Required |
|---|---|---|
| `BOOKING_USERNAME` | Test account username / email provided with the challenge | yes |
| `BOOKING_PASSWORD` | Test account password | yes |
| `APP_DOWNLOAD_TOKEN` | Fine-grained token with **Contents: read** on the repository that publishes the app build | yes |
| `ANTHROPIC_API_KEY` | Enables the Claude layer of the failure analyzer | optional |

## Variables (plain text)

| Name | Example | Required |
|---|---|---|
| `APP_RELEASE_REPO` | `EbrahimSalem1/booking-ios-builds` - repo whose releases contain `*-simulator.zip` (a zipped `.app` built with `-sdk iphonesimulator`) | yes |
| `BOOKING_BUNDLE_ID` | `com.example.booking` | yes |

## Providing the app build

If the app is delivered as a file rather than a release: create a repository (or a release in this one),
upload the simulator build zipped as `Booking-simulator.zip`, and point `APP_RELEASE_REPO` at it.
A device `.ipa` cannot run on a simulator; the simulator job needs a `.app` built for `iphonesimulator`.

## Running

- Automatically: pull requests (`@smoke`), pushes to `master`/`main`, nightly (`@regression or @smoke`).
- Manually: **Actions → iOS E2E → Run workflow**, choose tags and thread count.

The CLI equivalent, from a machine where `gh` is authenticated as a repo admin:

```bash
R=EbrahimSalem1/BookingIOSNativeMobileAppiumMCP
gh secret set BOOKING_USERNAME   -R $R      # prompts for the value, keeps it out of shell history
gh secret set BOOKING_PASSWORD   -R $R
gh secret set APP_DOWNLOAD_TOKEN -R $R
gh variable set APP_RELEASE_REPO  -R $R --body "<owner>/<repo>"
gh variable set BOOKING_BUNDLE_ID -R $R --body "<bundle id>"
```
