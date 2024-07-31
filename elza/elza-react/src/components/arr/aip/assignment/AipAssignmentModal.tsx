import {  FluentProvider, TreeItemValue } from "@fluentui/react-components";
import { DaAipDetailVO } from "api/DaAipDetailVO";
import { Modal, Button, Col, Row } from "react-bootstrap";
import "./AipAssignmentModal.scss";
import { Icon, i18n } from "components/shared";
import AipsLogicalTree from "./AipsLogicalContainer";
import { useEffect, useState } from "react";
import FundTree from "./FundTree";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { AIP_LOGICAL_TREE, fetchAipLogicalTreeIfNeeded } from "actions/aip/aip";
import { useThunkDispatch } from "utils/hooks";


type AipAssignmentModalProps = {
    aips: DaAipDetailVO[];
    tree: any
}

const AipAssignmentModal = ({aips, tree}: AipAssignmentModalProps) =>  {
    const [logicalTree, setLogicalTree] = useState(null);
    const [leftSelectedNode, setLeftSelectedNode] = useState<TreeItemValue>(null);
    const [rightSelectedNode, setRightSelectedNode] = useState<TreeItemValue>(tree.nodes[0].id);
    const structure = useSelector((state: AppState) => storeFromArea(state, AIP_LOGICAL_TREE))
    const dispatch = useThunkDispatch();

    useEffect(() => {
        const ids = aips.map(aip => aip.aipId)
        dispatch(fetchAipLogicalTreeIfNeeded(ids));
    },[]);

    useEffect(() => {
        if(structure.data) {
            setLogicalTree(structure.data);
            setLeftSelectedNode(structure.data.nodes[0].UUID);
        }
    },[structure]);

    const handleConnectToJP = () => {

    }

    const handleCreateFromSelected = () => {

    }

    return (
        <FluentProvider className="aip-assignment h-100">
            <Modal.Body>
                <Row className="h-100">
                    <Col xs={7}>
                        <AipsLogicalTree tree={logicalTree}  selectedNode={leftSelectedNode} setSelectedNode={setLeftSelectedNode}/>
                    </Col>
                    <Col xs={1}>
                        <div className="actions-container">
                            <Button onClick={handleConnectToJP}>
                                <Icon glyph="fa-solid fa-link" />
                                <div>{i18n('arr.aip.assignment.link')}</div>
                            </Button>
                            <Button onClick={handleCreateFromSelected}>
                                <Icon glyph="fa-solid fa-plus" />
                                <div>{i18n('arr.aip.assignment.create')}</div>
                            </Button>
                        </div>
                    </Col>
                    <Col xs={4}>
                        <div className="border h-100">
                            <FundTree tree={tree} expandedIds={tree.expandedIds} selectedNode={rightSelectedNode} setSelectedNode={setRightSelectedNode}/>
                        </div>
                    </Col>
                </Row>
            </Modal.Body>
        </FluentProvider>
    );
}
export default AipAssignmentModal;


