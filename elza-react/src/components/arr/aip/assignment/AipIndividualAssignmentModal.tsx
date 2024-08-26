import { TreeItemValue } from "@fluentui/react-components";
import { DaAipDetailVO } from "api/DaAipDetailVO";
import { Modal, Button, Col, Row } from "react-bootstrap";
import "./AipAssignmentModal.scss";
import { Icon, i18n } from "components/shared";
import { useEffect, useState } from "react";
import FundTree from "./FundTree";
import AipExplorer from "../../../aip/explorer/AipExplorer.tsx";
import {ExplorerMode} from "../../../aip/explorer/ExplorerContext.tsx";
import {WebApi} from "../../../../actions";
import { AREA_SELECTED_AIP_DAOS } from "actions/aip/aip.ts";
import { useSelector } from "react-redux";
import storeFromArea from "shared/utils/storeFromArea.jsx";
import { AppState } from "typings/store/AppState.types.ts";
import ConfirmForm from "../../../../components/shared/form/ConfirmForm";
import { modalDialogHide, modalDialogShow } from "actions/global/modalDialog.jsx";
import { useThunkDispatch } from "utils/hooks/useThunkDispatch.ts";


type AipAssignmentModalProps = {
    aipId: number;
    tree: any
}

const AipIndividualAssignmentModal = ({aipId, tree}: AipAssignmentModalProps) =>  {
    const [rightSelectedNode, setRightSelectedNode] = useState<TreeItemValue>(tree.nodes[0].id);
    const [leftSelectedNode, setLeftSelectedNode] = useState<number>(null);

    const selectedDaDaos = useSelector((state: AppState) => storeFromArea(state, AREA_SELECTED_AIP_DAOS));
    const dispatch = useThunkDispatch();

    const handleConnectToJP = () => {
        WebApi.connectAipToJp(rightSelectedNode as number, aipId);
    }

    const handleCreateFromSelected = () => {
        WebApi.connectAipPartToJp(rightSelectedNode as number, aipId, leftSelectedNode)
    }

    const handleSelectAndConnectToJP = () => {
        const confirmForm = (
            <ConfirmForm
                //@ts-ignore
                confirmMessage={i18n('arr.aip.assignment.part.confirm-first') + i18n('arr.aip.assignment.part.confirm-last')}
                submittingMessage={i18n('arr.aip.assignment.part.confirm-first') + i18n('arr.aip.assignment.part.confirm-last')}
                submitTitle={i18n('global.action.run')}
                onSubmit={() => {
                    return WebApi.createJpFromSelectedAip(rightSelectedNode as number, aipId, leftSelectedNode)
                }}
                onSubmitSuccess={() => {
                    dispatch(modalDialogHide());
                }}
            />
        );
        dispatch(modalDialogShow(this, null, confirmForm));

    }

    // možnost je skrytá pokud mam zvolený celý balíček
    const handleCreateAndLinkFromSelected = () => {
        const confirmForm = (
            <ConfirmForm
                //@ts-ignore
                confirmMessage={i18n('arr.aip.assignment.part.confirm-first') + i18n('arr.aip.assignment.part.confirm-last')}
                submittingMessage={i18n('arr.aip.assignment.part.confirm-first') + i18n('arr.aip.assignment.part.confirm-last')}
                submitTitle={i18n('global.action.run')}
                onSubmit={() => {
                    return WebApi.createJpLinkFromSelectedAip(rightSelectedNode as number, aipId, leftSelectedNode);
                }}
                onSubmitSuccess={() => {
                    dispatch(modalDialogHide());
                }}
            />
        );
        dispatch(modalDialogShow(this, null, confirmForm));
    }
;

    return (
        <Modal.Body>
            <Row className="h-100">
                <Col xs={7}>
                    <AipExplorer mode={ExplorerMode.SELECT} onSelect={(node) => setLeftSelectedNode(node.daoId)}/>
                </Col>
                <Col xs={1}>
                    <div className="actions-container">
                        <Button onClick={handleConnectToJP}>
                            <Icon glyph="fa-solid fa-link" />
                            <div>{i18n('arr.aip.assignment.link')}</div>
                        </Button>
                        <Button onClick={handleCreateFromSelected} disabled={!leftSelectedNode}>
                            <Icon glyph="fa-solid fa-plus" />
                            <div>{i18n('arr.aip.assignment.create')}</div>
                        </Button>
                        {leftSelectedNode && <>
                            <Button onClick={handleSelectAndConnectToJP}>
                                <Icon glyph="fa-solid fa-link" />
                                <div>{i18n('arr.aip.assignment.select-and-create')}</div>
                            </Button>
                            <Button onClick={handleCreateAndLinkFromSelected}>
                                <Icon glyph="fa-solid fa-plus" />
                                <div>{i18n('arr.aip.assignment.create-and-link')}</div>
                            </Button>
                        </>}
                    </div>
                </Col>
                <Col xs={4}>
                    <div className="border h-100">
                        <FundTree tree={tree} expandedIds={tree.expandedIds} selectedNode={rightSelectedNode} setSelectedNode={setRightSelectedNode}/>
                    </div>
                </Col>
            </Row>
        </Modal.Body>
    );
}
export default AipIndividualAssignmentModal;


