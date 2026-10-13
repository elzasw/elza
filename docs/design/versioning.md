# Versioning of the archival description

How ELZA keeps the history of a fund's description and how it reconstructs the description as it
was when a fund version was closed.

## Principles

- History is kept at two levels: the tree of units of description (levels) and the description
  items of each unit.
- Every change is recorded with the moment it happened, so it is known when each value was valid.
- Versioning happens in real time, as each change is saved. Closing a fund version is cheap: it only
  records one change and copies nothing.
- The goal is to show the open version as it is now, and every closed version as it was at the
  moment it was closed.
- An item value can be corrected without a new version, e.g. to fix a typo that does not need to be
  kept in the history (see [Change without a new version](#change-without-a-new-version)).

## Identity and instances

A versioned record has a stable identity and a sequence of instances. Each instance is valid from
its `create_change_id` up to its `delete_change_id`. While the instance is still valid,
`delete_change_id` is NULL.

| Record | Stable identity | Instance table |
|---|---|---|
| Unit of description | `arr_node.node_id`, plus `uuid` | `arr_level`: position in the tree (`node_id_parent`, `position`) |
| Description item | `arr_item.desc_item_object_id` | `arr_item` + `arr_desc_item`: type, specification, position, value, owning `node_id` |
| Fund version | — | `arr_fund_version`: `create_change_id`, `lock_change_id` |

- The `node_id` of a unit never changes, not even when the unit is moved. The structure of the tree
  is given by the parent references of the valid `arr_level` instances.
- The `desc_item_object_id` of an item never changes, neither when its value changes nor when it
  is moved. Each instance points to its value in `arr_data` (`data_id`). A new value is stored as a
  new `arr_data` row.
- A node belongs to exactly one fund (`arr_node.fund_id`). A unit cannot be shared between funds or
  placed twice in one tree.

## One time line of changes

Every change is a row in `arr_change`:

- `change_id`: generated, ascending; it orders all changes on one time line.
- `change_date`: when the change happened.
- `user_id`: who made it (NULL for system changes).
- `type` and `primary_node_id`: what kind of change it was and which unit it belongs to. These
  feed the history of a unit and the undo of changes.

One user action is one change: all instances created or ended by the action carry the same
`change_id`.

The same pattern is used by other records of the description that need history, such as
structured objects, node extensions, links of digital objects, and the assignment of nodes to
outputs. Access points use the same pattern on their own time line (`ap_change`: `ApState`,
`ApPart`, `ApItem`).

## Fund versions

- Creating a fund creates its first version. The version gets `create_change_id`.
- When a version is closed (approved), one new change is created. That change becomes the
  `lock_change_id` of the closed version and the `create_change_id` of the new open version. So
  versions follow each other without a gap or an overlap.
- There is exactly one open version (`lock_change_id IS NULL`) per fund.

## Which instances belong to a version

- **Open version:** every instance that has not ended (`delete_change_id IS NULL`).
- **Closed version** with lock change `L`: every instance with
  - `create_change_id < L`, and
  - `delete_change_id IS NULL` or `delete_change_id > L`.

The lock change is a change of its own: no instance is created or ended by it. So `<` and `<=`
give the same result; some queries use one, some the other.

## Changes of state

### Unit of description (`arr_level`)

| Action | What happens |
|---|---|
| Create | A new `arr_node` (new `node_id`) and a new `arr_level` instance with `create_change_id`. |
| Move | The current instance gets `delete_change_id`. A new instance of the same `node_id` gets the new parent and position, and `create_change_id` of the same change. Siblings whose position shifts get new instances in the same way. |
| Delete | The instance of the unit and the instances of all its descendants get `delete_change_id`. The same change also ends their description items and links to digital objects. Items elsewhere that refer to a deleted unit are updated in the same change. |

### Description item (`arr_desc_item`)

| Action | What happens |
|---|---|
| Create | A new instance with a new `desc_item_object_id` and `create_change_id`. |
| Change of value or position | The current instance gets `delete_change_id`. A new instance with the same `desc_item_object_id` and the new value or position gets `create_change_id` of the same change. |
| Delete | The current instance gets `delete_change_id`. |

### Change without a new version

`DescriptionItemService.updateDescriptionItem(..., createNewVersion = false, ...)` stores the new
value as a new `arr_data` row and points the current instance at it. No change is recorded, and
closed versions show the new value as well. The REST and WebSocket APIs offer the parameter
(`.../update/{createNewVersion}`). The web client always sends `true`.

## Concurrency

Each change of a unit goes through the unit's node (`saveNode`). The client sends the node version
it edited, and an optimistic lock rejects a change based on a stale node.

## Example

Changes on one time line, in ascending order of `change_id`:

| change_id | Event |
|---|---|
| 1 | Fund A created: version 1 opens (`create_change_id = 1`). |
| 2 | Unit I created under the root, position 1 (instance I/1 created by 2). |
| 3 | Item "title" of unit I created with value "x" (instance T/1 created by 3). |
| 120 | Version 1 closed: `lock_change_id = 120`; version 2 opens with `create_change_id = 120`. |
| 121 | Unit I moved under another parent: I/1 ended by 121, I/2 created by 121. |
| 353 | Value of "title" changed to "y": T/1 ended by 353, T/2 created by 353. |

Version 1 (`L = 120`) contains I/1 and T/1: unit I at its original place with the title "x". The
open version 2 contains I/2 and T/2.
