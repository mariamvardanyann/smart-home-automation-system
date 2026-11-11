# Test evidence and honest verification status

## Verified during repository preparation

- All custom Java engine source files were compiled with `javac` (JDK 21).
- `bash scripts/check-engine.sh` passed a dependency-free smoke test exercising queue sizing, multi-condition matching, state-isolated preview, event ID preservation, cycle rejection and operator aliasing.
- Scripts and repository content were checked for obvious plaintext passwords, stale compiled artifacts and missing authorship credit.

## Included automated tests

`backend/src/test/java/.../RuleEngineTest.java` covers seven JUnit 5 test cases without database access: event queue counts, AND rules, preview isolation, cycles, duplicate suppression, input/operator validation and disabled rules. `./mvnw test` should run these after Maven dependencies can be resolved. **These JUnit tests were not executed in the preparation environment because dependency download was blocked.**

## Not verified yet

- Spring Boot compilation or application startup with its full dependency graph.
- SQL Server DDL/trigger/procedure execution or database migrations against real data.
- REST requests and response codes through a live server, JDBC transaction behavior, or multi-request concurrency behavior.
- CI results on GitHub.

For those checks, follow [SETUP_WINDOWS.md](SETUP_WINDOWS.md), run `./mvnw.cmd test`, execute demo SQL, and exercise the calls in `../examples/requests.http`. If something fails, preserve the failure output and address it before representing the repo as fully tested.
