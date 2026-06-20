<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: ClickUp Token + Uwierzytelniona Łączność (F-01)

- **Plan**: context/changes/clickup-token-and-connectivity/plan.md
- **Scope**: All 3 phases (complete)
- **Date**: 2026-06-20
- **Verdict**: APPROVED
- **Findings**: 0 critical, 1 warning, 2 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | PASS |
| Scope Discipline | PASS |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | PASS |

## Findings

### F1 — RestClient has no connect/read timeout

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality (reliability)
- **Location**: server/src/main/java/com/example/clickupsimplifier/clickup/ClickupClientConfig.java:19
- **Detail**: RestClient bean built with only baseUrl, no timeouts. Plan names timeout as an UNREACHABLE trigger, but without a read timeout a hung connection blocks the request thread indefinitely instead of surfacing as UNREACHABLE. Unit test simulates the error via thrown ResourceAccessException, so the gap isn't exercised.
- **Fix**: Configure a ClientHttpRequestFactory with connect+read timeouts (e.g. 5s/10s) on the builder.
- **Decision**: FIXED — SimpleClientHttpRequestFactory with 5s connect / 10s read in ClickupClientConfig

### F2 — getCurrentUser NPEs on an unexpected 200 body

- **Severity**: 🔭 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality (reliability)
- **Location**: server/src/main/java/com/example/clickupsimplifier/clickup/ClickupClient.java:21
- **Detail**: response.user().id() assumes a well-formed body. A 200 with missing/null "user" throws NPE, not caught by ConnectivityService (only HttpClientErrorException / RestClientException) → unhandled 500 rather than a structured result. Low likelihood given ClickUp's stable contract.
- **Fix**: Null-check the deserialized body/user; treat absence as UNREACHABLE (or dedicated error) in ConnectivityService.
- **Decision**: FIXED — ClickupClient throws RestClientException on null body/user → ConnectivityService maps to UNREACHABLE

### F3 — Non-401 4xx collapses to UNREACHABLE

- **Severity**: 🔭 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: server/src/main/java/com/example/clickupsimplifier/clickup/ConnectivityService.java:30-37
- **Detail**: Only 401 maps to TOKEN_REJECTED; other 4xx (403, 429 rate-limit) fall to UNREACHABLE, reading as a network problem. Matches the plan's literal mapping, so not drift — just semantic coarseness worth noting for S-01/S-02 (rate limits).
- **Fix**: None required now; revisit error granularity when batch pulls (S-01+) make 429 handling matter.
- **Decision**: SKIPPED — accepted; matches plan, revisit at S-01+
