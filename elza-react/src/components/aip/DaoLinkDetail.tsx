import "./DaoLinkDetail.scss";
import {AREA_DAO_LINKS, daoLinksFetchIfNeeded} from "actions/aip/aip";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import {useEffect, useState} from "react";
import {useThunkDispatch} from "../../utils/hooks";
import {Button, Row} from "react-bootstrap";
import {Icon} from "../shared";
import {Api} from "../../api";
import {modalDialogHide, modalDialogShow} from "../../actions/global/modalDialog";
import AipExplorerModalWrapper from "./explorer/AipExplorerWrapper.tsx";
import {ExplorerMode} from "./explorer/ExplorerContext.tsx";
import * as aipActions from "../../actions/aip/aip.ts";
import {DaDaoTypeCaption} from "../../api/DaDaoType.ts";
import {DaDaoType, DaoLink} from "elza-api";

type DaoLinkDetailProps = {
    nodeId: number;
}

const DaoLinkDetail = ({nodeId}: DaoLinkDetailProps) => {
    const daoLinks = useSelector((state: AppState) => storeFromArea(state, AREA_DAO_LINKS));
    const dispatch = useThunkDispatch();
    const [collapsed, setCollapsed] = useState<boolean>(false);
    const [openItems, setOpenItems] = useState<number[]>([]);

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

    const handleOpenChange = (value: number, close: boolean) => {
        const opened = [...openItems];
        if (close) {
            setOpenItems(prev => prev.filter(item => item !== value))
        } else {
            if (!opened.includes(value)) {
                opened.push(value)
            }
            setOpenItems(opened);
        }
    }

    if(daoLinks.isFetching || !daoLinks.data?.data.items || daoLinks.data.data.items.length === 0) {
        return (
            <div>
            </div>
        );
    }

    const renderLinkDetail = (item: DaoLink) => {
        const openItem = openItems.includes(item.daoLinkId);

        return (<p>
            {DaDaoTypeCaption(item.daoType) + ": "}
            <Button key="explorerLink" variant="link" onClick={() => handleOpenExplorer(item.aipId)}>
                {item.name}
            </Button>
            {item.children && " komponenty: " + item.children.length}
            {item.daoType === DaDaoType.File && <Button key="detail" variant="action">
                <Icon glyph="fa-eye"/>
            </Button>}
            <Button key="deleteLink" variant="action" onClick={() => handleDeleteLink(item.daoLinkId)}>
                <Icon glyph="fa fa-close"/>
            </Button>
            {item.children && <Button key="expand" variant="action" onClick={() => handleOpenChange(item.daoLinkId, openItem)}>
                {openItem && <Icon glyph="fa fa-chevron-up"/>}
                {!openItem && <Icon glyph="fa fa-chevron-down"/>}
            </Button>}
        </p>);
    }

    const renderChildrenLinks = (items: DaoLink[]) => {
        return (items.map(child => {
            const openItem = openItems.includes(child.daoLinkId);

            return (
                <div key={'dao-link-div' + child.daoLinkId}>
                    <Row className="napojeni-row-child" key={'dao-link-row' + child.daoLinkId}>
                        {renderLinkDetail(child)}
                    </Row>
                    {child.children && openItem && renderChildrenLinks(child.children)}
                </div>
            );
        }));
    }

    const centerPanel = daoLinks.data.data.items.map(item => {
        const openItem = openItems.includes(item.daoLinkId);

        return (
            <div key={'dao-link-div' + item.daoLinkId}>
                <Row className="napojeni-row" key={'dao-link-row' + item.daoLinkId}>
                    {renderLinkDetail(item)}
                </Row>
                {item.children && openItem && renderChildrenLinks(item.children)}
            </div>
        );
    });

    return (
        <div className="napojeni">
            <p>
                <b>Napojení</b>
                <Button key="expand" variant="action" onClick={() => setCollapsed(!collapsed)}>
                    {collapsed && <Icon glyph="fa fa-chevron-down"/>}
                    {!collapsed && <Icon glyph="fa fa-chevron-up"/>}
                </Button>
            </p>
            {!collapsed && centerPanel}
        </div>
    );
}


export default DaoLinkDetail;
