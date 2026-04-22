import { FlatItem } from "./AipsLogicalContainer";
export const mapNodesToFlatItemArr = (tree) => {
    const items = []
        tree.nodes.forEach((node) => {
            const item: FlatItem = {
                value: node.UUID,
                content: node.name,
            }
            if (node.parent != null) {
                item.parentValue = node.parent;
            }
        
            items.push(item);
        });
    return items;
}

export const getTreeItems = (tree) => {
    const lastNodesAtDepth: { [depth: number]: number } = {};
    const items = []
        tree.nodes.forEach((node) => {
            const item: FlatItem = {
                value: node.id,
                content: node.name,
            }
            if (node.depth > 1) {
                const parentDepth = node.depth - 1;
                if (lastNodesAtDepth[parentDepth] !== undefined) {
                    item.parentValue = lastNodesAtDepth[parentDepth];
                }
            }
        
            lastNodesAtDepth[node.depth] = node.id;
        
            items.push(item);
        });
    return items;
}

export const findNodeInTree = (tree, node) => {
    return tree.nodes.find(n => n.id == node);
}

export const findNodeByValue = (nodes, value) => {
    return nodes.find(node => node.value == value);
}