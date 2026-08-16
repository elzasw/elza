import { describe, expect, it } from "vitest";
import { FormItemType } from "elza-api";
import { DescItemGroup, DescItemTypeRef } from "typings/store";
import { FormItem } from "./formItems";
import { buildGroupsForm } from "./utils";

function makeItem(itemTypeId: number, localId: string): FormItem {
  return { localId, item: { itemTypeId } };
}

function makeTypeRef(id: number, viewOrder: number): DescItemTypeRef {
  return { id, viewOrder } as DescItemTypeRef;
}

function makeGroup(code: string, itemTypeIds: number[]): DescItemGroup {
  return {
    code,
    name: code,
    itemTypes: itemTypeIds.map((id) => ({ id, width: 1 })),
  } as unknown as DescItemGroup;
}

function makeGroupRefs(
  groups: DescItemGroup[],
): Record<number, DescItemGroup> & { ids: string[] } {
  const refs: Record<string, unknown> = { ids: groups.map((g) => g.code) };
  groups.forEach((g) => {
    refs[g.code] = g;
  });
  return refs as Record<number, DescItemGroup> & { ids: string[] };
}

const itemTypeRefs: Record<number, DescItemTypeRef> = {
  1: makeTypeRef(1, 20),
  2: makeTypeRef(2, 10),
  3: makeTypeRef(3, 5),
};

const itemTypes: FormItemType[] = [];

describe("buildGroupsForm", () => {
  it("returns an empty array when there are no items", () => {
    const groupRefs = makeGroupRefs([makeGroup("A", [1])]);
    expect(buildGroupsForm([], itemTypes, groupRefs, itemTypeRefs)).toEqual([]);
  });

  it("groups items by their group and keeps groups in groupRefs.ids order", () => {
    // ids order is [B, A]; item types 1,2 live in A, type 3 in B.
    const groupRefs = makeGroupRefs([
      makeGroup("B", [3]),
      makeGroup("A", [1, 2]),
    ]);
    const items = [makeItem(1, "l1"), makeItem(3, "l3")];

    const result = buildGroupsForm(items, itemTypes, groupRefs, itemTypeRefs);

    expect(result.map((g) => g.group.code)).toEqual(["B", "A"]);
  });

  it("sorts item types within a group by viewOrder ascending", () => {
    const groupRefs = makeGroupRefs([makeGroup("A", [1, 2, 3])]);
    const items = [makeItem(1, "l1"), makeItem(2, "l2"), makeItem(3, "l3")];

    const [group] = buildGroupsForm(items, itemTypes, groupRefs, itemTypeRefs);

    // viewOrder: type3=5, type2=10, type1=20
    expect(group.descItemTypes.map((t) => t.typeRef.id)).toEqual([3, 2, 1]);
  });

  it("collects multiple items of the same type under one type entry", () => {
    const groupRefs = makeGroupRefs([makeGroup("A", [1])]);
    const items = [makeItem(1, "l1"), makeItem(1, "l2")];

    const [group] = buildGroupsForm(items, itemTypes, groupRefs, itemTypeRefs);

    expect(group.descItemTypes).toHaveLength(1);
    expect(group.descItemTypes[0].descItems.map((d) => d.localId)).toEqual([
      "l1",
      "l2",
    ]);
  });

  it("carries the item type width from the group ref", () => {
    const group = makeGroup("A", [1]);
    group.itemTypes[0].width = 4;
    const groupRefs = makeGroupRefs([group]);

    const [built] = buildGroupsForm(
      [makeItem(1, "l1")],
      itemTypes,
      groupRefs,
      itemTypeRefs,
    );

    expect(built.descItemTypes[0].typeWidth).toBe(4);
  });
});
