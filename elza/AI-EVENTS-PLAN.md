# AI task events in Elza — integration plan (temporary)

> Working document for the Elza-side consumption of the AI provider's task-event
> stream (`GET /tasks/{taskId}/events`, protocol 0.8.0; provider design:
> `doc/task-events-proposal.md` in elza-ai-provider.git). **Delete when the
> implementation is done** — durable knowledge goes into code javadoc and the
> provider proposal.

## Goal

Increase responsiveness of the AI panel and surface what a running task is
*doing* (phases, tool calls, later the streamed answer), plus keep a durable
per-request audit trail of the provider's internal work.

## Architecture

Two independent legs, joined by the DB and a per-user WebSocket push:

```
provider GET /tasks/{id}        provider GET /tasks/{id}/events
   (authoritative poll)            (advisory event stream)
        │                                 │
  AiRequestPoller                   AiEventPoller (new)
   state machine:                    persist events → ai_request_event
   output, usage, tools,             cursor → ai_request.event_seq
   terminal states                   phase → progress columns
        │                            answer_delta → in-memory buffer
        └───────────┬────────────────────┘
                    ▼
      AiRequestViewMapper (shared): AiRequest row + events + buffer
                    ▼  render-ready AiRequest VO
      /user/queue/ai-request  (STOMP user destination, per-user)
                    ▼
      client replaces the request in its conversation state
```

### Decisions (settled in review, 2026-07-14)

1. **Task poll stays authoritative.** `AiRequestPoller` is unchanged as the
   state-machine driver. The event stream is advisory by contract (a provider
   may emit none — S1 serves an empty stream), so nothing may depend on it.
2. **Per-user WebSocket push instead of notify+refetch.** The broker already
   configures `setUserDestinationPrefix("/user")` and the STOMP handshake is
   session-authenticated, so `convertAndSendToUser(username, "/queue/ai-request",
   …)` reaches only that user's sessions (all tabs). The old broadcast
   `AI_REQUEST_CHANGE` (id-only, whole-conversation refetch) is **replaced**.
3. **Push whole snapshots, not deltas.** The message carries the complete
   render-ready `AiRequest` VO (state, progress, activities, partialAnswer,
   blocks when done). The simple broker is fire-and-forget and the client
   deliberately disconnects on hidden tabs — idempotent snapshots make lost
   messages harmless (next push corrects; reconnect refetches the conversation).
4. **Events are persisted into `ai_request_event`** (except `answer_delta`),
   `event_type` = wire code (lowercase — visually distinct from Elza's own
   UPPERCASE codes in the same open set), `data` = the wire `TaskEvent` JSON.
   Row `create_date` = local receive time (stable ordering; display timestamps
   come from the JSON `createdAt`). Cursor `ai_request.event_seq` advances with
   the batch in the same transaction → restart-safe, no duplicates.
5. **`answer_delta` is not persisted** — appended to an in-memory per-request
   buffer, exposed as `AiRequest.partialAnswer` while running. Lost on restart
   (provisional text by contract; the durable answer is `output`).
6. **Client-tool dedup**: the provider also emits `tool_call` events for
   *client* tools, but Elza's own `TOOL_CALLS`/`TOOL_RESULTS` records are
   richer (full arguments → query, results → navigable links). The activity
   mapper skips provider tool events whose `tool` is registered in
   `AiToolRegistry`; internal tools (`search_knowledge`, …) map from provider
   events.
7. **`model_round` / lifecycle events**: persisted (audit), not shown as
   activities — visible via the transparency log (`GET /aiprovider/request/{id}/event`).
   `phase` events update `progress_message/percent` only (persisted for audit).
8. **UI API change is additive and minimal**: `AiRequest.partialAnswer` +
   activity-kind vocabulary (`PREPARATION`; internal tool names). No new REST
   endpoint — snapshot on panel open / WS reconnect uses the existing
   conversation detail GET.

## Stages

