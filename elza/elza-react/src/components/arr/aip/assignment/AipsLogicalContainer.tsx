
import { Row, Col } from "react-bootstrap";
import { HeadlessFlatTreeItemProps, TreeItemValue } from "@fluentui/react-components";
import TreeNavigation from "./TreeNavigation";
import Tree from "./Tree";
import { mapNodesToFlatItemArr } from "./utils";
import NodeDetail from "./NodeDetail";
import { useState } from "react";

export type FlatItem = HeadlessFlatTreeItemProps & { content: string };

type AipsLogicalTreeProps = {
    selectedNode: TreeItemValue;
    // setSelectedNode: (node: TreeItemValue) => void;
    setSelectedAips: (params: {aipIds: number[]; daLevelViewId: number}) => void
    tree: any;
}

const AipsLogicalContainer = ({tree, setSelectedAips, selectedNode}: AipsLogicalTreeProps) => {
    const [node, setNode] = useState<TreeItemValue>(selectedNode);
    if(!tree) {
        return null;
    }
    const nodes = mapNodesToFlatItemArr(tree);

    return (
        <div className="border d-flex flex-column h-100">
            <Row >
                {node && <TreeNavigation nodes={nodes} selectedNode={node} onSelect={setNode}/>}
            </Row>
            <Row className="flex-grow-1 m-0 border-top">
                <Col xs={6} className="p-0 flex-grow-1 border-end" >
                    {node && <Tree nodes={nodes} selectedNode={node} setSelectedNode={setNode}/>}
                </Col>
                <Col xs={6}>
                    {node && <NodeDetail tree={tree} setSelectedAips={setSelectedAips} selectedNode={node}  />}
                </Col>
            </Row>
        </div>
    );
}

export default AipsLogicalContainer;
