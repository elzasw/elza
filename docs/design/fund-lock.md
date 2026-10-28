# Fund lock: edits versus long-running runs

**Proposal, not implemented yet** — none of the components below exists in the code.
Tracked as bug 9714.

## Why

A bulk action, an import or a persistent sort reads a large part of a fund and rewrites it based
on what it read. While it runs, nothing stops a user from editing the same fund: a description
item saved in the middle of a run is either overwritten, or makes the run fail late on the
optimistic version check of `ArrNode` after an hour of work. The only existing guard is
`AsyncRequestService.isFundBulkActionRunning`, consulted when a version is closed or a fund is
deleted. Ordinary edits are not checked at all.

The lock exists to keep edits and runs apart. It is not a replacement for the database's own
locking between two short edits (PostgreSQL row locks and the node version check already serve
that), and it is not an isolation mechanism for reads.

## Decisions

- **In memory, one JVM.** The lock is a Spring singleton keyed by fund version id. It is not
  stored in the database. A crash releases every lock for free; a database lock would need a
  lease table with owner, expiry and heartbeat because a run spans several transactions
  (`AsyncBulkActionWorker` opens one for the prepare step, one for the body, one for the finish).
  Instances sharing one database are not supported by ELZA anyway: the async queue, the level
  tree cache, the node and access point caches, the Lucene index and `DaCommunicationLock` are all
  per JVM. The lock is the smallest piece of that problem, so it is kept simple and its interface
  is kept narrow enough for a database-backed implementation to replace it later
  (see [Open work](#open-work)).
- **Two modes: shared and exclusive.** An ordinary edit holds the lock *shared* for one
  transaction. A run holds it *exclusive* for its whole duration, across its transactions.
  Shared holders never conflict with each other.
- **Reads are never locked.** The tree, forms and searches stay available during a run. A reader
  sees the committed intermediate state of the run, as today.
- **The mode follows the kind of work, not its duration.** Exclusive is for operations that read
  and rewrite much of the fund and whose result must be consistent as a whole; in practice these
  are the operations dispatched to a worker or queue. Everything served inside one request
  transaction is shared, even when it takes a while: the database makes it atomic, and a conflict
  on one node surfaces as a version conflict exactly as it does today. The way to make a request
  operation exclusive is to turn it into a run. Consequence: every exclusive holder is long, so
  the waiting rules below need no "short" or "long" attribute on the lock.
- **Every `ArrChange` belongs to one fund.** `arr_change` gets a `fund_id` column and
  `ArrangementInternalService.createChange` requires the fund version. The two places that
  create one change for several funds (replacing an access point or a party in description
  items) create one change per fund instead. Multi-fund bulk actions already have one run and one
  change per fund; `ArrFundsChange` only groups the runs.
- **Enforcement by two nets, not by discipline.** `createChange` refuses to create a change
  unless the current thread holds the fund's lock, and a Hibernate listener refuses to persist or
  update a fund-bound entity whose change was not issued under a held lock. A forgotten lock
  fails on the first write in any test instead of racing silently in production.

## Design

### The lock

`FundLockService` (name indicative) keeps one lock object per fund version id, created on first
use. A lock object is a small `synchronized` state machine, not a `ReentrantReadWriteLock`:

- `exclusiveOwner`: a description of the holder (kind of run, its id, user, start time), or none;
- `sharedCount`: number of transactions currently holding the lock shared;
- `exclusivePending`: an exclusive request is waiting for the shared holders to finish.

A custom class is used because `ReentrantReadWriteLock.tryLock` ignores fairness, so a stream of
edits could starve a waiting run, and because the holder description is needed for the error
message shown to the user.

Interface, narrow on purpose:

```
void   acquireShared(int fundVersionId)                       // bound to the current transaction
Handle acquireExclusive(int fundVersionId, Owner owner, Duration maxWait)
void   release(Handle handle)
```

`acquireShared` registers a `TransactionSynchronization` and releases in `afterCompletion`, so
the hold ends at commit or rollback, never at method exit. Outside a transaction it is a
programming error. `acquireExclusive` returns a handle that the caller releases in a `finally`.

Holds are recorded in a thread-local context: which fund versions the thread holds, in which
mode, and the ids of the changes created under those holds. The context makes acquisition
reentrant (a run that calls a service method annotated for shared access gets a no-op) and gives
the two enforcement nets something cheap to check.

### Waiting rules

| Requester | Lock state | Behaviour |
|---|---|---|
| shared (edit) | free, or only shared holders | granted |
| shared (edit) | exclusive held | fails at once with a business exception naming the run and its start time |
| shared (edit) | exclusive pending | fails at once; the run would take the lock within seconds and hold it for minutes |
| exclusive, queued run | shared holders | marks `exclusivePending`, waits without timeout, interruptible by terminate; the run reports "waiting for edits to finish" |
| exclusive, queued run | exclusive held | not dispatched: the executor starts a run only when the fund version has no exclusive holder, so waiting runs do not occupy worker threads |
| exclusive, request thread (fund deletion, CSV import) | shared holders | waits a bounded time, about 30 s, then fails |
| exclusive, request thread | exclusive held | fails at once |

An edit never waits. Waiting 100 ms or so for the lock to clear was considered and rejected:
an exclusive holder is long by construction, and a pending exclusive request becomes a long
holder as soon as the in-flight edits commit, so a short wait would only delay the same error.
The wait of a pending run is bounded by the longest transaction already in flight; the statement
timeouts in [db-statement-timeouts.md](db-statement-timeouts.md) cap that.

The client learns about running and pending runs from the existing `BULK_ACTION_STATE_CHANGE`
event and disables editing proactively; the server-side rejection is the safety net, not the
primary user experience.

### Lock and `ArrChange`

`createChange(ArrFundVersion version, ArrChange.Type type, ArrNode primaryNode)`:

1. asserts from the thread-local context that the thread holds the lock of `version` in either
   mode; otherwise throws;
2. stores `fund_id` on the change;
3. saves the change and adds its id to the context.

Requiring the fund version forces every caller to know which fund it writes. The lock handle is
deliberately not a parameter: at nearly every call site it would be fetched from the thread-local
context anyway, and threading it through service signatures would prove nothing that the runtime
check does not. A later refactor may fold the hold into a per-fund write context
(`MultipleItemChangeContext` and `ItemService.FundContext` are the seeds of one) and make
`createChange` a method of it.

`fund_id` is nullable so that the column can be added before the backfill; rows of existing
installations are backfilled by a Java migration that walks the referencing tables in batches.
Fund creation saves the fund before its first change, so the change can carry the fund id, and
takes the lock of the new version in the same transaction.

### Persist listener

A Hibernate `PreInsert`/`PreUpdate` listener covers every entity that references `ArrChange`
(`ArrItem`, `ArrLevel`, `ArrNodeExtension`, `ArrDaoLink`, `ArrStructuredObject`, `ArrOutput`,
`ArrNodeOutput`, `ArrOutputResult`, `ArrFile`, `ArrInhibitedItem`, `ArrLockedValue`,
`ArrRequest`, `ArrRequestQueueItem`, `ArrFundStructureExtension`, `ArrBulkActionRun`,
`ArrFundVersion`). It checks that the entity's create change, and on update its delete change,
was issued under a lock held by the current thread. The check is a lookup in the thread-local
set of change ids: no database access and no lazy loading inside a flush. It stays enabled in
production; the cost is negligible.

Together the two nets mean: no change without a lock, no write without a change issued under a
lock. A hand-built `ArrChange` cannot pass the listener.

### Entry points

Service methods take the lock through an annotation in the style of `@AuthMethod`, for example
`@FundLock(SHARED)` on a method whose fund version parameter is already marked
`@AuthParam(FUND_VERSION)`. The aspect acquires, registers the release, and records the hold in
the context. Workers acquire exclusive explicitly around the run. A test over the Spring context
asserts that every method with a fund write permission also declares the annotation.

Classification of the operations known today; the final list is settled during implementation
by the rule above:

| Operation | Mode | Note |
|---|---|---|
| description item, level, DAO link, output, structured object edits | shared | one request transaction |
| replace values from the table view, set specification, copy sibling attributes | shared | one request transaction; may be slow on a large fund, which is the status quo |
| approve version | shared | holding shared keeps a pending run from starting; a running one is rejected at once, replacing the hand-written check |
| access point / party replace in description items | shared, per fund | fails as a whole when any affected fund is locked; a partial replace would be harder to explain than a retry |
| bulk actions, persistent sort, multiple actions | exclusive, queued | the body is one transaction today, but the run reads and rewrites much of the fund |
| node import (`ImportProcess`), DA import, fund import into an existing fund | exclusive | |
| CSV item import | exclusive, request thread | commits chunk by chunk |
| fund deletion, history deletion | exclusive, request thread | |
| output generation | shared, final write only | the generation reads for a long time; holding shared throughout would make queued runs wait for it. An edit landing mid-generation is the status quo |
| node validation (`AsyncNodeWorker`) | none | writes only conformity rows and runs after every edit; locking it would serialize edits |
| requests and request queue | shared | taken by the request's entry point |

A run that touches several funds acquires them in ascending fund version id, or one fund at a
time, so that two such runs cannot deadlock.

## Alternatives not taken

- **Database lock now, or a pluggable lock mechanism.** Rejected for the reasons under
  Decisions; the narrow interface is the whole provision for the future.
- **Locking through `createChange` only.** `createChange` has about seventy-five fund-bound
  callers and no fund parameter; it is kept as the first net, but the listener gives the same
  guarantee without routing every write through a handle.
- **Locking reads.** Would make the UI unusable during a run for no gain in consistency that
  users would notice.
- **A short/long attribute on the lock and a short wait for edits.** Exclusive already implies
  long; see Waiting rules.
- **`ReentrantReadWriteLock`.** Fairness does not apply to `tryLock`, and the holder description
  is needed for messages.

## Open work

- Database-backed implementation for instances sharing one database, when ELZA supports that:
  edits take `SELECT … FOR SHARE` on the `arr_fund_version` row for their transaction; a run takes
  `FOR UPDATE` in a short transaction, inserts a lease row (owner instance, acquired, expiry,
  heartbeat), commits; edits check for a lease after their `FOR SHARE`. Same interface as the
  in-memory lock.
- Decide whether the async executor should also refuse to *queue* a run for a fund version whose
  lock is held by a request-thread exclusive holder, or only refuse to dispatch it.
- Decide the backfill strategy for `arr_change.fund_id` on very large installations (one Java
  migration in batches is the default; an option to skip and treat NULL as legacy stays open).
