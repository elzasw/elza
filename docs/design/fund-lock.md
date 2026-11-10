# Fund lock: edits versus long-running runs

**Proposal, not implemented yet** — none of the components below exists in the code.
Tracked as bug 9714.

## Why

A bulk action, an import or a persistent sort reads a large part of a fund and rewrites it based
on what it read. While it runs, nothing stops a user from editing the same fund: a description
item saved in the middle of a run is either overwritten, or makes the run fail late on the
optimistic version check of `ArrNode` after an hour of work.

Today the only guards are these:

- The BULK queue starts at most `elza.asyncActions.bulk.maxPerFund` requests of one fund version
  at a time (default 1). Two bulk actions of one fund are kept apart by this setting, not by an
  invariant: raising it lets them overlap. A second action is queued without any indication of
  what it waits for.
- Each executor counts only its own requests. A bulk action and a DA import item
  (`BATCH_IMPORT`) or an AIP step (`AIP`) writing into the same fund run side by side; the latter
  two have no per-fund limit at all, because their fund is known only while they run.
- `AsyncRequestService.isFundBulkActionRunning` is consulted when a version is closed or a fund
  is deleted.

Ordinary edits and the bulk changes of the table view (`replaceDataValues`, `placeDataValues`,
`setSpecification`, `deleteDescItemsByType`) are plain request transactions with no check: two
bulk changes, or a bulk change and a running bulk action, run on one fund at the same time.

A run is not one transaction either. `BulkActionTransactional` (fund validation, unit id
generation, multiple actions, persistent sort) runs its body in one transaction, so an edit
landing meanwhile makes it fail on the node version check at the end or is overwritten.
`BulkActionDFS` (serial number, unit id sealing, item move, content migration) commits in
batches of levels, so an edit can land between two batches and the outcome depends on timing.

The lock exists to keep edits and runs apart, and to let at most one run or bulk change work on
a fund at a time. It is not a replacement for the database's own locking between two short
edits (PostgreSQL row locks and the node version check already serve that), and it is not an
isolation mechanism for reads.

## Decisions

