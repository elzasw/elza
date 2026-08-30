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
import { AIP_LOGICAL_TREE, fetchAipLogicalTreeIfNeeded } from "actions/aip/aip";
import { useThunkDispatch } from "utils/hooks";
import {AipConnectBlockedVO, AipDetailVO} from "elza-api";
import { useIntl } from "react-intl";
import { Api } from "../../../../api";
import AipConnectBlockedPanel from "../../../aip/AipConnectBlockedPanel";
import { runAipAction } from "../../../aip/AipActionRunner";
import { aipsFetchIfNeeded } from "actions/aip/aip";


type AipAssignmentModalProps = {
    aips: AipDetailVO[];
    tree: any,
}

const AipAssignmentModal = ({aips, tree}: AipAssignmentModalProps) =>  {
    const [logicalTree, setLogicalTree] = useState(null);
    const [selectedAips, setSelectedAips] = useState<{aipIds: number[], daLevelViewId: number}>(null);
    const [selectedArrNodeId, setSelectedArrNodeId] = useState<TreeItemValue>(tree.nodes[0].id);
    const structure = useSelector((state: AppState) => storeFromArea(state, AIP_LOGICAL_TREE))
    const dispatch = useThunkDispatch();
    const intl = useIntl();
    const [blocked, setBlocked] = useState<AipConnectBlockedVO[]>([]);

    /** Napojení už napojený AIP odmítne; uživatel to má vědět dřív, než potvrdí. */
    useEffect(() => {
        const aipIds = aips.map(a => a.aipId);
        if (selectedArrNodeId == null || aipIds.length === 0) {
            setBlocked([]);
            return;
        }
        Api.aips.aipConnectCheck(selectedArrNodeId as number, aipIds)
            .then(response => setBlocked(response.data.blocked ?? []))
            .catch(() => setBlocked([]));
    }, [selectedArrNodeId, aips]);

    const reloadAips = () => dispatch(aipsFetchIfNeeded(true));


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
        const title = i18n("arr.aip.assignment.link");
        // Napojení běží na pozadí, po jednom AIPu; dialog ukáže, jak dopadl každý z nich.
        runAipAction(dispatch, intl, title, () => selectedAips.daLevelViewId
            ? Api.aips.aipBulkConnectLogicToJp(selectedArrNodeId as number, selectedAips.aipIds,
                                               selectedAips.daLevelViewId)
            : Api.aips.aipBulkConnectToJp(selectedArrNodeId as number, selectedAips.aipIds), reloadAips);
    }

    const handleCreateFromSelected = () => {
        const title = i18n("arr.aip.assignment.create");
        runAipAction(dispatch, intl, title, () => selectedAips.daLevelViewId
            ? Api.aips.aipBulkCreateSelectedToJp(selectedArrNodeId as number, selectedAips.aipIds,
                                                 selectedAips.daLevelViewId)
            : Api.aips.aipBulkCreateFromSelected(selectedArrNodeId as number, selectedAips.aipIds), reloadAips);
    }

    return (
        <Modal.Body>
            <AipConnectBlockedPanel blocked={blocked}/>
            <Row>
                <Col xs={7}>
                {structure.data &&
                    <AipsLogicalTree tree={logicalTree} setSelectedAips={setSelectedAips} selectedNode={structure.data.nodes[0].UUID}/>
                }
                </Col>
                <Col xs={1}>
                    <div className="aip-actions-container">
                        <Button onClick={handleConnectToJP} disabled={blocked.length > 0}>
                            <Icon glyph="fa-solid fa-link" />
                            <div>{i18n('arr.aip.assignment.link')}</div>
                        </Button>
                        <Button onClick={handleCreateFromSelected} disabled={blocked.length > 0}>
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


