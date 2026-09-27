import { HeadlessFlatTreeItemProps, TreeItemValue } from "@fluentui/react-components";
import TreeNavigation from "./TreeNavigation";
import Tree from "./Tree";
import { mapNodesToFlatItemArr } from "./utils";
import { useEffect, useState } from "react";
import { useNodeName } from "components/aip/explorer/levels";

export type FlatItem = HeadlessFlatTreeItemProps & { content: string; count?: number };

/** Úroveň logické struktury balíčků, jak ji vrací server. */
export interface LogicalTreeNode {
    UUID: TreeItemValue;
    /** Balíčky, které úroveň zastupuje. */
    value: number[];
    /** Úroveň logické struktury (level view); virtuální úrovně ji nemají. */
    daLeveViewId?: number;
    parent?: TreeItemValue;
}

interface Props {
    tree: { nodes: LogicalTreeNode[] };
    /** Úroveň vybraná na začátku; dál si výběr drží komponenta sama. */
    initialNode: TreeItemValue;
    onSelect: (node: LogicalTreeNode) => void;
}

export type AipsLogicalContainerProps = Props;

/**
 * Logická struktura vybraných balíčků - úrovně sloučené napříč balíčky, u každé počet balíčků,
 * které zastupuje.
 */
function AipsLogicalContainer({ tree, initialNode, onSelect }: Props) {
    const [node, setNode] = useState<TreeItemValue>(initialNode);
    const nodeName = useNodeName();

    useEffect(() => {
        const selected = tree?.nodes.find(n => n.UUID == node);
        if (selected) {
            onSelect(selected);
        } else if (tree?.nodes.length) {
            // po načtení znovu vybraná úroveň zmizela (její balíčky jsou napojené) - vybere se kořen
            setNode(tree.nodes[0].UUID);
        }
    }, [node, tree]);

    if (!tree) {
        return null;
    }
    const nodes = mapNodesToFlatItemArr(tree, nodeName);

    return (
        <div className="border d-flex flex-column h-100">
            {node && <TreeNavigation nodes={nodes} selectedNode={node} onSelect={setNode} />}
            <div className="flex-grow-1 border-top overflow-auto">
                {node && <Tree nodes={nodes} selectedNode={node} setSelectedNode={setNode} openAll />}
            </div>
        </div>
    );
}

export default AipsLogicalContainer;
