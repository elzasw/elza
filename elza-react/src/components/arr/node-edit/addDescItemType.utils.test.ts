import { describe, it, expect } from "vitest";
import { FormItemType, MandatoryType } from "elza-api";
import { DescItemGroup, DescItemTypeRef } from "typings/store";
import { EditItem } from "./types";
import {
  resolveAvailableItemTypes,
  sortTypesByFormOrder,
  groupIndexOfType,
} from "./addDescItemType.utils";

// Minimal fixtures: the helpers only read id/code/name/viewOrder from a type, and itemTypeId/type
// from a form item type, so the rest of the (large) VO shapes is irrelevant here.
function descItemType(
  id: number,
  overrides: Partial<DescItemTypeRef> = {},
): DescItemTypeRef {
  return {
    id,
    code: `CODE_${id}`,
    name: `Type ${id}`,
    viewOrder: id,
    ...overrides,
  } as DescItemTypeRef;
}

function formItemType(itemTypeId: number, type: MandatoryType): FormItemType {
  return {
    itemTypeId,
    type,
    repeatable: false,
    undefinable: false,
    favoriteSpecIds: [],
  } as FormItemType;
}

function editItem(itemTypeId: number): EditItem {
  return { itemTypeId } as EditItem;
}

function groupsMap(
  groups: Array<{ code: string; typeIds: number[] }>,
): Record<string, DescItemGroup> & { ids: string[] } {
  const map = { ids: [] as string[] } as Record<string, DescItemGroup> & { ids: string[] };
  for (const { code, typeIds } of groups) {
    map.ids.push(code);
    map[code] = {
      code,
      name: code,
      itemTypes: typeIds.map((id) => ({ id, width: 1 })),
    };
  }
  return map;
}

describe("resolveAvailableItemTypes", () => {
  const possible = descItemType(1);
  const impossible = descItemType(2, { code: "IMP" });
  const added = descItemType(3);
  const allTypes = [possible, impossible, added];

  // possible (1) is offered by the server; added (3) is already on the node; impossible (2) is
  // absent from itemTypes (the server omits impossible types).
  const itemTypes: FormItemType[] = [
    formItemType(1, MandatoryType.Possible),
    formItemType(3, MandatoryType.Required),
  ];
  const descItems = [editItem(3)];

  it("excludes types already added to the node", () => {
    const result = resolveAvailableItemTypes(allTypes, itemTypes, descItems, [], true, []);
    expect(result.map(({ id }) => id)).not.toContain(3);
  });

  it("includes possible/required/recommended types regardless of strict mode", () => {
    const strict = resolveAvailableItemTypes(allTypes, itemTypes, descItems, [], true, []);
    const lenient = resolveAvailableItemTypes(allTypes, itemTypes, descItems, [], false, []);
    expect(strict.map(({ id }) => id)).toContain(1);
    expect(lenient.map(({ id }) => id)).toContain(1);
  });

  it("excludes impossible types in strict mode", () => {
    const result = resolveAvailableItemTypes(allTypes, itemTypes, descItems, ["IMP"], true, []);
    expect(result.map(({ id }) => id)).not.toContain(2);
  });

  it("includes impossible types in non-strict mode when the code is in the rule set", () => {
    const result = resolveAvailableItemTypes(allTypes, itemTypes, descItems, ["IMP"], false, []);
    expect(result.map(({ id }) => id)).toContain(2);
  });

  it("excludes impossible types in non-strict mode when the code is not in the rule set", () => {
    const result = resolveAvailableItemTypes(allTypes, itemTypes, descItems, ["OTHER"], false, []);
    expect(result.map(({ id }) => id)).not.toContain(2);
  });

  it("excludes impossible types in non-strict mode when rule set codes are not yet loaded", () => {
    const result = resolveAvailableItemTypes(allTypes, itemTypes, descItems, undefined, false, []);
    expect(result.map(({ id }) => id)).not.toContain(2);
  });

  it("tags each type with a className encoding its mandatory type", () => {
    const result = resolveAvailableItemTypes(allTypes, itemTypes, descItems, ["IMP"], false, []);
    expect(result.find(({ id }) => id === 1)?.className).toBe("type-possible");
    expect(result.find(({ id }) => id === 2)?.className).toBe("type-impossible");
  });

  it("adds the queued class to queued types", () => {
    const result = resolveAvailableItemTypes(allTypes, itemTypes, descItems, [], true, [1]);
    expect(result.find(({ id }) => id === 1)?.className).toBe("type-possible queued");
  });
});

describe("groupIndexOfType", () => {
  const groups = groupsMap([
    { code: "G1", typeIds: [10, 11] },
    { code: "G2", typeIds: [20] },
  ]);

  it("returns the group order index for a grouped type", () => {
    expect(groupIndexOfType(groups, 10)).toBe(0);
    expect(groupIndexOfType(groups, 20)).toBe(1);
  });

  it("sorts ungrouped types last", () => {
    expect(groupIndexOfType(groups, 99)).toBe(groups.ids.length);
  });
});

describe("sortTypesByFormOrder", () => {
  const groups = groupsMap([
    { code: "G1", typeIds: [11, 10] },
    { code: "G2", typeIds: [20] },
  ]);

  it("orders by group first, then by viewOrder within a group", () => {
    const t10 = descItemType(10, { viewOrder: 2 });
    const t11 = descItemType(11, { viewOrder: 1 });
    const t20 = descItemType(20, { viewOrder: 0 });

    // Input deliberately out of form order: G2 type first, then the two G1 types reversed.
    const sorted = sortTypesByFormOrder([t20, t10, t11], groups);

    // G1 (index 0) before G2 (index 1); within G1, viewOrder 1 before 2.
    expect(sorted.map(({ id }) => id)).toEqual([11, 10, 20]);
  });

  it("does not mutate the input array", () => {
    const input = [descItemType(20), descItemType(10)];
    const copy = [...input];
    sortTypesByFormOrder(input, groups);
    expect(input).toEqual(copy);
  });
});
