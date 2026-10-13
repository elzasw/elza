# AI task events in Elza

How Elza consumes the AI provider's task-event stream
(`GET /tasks/{taskId}/events`, protocol 0.8+; provider-side design:
`doc/task-events-proposal.md` in elza-ai-provider.git) and pushes request
updates to the AI panel.

## Goal

Make the AI panel responsive and show what a running task is *doing*
(phases, tool calls, the answer as it is composed), and keep a durable
per-request audit trail of the provider's internal work.

## Architecture

Two independent legs, joined by the database and a per-user WebSocket push:

```
provider GET /tasks/{id}        provider GET /tasks/{id}/events
   (authoritative poll)            (advisory event stream)
        │                                 │
  AiRequestPoller                   AiEventPoller
   state machine:                    persist events → ai_request_event
   output, usage, tools,             cursor → ai_request.event_seq
   terminal states                   phase → progress columns
        │                            answer_delta → AiAnswerBuffer (memory)
        └───────────┬────────────────────┘
                    ▼
      AiRequestPushService: commit, then render as the owner
      AiRequestViewMapper: AiRequest row + events + buffer → AiRequest VO
                    ▼
      UserEventPushService → /topic/user/{userId}
      {eventType: AI_REQUEST_UPDATE, conversationId, request}
                    ▼
      useAiConversation replaces the request (by id) in the open conversation
```

`AiConversationService` starts the event poller when a request is submitted
to the provider; `AiEventPoller.resumeOpenRequests` restarts it for all open
requests on application start.

## Decisions

1. **The task poll is authoritative.** `AiRequestPoller` drives the request's
   state machine (output, usage, tool calls, terminal states). The event
   stream is advisory by contract — a provider may emit no events at all — so
   nothing depends on it. A provider whose `GET /info` reports a protocol below
   0.8 is not polled for events; HTTP 404/405 from the events endpoint stops
   event polling for that request; other errors log a WARN and retry after
   15 s.

2. **Per-user push to a plain topic.** The server pushes to a generic per-user
   topic `/topic/user/{userId}` (`UserEventPushService`), not an AI-specific
   one: any user-targeted event can use it, discriminated by `eventType`
   exactly like the shared `/topic/api/changes` broadcast. Bootstrap system
   users without a persisted id share the topic `/topic/user/admin`.
   `UserTopicSubscriptionInterceptor` drops a SUBSCRIBE to another user's
   topic, so the payload may carry private content.

   *Why not a Spring user destination (`/user/queue/...`).* With
   `convertAndSendToUser` the outbound message is translated to the session
   queue, but in this application's customized STOMP channel stack
   (per-session executor decorators on the inbound/outbound channels) the
   client's translated user-destination subscription is never registered with
   the simple broker, so messages go to a destination with no subscriber.
   Plain `/topic/**` subscriptions register and deliver reliably.

3. **Whole snapshots, not deltas.** The message carries the complete
   render-ready `AiRequest` VO (state, progress, activities, `partialAnswer`,
   blocks when done). The simple broker is fire-and-forget and the client
   disconnects hidden tabs; idempotent snapshots make a lost message harmless —
   the next push corrects it, and the client refetches the open conversation
   whenever the WebSocket (re)connects. No new REST endpoint: the snapshot on
   panel open or reconnect uses the existing conversation detail GET.

4. **Commit first, render after, render as the owner**
   (`AiRequestPushService`). The snapshot is built in its own transaction from
   the committed row. Rendering inside the storing transaction would tie a
   received result (or the event cursor) to a derived view: a mapper failure
   would roll it back and the same data would be re-read forever. Block mappers
   run real permission checks and poller threads carry no security context, so
   rendering runs as the conversation owner.

5. **Events are persisted into `ai_request_event`** (except `answer_delta`).
   `event_type` = the wire code (lowercase, visually distinct from Elza's own
   UPPERCASE codes in the same open set), `data` = the wire `TaskEvent` JSON,
   `create_date` = local receive time (stable ordering; display timestamps come
   from the JSON `createdAt`). The cursor `ai_request.event_seq` advances with
   the batch in the same transaction, so a restart resumes without duplicate
   rows (the provider stream is replayable); events with `seq` ≤ cursor are
   skipped defensively.

6. **The request row is loaded under a row lock** when a batch is applied.
   Both pollers save the same `ai_request` row, and the provider releases both
   long polls at the moment the task finishes. Hibernate writes every column,
   so a stale copy saved by the event poller would overwrite the stored result
   with `running`, and the reverse race would regress the cursor. Under the
   lock the later writer reads the earlier commit; `phase` events do not touch
   the progress columns of a request that is already terminal.

7. **`answer_delta` is not persisted.** It is appended to an in-memory
   per-request buffer (`AiAnswerBuffer`) and exposed as
   `AiRequest.partialAnswer` while the request runs. It is lost on restart:
   the text is provisional by contract and its durable form is the request's
   output. The task poll clears the buffer when the request ends.

8. **Client-tool deduplication.** The provider also emits `tool_call` events
   for *client* tools, but Elza's own `TOOL_CALLS`/`TOOL_RESULTS` records are
   richer (full arguments → query, results → navigable links).
   `AiActivityMapper` skips provider tool events for tools registered as client
   tools in `AiToolRegistry`; provider-internal tools (`search_knowledge`, …)
   are mapped from the provider events, with a best-effort query extracted from
   the free-form `detail` (keys `query`, `fulltext`, `q`; otherwise none and the
   UI shows a generic step).

9. **What is shown.** `model_round` and lifecycle events are persisted for
   audit but not shown as activities; they are visible in the transparency log
   (`GET /aiprovider/request/{id}/event`). `phase` events update
   `progress_message`/`progress_percent` (and are persisted). The activity kind
   vocabulary adds `PREPARATION`.

## Notes and limits

- The events long poll (30 s window) doubles the parked provider connections
  per running task. `AiEventPoller` has its own executor of 4 threads, the
  same size as the task poller's. Both pools cap the number of concurrently
  polled requests; revisit (e.g. virtual threads) if that becomes a limit.
- The push message envelope is typed by hand on the client (same practice as
  the other WebSocket messages); its `request` field uses the generated
  `AiRequest` TypeScript type.

## Open work

- **Validate against a provider that emits real events** (provider stage S2):
  check the event shapes end to end, refine the `query` extraction from tool
  `detail`, polish activity labels/icons, pretty-print the transparency dialog.
- **Render `partialAnswer`** (provider stage S3): the server fills
  `AiRequest.partialAnswer`, but elza-react does not display it yet; show it as
  a provisional answer bubble in the AI panel while the request runs.
- Manual check: restart Elza in the middle of a task — event polling resumes
  and no duplicate `ai_request_event` rows appear.
