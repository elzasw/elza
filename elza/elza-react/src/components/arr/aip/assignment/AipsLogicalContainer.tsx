
import { Row, Col } from "react-bootstrap";
import { HeadlessFlatTreeItemProps, TreeItemValue } from "@fluentui/react-components";
import TreeNavigation from "./TreeNavigation";
import Tree from "./Tree";
import { mapNodesToFlatItemArr } from "./utils";
import NodeDetail from "./NodeDetail";

export type FlatItem = HeadlessFlatTreeItemProps & { content: string };

type AipsLogicalTreeProps = {
    selectedNode: TreeItemValue;
    setSelectedNode: (node: TreeItemValue) => void;
    tree: any;
}

const AipsLogicalContainer = ({tree, selectedNode, setSelectedNode}: AipsLogicalTreeProps) => {
    if(!tree) {
        return null;
    }
    const nodes = mapNodesToFlatItemArr(tree);

    return (
        <div className="border  h-100">
            <div className="border-bottom">
                {selectedNode && <TreeNavigation nodes={nodes} selectedNode={selectedNode} onSelect={setSelectedNode}/>}
            </div>
            <Row className="flex-grow-1 m-0">
                <Col xs={6}>
                    <div className="border-end h-100">
                        {selectedNode && <Tree nodes={nodes} selectedNode={selectedNode} setSelectedNode={setSelectedNode}/>}
                    </div>
                </Col>
                <Col xs={6}>
                    {selectedNode && <NodeDetail tree={tree} nodes={nodes} selectedNode={selectedNode}  />}
                </Col>
            </Row>
        </div>
    );
}

export default AipsLogicalContainer;
