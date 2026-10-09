#!/usr/bin/env bash
# Local convenience wrapper: run -> rerun failures once -> report -> AI triage.
#
#   ./scripts/run-tests.sh                 # @smoke on one simulator
#   TAGS="@regression" THREADS=2 ./scripts/run-tests.sh
set -uo pipefail

TAGS="${TAGS:-@smoke}"
THREADS="${THREADS:-1}"
ENV_NAME="${ENV_NAME:-local}"

rm -rf target/rerun target/allure-results   # never judge this run by a previous run's leftovers

mvn -q test -Denv="$ENV_NAME" -Dcucumber.filter.tags="$TAGS" -Dthreads="$THREADS" -Dmaven.test.failure.ignore=true

if [[ -s target/rerun/failed.txt ]]; then
  echo "Re-running failed scenarios once (flaky detection)..."
  mvn -q test -Prerun -Denv="$ENV_NAME" -Dthreads="$THREADS" -Dmaven.test.failure.ignore=true
fi

python3 ai/failure-analyzer/analyze_failures.py --results target/allure-results ${ANTHROPIC_API_KEY:+--llm} || true
mvn -q allure:report && echo "Report: target/site/allure-maven-plugin/index.html"

# Final verdict: fail only if something is still failing after the single rerun.
FINAL="target/rerun/failed.txt"
[[ -f target/rerun/failed-after-rerun.txt ]] && FINAL="target/rerun/failed-after-rerun.txt"
if [[ -s "$FINAL" ]]; then
  echo "FAILED scenarios:"; cat "$FINAL"; exit 1
fi
echo "All scenarios passed."
