# DA queue without blocking (download and export)

Communication with a digital archive (DA) is serialized by `DaCommunicationLock`. Preparing a
download batch or ingesting an export batch can take the DA arbitrarily long, so neither is waited
for under the lock: the lock is held only for single exchanges, and the synchronization, downloads
and exports interleave while the DA works.

## Decisions

- Both directions (download, export) are asynchronous. At most one batch is in flight per
  direction per repository.
- There is no time limit for PENDING/PREPARING. Status 403/404 is a permanent failure (DA spec);
  the request fails with the DA's `ErrorInfo`. A network error or 5xx is transient.
- Transient failures are retried for ever with a growing interval (poll interval × 2^n, capped at
  5 minutes, `DaService.retryDelay`); "Zkusit znovu teď" resets it.
- Manual withdrawal is allowed only for requests the DA has not received (`IMPORT_NEW`, `UPDATE`,
  `EXPORT_NEW`). A request the DA knows is never withdrawn.
- Actions on requests belong to the queue view, not to the AIP list ribbon.

## Design

- Queue item (`DaSyncQueueItem`): in-flight states `DOWNLOAD_REQUESTED` and `EXPORT_SENT`; columns
  `batch_id` and `next_attempt_at` (null = due now). The processors read only due items.
- Each step holds the lock only for one exchange: request/send a batch, ask the status once,
  fetch and process a finished batch (`DaImportExtSyncsProcessor`, `DaExportExtSyncsProcessor`).
- Export result: `accepted` → `EXPORT_OK`; `packageIngestError` (matched by sipId = AIP code)
  → `EXPORT_ERROR` with its text; neither → `EXPORT_ERROR` "Digitální archiv nepotvrdil přijetí
  balíčku."
- Download status 403/404: the batch is gone, its items return to their pending state with a
  retry delay — downloads never give up.
- Invalidation and supersede leave `EXPORT_SENT` alone; a `DOWNLOAD_REQUESTED` item may be
  deactivated — the finished batch then processes only its active items.
- In-flight state is in the DB, so a restart continues the batch instead of losing it.

## Queue view

- Admin page `AdminDaQueuePage`, URL `/admin/da/<repositoryId>/requests` (one DA per page, a
  selector when there are more; `/admin/da` opens the first). Newest first (id desc), server
  paging, polled refresh.
- The table keeps every request (finished, superseded, withdrawn). A stored package
  (`da_local_cache`) points to its queue item, so queue items are never physically deleted.
- Filters: preset *waiting* (active + waiting states) / *all*, states, direction, AIP code, batch,
  only failed. Columns: id, AIP, direction + package form, state, batch, attempts, next attempt,
  last change, message; a detail panel with the full message and who asked.
- Actions on the selected items (the server re-checks each; items that no longer qualify are
  counted as skipped): retry now (waiting), withdraw (undelivered only), repeat a failed one
  (download: a new request of the same form; export: a new change package from the current
  description).
- API by queue item ids under `/extsystem/digitalrepository/{id}/queue` (`DaQueueService`),
  ADMIN only.

## Open work

- The upload of a change package and the fetch of a prepared batch still run under the lock
  (bounded by size, not by the DA).
- More than one batch in flight per direction and repository.
- A cleanup rule for old finished queue items (they are kept because stored packages reference
  them).
