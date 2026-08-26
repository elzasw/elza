import { DataType, NodeItem } from "elza-api";
import { describe, expect, it } from "vitest";
import { isEmptyItemValue } from "./utils";

function item(data: Record<string, unknown> | undefined, rest: Partial<NodeItem> = {}): NodeItem {
  return { itemTypeId: 1, nodeId: 1, nodeVersion: 1, position: 1, data, ...rest } as NodeItem;
}

describe("isEmptyItemValue", () => {
  it("treats a missing data payload as empty", () => {
    expect(isEmptyItemValue(item(undefined))).toBe(true);
  });

  it("never reports an undefined-flagged item as empty", () => {
    expect(isEmptyItemValue(item(undefined, { undefined: true }))).toBe(false);
  });

  it.each([
    ["blank string", { dataType: DataType.String, stringValue: "   " }],
    ["missing string", { dataType: DataType.String }],
    ["blank text", { dataType: DataType.Text, textValue: "" }],
    ["missing int", { dataType: DataType.Int }],
    ["missing decimal", { dataType: DataType.Decimal }],
    ["blank unitid", { dataType: DataType.Unitid, unitId: "" }],
    ["missing date", { dataType: DataType.Date }],
    ["missing unitdate", { dataType: DataType.Unitdate }],
    ["blank coordinates", { dataType: DataType.Coordinates, value: "" }],
    ["missing uriRef", { dataType: DataType.UriRef }],
    ["uriRef without a uri", { dataType: DataType.UriRef, value: "  " }],
    ["uriRef keeping only a description", { dataType: DataType.UriRef, value: "", description: "note" }],
    ["uriRef keeping only a template", { dataType: DataType.UriRef, value: "", refTemplateId: 7 }],
  ])("reports %s as empty", (_label, data) => {
    expect(isEmptyItemValue(item(data))).toBe(true);
  });

  it.each([
    ["integer zero", { dataType: DataType.Int, integerValue: 0 }],
    ["decimal zero", { dataType: DataType.Decimal, value: 0 }],
    ["string zero", { dataType: DataType.String, stringValue: "0" }],
    ["filled text", { dataType: DataType.Text, textValue: "abc" }],
    ["filled uriRef", { dataType: DataType.UriRef, value: "http://example.org" }],
  ])("does not report %s as empty", (_label, data) => {
    expect(isEmptyItemValue(item(data))).toBe(false);
  });

  it("ignores data types it does not cover", () => {
    expect(isEmptyItemValue(item({ dataType: DataType.Bit }))).toBe(false);
    expect(isEmptyItemValue(item({ dataType: DataType.Structured }))).toBe(false);
  });
});
