# Design documents

Design records for developers: how a part of ELZA works and why it was built that way.
Administrators and users are served by the guides in `docs/admin-guide`, `docs/user-guide` and
`docs/implementation-guide`. Do not duplicate them here.

## Rules

- Describe the current state and its rationale: decisions, invariants, rejected alternatives,
  and behaviour that looks like a bug until you know why it was chosen.
- Do not record history: no dates, no "implemented in", no "changed / used to", no lists of
  completed steps. Git history, commit messages and release notes keep that.
- Work that is not done yet goes into one final `## Open work` section. When an item is done,
  delete it from the list and update the description. A document that is still only a proposal
  says so in its first line.
- Research notes in `research/` are the evidence behind a design document: cited findings
  with links to external sources. They state when and which versions of the sources were
  checked, because that tells how current the evidence is.
- Code may refer to a document by its file name, so rename a document only together with the
  references (`git grep <name>`).

## Index

| Document | Topic |
|---|---|
| [versioning.md](versioning.md) | Versioning of the archival description: changes, fund versions, instances |
| [da-migration.md](da-migration.md) | Link model of digital objects (`arr_dao_link` hierarchy), removing the `arr_dao` family |
| [fs-repo-analysis.md](fs-repo-analysis.md) | Filesystem repository: invariants and decisions |
| [da-aip-new-version.md](da-aip-new-version.md) | What happens when the digital archive reports a new version of an AIP |
| [da-aip-matching.md](da-aip-matching.md) | Automatic placement of received AIPs (`DA_MATCH`, `DA_IMPORT`) |
| [da-async-queue.md](da-async-queue.md) | Asynchronous download and export queue of the digital archive |
| [dms-storage.md](dms-storage.md) | DMS file storage: layout, trash, consistency |
| [db-statement-timeouts.md](db-statement-timeouts.md) | Database statement timeouts (proposal) |
| [rules-customization-layer.md](rules-customization-layer.md) | Rules-package customization layer, entity frameworks, internationalized version |
| [entity-framework-research.md](entity-framework-research.md) | Research: agent types and vocabularies in ISAAR(CPF), EAC-CPF, RiC; source notes in [research/entity-frameworks](research/entity-frameworks) |
| [isaar-cpf-cam-comparison.md](isaar-cpf-cam-comparison.md) | ISAAR(CPF) / EAC-CPF compared with CAM (CZ_BASE) |
| [password-policy.md](password-policy.md) | Password policy, forced change, users with a password from the configuration |
| [api-keys.md](api-keys.md) | Personal API keys |
| [siem-log.md](siem-log.md) | Security audit log for a SIEM: field schema, client address behind a proxy |
| [ai-task-events.md](ai-task-events.md) | AI provider task events and the per-user push |
| [entity-real-name-index.md](entity-real-name-index.md) | Entity search: the `REAL_NAME` index of names and the search fields of an index type (`index-search`) |
