import "@typespec/http";
import "@typespec/openapi";

using TypeSpec.Http;
using TypeSpec.OpenAPI;

/**
 * Elza AI Provider API.
 *
 * The protocol between Elza (the client) and an **AI provider** — a service that
 * executes AI tasks over archival description data. Elza is open-source software;
 * this protocol is vendor-neutral, and any party may implement it to offer AI
 * services to Elza installations. A provider gives each subscriber a base
 * **endpoint URL** (which may embed the provider's own account routing) plus
 * signing credentials; all paths below are relative to that endpoint.
 *
 * The provider is a gateway to actual AI models: it holds the model/vendor
 * credentials, selects and renders the prompt for each task type, calls the
 * model, enforces the client-supplied output schema, and meters usage for its
 * own accounting/billing.
 *
 * ## Interaction model
 *
 * Elza typically runs inside infrastructure that is not reachable from the
 * internet, so **every connection is client → provider**; the provider never
 * calls the client. The protocol is therefore an asynchronous task loop:
 *
 *   1. `POST /tasks` — submit a task (type, input, output schema, optional tools).
 *   2. `GET /tasks/{taskId}?wait=…` — long-poll for state, progress and result.
 *   3. If the task needs data only the client can provide, it enters state
 *      `awaiting_tools` and lists `toolCalls` drawn from the tools the client
 *      declared at submit time. The client executes them locally (under its own
 *      permissions) and returns the results via `POST /tasks/{taskId}/tool-results`;
 *      the loop continues. Only tool *results* ever leave the client.
 *   4. `POST /tasks/{taskId}/cancel` — the user pressed STOP; best-effort cancel.
 *
 * The provider stores the whole conversation (model messages, tool calls/results)
 * per task, so the protocol is **delta-only**: the client never re-sends
 * accumulated context, and a completed task can be continued — refined with a
 * follow-up — by submitting a new task with `parentTaskId`.
 *
 * ## Contract ownership
 *
 * The *client* owns the task contract: the `taskType`, the shape of `input`, and
 * the `outputSchema` the result must validate against (they change only with a
 * client release). The *provider* owns the prompt for each task type and may
 * improve it at any time; every result reports the `promptVersion` used. Prompt
 * wording can never break a client, because the provider guarantees the output
 * validates against the submitted schema.
 *
 * ## Evolution
 *
 * The contract grows **additively only**: new task types, new task states, new
 * optional fields. Clients must ignore unknown fields and treat unknown task
 * states as "still in progress, keep polling". `GET /info` reports the protocol
 * version and the capabilities of the provider.
 *
 * Authentication is request signing (`ELZA-AI-HMAC-SHA256`) with keys issued by
 * the provider, scoped either to the whole installation/organization or to a
 * single named user. Every operation is signed except the liveness probe
 * `GET /ping`. See `SignatureAuth` and the companion `security.md`.
 */
