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

export type AnyMessage = ChangeMessage | TestMessage | PolicyChangeMessage;
