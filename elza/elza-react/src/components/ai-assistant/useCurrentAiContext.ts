import { useLocation } from "react-router-dom";
import { AiContextObject, AiContextType, AiContextFund, AiContextNode, AiContextAccesspoint } from "elza-api";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { objectById } from "shared/utils";
import { URL_ENTITY, URL_FUND, TREE, NODE, GRID, AIP, PUBLICATION, MOVEMENTS, OUTPUTS, ACTIONS, DAOS, REQUESTS } from "../../constants";

const ARRANGEMENT_SUBMODULES = [TREE, NODE, GRID, AIP, PUBLICATION, MOVEMENTS, OUTPUTS, ACTIONS, DAOS, REQUESTS];

export type AiContextSegmentLabel = "module" | "fund" | "node" | "accessPoint";

export type AiModule = "arrangement" | "registry";

export interface AiContextSegment {
    labelId: AiContextSegmentLabel;
    value: string;
}

export interface AiContext {
    /** Typed context objects sent to the API (`AiConversationCreate.context`). */
    objects: AiContextObject[];
    module: AiModule;
    /** Display chips for the panel. */
    segments: AiContextSegment[];
}

export function useCurrentAiContext(): AiContext | null {
    const { pathname } = useLocation();
    const arrRegion = useAppSelector(state => state.arrRegion);
    const registryDetail = useAppSelector(state => state.app.registryDetail);

    const pathSegments = pathname.split("/");
    const isArrangement = pathname.startsWith(URL_FUND)
        && ARRANGEMENT_SUBMODULES.some(submodule => pathSegments.includes(submodule));
    const isEntity = pathname.startsWith(URL_ENTITY);

    const activeIndex = arrRegion.activeIndex;
    const activeFund = activeIndex !== null ? arrRegion.funds[activeIndex] : undefined;

    if (isArrangement && activeFund && activeFund.id != null) {
        const nodes = activeFund.nodes;
        const activeNode = nodes.activeIndex != null ? nodes.nodes[nodes.activeIndex] : undefined;
        const selectedSubNodeId = activeNode?.selectedSubNodeId;
        const selectedSubNode = activeNode && selectedSubNodeId != null
            ? objectById(activeNode.childNodes, selectedSubNodeId)
            : null;
        const fundName = typeof activeFund.name === "string" ? activeFund.name : "";

        const segments: AiContextSegment[] = [
            { labelId: "fund", value: fundName },
        ];
        const selectedNodeName = selectedSubNode?.accordionLeft || selectedSubNode?.name;
        if (selectedNodeName) {
            segments.push({ labelId: "node", value: selectedNodeName });
        }

        const objects: AiContextObject[] = selectedSubNodeId != null
            ? [{ type: AiContextType.Node, fundId: activeFund.id, fundVersionId: activeFund.versionId, nodeId: selectedSubNodeId } as AiContextNode]
            : [{ type: AiContextType.Fund, fundId: activeFund.id } as AiContextFund];

        return { objects, module: "arrangement", segments };
    }

    if (isEntity && registryDetail.id != null) {
        return {
            objects: [{ type: AiContextType.Accesspoint, accessPointId: registryDetail.id } as AiContextAccesspoint],
            module: "registry",
            segments: [{ labelId: "accessPoint", value: registryDetail.data?.name ?? "" }],
        };
    }

    return null;
}
