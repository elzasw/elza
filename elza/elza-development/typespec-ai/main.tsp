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
 * model, enforces the result type's shape, and meters usage for its own
 * accounting/billing.
 *
 * ## Interaction model
 *
 * Elza typically runs inside infrastructure that is not reachable from the
 * internet, so **every connection is client → provider**; the provider never
 * calls the client. The protocol is therefore an asynchronous task loop:
 *
 *   1. `POST /tasks` — submit a task (type, typed parameters, optional tools
 *      and context).
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
 * Three things are owned separately. **This contract** owns the typed object
 * shapes: every `ObjectType` — used for task parameters, context and results —
 * has its payload defined here, so client and provider generate the same models
 * and cannot drift; the vocabulary grows additively, only with a release. The
 * **provider** owns the task catalog — which `taskType`s exist and are currently
 * available, the parameters each declares (`GET /info`), and the prompt for each,
 * which it may improve at any time (every result reports the `promptVersion`
 * used). The **client** owns marshalling its domain data into these typed
 * objects, choosing which task to run, and supplying the parameters the task
 * declares. Prompt wording can never break a client, because the provider
 * guarantees each result block conforms to the shape defined for its object type.
 *
 * ## Evolution
 *
 * The contract grows **additively only**: new task types, new object types, new
 * task states, new optional fields. Clients must ignore unknown fields, unknown
 * object types, and treat unknown task states as "still in progress, keep
 * polling". `GET /info` reports the protocol version and the capabilities of the
 * provider.
 *
 * Authentication is request signing (`ELZA-AI-HMAC-SHA256`) with keys issued by
 * the provider, scoped either to the whole installation/organization or to a
 * single named user. Every operation is signed except the liveness probe
 * `GET /ping`. See `SignatureAuth` and the companion `security.md`.
 */
