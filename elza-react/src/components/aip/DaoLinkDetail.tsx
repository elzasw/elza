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
import {DaDaoType, DaoLink, DaoViewRequestVO} from "elza-api";
import CrossTabHelper, {CrossTabEventType, getThisLayout} from "../CrossTabHelper.tsx";
import {WebApi} from "../../actions";

type DaoLinkDetailProps = {
    nodeId: number;
}

const DaoLinkDetail = ({nodeId}: DaoLinkDetailProps) => {
    const daoLinks = useSelector((state: AppState) => storeFromArea(state, AREA_DAO_LINKS));
    const dispatch = useThunkDispatch();
    const [collapsed, setCollapsed] = useState<boolean>(false);
    const [openItems, setOpenItems] = useState<string[]>([]);
    const [showAllChildren, setShowAllChildren] = useState<string[]>([]);

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

    const handleOpenExplorer = (aipId: number, daoCode?: string) => {
        dispatch(aipActions.selectAip(aipId));
        dispatch(
            modalDialogShow(
                this,
                "AIP Průzkumník",
                <AipExplorerModalWrapper
                    //@ts-ignore
                    onOk={() => dispatch(modalDialogHide())}
                    mode={ExplorerMode.VIEW}
                    selected={daoCode}
                />,
                "aip-explorer"
            ),
        );
    }

    const handleOpenChange = (value: string, close: boolean) => {
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

    const handleShowAllChange = (value: string, close: boolean) => {
        const showAll = [...showAllChildren];
        if (close) {
            setShowAllChildren(prev => prev.filter(item => item !== value))
        } else {
            if (!showAll.includes(value)) {
                showAll.push(value)
            }
            setShowAllChildren(showAll);
        }
    }

    const handleOpenComponent = (daoId: number) => {
        const thisLayout = getThisLayout();

        WebApi.getDaoViewRequestInfo(daoId).then((result: DaoViewRequestVO) => {
                if (thisLayout) {
                    CrossTabHelper.sendEvent(
                        thisLayout, {
                            type: CrossTabEventType.DISPLAY_COMPONENT,
                            data: {
                                viewUrl: result.viewUrl,
                                request: {
                                    type: "ViewRequest",
                                    daoId: result.daoId,
                                    entityRef: result.entityRef
                                }
                            }}
                    );
                }
            }
        )
    }

    if(daoLinks.isFetching || !daoLinks.data?.data.items || daoLinks.data.data.items.length === 0) {
        return (
            <div>
            </div>
        );
    }

    const renderLinkDetail = (item: DaoLink) => {
        const openItem = openItems.includes(item.daoLinkUuid);

        return (<p>
            {DaDaoTypeCaption(item.daoType) + ": "}
            <Button key="explorerLink" variant="link" onClick={() => handleOpenExplorer(item.aipId, item.daoCode)}>
                {item.name}
            </Button>
            {item.children && " komponenty: " + item.children.length}
            {item.daoType === DaDaoType.File && <Button key="detail" variant="action" onClick={() => handleOpenComponent(item.daoId)}>
                <Icon glyph="fa-eye"/>
            </Button>}
            {item.daoLinkId && <Button key="deleteLink" variant="action" onClick={() => handleDeleteLink(item.daoLinkId)}>
                <Icon glyph="fa fa-close"/>
            </Button>}
            {item.children && <Button key="expand" variant="action" onClick={() => handleOpenChange(item.daoLinkUuid, openItem)}>
                {openItem && <Icon glyph="fa fa-chevron-up"/>}
                {!openItem && <Icon glyph="fa fa-chevron-down"/>}
            </Button>}
        </p>);
    }

    const renderChildrenLinks = (item: DaoLink) => {
        let count = -1;
        const maxCount = 5;
        const showAll = showAllChildren.includes(item.daoLinkUuid);

        return (item.children.map(child => {
            const openItem = openItems.includes(child.daoLinkUuid);
            count = count + 1;
            let skryt;

            if (!showAll) {
                if (count > maxCount) {
                    return;
                } else if (count === maxCount && item.children.length > maxCount) {
                    return (
                        <div key={'dao-link-div' + child.daoLinkUuid + "dalsi"}>
                            <Row className="napojeni-row-child" key={'dao-link-row' + child.daoLinkUuid + "dalsi"}>
                                <p>
                                    <Button key="showAll" variant="link" onClick={() => handleShowAllChange(item.daoLinkUuid, showAll)}>
                                        a {item.children.length - maxCount} dalších...
                                    </Button>
                                </p>
                            </Row>
                        </div>
                    );
                }
            }

            if (showAll && item.children.length > maxCount && item.children.length === (count + 1)) {
                skryt = (
                    <div key={'dao-link-div' + child.daoLinkUuid + "skryt"}>
                        <Row className="napojeni-row-child" key={'dao-link-row' + child.daoLinkUuid + "skryt"}>
                            <p>
                                <Button key="hideAll" variant="link" onClick={() => handleShowAllChange(item.daoLinkUuid, showAll)}>
                                    Skrýt
                                </Button>
                            </p>
                        </Row>
                    </div>
                )
            }

            return (
                <div key={'dao-link-div' + child.daoLinkUuid}>
                    <Row className="napojeni-row-child" key={'dao-link-row' + child.daoLinkUuid}>
                        {renderLinkDetail(child)}
                    </Row>
                    {child.children && openItem && renderChildrenLinks(child)}
                    {skryt}
                </div>
            );
        }));
    }

    const centerPanel = daoLinks.data.data.items.map(item => {
        const openItem = openItems.includes(item.daoLinkUuid);

        return (
            <div key={'dao-link-div' + item.daoLinkUuid}>
                <Row className="napojeni-row" key={'dao-link-row' + item.daoLinkUuid}>
                    {renderLinkDetail(item)}
                </Row>
                {item.children && openItem && renderChildrenLinks(item)}
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