- **In memory, one JVM.** The lock is a Spring singleton keyed by fund version id. It is not
  stored in the database. A crash releases every lock for free; a database lock would need a
  lease table with owner, expiry and heartbeat because a run spans several transactions
  (`AsyncBulkActionWorker` opens one for the prepare step, one or more for the body, one for the
  finish). Instances sharing one database are not supported by ELZA anyway: the async queue, the
  level tree cache, the node and access point caches, the Lucene index and `DaCommunicationLock`
  are all per JVM. The lock is the smallest piece of that problem, so it is kept simple and its
  interface is kept narrow enough for a database-backed implementation to replace it later
  (see [Open work](#open-work)).
- **Two modes: shared and exclusive, plus a reservation.** An ordinary edit holds the lock
  *shared* for one transaction. A run or a bulk change holds it *exclusive*: a run for its whole
  duration across its transactions, a bulk change of the table view for its request
  transaction. Shared holders never conflict with each other. A queued run *reserves* the lock
  when it is enqueued and turns the reservation into an exclusive hold when it is dispatched.
- **Reads are never locked.** The tree, forms and searches stay available during a run. A reader
  sees the committed intermediate state of the run, as today.
- **Busy means refuse.** No user waits for another user's run or bulk change, and no run is
  queued behind another run of the same fund. When the fund is taken, the request fails at once
  with a message naming the holder: kind of operation, user, start time. The only wait is for
  edits already in flight, which are single short transactions. Fast feedback and a clear reason
  are preferred over doing the work later.
- **The mode follows the kind of work.** Exclusive is for operations that read or rewrite much
  of the fund and whose result must be consistent as a whole: queued runs, the bulk changes of
  the table view, CSV item import, fund deletion. Everything else that writes within one
  transaction is shared: ordinary edits, but also a DA import item and an AIP step, which add a
  subtree or a few DAO links and need no consistency of the whole fund.
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

- `exclusiveOwner`: the holder of the exclusive lock, or none;
- `reservation`: a queued run that will take the lock when dispatched, or none. It blocks other
  exclusive requests and other reservations, not shared ones;
- `sharedOwners`: the transactions currently holding the lock shared;
- `exclusivePending`: an exclusive request is waiting for the shared holders to finish.

Every holder carries an `Owner`: kind of operation (bulk action code, "table bulk change",
"DA import, batch 123", "edit"), its id where there is one, user and start time. The refusal
message is built from it, for shared holders too, so that a bulk change refused because a long
DA import item is still writing says so.

A custom class is used because `ReentrantReadWriteLock.tryLock` ignores fairness, so a stream of
edits could starve a waiting run, and because the holder description is needed for the error
message shown to the user.

Interface, narrow on purpose:

```
void   acquireShared(int fundVersionId, Owner owner)          // bound to the current transaction
Handle reserve(int fundVersionId, Owner owner)                // at enqueue, fails if taken
void   activate(Handle reservation)                           // at dispatch, waits for in-flight edits
Handle acquireExclusive(int fundVersionId, Owner owner)       // request thread
void   release(Handle handle)
```

`acquireShared` registers a `TransactionSynchronization` and releases in `afterCompletion`, so
the hold ends at commit or rollback, never at method exit. Outside a transaction it is a
programming error. A nested `REQUIRES_NEW` transaction registers its own hold; its completion
does not release the hold of the enclosing transaction. `reserve` and `acquireExclusive` return
a handle that the caller releases in a `finally`.

Holds are recorded in a thread-local context: which fund versions the thread holds, in which
mode, and the ids of the changes created under those holds. The context makes acquisition
reentrant (a run that calls a service method annotated for shared access gets a no-op) and gives
the two enforcement nets something cheap to check. The context does not follow work handed to
other threads; a run must not write from a thread pool of its own.

### Waiting rules

| Requester | Lock state | Behaviour |
|---|---|---|
| shared (edit, DA import item, AIP step) | free, reserved, or only shared holders | granted |
| shared | exclusive held or pending | fails at once, naming the holder |
| run, at enqueue | free or only shared holders | reserved; queued |
| run, at enqueue | reserved or exclusive held | fails at once, naming the holder |
| run, at dispatch | shared holders | marks `exclusivePending`, waits for them without timeout, interruptible by terminate; the run reports "waiting for edits to finish" |
| exclusive, request thread (table bulk change, CSV import, fund deletion) | reserved or exclusive held | fails at once, naming the holder |
| exclusive, request thread | shared holders | marks `exclusivePending`, waits about 2 s, then fails naming the shared holders |

An edit never waits. Waiting 100 ms or so for the lock to clear was considered and rejected:
a run holds the lock for minutes, and a pending exclusive request becomes a holder as soon as
the in-flight edits commit, so a short wait would only delay the same error.

The 2 s wait of a request-thread exclusive covers only edits already in flight; failing on them
at once would make a bulk change fail whenever someone happens to be saving a field. The wait of
a dispatched run is bounded by the longest transaction already in flight; the statement
timeouts in [db-statement-timeouts.md](db-statement-timeouts.md) cap that.

The refusal is a business exception with its own code, carrying the holder's kind, user and
start time as properties, so the client shows it in Czech without parsing a message.

The client learns about taken funds from an event (name indicative: `FUND_LOCK_CHANGED`)
published whenever a lock of a fund version is reserved, taken, activated or released, carrying
the holder. The existing `BULK_ACTION_STATE_CHANGE` is not enough: imports, bulk changes of the
table view and fund deletion do not publish it. The client disables editing and shows which
operation runs; the server-side rejection is the safety net, not the primary user experience.
A form whose save is refused keeps the value the user entered.

### Executors

Runs bound to one fund version at enqueue (BULK: bulk actions, persistent sort, multiple
actions, each fund of a multi-fund action) reserve the lock in
`BulkActionService.queueInternal`, inside the enqueuing transaction. A failed reservation rolls
the run back and the user gets the refusal. The reservation is released when the run finishes,
fails or is terminated, and when the request is removed from the queue (`terminateFund`, fund
deletion). While a run only waits for a worker thread, edits of the fund go on; they are
refused from the moment the run is dispatched.

With the reservation, `elza.asyncActions.bulk.maxPerFund` no longer decides whether two bulk
actions of one fund overlap. It has no effect above 1 and can be removed.

DA import items and AIP steps learn their fund only while processing, so they cannot reserve.
Each takes the lock shared, with an owner naming the batch or the AIP action, for the
transaction in which it writes into the fund:

- A DA import item is one transaction (`ImpBatchService.runBatch`). An item going into a new
  fund (`ALWAYS_NEW`) cannot meet a holder. A dry run does not write and takes no lock.
- An item or step that finds its fund taken ends in `ERROR` with the refusal as its reason. A
  taken fund does not stop the batch, whatever its `skipError`: the batch goes on with the next
  item, and running the batch again picks up the items in `ERROR`.
- An AIP that belongs to no fund writes into no fund and takes no lock.

Node validation (`NODE`), output generation (`OUTPUT`) and export (`EXPORT`) do not reserve; see
the classification below.

### Restart

Queued requests are reloaded from `arr_async_request` at startup in their original order and
reserve their fund again. When two requests of one fund survive from before the lock existed,
the later one is failed with the same message as at enqueue.

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

A reservation is not a hold: `BulkActionService.queueInternal` creates the change of the run
under a shared hold of the enqueuing request, next to the reservation.

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
the context. Workers reserve, activate and acquire explicitly. A test over the Spring context
asserts that every method with a fund write permission also declares the annotation; edits
arriving over STOMP reach the same service methods and are covered by it.

Classification of the operations known today; the final list is settled during implementation
by the rule above:

| Operation | Mode | Note |
|---|---|---|
| description item, level, DAO link, output, structured object edits | shared | one request transaction |
| copy sibling attributes | shared | one request transaction on one node |
| replace values from the table view, place values, set specification, delete items by type | exclusive, request thread | one request transaction; two bulk changes of one fund never overlap, and none runs during a run |
| approve version | shared | holding shared keeps a dispatched run from activating; a running one is rejected at once, replacing the hand-written check |
| access point / party replace in description items | shared, per fund | fails as a whole when any affected fund is taken; a partial replace would be harder to explain than a retry |
| bulk actions, persistent sort, multiple actions | exclusive, reserved at enqueue | a second run of the fund is refused, not queued |
| multi-fund bulk action | exclusive per fund, reserved at enqueue | a fund that is taken is reported in `skipped`; the others are queued |
| node import (`ImportProcess`), fund import into an existing fund from the client | exclusive, request thread | see [Open work](#open-work) |
| DA import item | shared, per item | a taken fund puts the item into `ERROR`, the batch goes on |
| AIP step writing DAO links | shared, per step | as a DA import item |
| CSV item import | exclusive, request thread | commits chunk by chunk |
| fund deletion, history deletion | exclusive, request thread | also removes the fund's queued requests and their reservations |
| output generation | shared, final write only | the generation reads for a long time; holding shared throughout would make dispatched runs wait for it. An edit landing mid-generation is the status quo |
| node validation (`AsyncNodeWorker`) | none | writes only conformity rows and runs after every edit; locking it would serialize edits |
| requests and request queue | shared | taken by the request's entry point |

A multi-fund run reserves each fund on its own and never waits for one, so runs cannot deadlock.
An operation holding several funds exclusively from one request thread (fund deletion of
several funds, if ever) acquires them in ascending fund version id.

## Alternatives not taken

- **Database lock now, or a pluggable lock mechanism.** Rejected for the reasons under
  Decisions; the narrow interface is the whole provision for the future.
- **Locking through `createChange` only.** `createChange` has about seventy-five fund-bound
  callers and no fund parameter; it is kept as the first net, but the listener gives the same
  guarantee without routing every write through a handle.
- **Locking reads.** Would make the UI unusable during a run for no gain in consistency that
  users would notice.
- **Queueing a second run of a taken fund.** The user who queued it would not learn why it does
  not start nor when it will; a refusal naming the holder is clearer, and the user can queue
  again later.
- **Checking the lock only at dispatch.** Two runs queued at nearly the same time would both pass
  the check at enqueue, and the second would wait in the queue after all. The reservation makes
  the refusal happen when the user acts.
- **Making edits wait during a reservation.** A run may wait minutes for a worker thread while
  other funds are processed; refusing edits for that time would block users for nothing.
- **Exclusive lock for DA import items and AIP steps.** An item would be refused by a mere
  reservation, and edits of the fund would be refused for as long as a large item writes, with
  no gain: the item needs no consistency of the whole fund.
- **Retrying DA import items on a taken fund.** Running the batch again already picks up items in
  `ERROR`; an automatic retry would need a new mechanism for the same result.
- **A long wait for request-thread exclusives.** A bulk change waiting tens of seconds for
  another user's work contradicts "busy means refuse"; only edits in flight are waited for.
- **`ReentrantReadWriteLock`.** Fairness does not apply to `tryLock`, and the holder description
  is needed for messages.

## Open work

- Database-backed implementation for instances sharing one database, when ELZA supports that:
  edits take `SELECT … FOR SHARE` on the `arr_fund_version` row for their transaction; a run takes
  `FOR UPDATE` in a short transaction, inserts a lease row (owner instance, acquired, expiry,
  heartbeat), commits; edits check for a lease after their `FOR SHARE`. Reservations become
  lease rows in a reserved state. Same interface as the in-memory lock.
- Decide whether a node or fund import into an existing fund started from the client should be
  shared like a DA import item: both go through the same import, and the reasoning for DA items
  applies when the client import runs in one transaction.
- Decide the backfill strategy for `arr_change.fund_id` on very large installations (one Java
  migration in batches is the default; an option to skip and treat NULL as legacy stays open).
