import { FlatItem } from "./AipsLogicalContainer";
import { NamedNode } from "components/aip/explorer/levels";

/**
 * Virtuální úrovně stromu pojmenovává klient podle jejich typu, proto se název neřeší tady,
 * ale předává se překladač z komponenty.
 */
export const mapNodesToFlatItemArr = (tree, nodeName: (node: NamedNode) => string) => {
    const items = []
        tree.nodes.forEach((node) => {
            const item: FlatItem = {
                value: node.UUID,
                content: nodeName(node),
                // kolik balíčků úroveň zastupuje
                count: node.value?.length ?? 0,
            }
            if (node.parent != null) {
                item.parentValue = node.parent;
            }
        
            items.push(item);
        });
    return items;
}

export const findNodeByValue = (nodes, value) => {
    return nodes.find(node => node.value == value);
}