@service(#{ title: "Elza AI Provider API" })
@info(#{ version: "0.5.0" })
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
// Typed objects
// ---------------------------------------------------------------------------

/**
 * Object type — names a typed payload defined by this contract. A **closed**,
 * namespaced set of string codes modelled as an `enum`, so code generators
 * produce a real enumerated type and `AiObject` a clean discriminated union —
 * straightforward to emit as OpenAPI and to process on the Java side. The set
 * grows additively with a client release, never dynamically. Each code has a
 * payload shape defined below, so client and provider generate the *same* model
 * from this spec and their representations cannot drift. It is used three ways:
 * the discriminator of `AiObject`, a parameter's declared `type` in the task
 * catalog (`TaskParameterInfo`), and the result block types a task declares
 * (`TaskTypeInfo.resultTypes`). Defined codes:
 *
 * - `elza.text` — free text (payload `TextPayload`); the result of `elza.echo`.
 * - `elza.markdown` — formatted text (CommonMark + GFM, no raw HTML), a common
 *   result block the client renders; payload `MarkdownPayload`.
 * - `elza.fundInfo` — basic descriptive information about an archival fund and
 *   its holding institution (payload `FundInfo`); typically supplied as
 *   `context`.
 * - `elza.archivalEntity` — a CAM access point / entity. Payload fields are
 *   introduced as real tasks need them.
 * - `elza.archivalDescription` — a description unit (node) and its related data.
 *   Payload fields are introduced as real tasks need them.
 * - `elza.revisionFindings` — result of `elza.revision`. Payload fields are
 *   introduced as the task's result shape is finalized.
 * - `elza.docCitations` — the knowledge/documentation sources an answer relied on
 *   (payload `DocCitationsPayload`): each cited section as a stable citation id and,
 *   when the provider can resolve one, a full web URL. Returned alongside the text
 *   answer of an assistant-style task. Distinct from citing an archival record,
 *   which will get its own object type.
 */
enum ObjectType {
  /** Free text; payload `TextPayload`. */
  text: "elza.text",

  /** Formatted text (CommonMark + GFM, no raw HTML); payload `MarkdownPayload`. */
  markdown: "elza.markdown",

  /** Basic descriptive information about a fund and its institution; payload `FundInfo`. */
  fundInfo: "elza.fundInfo",

  /** A CAM access point / entity; payload `ArchivalEntity`. */
  archivalEntity: "elza.archivalEntity",

  /** A description unit (node) and its related data; payload `ArchivalDescription`. */
  archivalDescription: "elza.archivalDescription",

  /** Result of `elza.revision`; payload `RevisionFindings`. */
  revisionFindings: "elza.revisionFindings",

  /** The knowledge/documentation sources an answer relied on; payload `DocCitationsPayload`. */
  docCitations: "elza.docCitations",
}

/**
 * A typed object: an `objectType` tag plus the `data` payload for that type. The
 * polymorphic carrier for every typed value on the wire — task parameters,
 * supplementary context, and results. New concrete types are added as subtypes
 * below; a client ignores an object whose `objectType` it does not implement.
 */
@discriminator("objectType")
model AiObject {
  /** Selects the payload shape; one of the defined `ObjectType` codes. */
  objectType: ObjectType;
}

/** Payload of `elza.text`. */
model TextPayload {
  /** The text. */
  text: string;
}

/** Free text object. */
model TextObject extends AiObject {
  @invisible(Lifecycle)
  objectType: ObjectType.text;

  data: TextPayload;
}

/** Payload of `elza.markdown`. */
model MarkdownPayload {
  /** Formatted text as CommonMark + GFM (no raw HTML); the client renders it. */
  markdown: string;
}

/** Markdown text object — a formatted text block the client renders. */
model MarkdownObject extends AiObject {
  @invisible(Lifecycle)
  objectType: ObjectType.markdown;

  data: MarkdownPayload;
}

/** Holding archival institution of a fund. */
model InstitutionInfo {
  /** Institution code (`ParInstitution.internalCode`). */
  code?: string;

  /** Institution name — the preferred name of its access point. */
  name?: string;
}

/**
 * Basic descriptive information about an archival fund and its holding
 * institution — enough for a task to know which fund it operates within, without
 * the full arrangement. Mirrors `ArrFund`.
 */
model FundInfo {
  /** Fund name (`ArrFund.name`). */
  name: string;

  /** Internal (evidence) code of the fund, if set. */
  internalCode?: string;

  /**
   * Rule set the fund uses, e.g. `ZP2015` (`RulRuleSet.code`) — selects the
   * dictionary that resolves the item-type/spec codes used in
   * `elza.archivalDescription`.
   */
  ruleSetCode?: string;

  /** Fund number, if set. */
  fundNumber?: int32;

  /** Fund mark / signature, if set. */
  mark?: string;

  /** Dating of the fund (free text), if set. */
  unitDate?: string;

  /** Holding institution. */
  institution?: InstitutionInfo;
}

/** Fund information object. */
model FundInfoObject extends AiObject {
  @invisible(Lifecycle)
  objectType: ObjectType.fundInfo;

  data: FundInfo;
}

/**
 * A CAM access point / entity. Payload fields are added as real tasks need them;
 * open for additive growth.
 */
model ArchivalEntity {
  ...Record<unknown>;
}

/** Archival entity object. */
model ArchivalEntityObject extends AiObject {
  @invisible(Lifecycle)
  objectType: ObjectType.archivalEntity;

  data: ArchivalEntity;
}

/**
 * One description item (attribute) of a level, as stable codes plus its value.
 * The human-readable names of `type`/`spec` are resolved by the provider from
 * the rule-set dictionary (keyed by `FundInfo.ruleSetCode`), not repeated here —
 * the codes are the canonical, documented identifiers.
 */
model DescriptionItem {
  /** Stable item-type code, e.g. `ZP2015_TITLE` (`RulItemType.code`). */
  type: string;

  /**
   * Stable specification code for enumerated item types, e.g.
   * `ZP2015_UNIT_TYPE_UKN` (`RulItemSpec.code`); absent for non-enumerated types.
   */
  spec?: string;

  /**
   * The item's value in text form for non-enumerated types; absent when the
   * meaning is fully carried by `spec`, or the item is value-less.
   */
  value?: string;
}

/**
 * A description unit (level / node): its identity, its place in the arrangement
 * and its description items as codes. Self-contained given the accompanying
 * `elza.fundInfo`, whose `ruleSetCode` resolves the item codes.
 */
model ArchivalDescription {
  /**
   * Client-local level id (`ArrNode.nodeId`); echo-only — the provider returns
   * it (e.g. in findings), never interprets it.
   */
  nodeId: int32;

  /** Stable level UUID (`ArrNode.uuid`), for durable cross-request identity. */
  uuid?: string;

  /** Parent level id (`ArrNode.nodeId`); echo-only, absent at a fund root. */
  parentId?: int32;

  /**
   * Reference designation from the fund root, e.g. `["1", "2", "3"]` — the
   * archival "signatura" path.
   */
  referenceMark?: string[];

  /** Depth in the arrangement tree; a fund root is depth 0. */
  depth?: int32;

  /** Whether the level has child levels (for reasoning about aggregation). */
  hasChildren?: boolean;

  /**
   * True for the level the user is on (the "active" level); absent or false for
   * supporting levels (ancestors) sent only to give hierarchy. Lets a task tell
   * the focus level from its context when several are sent together.
   */
  focus?: boolean;

  /** The level's description items, in display order. */
  items: DescriptionItem[];
}

/** Archival description object. */
model ArchivalDescriptionObject extends AiObject {
  @invisible(Lifecycle)
  objectType: ObjectType.archivalDescription;

  data: ArchivalDescription;
}

/**
 * Result of `elza.revision`. Payload fields are added as the task's result shape
 * is finalized; open for additive growth.
 */
model RevisionFindings {
  ...Record<unknown>;
}

/** Revision findings object. */
model RevisionFindingsObject extends AiObject {
  @invisible(Lifecycle)
  objectType: ObjectType.revisionFindings;

  data: RevisionFindings;
}

/**
 * One document citation: a knowledge/documentation section an answer relied on,
 * carrying a resolvable web URL when the provider knows one. Distinct from a
 * citation of an archival record (a future object type).
 */
model DocCitation {
  /**
   * Stable corpus citation id of the cited section (e.g.
   * `zp2015/04-prvky-popisu/ukladaci-jednotka`) — the provider's internal address
   * for the section, unique within a corpus version. Always present, even when no
   * public URL is known.
   */
  fragment: string;

  /**
   * Full web URL of the cited section in the published source document, when the
   * provider can resolve one: a deep link to the section where the source exposes
   * an anchor, otherwise the document page. Absent when the source has no published
   * home or the section could not be located.
   */
  url?: string;

  /** Human-readable title of the cited section. */
  title?: string;

  /** Human-readable name of the source document the section belongs to. */
  source?: string;

  /**
   * Id prefix identifying the source document (the part of `fragment` before the
   * first `/`, e.g. `zp2015`) — lets a client group citations by source.
   */
  sourceId?: string;
}

/** Payload of `elza.docCitations`. */
model DocCitationsPayload {
  /** The cited sections, in the order first referenced by the answer. */
  citations: DocCitation[];
}

/** Document-citations object — the sources an answer relied on. */
model DocCitationsObject extends AiObject {
  @invisible(Lifecycle)
  objectType: ObjectType.docCitations;

  data: DocCitationsPayload;
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
 * - `elza.echo` — integration-test task: echoes the submitted `userInstructions`
 *   back as an `elza.text` result; declares no parameters. No archival semantics —
 *   exists so both sides can validate the full protocol loop (auth, submit, poll,
 *   typed output, usage metering) cheaply before any real scenario.
 * - `elza.revision` — advisory revision of archival description units: finds
 *   logical inconsistencies, dating mismatches, terminology problems and other
 *   issues that formal (rule-based) validation cannot catch. Read-only;
 *   findings are returned as `elza.revisionFindings`. The first production
 *   scenario.
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

  /** Finished successfully; `output` is present and conforms to the task's `resultType`. */
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

// ---------------------------------------------------------------------------
// Standard tools
//
// Tools this contract defines by name, with a typed argument and result shape,
// so the client and provider generate the *same* models instead of hand-rolling
// JSON against a prose description. On the wire they use the generic
// ToolDefinition / ToolCall / ToolResult envelope: a client that offers a
// standard tool declares it under its well-known `name` with the matching
// `…Params` model as `inputSchema`; the provider calls it and receives the
// matching result model as `ToolResult.result`. Defined standard tools:
//
//   - getItemTypes — resolve the rule-set dictionary. Arguments
//     `GetItemTypesParams`, result `ItemTypeDictionary`. The client sends
//     `elza.archivalDescription` items as bare type/spec codes (compact,
//     documentation-aligned); the provider calls this tool once per
//     `FundInfo.ruleSetCode` to turn those codes into names, data types and the
//     enumerated spec lists, then caches it.
// ---------------------------------------------------------------------------

/** Arguments of the `getItemTypes` tool. */
model GetItemTypesParams {
  /** Rule set whose item types to return (`RulRuleSet.code`), e.g. `ZP2015`. */
  ruleSetCode: string;
}

/** One specification (enumerated value) of an item type in the dictionary. */
model ItemTypeSpecInfo {
  /** Stable spec code, e.g. `ZP2015_UNIT_TYPE_UKN` (`RulItemSpec.code`). */
  code: string;

  /** Human-readable name in the rule set's language, e.g. `úřední kniha`. */
  name: string;
}

/** One item type in the dictionary — the meaning behind a `DescriptionItem.type` code. */
model ItemTypeInfo {
  /** Stable item-type code, e.g. `ZP2015_TITLE` (`RulItemType.code`). */
  code: string;

  /** Human-readable name, e.g. `Obsah`. */
  name: string;

  /** Underlying value type, e.g. `STRING`, `UNITDATE`, `ENUM` (`RulItemType.dataType`). */
  dataType: string;

  /**
   * Specifications for enumerated item types (`dataType == ENUM`), in display
   * order; absent for non-enumerated types.
   */
  specs?: ItemTypeSpecInfo[];
}

/** Result of the `getItemTypes` tool — the item-type dictionary of one rule set. */
model ItemTypeDictionary {
  /** Rule set the dictionary is for; echoes `GetItemTypesParams.ruleSetCode`. */
  ruleSetCode: string;

  /** The rule set's item types, resolving the codes used in `elza.archivalDescription`. */
  itemTypes: ItemTypeInfo[];
}

/**
 * Audit/attribution facts about a task — who ran it and from which application.
 * Not interpreted by the AI and not task data (the data travels in `parameters`
 * and `context`); used only for the provider's support and usage reporting.
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

  /**
   * The task's declared parameters, keyed by the parameter `name` the provider
   * advertises in `TaskTypeInfo.parameters`. Each value is an `AiObject` whose
   * `objectType` matches the declared parameter `type`. Every parameter the task
   * marks `required` must be present.
   */
  parameters: Record<AiObject>;

  /**
   * Supplementary context — an open list of typed objects that may improve the
   * result (e.g. the archival units the user recently worked with, or the
   * `elza.fundInfo` of the fund in scope). Not part of the task's declared
   * parameters; the provider uses what it understands and ignores the rest.
   */
  context?: AiObject[];

  /**
   * Tools the client offers for this task. If present, the task may enter
   * `awaiting_tools` and request calls; if absent, the task runs on `parameters`
   * (and `context`) alone.
   */
  tools?: ToolDefinition[];

  /**
   * Id of a finished task whose conversation this task continues (a follow-up /
   * refinement). The provider restores that task's context server-side; the
   * client does not re-send it. The parent must belong to the same subscriber
   * and be within the retention window.
   */
  parentTaskId?: string;

  /** Audit/attribution facts about the task; not task data (see `parameters` and `context`). */
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
   * produce output conforming to the task's declared result types), `TOOL_TIMEOUT` (client did
   * not return tool results in time), `INPUT_TOO_LARGE`, `UNSUPPORTED_TASK_TYPE`,
   * `INTERNAL`.
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

  /**
   * Credits charged to the caller's account for this task, when the provider
   * bills in credits: `costUnits × account multiplier` (a shared/organization
   * account may burn credits faster than a personal one). Absent when the
   * provider does not use credit accounting.
   */
  chargedCredits?: float64;
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
   * The result; present when `done`. An ordered list of typed blocks — the task
   * type fixes which block types may appear (`TaskTypeInfo.resultTypes`), not the
   * caller. Each block's `data` conforms to its `objectType`'s payload shape,
   * guaranteed by the server.
   */
  output?: AiObject[];

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
// Usage / credit state
// ---------------------------------------------------------------------------

/**
 * Credit state of the account the signing key selects — the caller's personal
 * account (user-scoped key) or the organization's shared account
 * (installation-wide key). All values are provider-computed; the client only
 * displays them (typically as relative consumption:
 * `spentCredits / allowanceCredits`).
 */
model AccountUsageInfo {
  /** Kind of account, e.g. `personal` or `shared` (open set). */
  accountType?: string;

  /** Plan label, e.g. `basic`; informational. */
  plan?: string;

  /** Credit allowance for the current period; absent = no per-account cap. */
  allowanceCredits?: float64;

  /** Credits charged to this account in the current period. */
  spentCredits: float64;

  /** Start of the current accounting period (UTC). */
  periodStart: utcDateTime;

  /** End of the current accounting period — when the allowance resets. */
  periodEnd?: utcDateTime;
}

/** Subscriber-level (whole organization) usage state — the outer budget cap. */
model CustomerUsageInfo {
  /** Subscriber budget for the current period; absent = no cap. */
  budgetCredits?: float64;

  /** Credits spent by the whole subscriber in the current period. */
  spentCredits: float64;

  /** Start of the current accounting period (UTC). */
  periodStart: utcDateTime;
}

/**
 * The caller's usage/credit balance — the data behind "how much have I spent".
 * Served from the same (cached) state the provider's submit-time budget check
 * uses, so the displayed balance never disagrees with a `402` refusal. Both
 * members are optional: a provider without credit accounting may return an
 * empty object (a `200` still proves auth + subscription).
 */
model UsageInfo {
  /** Credit state of the account the signing key selects. */
  account?: AccountUsageInfo;

  /** Subscriber-level usage state (the outer cap). */
  customer?: CustomerUsageInfo;

  /** Additional, additively-introduced usage facts. */
  ...Record<unknown>;
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

/**
 * A typed parameter a task type declares in the catalog. Names one of the task's
 * inputs and the `ObjectType` its value must have; the client supplies it under
 * this `name` in `SubmitTask.parameters`.
 */
model TaskParameterInfo {
  /** Parameter name, unique within the task (the key under `SubmitTask.parameters`). */
  @pattern("^[A-Za-z][A-Za-z0-9_]*$")
  name: string;

  /** The parameter's object type (an `ObjectType` code). */
  type: ObjectType;

  /** Human-readable description for the client UI. */
  description?: string;

  /**
   * Whether the caller must supply it. A task cannot be invoked unless all its
   * required parameters are present.
   */
  required?: boolean;
}

/** A task type supported by this provider. */
model TaskTypeInfo {
  /** The task type code, e.g. `elza.revision`. */
  code: string;

  /** Human-readable name. */
  name?: string;

  /** What the task does. */
  description?: string;

  /**
   * The typed parameters this task declares. A client can offer the task only if
   * its build implements every parameter `type` (and every `resultTypes` entry),
   * and only when it can supply the parameters the task marks `required`.
   */
  parameters?: TaskParameterInfo[];

  /**
   * The object types the task's result may contain — an ordered set of output
   * blocks (e.g. a text summary plus a structured findings object). Fixed by the
   * task, not chosen by the caller; the client must be able to handle each type.
   */
  resultTypes?: ObjectType[];
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
 * `QUOTA_EXCEEDED` — the subscriber's budget, `ACCOUNT_QUOTA_EXCEEDED` — the
 * signing key's account allowance; the set may grow, treat an unknown code
 * generically per status); `409` — the operation does not fit the task's
 * current state (e.g. tool results for a task not in `awaiting_tools`);
 * `429` — rate limited, retry later.
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
 * Describe this provider: protocol version, supported task types (with their
 * declared parameters and result types), profiles, limits. Doubles as the
 * connection test (auth + subscription check).
 */
@route("/info")
@get
op getInfo(
  ...SignedRequest,

  /**
   * Preferred language(s) for the human-readable labels in the response
   * (task-type and profile names/descriptions), as a standard BCP-47
   * `Accept-Language` value — e.g. `cs` or `cs-CZ, en;q=0.8`. Advisory: the
   * provider returns the best match it has, falling back to its default
   * language; it never affects machine-readable fields (codes, object types).
   */
  @header("Accept-Language") acceptLanguage?: string,
): AiServiceInfo | ServiceError;

/**
 * The caller's usage/credit balance — how much of the account's allowance is
 * spent this period, plus the subscriber-level state. For user display; see
 * `UsageInfo`.
 */
@route("/usage")
@get
op getUsage(...SignedRequest): UsageInfo | ServiceError;

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
