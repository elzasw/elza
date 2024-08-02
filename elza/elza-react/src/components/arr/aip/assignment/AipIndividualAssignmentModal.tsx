import {  FluentProvider, TreeItemValue } from "@fluentui/react-components";
import { DaAipDetailVO } from "api/DaAipDetailVO";
import { Modal, Button, Col, Row } from "react-bootstrap";
import "./AipAssignmentModal.scss";
import { Icon, i18n } from "components/shared";
import { useState } from "react";
import FundTree from "./FundTree";
import AipExplorer from "../../../aip/explorer/AipExplorer.tsx";
import {ExplorerMode} from "../../../aip/explorer/ExplorerContext.tsx";


type AipAssignmentModalProps = {
    aip: DaAipDetailVO;
    tree: any
}

const AipIndividualAssignmentModal = ({aip, tree}: AipAssignmentModalProps) =>  {
    const [rightSelectedNode, setRightSelectedNode] = useState<TreeItemValue>(tree.nodes[0].id);


    const handleConnectToJP = () => {

    }

    const handleCreateFromSelected = () => {

    }

    const handleSelectAndConnectToJP = () => {

    }

    const handleCreateAndLinkFromSelected = () => {

    }

    return (
        <FluentProvider className="aip-assignment h-100">
            <Modal.Body>
                <Row className="h-100">
                    <Col xs={7}>
                        <AipExplorer mode={ExplorerMode.VIEW}/>
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
                            <Button onClick={handleSelectAndConnectToJP}>
                                <Icon glyph="fa-solid fa-link" />
                                <div>{i18n('arr.aip.assignment.select-and-create')}</div>
                            </Button>
                            <Button onClick={handleCreateAndLinkFromSelected}>
                                <Icon glyph="fa-solid fa-plus" />
                                <div>{i18n('arr.aip.assignment.create-and-link')}</div>
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
export default AipIndividualAssignmentModal;


