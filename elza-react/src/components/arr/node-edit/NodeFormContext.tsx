import { createContext, useContext } from "react";
import { useNodeFormData } from "./hooks";

type NodeFormContextValue = ReturnType<typeof useNodeFormData>;

export const NodeFormContext = createContext<NodeFormContextValue | undefined>(undefined);

export function useNodeFormContext() {
    const nodeFormContext = useContext(NodeFormContext);
    if (!nodeFormContext) {
        console.warn("useNodeFormContext must be used within NodeFormContext.Provider");
    }
    return nodeFormContext;
}