@service(#{ title: "Elza AI Provider API" })
@info(#{ version: "0.2.0" })
@server(
  "{endpoint}",
  "Provider-assigned base endpoint, e.g. https://ai.example.com/elza",
  {
    /** Base URL the provider assigned to the subscriber; may embed the provider's own account routing. */
    endpoint: url,
  }
)
@useAuth(SignatureAuth)
@tag("ElzaAi")
namespace ElzaAiProvider;

/**
 * Request signing with a shared secret (HMAC-SHA256); the secret is never sent.
 * See `security.md` for the authoritative definition, canonical string and test
 * vectors.
 *
 * Send two headers:
 *   - `X-AI-Date`: request creation time (UTC, ISO 8601); within ±2 min of server time.
 *   - `Authorization`: `ELZA-AI-HMAC-SHA256 KeyId=<keyId>,Signature=<base64>`
 *
 * `KeyId` and the matching secret are issued by the provider. The `KeyId` is
 * opaque to the client; the provider chooses its form and scope — a key for the
 * whole installation/organization (e.g. `ACME`), or a key scoped to one named
 * user (e.g. `ACME/petr`) enabling per-user attribution and quotas. A subscriber
 * may hold several keys for rotation; the server accepts a signature valid under
 * any active one.
 */
@doc("ELZA-AI-HMAC-SHA256 request signature carried in the `Authorization` header (the secret is never sent). See the API description and security.md for the canonical string and KeyId scoping.")
model SignatureAuth is ApiKeyAuth<ApiKeyLocation.header, "Authorization">;

// ---------------------------------------------------------------------------
// Common request parameters
// ---------------------------------------------------------------------------

/** Signing parameters common to every request. */
model SignedRequest {
  /**
   * Request creation time (UTC, ISO 8601 / RFC 3339); part of the signature,
   * within ±2 min of server time.
   */
  @encode(DateTimeKnownEncoding.rfc3339)
  @header("X-AI-Date")
  xAiDate: utcDateTime;
}

// ---------------------------------------------------------------------------
// Task model
// ---------------------------------------------------------------------------

/**
 * Task type — names the AI capability requested and selects the provider-side
 * prompt; also the provider's unit of metering/pricing. An **open** set of
 * string codes (a plain string on the wire, so code generators produce a
 * simple type): new types are added without a contract change, and a provider
 * advertises the types it supports via `GET /info`. Namespaced by client
 * product (`elza.…`). Defined types:
 *
 * - `elza.echo` — integration-test task: the model restates `input` according
 *   to the output schema. No archival semantics — exists so both sides can
 *   validate the full protocol loop (auth, submit, poll, schema-enforced
 *   output, usage metering) cheaply before any real scenario.
 * - `elza.revision` — advisory revision of archival description units: finds
 *   logical inconsistencies, dating mismatches, terminology problems and other
 *   issues that formal (rule-based) validation cannot catch. Read-only;
 *   findings are returned per node. The first production scenario.
 */
scalar TaskType extends string;

/**
 * State of a task. Terminal states are `done`, `error`, `cancelled`. The set may
 * grow additively; a client must treat an unknown state as "in progress" and
 * keep polling.
 */
enum TaskState {
  /** Accepted, waiting for a processing slot. */
  queued,

  /** The model is working. */
  running,

  /**
   * The task is paused waiting for the client: `toolCalls` lists what to
   * execute. The client answers with `POST …/tool-results`. A task that waits
   * longer than the provider's tool-result timeout (see `Limits`) fails with
   * error code `TOOL_TIMEOUT`.
   */
  awaiting_tools,

  /** Finished successfully; `output` is present and validates against the submitted schema. */
  done,

  /** Failed; `error` is present. Usage already incurred is still reported. */
  error,

  /** Cancelled on client request (or by the server, e.g. on timeout — see `error`). */
  cancelled,
}

/**
 * A tool the client offers for this task — something the *client* can execute
 * locally on the provider's request (a search over the client's index, a record
 * lookup, …). Mirrors the tool-definition shape of AI model APIs.
 */
model ToolDefinition {
  /** Tool name, unique within the task, e.g. `searchNodes`. */
  @pattern("^[A-Za-z][A-Za-z0-9_]*$")
  name: string;

  /** What the tool does and when to use it — written for the model. */
  description: string;

  /** JSON Schema of the tool's arguments object. */
  inputSchema: Record<unknown>;
}

/** A pending call of a client-declared tool (present while `awaiting_tools`). */
model ToolCall {
  /** Server-assigned id of this call; echo it back in the matching `ToolResult`. */
  callId: string;

  /** Name of the tool to execute (one of the task's declared tools). */
  tool: string;

  /** Arguments object; conforms to the tool's declared `inputSchema`. */
  arguments: unknown;
}

/** The client's answer to one `ToolCall`. Exactly one of `result` / `error` is present. */
model ToolResult {
  /** `callId` of the `ToolCall` this answers. */
  callId: string;

  /** Tool output (any JSON). */
  result?: unknown;

  /** Human-readable failure description when the tool could not be executed. */
  error?: string;
}

/**
 * Client-supplied context of a task. Not interpreted by the AI; used for audit,
 * support and the provider's reporting. Open for additive growth.
 */
model TaskMetadata {
  /** Identifier of the client instance the task originates from, e.g. `PROD`. */
  instanceName?: string;

  /** Originating application, e.g. `elza`. */
  app?: string;

  /** Application version, e.g. `3.5.0`. */
  appVersion?: string;

  /**
   * User on whose behalf the task runs (e.g. the Elza username), for audit and
   * attribution when the signing key is installation-wide. With a user-scoped
   * key this should match the key's user.
   */
  requestedBy?: string;

  /** Identifier of the archival fund concerned, if any (UUID). */
  fundUuid?: string;

  /** Additional client-specific context. */
  ...Record<unknown>;
}

/** Task submission. */
model SubmitTask {
  /**
   * Client-generated idempotency key (e.g. a UUID). Retrying a failed submit with
   * the same `requestId` never creates a second task: the server replies with the
   * existing task and `duplicate: true`.
   */
  requestId: string;

  /** The AI capability requested; selects the provider-side prompt and the price. */
  taskType: TaskType;

  /**
   * Quality/cost tier, e.g. `standard`. Available profiles are listed by
   * `GET /info`; omitted means the provider's default.
   */
  profile?: string;

  /**
   * Free-form instructions from the *user* (not the integrating application) —
   * e.g. the inquiry text, or how content should be reformatted. Composed by the
   * provider with its own prompt for the task type.
   */
  userInstructions?: string;

  /** Task-type-specific input payload (the data to work on). */
  input: unknown;

  /**
   * JSON Schema the `output` must validate against. Owned by the client (it is
   * what the client's code parses); the provider guarantees conformance of
   * `output`.
   */
  outputSchema: Record<unknown>;

  /**
   * Tools the client offers for this task. If present, the task may enter
   * `awaiting_tools` and request calls; if absent, the task runs on `input` alone.
   */
  tools?: ToolDefinition[];

  /**
   * Id of a finished task whose conversation this task continues (a follow-up /
   * refinement). The provider restores that task's context server-side; the
   * client does not re-send it. The parent must belong to the same subscriber
   * and be within the retention window.
   */
  parentTaskId?: string;

  /** Client-supplied context for audit and reporting. */
  metadata?: TaskMetadata;
}

/**
 * Progress signal for the user while the task runs. Best-effort and advisory —
 * a client must not depend on any particular granularity.
 */
model TaskProgress {
  /** Short human-readable status, suitable for direct display. */
  message?: string;

  /** Rough completion estimate, 0–100. */
  @minValue(0)
  @maxValue(100)
  percent?: float32;
}

/** Failure description of a task in state `error`. */
model TaskError {
  /**
   * Machine-readable code. Defined codes (the set may grow): `MODEL_ERROR`
   * (upstream model failure after retries), `SCHEMA_ERROR` (model could not
   * produce schema-valid output), `TOOL_TIMEOUT` (client did not return tool
   * results in time), `INPUT_TOO_LARGE`, `UNSUPPORTED_TASK_TYPE`, `INTERNAL`.
   */
  code: string;

  /** Human-readable message. */
  message: string;
}

/**
 * Metered usage of a task so far — running totals, present from the first model
 * call onward (also on failed tasks: incurred usage counts). `costUnits` is the
 * billable quantity: a model-independent unit defined by the provider's price
 * list, so model changes on the provider side never change the unit the
 * subscriber sees.
 */
model Usage {
  /** Model input tokens consumed (informative). */
  inputTokens: int64;

  /** Model output tokens produced (informative). */
  outputTokens: int64;

  /** Billable cost units per the provider's price list. */
  costUnits: float64;
}

/** A task and its current state — the poll response. */
model Task {
  /** Server-assigned task id (opaque). */
  taskId: string;

  /** The client's idempotency key from the submission. */
  requestId: string;

  /** Requested capability. */
  taskType: TaskType;

  /** Current state. Unknown values must be treated as "in progress". */
  state: TaskState;

  /** When the task was accepted. */
  createdAt: utcDateTime;

  /** When processing began (absent while `queued`). */
  startedAt?: utcDateTime;

  /** When the task reached a terminal state. */
  finishedAt?: utcDateTime;

  /** Advisory progress for user display. */
  progress?: TaskProgress;

  /**
   * Pending tool calls; present exactly while `awaiting_tools`. Answer all of
   * them in one `POST …/tool-results`.
   */
  toolCalls?: ToolCall[];

  /**
   * The result; present when `done`. Validates against the submitted
   * `outputSchema` — guaranteed by the server.
   */
  output?: unknown;

  /** Failure description; present when `error`. */
  error?: TaskError;

  /**
   * Version of the provider-side prompt used, e.g. `elza.revision/2026-07-02.1`.
   * Record it with the task's results: it is needed to reproduce, audit, and to
   * correlate result-quality feedback with prompt changes.
   */
  promptVersion?: string;

  /** Metered usage so far (running totals; final once the state is terminal). */
  usage?: Usage;
}

// ---------------------------------------------------------------------------
// Service info
// ---------------------------------------------------------------------------

/** Liveness probe response. */
model PingResponse {
  /** Always `ok` — a `200` with this body proves the endpoint is alive. */
  status: "ok";

  /**
   * Current server time (UTC). Compare with the local clock to diagnose
   * signature failures caused by clock skew — `X-AI-Date` must be within
   * ±2 minutes of this value for signed requests to be accepted.
   */
  serverTime: utcDateTime;
}

/** A task type supported by this provider. */
model TaskTypeInfo {
  /** The task type code, e.g. `elza.revision`. */
  code: string;

  /** Human-readable name. */
  name?: string;

  /** What the task does. */
  description?: string;
}

/** A quality/cost tier offered by this provider. */
model ProfileInfo {
  /** Profile code to pass in `SubmitTask.profile`, e.g. `standard`. */
  code: string;

  /** Human-readable name. */
  name?: string;

  /** What the tier offers (quality, speed, price class). */
  description?: string;

  /** True for the profile used when a submission names none. */
  default?: boolean;
}

/** Operational limits of this provider. Advisory for clients; enforced server-side. */
model Limits {
  /** Maximum size of a serialized `SubmitTask` body, in bytes. */
  maxInputBytes?: int64;

  /** Maximum size of a serialized tool-results body, in bytes. */
  maxToolResultBytes?: int64;

  /** Maximum accepted `wait` value on task polling, in seconds. */
  maxWaitSeconds?: int32;

  /** Maximum number of a subscriber's tasks in a non-terminal state at once. */
  maxOpenTasks?: int32;

  /** Seconds a task may spend in `awaiting_tools` before failing with `TOOL_TIMEOUT`. */
  toolResultTimeoutSeconds?: int32;

  /**
   * Days a finished task (and its result) stays retrievable. Conversation
   * payloads are deleted after this window; only usage/accounting metadata is
   * kept longer.
   */
  taskRetentionDays?: int32;
}

/**
 * Capabilities of this provider. Also serves as the "test connection" call:
 * a `200` proves reachability, authentication and an active subscription.
 */
model AiServiceInfo {
  /**
   * Protocol version, `major.minor`. Minor grows additively; a major bump (not
   * planned) would be a new base path.
   */
  protocolVersion: string;

  /** Human-readable name of the provider/service. */
  providerName?: string;

  /** Task types this provider supports. */
  taskTypes: TaskTypeInfo[];

  /** Quality/cost tiers this provider offers. */
  profiles: ProfileInfo[];

  /** Operational limits. */
  limits: Limits;

  /** Additional, additively-introduced capabilities. */
  ...Record<unknown>;
}

// ---------------------------------------------------------------------------
// Responses
// ---------------------------------------------------------------------------

/** The task was accepted (newly created, or recognised as an idempotent retry). */
model TaskAccepted {
  @statusCode statusCode: 202;

  /** Id of the task (new, or pre-existing on an idempotent retry). */
  taskId: string;

  /** Initial state (normally `queued`). */
  state: TaskState;

  /** True if this `requestId` was already seen for the subscriber (idempotent retry, no new task). */
  duplicate?: boolean;
}

/**
 * Standard error payload. Notable statuses: `402` — no active subscription or
 * quota exhausted (the body's `code` distinguishes, e.g. `NO_SUBSCRIPTION`,
 * `QUOTA_EXCEEDED`); `409` — the operation does not fit the task's current state
 * (e.g. tool results for a task not in `awaiting_tools`); `429` — rate limited,
 * retry later.
 */
@error
model ServiceError {
  @statusCode statusCode: 400 | 401 | 402 | 403 | 404 | 409 | 429 | 500;

  /** Machine-readable error code. */
  code: string;

  /** Human-readable message. */
  message: string;
}

// ---------------------------------------------------------------------------
// Operations
// ---------------------------------------------------------------------------

/**
 * Liveness probe — the only **unauthenticated** operation (requests to it are
 * not signed). Answers two questions the signed operations cannot separate:
 * is the endpoint URL right and the service up (vs. bad credentials), and
 * what is the server's clock (a client whose `X-AI-Date` falls outside the
 * ±2 min window can detect its own skew from `serverTime`). Returns no
 * provider information beyond liveness; for the full connection test
 * (auth + subscription) use `GET /info`.
 */
@route("/ping")
@get
@useAuth(NoAuth)
op ping(): PingResponse;

/**
 * Describe this provider: protocol version, supported task types, profiles,
 * limits. Doubles as the connection test (auth + subscription check).
 */
@route("/info")
@get
op getInfo(...SignedRequest): AiServiceInfo | ServiceError;

/** Submit a task. Idempotent on `requestId`. */
@route("/tasks")
@post
op submitTask(
  ...SignedRequest,
  @body task: SubmitTask,
): TaskAccepted | ServiceError;

/**
 * Read a task's current state (long poll). With `wait`, the server holds the
 * request up to that many seconds and responds as soon as the state changes —
 * the recommended way to drive the loop without tight polling.
 */
@route("/tasks/{taskId}")
@get
op getTask(
  ...SignedRequest,

  /** Task id from `TaskAccepted`. */
  @path taskId: string,

  /** Long-poll timeout in seconds; `0`/absent = answer immediately. Capped by `Limits.maxWaitSeconds`. */
  @minValue(0)
  @query wait?: int32,
): Task | ServiceError;

/**
 * Return the results of the pending tool calls of a task in `awaiting_tools`;
 * the task then continues (the response reflects the new state). Answer every
 * pending `callId` at once. Retry-safe: results for already-resolved calls are
 * ignored, so re-sending after a network failure is harmless.
 */
@route("/tasks/{taskId}/tool-results")
@post
op submitToolResults(
  ...SignedRequest,

  /** Task id from `TaskAccepted`. */
  @path taskId: string,

  @body body: {
    /** One result per pending tool call. */
    results: ToolResult[];
  },
): Task | ServiceError;

/**
 * Cancel a task — the user pressed STOP. Best-effort: a task already terminal
 * stays as it is (the response tells). Usage incurred before the cancel still
 * counts.
 */
@route("/tasks/{taskId}/cancel")
@post
op cancelTask(
  ...SignedRequest,

  /** Task id from `TaskAccepted`. */
  @path taskId: string,
): Task | ServiceError;
