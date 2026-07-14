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
 * The poll returns the task's state plus a single advisory `progress` line. A
 * client that wants to show finer live activity — the phases, tool calls and
 * (when the provider streams it) the answer as it is composed — may
 * additionally long-poll `GET /tasks/{taskId}/events`, a cursor-based,
 * replayable stream of execution events. It is **optional and advisory**:
 * authoritative state, result and usage always come from `GET /tasks/{taskId}`,
 * and a provider may emit no events at all.
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
@info(#{ version: "0.8.0" })
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
 * - `elza.archivalEntity` — a CAM access point / entity in full (payload
 *   `ArchivalEntity`): its identity and classification plus the whole tree of parts.
 * - `elza.archivalEntityInfo` — the lightweight identity of an entity (payload
 *   `ArchivalEntityInfo`): names, classification and external-system identity,
 *   without the parts.
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

  /** A CAM access point / entity in full; payload `ArchivalEntity`. */
  archivalEntity: "elza.archivalEntity",

  /** Lightweight identity of a CAM access point / entity; payload `ArchivalEntityInfo`. */
  archivalEntityInfo: "elza.archivalEntityInfo",

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
  /**
   * Fund id (`ArrFund.fundId`); echo-only — the provider never interprets it,
   * but tools can reference the fund by it, e.g. `SearchNodesParams.fundIds`.
   */
  fundId?: int32;

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
 * Shared identity and classification of a CAM access point / archival entity —
 * the fields common to a lightweight `ArchivalEntityInfo` reference and a full
 * `ArchivalEntity`. Spread into both, so **neither type is built from the other**;
 * both independently carry an entity's identity.
 *
 * Classification follows Elza's access-point type hierarchy (`ApType`): the
 * entity's own type is the leaf (the *subclass*, e.g. "fyzická osoba"); its class
 * is the root of that hierarchy (the read-only super-type, e.g. "osoba / bytost").
 * Names are the localized display strings — self-explanatory for a model to reason
 * over — while the codes give stable, language-independent identity.
 */
alias ArchivalEntityIdentity = {
  /**
   * Access point id (`ApAccessPoint.accessPointId`); echo-only — the provider
   * never interprets it, but tools and results can reference the entity by it.
   */
  accessPointId: int32;

  /** Stable access point UUID (`ApAccessPoint.uuid`), for durable cross-request identity. */
  uuid?: string;

  /**
   * Name of the entity class — the top of the type hierarchy, e.g.
   * "osoba / bytost" (the root, read-only `ApType.name`).
   */
  className?: string;

  /** Stable code of the entity class (root `ApType.code`), e.g. `PERSON`. */
  classCode?: string;

  /**
   * Name of the entity type (its subclass) — the access point's own type, e.g.
   * "fyzická osoba" or "bytost" (`ApType.name`).
   */
  typeName?: string;

  /** Stable code of the entity type (`ApType.code`), e.g. `PERSON_INDIVIDUAL`. */
  typeCode?: string;

  /**
   * Code of the external system the entity is bound to (`ApExternalSystem.code`),
   * when it originates from or is synchronized with one (e.g. a CAM instance);
   * absent for a purely local entity. An entity is bound to at most one system.
   */
  externalSystemCode?: string;

  /**
   * The entity's identifier within that external system (`ApBinding.value`);
   * present together with `externalSystemCode`.
   */
  externalId?: string;
};

/**
 * Lightweight reference to a CAM access point / archival entity — its identity,
 * classification and preferred name, without the full structured description.
 * Sent on its own as `elza.archivalEntityInfo`, and nested in a `RECORD_REF`
 * description item (`DescriptionItem.entity`) to name what a reference points at.
 * Having no parts, it carries the preferred name directly.
 */
model ArchivalEntityInfo {
  ...ArchivalEntityIdentity;

  /**
   * Preferred (display) name of the entity — the display-name index of its
   * preferred part (`ApIndex` of type `DISPLAY_NAME`).
   */
  preferredName?: string;
}

/** Lightweight archival entity object — a reference by identity, without the parts. */
model ArchivalEntityInfoObject extends AiObject {
  @invisible(Lifecycle)
  objectType: ObjectType.archivalEntityInfo;

  data: ArchivalEntityInfo;
}

/**
 * One part of an archival entity — a block of its structured description
 * (`ApPart`, e.g. a name or a body of description). A part has a type, a text
 * representation, its own description items and, recursively, nested sub-parts.
 */
model EntityPart {
  /**
   * Part id (`ApPart.partId`); echo-only — stable within the entity, so items and
   * sub-parts can be tied back to their part.
   */
  partId?: int32;

  /** Stable part-type code (`RulPartType.code`), e.g. `PT_NAME`, `PT_BODY`. */
  partType: string;

  /** Name of the part type (`RulPartType.name`) — its display label. */
  partTypeName?: string;

  /**
   * True for the entity's preferred part — the one whose text is the entity's
   * preferred (display) name (`ApAccessPoint.preferredPart`). Absent or false for
   * the others. Lets a reader identify the entity's name without a naming
   * convention, since `ArchivalEntity` carries no separate `preferredName`.
   */
  preferred?: boolean;

  /**
   * Text representation of the part — its display-name index (`ApIndex` of type
   * `DISPLAY_NAME`), the same text Elza renders for the part. This is how the
   * entity reads in text form. Absent when the part has no rendered display name.
   */
  value?: string;

  /**
   * The part's description items, in display order — the same `DescriptionItem`
   * shape used for a level's items (item codes resolved through the rule-set
   * dictionary, references carried as nested objects).
   */
  items?: DescriptionItem[];

  /** Nested sub-parts of this part (e.g. the name parts under a body part). */
  parts?: EntityPart[];
}

/**
 * A CAM access point / archival entity in full: its identity and classification
 * plus its complete structured description as a tree of parts. Sent as context
 * when the user is working on an entity. The name is not repeated at the top
 * level — it is the `value` of the part marked `preferred`.
 */
model ArchivalEntity {
  ...ArchivalEntityIdentity;

  /**
   * Rule set governing this entity's description, e.g. `ZP2015` (`RulRuleSet.code`)
   * — taken from the entity's scope (`ApScope.rulRuleSet`). Selects the dictionary
   * (the same `getItemTypes` tool that funds use) that resolves the item-type/spec
   * codes carried in the parts' `DescriptionItem`s. Optional: a scope may have no
   * rule set, in which case those codes render unresolved. Not on
   * `ArchivalEntityInfo`, which carries no items to resolve.
   */
  ruleSetCode?: string;

  /**
   * Top-level parts of the entity, in display order — the roots of the part tree;
   * each part's sub-parts nest under its own `parts`.
   */
  parts?: EntityPart[];
}

/** Archival entity object — the full entity with its parts. */
model ArchivalEntityObject extends AiObject {
  @invisible(Lifecycle)
  objectType: ObjectType.archivalEntity;

  data: ArchivalEntity;
}

/**
 * Data type of a description item's value — Elza's closed set of value kinds
 * (`RulItemType.dataType`). It tells a reader how to interpret an item without a
 * dictionary lookup: which reference fields are present and how the text `value`
 * is produced. Stable and closed; the set may grow additively, and a reader
 * treats an unknown type as plain text carried in `value`.
 */
enum DataType {
  /** Whole number; `value` holds its text form. */
  INT: "INT",

  /** Short string; `value` holds it. */
  STRING: "STRING",

  /** Long / multi-line text; `value` holds it. */
  TEXT: "TEXT",

  /** Formatted (rich) text; `value` holds the plain-text rendering. */
  FORMATTED_TEXT: "FORMATTED_TEXT",

  /** Archival dating; `value` holds the normalized dating text. */
  UNITDATE: "UNITDATE",

  /** Unit identifier (e.g. inventory number); `value` holds it. */
  UNITID: "UNITID",

  /** Decimal number; `value` holds its text form. */
  DECIMAL: "DECIMAL",

  /** Calendar date; `value` holds its text form. */
  DATE: "DATE",

  /** Boolean flag; `value` holds its text form. */
  BIT: "BIT",

  /**
   * Enumerated value — the meaning is carried by `spec`; `value` may hold the
   * spec's name.
   */
  ENUM: "ENUM",

  /**
   * Reference to an access point; `accessPointId` is set and `value` holds its
   * preferred name.
   */
  RECORD_REF: "RECORD_REF",

  /**
   * Reference to a structured object; `structuredObjectType` / `structuredObjectId`
   * are set (with optional `complement`) and `value` holds its serialized name.
   */
  STRUCTURED: "STRUCTURED",

  /** Reference to a file; `value` holds the file name when available. */
  FILE_REF: "FILE_REF",

  /** External URI reference; `value` holds the URI or its label. */
  URI_REF: "URI_REF",

  /** Geographic coordinates; `value` may be absent (not rendered as text). */
  COORDINATES: "COORDINATES",

  /** Structured JSON table; `value` may be absent (not rendered as text). */
  JSON_TABLE: "JSON_TABLE",
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
   * The value's data type — the same value the `getItemTypes` dictionary reports
   * for this item type (`ItemTypeInfo.dataType`), denormalized here so a block is
   * self-describing without the dictionary: it says how to read the value and which
   * reference field is present, e.g. `RECORD_REF` (→ `entity`),
   * `STRUCTURED` (→ `structuredObject`), `ENUM` (→ `spec`),
   * or a text/number/date type (→ `value`).
   */
  dataType?: DataType;

  /**
   * Stable specification code for enumerated item types, e.g.
   * `ZP2015_UNIT_TYPE_UKN` (`RulItemSpec.code`); absent for non-enumerated types.
   */
  spec?: string;

  /**
   * The item's value in text form for non-enumerated types; absent when the
   * meaning is fully carried by `spec`, or the item is value-less. For an
   * access-point or structured-object item it holds the human-readable value —
   * the entity's preferred name or the object's serialized name — while
   * `entity` / `structuredObject` below add the machine-readable reference.
   */
  value?: string;

  /**
   * For an access-point item (`RECORD_REF`): the referenced entity as a
   * lightweight `ArchivalEntityInfo` (its id, preferred name and classification).
   * `value` also holds the entity's preferred name. Absent for other item types.
   */
  entity?: ArchivalEntityInfo;

  /**
   * For a structured-object item (`STRUCTURED`, e.g. a storage unit or an
   * accessibility restriction): the referenced object as a `StructuredObjectInfo`.
   * `value` holds the object's serialized name. Absent for other item types.
   */
  structuredObject?: StructuredObjectInfo;
}

/**
 * Reference to a structured object (`ArrStructuredObject`) — a composite value
 * such as a storage unit or an accessibility restriction. Carried by a
 * `STRUCTURED` description item; the item's `value` holds the object's serialized
 * (human-readable) name, this model its machine-readable identity.
 */
model StructuredObjectInfo {
  /** Structured object id (`ArrStructuredObject.structuredObjectId`). */
  structuredObjectId: int32;

  /** Stable structured-type code (`RulStructuredType.code`). */
  structuredType?: string;

  /**
   * The object's optional complement (`ArrStructuredObject.complement`) — a
   * supplementary qualifier of the value; absent when the object has none.
   */
  complement?: string;
}

/**
 * Kind of a problem found by Elza's automatic (rule-based) checks on a level.
 * The set may grow additively; a client treats an unknown kind as a generic
 * problem.
 */
enum NodeIssueKind {
  /** A description item has an invalid value (an `ArrNodeConformityError`). */
  invalidValue: "invalid_value",

  /** A required description item is missing (an `ArrNodeConformityMissing`). */
  missing: "missing",
}

/**
 * One problem found by Elza's automatic checks on a level — the kind of formal
 * (rule-based) validation that runs continuously while a fund is arranged.
 * Advisory context for a task: it lets the AI see what formal validation already
 * flagged, complementing the logical/semantic issues an AI task looks for.
 */
model NodeIssue {
  /** Whether an item has an invalid value or a required item is missing. */
  kind: NodeIssueKind;

  /**
   * Human-readable description of the problem, as authored by the check. In the
   * rule set's language (data, not UI chrome — the client adds no wrapping here).
   */
  description?: string;

  /**
   * Code of the check (policy) that raised the problem (`RulPolicyType.code`);
   * absent when the check declares none.
   */
  policyType?: string;

  /**
   * Item-type code the problem relates to (`RulItemType.code`) — the type of the
   * invalid item, or of the missing required item. Lets the AI tie the problem to
   * an entry in `items`.
   */
  itemType?: string;

  /**
   * Specification code the problem relates to (`RulItemSpec.code`), when the
   * check is spec-specific; absent otherwise.
   */
  spec?: string;
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

  /**
   * Human-readable title of the level, exactly as shown in the arrangement tree
   * (built from the fund's configured title items) — a quick summary of what the
   * level is, before reading its individual items.
   */
  title?: string;

  /**
   * Problems found by Elza's automatic checks on this level (0..n); empty or
   * absent when the level is valid or has not been validated.
   */
  issues?: NodeIssue[];

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
 * Standard tools — the **closed, contract-owned** set of tools a client can
 * execute locally on the provider's request during `awaiting_tools`. The set is
 * versioned with the client: a new tool ships in a client release (its executor
 * is client code), so tools are *not* dynamically declared. This contract owns
 * each tool's **interface** — its name here plus the typed argument and result
 * models below; the client advertises which tools it supports in
 * `SubmitTask.tools`, and the **provider** owns the tool's model-facing
 * prompt/description and derives the model's argument schema from the argument
 * model. Defined tools:
 *
 * - `getItemTypes` — resolve the rule-set dictionary. Arguments
 *   `GetItemTypesParams`, result `ItemTypeDictionary`: the client sends
 *   `elza.archivalDescription` items as bare type/spec codes (compact,
 *   documentation-aligned) and the provider calls this tool once per
 *   `FundInfo.ruleSetCode` to turn those codes into names, data types and the
 *   enumerated spec lists, then caches it.
 * - `searchNodes` — search description units across the archive's funds.
 *   Arguments `SearchNodesParams`, result `SearchNodesResult`: the model asks
 *   for records the client did not send (full text and/or item conditions);
 *   the client executes the search under the requesting user's read
 *   permissions and returns a capped hit list grouped by fund.
 * - `getArchivalEntity` — fetch one archival entity (access point) in full.
 *   Arguments `GetArchivalEntityParams`, result `ArchivalEntity` (the same
 *   model the client sends as `elza.archivalEntity` context): the model asks
 *   for an entity the client did not send — typically one referenced from a
 *   description item (`RECORD_REF`) or from another entity's parts; the client
 *   resolves it under the requesting user's read permissions and returns its
 *   identity, classification, `ruleSetCode` and complete part tree.
 * - `searchEntities` — search archival entities (access points). Arguments
 *   `SearchEntitiesParams`, result `SearchEntitiesResult`: full text over
 *   names/description, optionally constrained to an entity class/type or to
 *   entities referencing a given entity (incoming relations); executed under
 *   the requesting user's scope read permissions, capped hit list of
 *   lightweight identities.
 * - `getArchivalDescription` — fetch one description level in full and/or its
 *   surroundings in the arrangement tree. Arguments
 *   `GetArchivalDescriptionParams`, result `ArchivalDescriptionDetail`: the
 *   level always comes with its complete description items (the round-trip
 *   dominates the cost); ordered, windowed lists of its children and siblings
 *   (designed for levels with thousands of nodes) and the parent chain are
 *   opt-in. Executed under the requesting user's fund read permissions.
 */
enum StandardToolName {
  getItemTypes,
  searchNodes,
  getArchivalEntity,
  searchEntities,
  getArchivalDescription,
}

/** A pending call of a client-declared tool (present while `awaiting_tools`). */
model ToolCall {
  /** Server-assigned id of this call; echo it back in the matching `ToolResult`. */
  callId: string;

  /** The tool to execute — one the client declared in `SubmitTask.tools`. */
  tool: StandardToolName;

  /** Arguments object; conforms to the tool's argument model (e.g. `GetItemTypesParams`). */
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
// Standard tool payloads — the typed argument/result models named by
// `StandardToolName` (see its doc for the tool catalog). Carried inside the
// generic `ToolCall.arguments` / `ToolResult.result` envelope.
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
  dataType: DataType;

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
 * Arguments of the `searchNodes` tool. Deliberately flat (not Elza's internal
 * filter tree): a full-text query and/or ANDed item conditions, optionally
 * scoped to funds. Extensions grow additively.
 */
model SearchNodesParams {
  /**
   * Full-text query matched against the whole description unit (all item
   * values). At least one of `fulltext` / `itemConditions` must be given.
   */
  fulltext?: string;

  /**
   * Conditions on individual description items; all must hold (AND). Item
   * types are named by their stable codes — the same vocabulary as
   * `DescriptionItem.type` and the `getItemTypes` dictionary.
   */
  itemConditions?: ItemCondition[];

  /**
   * Restrict the search to these funds (`FundInfo.fundId`). Omitted = every
   * fund the requesting user may read.
   */
  fundIds?: int32[];

  /**
   * Only description units with an item referencing this entity
   * (`ApAccessPoint.accessPointId`), regardless of the item type — the
   * description-side usage of an entity. For a type-specific reference match
   * use an `ItemCondition` on the `RECORD_REF` item type with the id as
   * `value` instead (a numeric value on a reference item matches by id).
   */
  referencesEntityId?: int32;

  /**
   * Requested maximum number of hits in total; the client may cap it lower
   * (the result then reports `partial`). Omitted = the client's default.
   */
  limit?: int32;
}

/**
 * One condition on a description item, e.g. item `ZP2015_UNIT_DATE`
 * `INTERSECT` `1930-1939`.
 */
model ItemCondition {
  /** Item-type code (`RulItemType.code`), e.g. `ZP2015_TITLE`. */
  type: string;

  /**
   * Specification code (`RulItemSpec.code`), when the condition targets one
   * specification of an enumerated item type.
   */
  spec?: string;

  /** How the item's value is compared with `value`. */
  operation: ItemConditionOperation;

  /**
   * The comparison value; absent for `NOT_NULL`. Dating items (`UNITDATE`)
   * use Elza's interval syntax, e.g. `1930-1939`.
   */
  value?: string;
}

/**
 * Comparison operators of `ItemCondition` — a subset of Elza's search
 * operations; may grow additively. `INTERSECT` applies to dating items
 * (`UNITDATE`): true when the item's interval overlaps the queried one.
 */
enum ItemConditionOperation {
  EQ: "EQ",
  CONTAINS: "CONTAINS",
  GT: "GT",
  GTE: "GTE",
  LT: "LT",
  LTE: "LTE",
  INTERSECT: "INTERSECT",
  NOT_NULL: "NOT_NULL",
}

/**
 * Result of the `searchNodes` tool. Hits are grouped by fund with the first
 * hits of each inlined, so the distribution (which funds, how many) is visible
 * even when the hit lists are capped.
 */
model SearchNodesResult {
  /** Funds with hits, each carrying its first hits in tree order. */
  funds: FundHits[];

  /** Total matching description units across all funds (before capping). */
  totalCount: int64;

  /**
   * True when the result is incomplete — a hit cap, a per-fund cap or the
   * search time limit was reached. The tool does not page: refine the query
   * (sharper terms, `fundIds` scoping) instead of re-asking for more.
   */
  partial: boolean;
}

/** The hits within one fund. */
model FundHits {
  /** Fund id (matches `FundInfo.fundId`). */
  fundId: int32;

  /** Fund name (`ArrFund.name`). */
  name: string;

  /** Holding institution name, when known. */
  institution?: string;

  /** Total hits in this fund; may exceed `nodes.length` when capped. */
  count: int32;

  /** The first hits, in arrangement-tree order. */
  nodes: NodeHit[];
}

/** One matching description unit (level). */
model NodeHit {
  /** Level id (`ArrNode.nodeId`) — the same identity `elza.archivalDescription` uses. */
  nodeId: int32;

  /** Stable level UUID (`ArrNode.uuid`). */
  uuid?: string;

  /** Tree title of the level, as in `ArchivalDescription.title`. */
  title?: string;

  /**
   * Reference designation from the fund root, as in
   * `ArchivalDescription.referenceMark`; its length also tells the hit's depth
   * in the arrangement tree.
   */
  referenceMark?: string[];
}

/**
 * Arguments of the `getArchivalEntity` tool. Exactly one of the identifiers is
 * given; the result is the referenced entity as a full `ArchivalEntity` —
 * identity, classification, `ruleSetCode` and the complete part tree, the same
 * payload the client sends as `elza.archivalEntity` context. An entity the
 * requesting user may not read (or that does not exist) is answered with
 * `ToolResult.error`.
 */
model GetArchivalEntityParams {
  /** Access point id (`ApAccessPoint.accessPointId`) of the entity to fetch. */
  accessPointId?: int32;

  /** Stable access point UUID (`ApAccessPoint.uuid`) — durable across installations. */
  uuid?: string;
}

/**
 * Arguments of the `searchEntities` tool. At least one of `fulltext`,
 * `typeCode`, `relatedTo` must be given (an unconstrained listing is refused).
 */
model SearchEntitiesParams {
  /** Full-text query over entity names/description (`"…"` = exact phrase). */
  fulltext?: string;

  /**
   * Where the full text matches: the preferred name only, every name part
   * (the default), or the whole description.
   */
  area?: EntitySearchArea;

  /**
   * Restrict the full text to the main part of names (`NM_MAIN`) — e.g. a
   * surname-only match for persons.
   */
  onlyMainPart?: boolean;

  /**
   * Restrict to one entity class or type (`ApType.code`, e.g. `PERSON` or
   * `PERSON_INDIVIDUAL`); a non-leaf code includes all its subtypes.
   */
  typeCode?: string;

  /**
   * Only entities whose description references a given entity — the incoming
   * relations of that entity (relations are stored one-way: son→father lives
   * on the son, so "find X's children" = entities whose relation items point
   * at X).
   */
  relatedTo?: EntityRelationFilter;

  /**
   * Requested maximum number of hits; the client may cap it lower (the result
   * then reports `partial`). Omitted = the client's default.
   */
  limit?: int32;
}

/** Scope of the `searchEntities` full-text match. */
enum EntitySearchArea {
  /** The preferred (display) name only. */
  preferNames,

  /** Every name part (the default). */
  allNames,

  /** The whole structured description — all parts and items. */
  allParts,
}

/** "Has a reference to entity X", optionally narrowed to one relation item type/spec. */
model EntityRelationFilter {
  /** The referenced entity (`ApAccessPoint.accessPointId`). */
  accessPointId: int32;

  /**
   * Relation item type code (`RulItemType.code`) the reference must use;
   * omitted = any reference anywhere in the description.
   */
  relationTypeCode?: string;

  /** Specification code, when the relation type is enumerated (e.g. the kind of kinship). */
  relationSpecCode?: string;
}

/** Result of the `searchEntities` tool. */
model SearchEntitiesResult {
  /** The matching entities as lightweight identities, best match first. */
  entities: ArchivalEntityInfo[];

  /** Total matching entities (before capping). */
  totalCount: int64;

  /**
   * True when the result is incomplete (a hit cap was reached). The tool does
   * not page: refine the query (sharper terms, `typeCode`, `onlyMainPart`)
   * instead of re-asking with the same terms.
   */
  partial: boolean;
}

/** Arguments of the `getArchivalDescription` tool. Exactly one of the ids. */
model GetArchivalDescriptionParams {
  /** Level id (`ArrNode.nodeId`) — the same identity `elza.archivalDescription` and `NodeHit` use. */
  nodeId?: int32;

  /** Stable level UUID (`ArrNode.uuid`). */
  uuid?: string;

  /** Include a window of the direct child levels, in arrangement order (default false). */
  withChildren?: boolean;

  /** 0-based offset of the children window (default 0 = the first children). */
  childrenFrom?: int32;

  /**
   * Include a window of the sibling levels (same parent), in arrangement order
   * (default false). Without `siblingsFrom` the window is centered on the
   * level itself — its neighborhood; the result reports the level's own index.
   */
  withSiblings?: boolean;

  /** 0-based offset of the siblings window; omitted = centered on the level. */
  siblingsFrom?: int32;

  /** Include the parent chain, root first (default false). */
  withParents?: boolean;

  /** Requested maximum nodes per window (children / siblings); the client may cap it lower. */
  limit?: int32;
}

/**
 * Result of the `getArchivalDescription` tool: the level — always with its
 * complete description items (the round-trip dominates the cost; the provider
 * decides how much reaches the model) — plus the requested surroundings in the
 * arrangement tree.
 */
model ArchivalDescriptionDetail {
  /** The level itself, the same model the client sends as context, items included. */
  node: ArchivalDescription;

  /** Fund the level belongs to — scopes follow-up searches (`SearchNodesParams.fundIds`). */
  fundId: int32;

  /**
   * Rule set of the fund (`RulRuleSet.code`) — resolves the items' type/spec
   * codes, the same way `ArchivalEntity.ruleSetCode` does for a fetched entity.
   */
  ruleSetCode?: string;

  /** Parent chain, root first (when `withParents`; never windowed — depth is small). */
  parents?: NodeHit[];

  /** Sibling window (when `withSiblings`). */
  siblings?: NodeWindow;

  /** Children window (when `withChildren`). */
  children?: NodeWindow;
}

/**
 * One window of an ordered node list. Self-describing — the slice, where it
 * sits and how long the whole list is — because arrangement lists can be huge
 * (thousands of units under one series) and, unlike search results, ordered
 * lists page by design: the caller aims the next window instead of refining.
 */
model NodeWindow {
  /** The window's nodes, in arrangement order. */
  nodes: NodeHit[];

  /** 0-based index of `nodes[0]` within the whole ordered list. */
  from: int32;

  /** Size of the whole ordered list. */
  totalCount: int32;

  /**
   * Siblings only: the anchor level's own 0-based index within the whole list
   * — its position among its siblings, and the aim point for a next window.
   */
  nodeIndex?: int32;
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
   * Standard tools the client offers for this task, by name (see
   * `StandardToolName`). If present, the task may enter `awaiting_tools` and
   * request calls; if absent, the task runs on `parameters` (and `context`)
   * alone. The client sends names only — the provider knows each tool's argument
   * schema and result shape from the contract.
   */
  tools?: StandardToolName[];

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
// Task events (optional live activity stream)
// ---------------------------------------------------------------------------

/**
 * Task event type — names one observable step in a task's execution. An **open**
 * set of string codes (a plain string on the wire): the provider may introduce
 * new event types without a contract change, and a client **ignores an event
 * whose type it does not recognize** (just as it ignores an unknown object
 * type). Whether any events are emitted at all — and at what granularity — is up
 * to the provider; the stream is advisory, the point-in-time counterpart being
 * `TaskProgress` on a `Task`. The vocabulary a provider may emit:
 *
 * Lifecycle (coarse; mirror the task's state changes):
 * - `submitted` — the task was accepted.
 * - `started` — processing began (`queued` → `running`).
 * - `phase` — an advisory progress phase: `message` is display-ready and
 *   `percent` a rough estimate (the streamed form of `TaskProgress`).
 * - `awaiting_tools` — the task suspended for client tool calls; `detail` lists
 *   the pending calls (`{callId, tool}`). The calls to execute still come from
 *   `Task.toolCalls`.
 * - `tools_resolved` — the client's tool results arrived and the task resumed.
 * - `done` — the task finished successfully (its `output` is now readable via
 *   `GET /tasks/{taskId}`).
 * - `error` — the task failed; `detail` carries the error `{code}`.
 * - `cancelled` — the task was cancelled.
 *
 * Turn internals (fine-grained; a provider may omit them entirely):
 * - `preparation` — a preparatory step before the model turn (e.g. resolving
 *   rule-set dictionaries), including any client tool round it drives.
 * - `model_round` — one model request/response completed.
 * - `tool_call` — the model requested a tool: `tool` names it, `detail` may
 *   summarize the arguments. Covers both provider-internal tools and the client
 *   tools of `StandardToolName`.
 * - `tool_result` — a tool the provider ran internally produced a result
 *   (a summary only — internal tool results never leave the provider).
 * - `answer_delta` — a slice of the answer text as it is composed, in
 *   `message`; batched, never per-token.
 *
 * A client drives the task with `GET /tasks/{taskId}` regardless; consuming this
 * vocabulary only enriches what it can display.
 */
scalar TaskEventType extends string;

/**
 * One observable event in a task's execution — the streamed, append-only
 * counterpart to the point-in-time `TaskProgress` on a `Task`. Advisory (a
 * provider need emit none) and additive (an unknown `type` is ignored); an event
 * never carries authoritative state — a task's state, output and usage always
 * come from `GET /tasks/{taskId}`.
 */
model TaskEvent {
  /**
   * Monotonic per-task sequence number, strictly increasing in emission order.
   * The resume cursor: pass the last value seen as `since` to receive only later
   * events. Stable — an event keeps its `seq` across reads.
   */
  seq: int64;

  /** What happened; a `TaskEventType` code (an unrecognized one is ignored). */
  type: TaskEventType;

  /** When the event was recorded (UTC). */
  createdAt: utcDateTime;

  /**
   * Display-ready one-line message, when the event has one — e.g. a `phase`
   * label or the text fragment of an `answer_delta`. In the task's language,
   * suitable for direct display.
   */
  message?: string;

  /**
   * The tool a `tool_call` / `tool_result` event concerns — the provider's
   * model-facing tool name. May be a client tool (a `StandardToolName` value) or
   * a provider-internal tool the protocol otherwise never exposes; treat it as
   * an opaque label. Absent on non-tool events.
   */
  tool?: string;

  /** Rough completion estimate for a `phase` event, 0–100; absent otherwise. */
  @minValue(0)
  @maxValue(100)
  percent?: float32;

  /**
   * Curated, event-specific detail as free JSON — e.g. the pending
   * `{callId, tool}` list of an `awaiting_tools` event, an error `{code}`, or a
   * short summary of a tool call's arguments. Never a full tool result or any
   * knowledge-corpus content. Absent when the event needs none; a client may
   * ignore it entirely.
   */
  detail?: unknown;
}

/**
 * A batch of task events past a cursor — the `GET /tasks/{taskId}/events`
 * response. Always safe to consume: `events` may be empty (a long poll that
 * timed out with nothing new), and the current `state` is included so a client
 * can stop polling once the task is terminal without a separate `GET /tasks`
 * call.
 */
model TaskEvents {
  /**
   * Events with `seq` greater than the requested `since`, in ascending `seq`
   * order. Empty when nothing new occurred within the wait.
   */
  events: TaskEvent[];

  /**
   * The task's current state — lets the client end its polling loop on a
   * terminal state (`done` / `error` / `cancelled`). Authoritative state,
   * output and usage still come from `GET /tasks/{taskId}`.
   */
  state: TaskState;

  /**
   * Cursor to pass as `since` on the next call: the greatest `seq` in `events`,
   * or the requested `since` unchanged when `events` is empty. Monotonic, so a
   * client always resumes from here without missing or repeating an event.
   */
  nextSince: int64;
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
 * Stream a task's execution events (long poll) — an **optional** finer-grained
 * companion to `GET /tasks/{taskId}`. Where the poll returns the task's current
 * state and a single advisory `progress`, this returns the append-only sequence
 * of what the task has been *doing* — phases, tool calls, and (when the provider
 * streams it) the answer as it is composed — so a client can show live activity
 * instead of one changing status line.
 *
 * Cursor-based and replayable: pass the `seq` of the last event seen as `since`
 * (omit or `0` for the whole history), then use `TaskEvents.nextSince` for the
 * next call. With `wait`, the server holds the request until new events exist or
 * the timeout elapses, then returns whatever accrued (possibly none).
 *
 * Consuming this stream is never required: a client drives the task with
 * `GET /tasks/{taskId}` as usual, and a provider may emit no events at all (an
 * empty stream is conformant). Events are advisory — authoritative state,
 * output, usage, and the `toolCalls` to execute all come from
 * `GET /tasks/{taskId}`.
 */
@route("/tasks/{taskId}/events")
@get
op getTaskEvents(
  ...SignedRequest,

  /** Task id from `TaskAccepted`. */
  @path taskId: string,

  /**
   * Return only events with `seq` greater than this cursor; omit or `0` for the
   * full history. Use `TaskEvents.nextSince` from the previous call.
   */
  @minValue(0)
  @query since?: int64,

  /**
   * Long-poll timeout in seconds; `0`/absent = answer immediately with whatever
   * events already accrued. Capped by `Limits.maxWaitSeconds` (shared with
   * `GET /tasks/{taskId}`).
   */
  @minValue(0)
  @query wait?: int32,
): TaskEvents | ServiceError;

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
