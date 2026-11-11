# Local prototype API

Base URL: `http://localhost:8080`. All routes except `GET /hello` require `X-API-Key: <your local key>`. This is a **shared demo key**, not per-user login.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/hello` | Basic liveness response (does not check DB health) |
| GET | `/devices` | List devices (`on` vs `active`) |
| PUT | `/devices/{id}/toggle` | Toggle a simulated on/off state |
| GET | `/rules` | All loaded rules |
| POST | `/rules` | Create a rule with conditions + actions in SQL |
| PUT | `/rules/{id}/enable` | Mark rule active |
| PUT | `/rules/{id}/disable` | Mark rule inactive |
| POST | `/rules/preview` | Plan actions without changing states |
| GET | `/rules/by-sensor/{sensorId}` | Index lookup |
| POST | `/events` | Persist a sensor event and queue it |
| POST | `/events/process` | Process all queued events |
| GET | `/sensors` | Latest in-memory readings |
| GET | `/logs` | In-memory applied action log |
| GET | `/logs/details` | Human-friendly SQL execution history |
| GET | `/homes/{homeId}/logs/details` | SQL execution history by rule home |
| GET | `/homes/{homeId}/dashboard` | Counts by home |
| GET | `/users/{userId}/homes` | Demo dataset membership lookup (NOT secure per-user access) |
| GET | `/homes/{homeId}/rooms` | Rooms within a home |
| GET | `/rooms/{roomId}/devices` | Devices within a room |
| GET | `/dependency-graph` | In-memory dependency edges |
| POST | `/dependencies` | Add an edge, reject cycles; not persistent |
| GET | `/summary` | Event/rule/action processing counters |
| POST | `/devices/{id}/test` | Responds 501: no physical device testing |

## Example JSON

### Trigger sensor reading

`POST /events`:

```json
{"sensorId": 1, "value": 1}
```

Use the actual sensor ID from your installed database. Then call `POST /events/process`. A reading is **stored once** in `Event`; resulting `ExecutionLog` rows reference its generated `event_id`.

### Create a rule

`POST /rules`:

```json
{
  "name": "Warm up hallway",
  "homeId": 1,
  "conditions": [{"sensorId": 2, "operator": "<", "value": 18}],
  "actions": [{"deviceId": 4, "command": "turn_on"}]
}
```

Both sensor and action device must belong to the requested home. Supported simulated commands include `on`, `off`, `turn_on`, `turn_off`, `turn_on_*`, `turn_off_*`, `shut_off*` and `send_alert`; these are simulations and not hardware commands. Use valid ID values from your own database, not the example numbers here.

### Non-mutating preview

`POST /rules/preview`:

```json
{"sensorId": 2, "value": 16}
```

The response lists proposed actions only if **all** conditions of a rule are satisfied by the hypothetical reading and previously processed readings. It does not promise physical success or change server state.

### Dependency edge

`POST /dependencies`:

```json
{"fromRuleId": 1, "toRuleId": 2}
```

Rejects self-links and cycles. Edges are not persisted and are lost on restart.

## Validation and errors

Malformed rule fields and invalid readings return a client error where explicitly validated. Unknown entities may return 404; database failures return a generic 503 from the global error handler. **Not all endpoints have full production-grade validation or tenant authorization.** Do not send personal data to this demo API.
