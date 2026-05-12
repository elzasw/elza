import type {NodeInfo} from "elza-api";
import {Api} from "../api";

const UUID_LENGTH = 36;

/**
 * Resolve a node identifier (numeric ID or 36-char UUID) to its NodeInfo.
 *
 * Lives in its own module so unit tests can exercise the routing logic
 * without pulling in the full ArrShared dependency graph (Redux actions,
 * component utilities, etc.).
 */
export async function fetchNodeInfo(identifier: string): Promise<NodeInfo> {
    if (identifier.length === UUID_LENGTH) {
        return (await Api.node.nodeGetNodeInfoByUuid(identifier)).data;
    }
    return (await Api.node.nodeGetNodeInfoById(parseInt(identifier, 10))).data;
}
