# Roadmap and limitations

## Before public GitHub publication

- [ ] Two group members confirm attribution, repo visibility and desired license.
- [ ] Rotate any shared SQL passwords from the original coursework; do not publish the legacy PDF unchanged.
- [ ] Run Maven unit tests on a machine with dependencies available.
- [ ] Execute SQL creation + seed + REST scenarios on SQL Server and keep sample request outputs (without credentials).
- [ ] Verify legacy migration against a **backup** if preserving the existing coursework database.

## Engineering backlog

1. Add **real user authentication and per-home authorization** (Spring Security, ownership checks) before any hosted deployment.
2. Add SQL integration tests with disposable SQL Server containers and a repeatable migration tool.
3. Introduce durable background event processing / replay and bounded in-memory queues.
4. Persist rule dependency edges; validate cross-home dependency boundaries.
5. Model richer condition groups (AND/OR), priority conflict resolution and debouncing for noisy sensors.
6. Add structured error handling and transactional audit records for every failed device command.
7. Add IoT transport and hardware adapters (MQTT), plus a separate UI only if required.
8. Benchmark database indexes and large-scale event volumes; consider connection pooling and telemetry.

## Scope boundaries

A database update does not guarantee an actual electrical appliance changed state. No real-time notification delivery or access control between residents has been implemented. These are honest project limitations, not documented as completed features.