- **E1 (this change)** — everything except the streaming-answer UI: events
  poller + persistence + cursor, per-user push replacing `AI_REQUEST_CHANGE`,
  shared VO mapper, `partialAnswer` in the spec + buffer plumbing, activity
  mapping for the S2 vocabulary, client merge + reconnect resync. Against an
  S1 provider this is a runtime no-op (empty stream) — releasable now.
- **E2 (provider S2 live)** — validate real events end-to-end, refine `query`
  extraction from event `detail`, labels/icons polish, transparency dialog
  pretty-print.
- **E3 (provider S3 live)** — render `partialAnswer` as a provisional answer
  bubble in the panel.

## E1 work list

Backend (elza-core):
- [x] Liquibase: `ai_request.event_seq bigint not null default 0` + entity field
- [x] `AiAnswerBuffer` — in-memory partial-answer store (append/read/clear)
- [x] `AiRequestUpdateMessage` + `AiRequestPushService` — build & send
      `{eventType: "AI_REQUEST_UPDATE", conversationId, request}` to
      `/user/queue/ai-request` of the conversation owner
- [x] `AiRequestViewMapper` — `toVO(request, events)` extracted from
      `AiConversationService`, reads `AiAnswerBuffer`, used by service + pollers
- [x] `AiRequestPoller` — push instead of `AI_REQUEST_CHANGE`; starts
      `AiEventPoller` alongside itself
- [x] `AiEventPoller` — per open request long-poll `getTaskEvents(since, wait)`;
      persist batch + cursor in one tx; push after commit; stops on terminal
      envelope state; disables itself per external system on 404/405 (older
      provider); WARN+backoff on other errors
- [x] `AiActivityMapper` — provider wire events → activities (internal
      `tool_call`/`tool_result` pairing by callId/order, `preparation` kind,
      client-tool skip via `AiToolRegistry`)
- [x] remove `EventType.AI_REQUEST_CHANGE`

API (elza-openapi.yml → regenerate Java VOs + TS client):
- [x] `AiRequest.partialAnswer?: string`
- [x] doc updates: `AiRequestActivity.kind` (+`PREPARATION`), `AiRequestEvent.eventType`
      (provider wire codes), `AiRequest` (push channel note)

Frontend (elza-react):
- [x] subscribe `/user/queue/ai-request` in `websocketActions.jsx`
- [x] `EventType`: `AI_REQUEST_CHANGE` → `AI_REQUEST_UPDATE`
- [x] `useAiConversation`: merge pushed request VO into `detail.requests`
      (replace by id / append), recompute `pending`; refetch conversation on
      WS reconnect while a conversation is open
- [x] activity labels for `search_knowledge`, `get_section`, `PREPARATION` kind

Verification:
- [x] `mvn -pl elza-core test-compile` — BUILD SUCCESS (regenerated AI client
      with `getTaskEvents`/`TaskEvent(s)`, VOs with `partialAnswer`)
- [x] frontend: `tsc --noEmit` clean, `npm test` 44/44, `locale:check` in sync
- [ ] manual: echo task against a 0.8.0 provider (empty stream) — behavior
      unchanged; kill/restart Elza mid-task — polling resumes, no duplicate rows
- [ ] manual: verify the user-queue push reaches the browser through the
      production security config (session auth, reverse proxy)

## Notes / risks

- The events long poll doubles parked provider connections per running task —
  `AiEventPoller` gets its own small executor (4 threads, same sizing as the
  task poller). Both pools are a known scaling ceiling (max N concurrently
  polled requests); acceptable now, revisit with virtual threads on Java 21.
- Provider `detail` payload shapes for tool events are not final until S2
  lands; `query` extraction is deliberately defensive (common keys, else null —
  UI shows a generic step). Refine in E2.
- The push message envelope is hand-typed on the client (same practice as
  existing WS messages); the `request` field reuses the generated `AiRequest`
  TS type.
