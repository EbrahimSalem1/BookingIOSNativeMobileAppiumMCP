#!/usr/bin/env python3
"""
AI-assisted failure analysis for the iOS Booking suite.

Two layers, on purpose:

1. Deterministic triage (always runs, no network, no secrets)
   - reads Allure results, groups failures by signature, classifies them with transparent rules,
     detects flaky tests (failed + passed for the same test in one run, e.g. after the rerun pass),
     and joins the locator drift report.

2. LLM analysis (optional: --llm and ANTHROPIC_API_KEY set)
   - sends only the compact, already-structured summary (no screenshots, no credentials) to Claude
     and asks for root-cause hypotheses and next actions. The deterministic result is always kept,
     so the report is useful even when the model is unavailable or wrong.

Output: target/ai-failure-analysis.md (human) and target/ai-failure-analysis.json (machine, e.g. Slack/Jira bots).

Usage:
    python3 ai/failure-analyzer/analyze_failures.py --results target/allure-results [--llm]
Only the Python standard library is used, so it runs on any CI agent.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from collections import defaultdict
from dataclasses import dataclass, field, asdict
from pathlib import Path

CATEGORIES = [
    # (category, rule description, regex over "exception message trace")
    ("INFRASTRUCTURE", "Appium/WDA/simulator could not start or the session died",
     r"SessionNotCreated|WebDriverAgent|xcodebuild|ECONNREFUSED|invalid session id|Could not create an iOS session"),
    ("TEST_DATA", "Missing credentials, unknown dataset or invalid configuration",
     r"TestDataException|ConfigurationException"),
    ("LOCATOR_CHANGE", "An element was not found - the UI or its accessibility ids probably changed",
     r"NoSuchElementException|not found on|to be (visible|present) on|could not be found to open"),
    ("SYNCHRONISATION", "A condition never became true within the timeout",
     r"TimeoutException|Waited \d+s for"),
    ("PRODUCT_BUG", "The app responded, but a business rule was violated",
     r"AssertionError|ordered by|satisfies|property name|price on details|mentions|expected"),
]

MAX_TRACE_CHARS = 1200


@dataclass
class Failure:
    name: str
    full_name: str
    status: str
    message: str
    trace: str
    failed_step: str
    tags: list[str]
    device: str
    attachments: list[str]
    category: str = "UNKNOWN"
    rule: str = "No rule matched"


@dataclass
class Cluster:
    signature: str
    category: str
    rule: str
    tests: list[str] = field(default_factory=list)
    example_message: str = ""
    failed_step: str = ""


def load_results(results_dir: Path) -> list[dict]:
    results = []
    for path in sorted(results_dir.glob("*-result.json")):
        try:
            results.append(json.loads(path.read_text(encoding="utf-8")))
        except (OSError, json.JSONDecodeError) as exc:
            print(f"warning: skipping unreadable {path.name}: {exc}", file=sys.stderr)
    return results


def first_failed_step(steps: list[dict]) -> str:
    for step in steps or []:
        nested = first_failed_step(step.get("steps", []))
        if nested:
            return nested
        if step.get("status") in ("failed", "broken"):
            return step.get("name", "")
    return ""


def to_failure(result: dict) -> Failure:
    details = result.get("statusDetails") or {}
    labels = result.get("labels") or []
    params = {p.get("name"): p.get("value") for p in result.get("parameters") or []}
    return Failure(
        name=result.get("name", "?"),
        full_name=result.get("fullName") or result.get("name", "?"),
        status=result.get("status", "?"),
        message=(details.get("message") or "").strip(),
        trace=(details.get("trace") or "")[:MAX_TRACE_CHARS],
        failed_step=first_failed_step(result.get("steps", [])),
        tags=[l.get("value") for l in labels if l.get("name") == "tag"],
        device=params.get("Device", ""),
        attachments=[a.get("source", "") for a in result.get("attachments") or []],
    )


def classify(failure: Failure) -> None:
    haystack = f"{failure.message}\n{failure.trace}"
    for category, rule, pattern in CATEGORIES:
        if re.search(pattern, haystack, re.IGNORECASE):
            failure.category, failure.rule = category, rule
            return


def signature(failure: Failure) -> str:
    """Normalises volatile parts (numbers, ids, quoted values) so equal root causes cluster together."""
    first_line = (failure.message.splitlines() or [""])[0]
    normalised = re.sub(r"'[^']*'", "'<v>'", first_line)
    normalised = re.sub(r"\d+(\.\d+)?", "<n>", normalised)
    exception = re.search(r"([A-Za-z]+(Exception|Error))", failure.trace)
    return f"{exception.group(1) if exception else failure.category}: {normalised[:160]}"


def find_flaky(results: list[dict]) -> list[str]:
    statuses = defaultdict(set)
    for r in results:
        statuses[r.get("historyId") or r.get("fullName")].add(r.get("status"))
    names = {r.get("historyId") or r.get("fullName"): r.get("name") for r in results}
    return sorted(names[k] for k, s in statuses.items() if "passed" in s and s & {"failed", "broken"})


def analyse(results_dir: Path, drift_report: Path) -> dict:
    results = load_results(results_dir)
    flaky = set(find_flaky(results))
    # A test that eventually passed is reported as flaky, not as a failure.
    passed_ids = {r.get("historyId") for r in results if r.get("status") == "passed"}
    failures = [to_failure(r) for r in results
                if r.get("status") in ("failed", "broken") and r.get("historyId") not in passed_ids]
    for f in failures:
        classify(f)

    clusters: dict[str, Cluster] = {}
    for f in failures:
        sig = signature(f)
        cluster = clusters.setdefault(sig, Cluster(sig, f.category, f.rule, example_message=f.message[:400],
                                                   failed_step=f.failed_step))
        cluster.tests.append(f.name)

    drift = []
    if drift_report.exists():
        try:
            drift = json.loads(drift_report.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            pass

    totals = defaultdict(int)
    for r in results:
        totals[r.get("status", "unknown")] += 1

    return {
        "totals": dict(totals),
        "flaky": sorted(flaky),
        "clusters": [asdict(c) for c in sorted(clusters.values(), key=lambda c: -len(c.tests))],
        "failures": [asdict(f) for f in failures],
        "locator_drift": drift,
    }


SYSTEM_PROMPT = """You are a senior SDET triaging a native iOS (Appium XCUITest, Java, Cucumber) test run.
You receive a deterministic pre-analysis. For each cluster:
1. State the most likely root cause and your confidence (low/medium/high), citing the evidence fields you used.
2. Say whether you agree with the rule-based category; if not, explain why.
3. Propose the next concrete action and its owner (QA, iOS dev, backend, DevOps).
Rules: never suggest Thread.sleep or weakening assertions; for locator changes recommend verifying the new
locator with the Appium MCP server before changing code; say "insufficient evidence" rather than guessing.
Answer in concise Markdown with one section per cluster, then a 3-line overall summary."""


def ask_claude(summary: dict) -> str:
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        return "_LLM analysis skipped: ANTHROPIC_API_KEY is not set._"
    compact = {k: summary[k] for k in ("totals", "flaky", "clusters", "locator_drift")}
    body = json.dumps({
        "model": os.environ.get("AI_MODEL", "claude-sonnet-5-5"),
        "max_tokens": 2000,
        "system": SYSTEM_PROMPT,
        "messages": [{"role": "user", "content": "Pre-analysis JSON:\n" + json.dumps(compact, indent=2)[:60000]}],
    }).encode("utf-8")
    request = urllib.request.Request(
        "https://api.anthropic.com/v1/messages", data=body, method="POST",
        headers={"x-api-key": api_key, "anthropic-version": "2023-06-01", "content-type": "application/json"})
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            payload = json.loads(response.read().decode("utf-8"))
        return "\n".join(block.get("text", "") for block in payload.get("content", []) if block.get("type") == "text")
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
        return f"_LLM analysis unavailable ({exc}). Deterministic triage above is complete._"


def to_markdown(summary: dict, llm_text: str | None) -> str:
    t = summary["totals"]
    lines = [
        "# Test failure analysis",
        "",
        f"**Results:** {t.get('passed', 0)} passed, {t.get('failed', 0)} failed, {t.get('broken', 0)} broken, "
        f"{t.get('skipped', 0)} skipped  |  **Flaky:** {len(summary['flaky'])}  |  "
        f"**Locator drift:** {len(summary['locator_drift'])}",
        "",
    ]
    if not summary["clusters"]:
        lines += ["No unresolved failures. :white_check_mark:", ""]
    else:
        lines += ["## Failure clusters (deterministic triage)", "",
                  "| # | Category | Tests | Signature | Failing step |", "|---|---|---|---|---|"]
        for i, c in enumerate(summary["clusters"], 1):
            lines.append(f"| {i} | `{c['category']}` | {len(c['tests'])} | {c['signature'].replace('|', '/')} | "
                         f"{(c['failed_step'] or '-').replace('|', '/')} |")
        lines.append("")
        for i, c in enumerate(summary["clusters"], 1):
            lines += [f"### {i}. {c['category']} - {c['rule']}", "",
                      "Tests: " + ", ".join(c["tests"]), "", "```", c["example_message"] or "(no message)", "```", ""]
    if summary["flaky"]:
        lines += ["## Flaky (failed, then passed on rerun)", ""] + [f"- {n}" for n in summary["flaky"]] + [""]
    if summary["locator_drift"]:
        lines += ["## Locator drift (found only by a fallback strategy)", "",
                  "| Locator | Broken primary | Matched fallback | Screen | Hits |", "|---|---|---|---|---|"]
        for d in summary["locator_drift"]:
            lines.append(f"| {d.get('locator')} | `{d.get('brokenPrimary')}` | `{d.get('matchedFallback')}` | "
                         f"{d.get('screen')} | {d.get('occurrences')} |")
        lines.append("")
    if llm_text is not None:
        lines += ["## AI root-cause hypotheses (Claude) - requires human review", "", llm_text, ""]
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--results", default="target/allure-results", type=Path)
    parser.add_argument("--drift", default="target/locator-drift-report.json", type=Path)
    parser.add_argument("--out", default="target", type=Path)
    parser.add_argument("--llm", action="store_true", help="add Claude root-cause hypotheses (needs ANTHROPIC_API_KEY)")
    args = parser.parse_args()

    if not args.results.is_dir():
        print(f"No Allure results at {args.results}", file=sys.stderr)
        return 2

    summary = analyse(args.results, args.drift)
    llm_text = ask_claude(summary) if args.llm else None
    if llm_text is not None:
        summary["llm_analysis"] = llm_text

    args.out.mkdir(parents=True, exist_ok=True)
    (args.out / "ai-failure-analysis.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    report = to_markdown(summary, llm_text)
    (args.out / "ai-failure-analysis.md").write_text(report, encoding="utf-8")
    print(report)

    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as fh:
            fh.write(report + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
