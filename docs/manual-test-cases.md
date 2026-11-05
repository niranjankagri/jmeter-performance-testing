# Manual test cases

The test cases this project automates, written out so a tester can review or run them by hand. Each JUnit method starts
with the case ID (`tc111_theLastSeatsCanOnlyBeSoldOnce`), so a failure in the report leads straight to the case below.

| ID range | Area | Automated in |
|---|---|---|
| TC-101 to TC-111 | Booking API | `BookingApiTest` |
| TC-121 | Seat count under concurrency | `EventTest` |
| TC-201 to TC-203 | Statistics | `LabelStatsTest` |
| TC-211 to TC-214 | Results file | `JtlReaderTest` |
| TC-221 to TC-229 | SLO gate | `SloGateTest` |
| TC-301 to TC-306 | Performance | `booking.jmx` with a Maven profile |

## Setup

Start the API with
`mvnw.cmd compile exec:java -Dexec.mainClass=com.niranjankagri.booking.Main -Dexec.args=18080`. Then use curl or Postman against `http://localhost:18080`. The test users are `user001` to `user200`,
with passwords `pass-001` to `pass-200`. Send the token as `Authorization: Bearer <token>`.

## Booking API

| ID | Action | Expected |
|---|---|---|
| TC-101 | `GET /health`, no token | `200`, status `UP` |
| TC-102 | `POST /login` with `user001` / `pass-001` | `200` with a token |
| TC-103 | `POST /login` with a wrong password | `401` |
| TC-104 | `GET /events` | `200`, 20 events |
| TC-105 | `GET /events/E-99` | `404` |
| TC-106 | `POST /bookings` with no token | `401` |
| TC-107 | Book 2 seats on `E-01`, then read the booking | `201`, then `200` with 2 seats |
| TC-108 | Read user001's booking with user002's token | `404` |
| TC-109 | Book 0 seats, then 5 seats | `400` both times |
| TC-110 | `POST /login` with the body `not json` | `400` |
| TC-111 | Book `E-LIMITED` 25 times with 4 seats, then 2 seats, then 1 seat | 25 x `201`, then `409`, then `201`; `sold` is 101 |

## Seat count under concurrency

| ID | Setup | Expected |
|---|---|---|
| TC-121 | 101 seats; 64 threads each take 2 seats, 10 times, released together; repeated 20 times | Exactly 100 sold, and the threads' own count is 100 |

## Statistics

| ID | Input | Expected |
|---|---|---|
| TC-201 | 100 samples of 1 to 100 ms, unsorted | p95 is 95 ms |
| TC-202 | One sample of 7 ms | p95 is 7 ms |
| TC-203 | 3 passed, 1 failed | 25 % errors |

## Results file

| ID | Input | Expected |
|---|---|---|
| TC-211 | Columns in an unusual order | Read correctly by name |
| TC-212 | A quoted failure message with a comma | Columns don't shift |
| TC-213 | Doubled quotes in a quoted cell | Read as one quote |
| TC-214 | No `elapsed` column | Error naming the column |

## SLO gate

The rule in these cases: `Book seats`, p95 at most 500 ms, errors at most 1 %, at least 10 samples.

| ID | Results | Expected |
|---|---|---|
| TC-221 | 100 samples of 400 ms | Pass |
| TC-222 | 100 samples of 501 ms | Miss: p95 |
| TC-223 | 100 samples of exactly 500 ms | Pass |
| TC-224 | 98 passed, 2 failed | Miss: errors 2 % |
| TC-225 | Samples for another label only | Miss: no samples |
| TC-226 | 9 fast samples | Miss: only 9 samples |
| TC-227 | A miss, with enforcing on | The build fails |
| TC-228 | A miss, with enforcing off | The report shows it; the build passes |
| TC-229 | An SLO file without `maxErrorPercent` | Error naming the setting |

## Performance

| ID | Command | Load | Expected |
|---|---|---|---|
| TC-301 | `mvnw.cmd verify` | 5 users, 30 s | Every transaction within the smoke SLOs, no errors |
| TC-302 | `mvnw.cmd verify -Pload` | 30 users, 120 s | `Book seats` p95 within 300 ms, no errors |
| TC-303 | `mvnw.cmd verify -Pspike` | 15 users, plus 80 for 20 s | `Book seats` p95 within 1.5 s, no errors, normal again after the burst |
| TC-304 | `mvnw.cmd verify -Pstress` | Rising to 120 users over 170 s | Report only: throughput levels off at about 66 per second, no errors |
| TC-305 | Any profile | | `Check no overselling` passes: `sold` within capacity and equal to the seats confirmed |
| TC-306 | Any profile | | Results come from this run: each transaction has at least the profile's minimum samples |
