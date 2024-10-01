import "./DaoLinkDetail.scss";
import {AREA_DAO_LINKS, daoLinksFetchIfNeeded} from "actions/aip/aip";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import {useEffect} from "react";
import {useThunkDispatch} from "../../utils/hooks";
import {Button, Col, Row} from "react-bootstrap";
import {Icon} from "../shared";
import {Api} from "../../api";
import {modalDialogHide, modalDialogShow} from "../../actions/global/modalDialog";
import AipExplorerModalWrapper from "./explorer/AipExplorerWrapper.tsx";
import {ExplorerMode} from "./explorer/ExplorerContext.tsx";
import * as aipActions from "../../actions/aip/aip.ts";

type DaoLinkDetailProps = {
    nodeId: number;
}

const DaoLinkDetail = ({nodeId}: DaoLinkDetailProps) => {
    const daoLinks = useSelector((state: AppState) => storeFromArea(state, AREA_DAO_LINKS));
    const dispatch = useThunkDispatch();

    useEffect(() => {
        dispatch(daoLinksFetchIfNeeded(nodeId, true));
    },[
        nodeId,
        dispatch,
    ]);

    const handleDeleteLink = (linkId: number) => {
        Api.aips.aipDeleteDaoLink(linkId).then(() => {
            dispatch(daoLinksFetchIfNeeded(nodeId, true))
        });
    }

    const handleOpenExplorer = (aipId: number) => {
        dispatch(aipActions.selectAip(aipId));
        dispatch(
            modalDialogShow(
                this,
                "AIP Průzkumník",
                <AipExplorerModalWrapper
                    //@ts-ignore
                    onOk={() => dispatch(modalDialogHide())}
                    onClose={() => dispatch(modalDialogHide())}
                    mode={ExplorerMode.VIEW}
                />,
                "aip-explorer"
            ),
        );
    }

    if(daoLinks.isFetching || !daoLinks.data?.data.items || daoLinks.data.data.items.length === 0) {
        return (
            <div>
            </div>
        );
    }

    const centerPanel = daoLinks.data.data.items.map(item => {
        return (<Row>
            <p>
                {item.linkType + ": "}
                <Button key="explorerLink" variant="link" onClick={() => handleOpenExplorer(item.aipId)}>
                    {item.name}
                </Button>
                {item.children && " komponenty: " + item.children.length}
                {item.linkType === "Komponenta" && <Button key="detail" variant="action">
                    <Icon glyph="fa-eye" />
                </Button>}
                <Button key="deleteLink" variant="action" onClick={() => handleDeleteLink(item.daoLinkId)}>
                    <Icon glyph="fa fa-close" />
                </Button>
                {item.children && <Button key="expand" variant="action">
                    <Icon glyph="fa fa-chevron-down" />
                </Button>}
            </p>
        </Row>);
    });

    return (
        <div className="napojeni">
            <p>
                <b>Napojení</b>
                <Button key="expand" variant="action">
                    <Icon glyph="fa fa-chevron-up" />
                </Button>
            </p>
            {centerPanel}
        </div>
    );
}


export default DaoLinkDetail;
