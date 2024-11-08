import { TreeItemValue } from "@fluentui/react-components";
import { Modal, Button, Col, Row } from "react-bootstrap";
import "./AipAssignmentModal.scss";
import { Icon, i18n } from "components/shared";
import AipsLogicalTree from "./AipsLogicalContainer";
import { useEffect, useState } from "react";
import FundTree from "./FundTree";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { AIP_LOGICAL_TREE, AREA_AIP, aipFetchIfNeeded, fetchAipLogicalTreeIfNeeded } from "actions/aip/aip";
import { useThunkDispatch } from "utils/hooks";
import { WebApi } from "actions";
import {AipDetailVO} from "elza-api";


type AipAssignmentModalProps = {
    aips: AipDetailVO[];
    tree: any,
}

const AipAssignmentModal = ({aips, tree}: AipAssignmentModalProps) =>  {
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP))
    const [logicalTree, setLogicalTree] = useState(null);
    const [selectedAips, setSelectedAips] = useState<{aipIds: number[], daLevelViewId: number}>(null);
    const [selectedArrNodeId, setSelectedArrNodeId] = useState<TreeItemValue>(tree.nodes[0].id);
    const structure = useSelector((state: AppState) => storeFromArea(state, AIP_LOGICAL_TREE))
    const dispatch = useThunkDispatch();


    useEffect(() => {
        const ids = aips.map(aip => aip.aipId)
        dispatch(fetchAipLogicalTreeIfNeeded(ids));
    },[]);

    useEffect(() => {
        if(structure.data) {
            setLogicalTree(structure.data);
            setSelectedAips(structure.data.nodes[0].UUID);
        }
    },[structure]);

    const handleConnectToJP = () => {
        if(!selectedAips.daLevelViewId) {
            // Bez logické struktury
            WebApi.connectSelectedAipToJp(selectedArrNodeId as number, selectedAips.aipIds).then(() => {
                dispatch(aipFetchIfNeeded(aip.id, true));
            });
        } else {
            // S logickou strukturou
            WebApi.connectAipLogicalStructureToJpBulk(
                selectedArrNodeId as number,
                selectedAips.aipIds,
                selectedAips.daLevelViewId
            ).then(() => {
                dispatch(aipFetchIfNeeded(aip.id, true));
            });
        }
    }

    const handleCreateFromSelected = () => {
        if(!selectedAips.daLevelViewId) {
            // Bez logické struktury
            WebApi.createJpFromSelectedAipBulk(selectedArrNodeId as number, selectedAips.aipIds).then(() => {
                dispatch(aipFetchIfNeeded(aip.id, true));
            });
        } else {
            // S logickou strukturou
            WebApi.createJpFromSelectedAipAnConnectBulk(
                selectedArrNodeId as number,
                selectedAips.aipIds,
                selectedAips.daLevelViewId
            ).then(() => {
                dispatch(aipFetchIfNeeded(aip.id, true));
            });
        }
    }

    return (
        <Modal.Body>
            <Row>
                <Col xs={7}>
                {structure.data &&
                    <AipsLogicalTree tree={logicalTree} setSelectedAips={setSelectedAips} selectedNode={structure.data.nodes[0].UUID}/>
                }
                </Col>
                <Col xs={1}>
                    <div className="aip-actions-container">
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
                        <FundTree tree={tree} expandedIds={tree.expandedIds} selectedNode={selectedArrNodeId} setSelectedNode={setSelectedArrNodeId}/>
                    </div>
                </Col>
            </Row>
        </Modal.Body>
    );
}
export default AipAssignmentModal;


