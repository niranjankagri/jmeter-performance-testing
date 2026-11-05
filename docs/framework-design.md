# Framework design

Why the project is built the way it is. The README says what it does and how to run it.

## 1. Goals

1. Show a JMeter setup a team could really use: a readable plan, repeatable runs from one command, and a result that is
   a pass or a fail, not a graph someone has to interpret.
2. Measure something real: a system with a known limit, so the test's answer can be checked.
3. Check correctness under load, not only speed.

## 2. The parts

| Part | Role | Tools |
|---|---|---|
| `booking/` | The API under test | JDK `HttpServer`, virtual threads, Jackson |
| `src/test/jmeter/booking.jmx` | The test plan | JMeter 5.6.3 |
| `src/test/jmeter/users.csv` | 200 test users | CSV Data Set |
| `pom.xml` profiles | The load shape of each test | Maven, jmeter-maven-plugin |
| `gate/` | Reads the results and decides | Plain Java |
| `config/slo/` | The SLOs, one file per profile | Properties files |

## 3. The app is small on purpose

The API has no framework, so the only limit in play is the one built in: four payment connections held for 40-80 ms.
That allows 4 / 0.06 s = about 66 bookings per second. Because the expected answer is known, the stress test can be
checked: it measured 66.

The limited event has 101 seats and the plan books 2 at a time. An odd number means correct code stops at 100, so a bug
can't reach exactly the capacity by luck.

## 4. One plan, many profiles

The flow exists once. Load comes from JMeter properties, set by Maven profiles, read with `${__P(users,5)}`. A second
thread group, `Burst`, has 0 users except in the spike profile. Four copies of the plan would drift apart.

## 5. Why a gate

JMeter's assertions fail single samples. Nothing in JMeter fails a run because p95 is too high, and the stress result
shows why that matters: p95 rose more than tenfold with no errors at all.

`SloGate` is plain Java with unit tests, and its rules are strict about missing data:

- A label with no samples is a miss, so a renamed transaction can't vanish from the check.
- Each SLO has a minimum sample count, so a short run or an old file can't pass.
- Columns are found by name, and a missing column stops the gate.

The stress profile runs with `slo.enforce=false`. It looks for the limit, so passing it is the point: the table still
shows the misses.

## 6. Correctness under load

`Book seats` counts the seats the API confirmed on the limited event. After all load has stopped, a tearDown group reads
the event and compares: `sold` must not exceed `capacity`, and must equal the test's own count. A lost update or a double
sale fails the run.

A concurrent bug is unlikely to appear in a single-user test, so this runs in every profile. `EventTest` covers the same
risk at unit level, with 64 threads released at once, repeated 20 times.

## 7. Choices in the plan

- **Transaction Controllers with a parent sample:** results and SLOs use business steps. The requests inside still appear
  in the results file for diagnosis.
- **`409` accepted on booking:** sold out is correct behaviour, so it isn't an error.
- **Login once per user:** a real session logs in once, not on every loop.
- **A random event:** the extractor's match number 0 spreads load over all events.
- **No listeners:** the plan writes results to a file, and the report is built afterwards.
- **Comments in the plan:** each element's comment field says why it's there, because `.jmx` XML is hard to review.

## 8. What went wrong while building it

**A false pass.** The first "load" runs took 14 seconds and showed the smoke test's numbers. `-DskipTests` was meant for
the unit tests, but the JMeter plugin obeys it too, so JMeter never ran and the gate passed on the previous results. The
plugin now has `skipTests` set to false, and the minimum sample counts mean an old file can't pass.

**A think time that was much too long.** 20 users made 8 bookings per second where about 26 were expected. A timer applies
to every sampler in its scope, so both think times were delayed onto every request. Each timer now sits under its own
Flow Control Action, and the load reached the API's real limit.

**A plan that never stopped.** An early test plan wrote 20 million rows in about two minutes, and looked like JMeter hanging
on a new Java version. `LoopController.continue_forever` defaults to true when it's left out of the XML.

## 9. How I know the gate can fail

Two bugs were put into the API on purpose and run through the full load profile, with the unit tests skipped so that only
the performance test could catch them:

| Bug | What failed |
|---|---|
| One seat too many (`capacity + 1`) | `Check no overselling` |
| One payment connection, not four | `Book seats` p95 of 1,329 ms against 300 ms, and too few samples on three transactions |

## 10. Limits

- The numbers describe this demo, measured on one laptop with the generator and API on the same machine.
- JMeter's thread groups are a closed model, so overload looks milder than with real public traffic. My
  [k6 project](https://github.com/niranjankagri/k6-performance-testing) covers open-model tests.
- No soak test and no server-side monitoring. Finding the limit here relied on knowing the design; a real system would
  need server metrics to find the cause.
- CI runners vary, so the SLOs catch large regressions, not small ones.
