import { EventType } from "./EventType";

export interface Message {
  eventType: EventType;
}

export interface ChangeMessage extends Message {
  eventType: EventType.NODES_CHANGE | EventType.CONFORMITY_INFO;
  state?: unknown;
  code?: unknown;
  versionId: number;
  entityIds: number[];
}
export interface PolicyChangeMessage extends Message {
  eventType: EventType.VISIBLE_POLICY_CHANGE;
  invalidateNodes?: string;
  versionId: number;
  nodeIds: number[];
  code?: unknown;
  state?: unknown;
}

interface TestMessage extends Message {
  eventType: EventType.DELETE_NODES;
  test?: boolean;
}

export interface StructureDataChangeMessage extends Message {
  eventType: EventType.STRUCTURE_DATA_CHANGE;
  fundId: number;
  structureTypeCode: string;
  tempIds: number[];
  createIds: number[];
  updateIds: number[];
  deleteIds: number[];
}

export interface OutputItemChangeMessage extends Message {
  eventType: EventType.OUTPUT_ITEM_CHANGE;
  versionId: number;
  itemObjectId: number;
  outputId: number;
  version: number;
}

export interface OutputStateChangeMessage extends Message {
  eventType: EventType.OUTPUT_STATE_CHANGE;
  versionId: number;
  /** Output id. */
  entityId: number;
  /** Output state name, or "ERROR" when generating failed. */
  entityString: string;
}

export type AnyMessage =
  | ChangeMessage
  | TestMessage
  | PolicyChangeMessage
  | StructureDataChangeMessage
  | OutputItemChangeMessage
  | OutputStateChangeMessage;
