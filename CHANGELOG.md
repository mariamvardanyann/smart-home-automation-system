# Change log

## Unreleased — portfolio repository preparation (AI-assisted revision)

**Sources:** original SQL archive, standalone Java engine, earlier backend, later expanded backend, original 40-page team coursework report. The expanded backend is the main codebase retained here.

### Improved

- Reorganized into a single professional `backend/`, `database/`, `docs/`, `examples/` repository.
- Removed committed application database credentials and SQL login passwords; supplied environment-only config and `.env.example`.
- Added fail-closed shared API-key access gate and origin-restricted CORS **for local demo use only**.
- Fixed linked-queue size accounting and added finite event-value validation.
- Fixed rule preview to use the engine's full-condition evaluator with no state mutation.
- Made action planning avoid duplicate execution for graph paths, reject cycles and retain original event database IDs.
- Added `ConditionTable.sensor_id` so new rules store the exact sensor rather than writing `motion` for every condition; legacy ambiguous mappings are skipped.
- Split device availability (`is_active`) from simulated power state (`on_state`).
- Persisted each incoming sensor reading once; simulated device-state updates and action logs now share a transaction.
- Replaced mutating GET operations with POST and corrected the preview response class spelling.
- Serialized facade operations for basic thread safety; returned defensive snapshots from the rule engine.
- Replaced misleading physical test endpoint behavior with explicit `501 Not Implemented`.
- Replaced unwieldy unverified sample dataset with a small idempotent demo seed; removed SQL test INSERTs from trigger definitions.
- Added migration guide/script, SQL views, indexes and procedures kept from original coursework.
- Added a local smoke test, JUnit tests, CI and professional repository documentation.

### Security and provenance

- Original PDF is **not** bundled because its examples contain fixed login passwords and stale SQL/code snippets.
- No license was assumed without team agreement.
- Group authorship is prominent; original individual contributions remain **unverified**.

### Not yet verified

- Maven dependency resolution and Spring Boot runtime (network unavailable in this preparation environment).
- SQL Server execution, migration against real data and REST end-to-end tests (no SQL Server available).
- Production-grade authentication, authorization, resilient delivery, or hardware support (not implemented).
