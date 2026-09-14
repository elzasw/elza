import "./DaoLinkDetail.scss";
import {AREA_DAO_LINKS, daoLinksFetchIfNeeded} from "actions/aip/aip";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import {useEffect, useState} from "react";
import {useThunkDispatch} from "../../utils/hooks";
import {Button, Row} from "react-bootstrap";
import { Icon } from "../shared";
import { FormattedMessage, defineMessages, useIntl } from "react-intl";
import { globalMessages } from "components/shared/lang";

// Id je převzaté z legacy katalogu beze změny. ConfirmForm chce řetězce,
// proto formatMessage a ne FormattedMessage.
const messages = defineMessages({
    deleteLink: { id: "arr.aip.dao.link.delete", defaultMessage: "Opravdu chcete smazat napojení?" },
});
import {Api} from "../../api";
import {modalDialogHide, modalDialogShow} from "../../actions/global/modalDialog";
import AipExplorerModalWrapper from "./explorer/AipExplorerWrapper.tsx";
import {ExplorerMode} from "./explorer/ExplorerContext.tsx";
import * as aipActions from "../../actions/aip/aip.ts";
import { daoTypeMessages, levelMessages } from "./messages";
import {AipLevelType, DaDaoType, DaoLink, DaoViewRequestVO} from "elza-api";
import CrossTabHelper, {CrossTabEventType, getThisLayout} from "../CrossTabHelper.tsx";
import {WebApi} from "../../actions";
import ConfirmForm from "../shared/form/ConfirmForm";
import { daoLinkMessages, explorerMessages } from "./messages";

type DaoLinkDetailProps = {
    nodeId: number;
}

const DaoLinkDetail = ({nodeId}: DaoLinkDetailProps) => {
    const intl = useIntl();
    const daoLinks = useSelector((state: AppState) => storeFromArea(state, AREA_DAO_LINKS));
    const dispatch = useThunkDispatch();
    const [collapsed, setCollapsed] = useState<boolean>(false);
    const [openItems, setOpenItems] = useState<string[]>([]);
    const [showAllMainLinks, setShowAllMainLinks] = useState<boolean>();

    useEffect(() => {
        dispatch(daoLinksFetchIfNeeded(nodeId, true));
    },[
        nodeId,
        dispatch,
    ]);

    const handleDeleteLink = (linkId: number) => {
        const confirmForm = (
            <ConfirmForm
                //@ts-ignore
                confirmMessage={intl.formatMessage(messages.deleteLink)}
                submittingMessage={intl.formatMessage(messages.deleteLink)}
                submitTitle={intl.formatMessage(globalMessages.delete)}
                onSubmit={() => {
                    return Api.aips.aipDeleteDaoLink(linkId)
                }}
                onSubmitSuccess={() => {
                    dispatch(modalDialogHide());
                    dispatch(daoLinksFetchIfNeeded(nodeId, true))
                }}
            />
        );
        dispatch(modalDialogShow(this, null, confirmForm));
    }

    const handleOpenExplorer = (aipId: number, daoCode?: string) => {
        dispatch(aipActions.selectAip(aipId));
        dispatch(
            modalDialogShow(
                this,
                intl.formatMessage(explorerMessages.title),
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

    const renderLinkDetail = (item: DaoLink, deleteLink: boolean) => {
        const openItem = openItems.includes(item.daoLinkUuid);

        return (<p>
            {/* Neznámý typ zůstává u původního znění - popisuje celý balíček. */}
            {item.path ? item.path + " "
                : intl.formatMessage(daoTypeMessages[item.daoType] ?? levelMessages[AipLevelType.Package]) + ": "}
            <Button key="explorerLink" variant="link" onClick={() => handleOpenExplorer(item.aipId, item.daoCode)}>
                {item.name}
            </Button>
            {item.childrenCount != null && item.childrenCount > 0 && " komponenty: " + item.childrenCount}
            {item.daoType === DaDaoType.File && <Button key="detail" variant="action" onClick={() => handleOpenComponent(item.daoId)}>
                <Icon glyph="fa-eye"/>
            </Button>}
            {deleteLink && item.daoLinkId && <Button key="deleteLink" variant="action" onClick={() => handleDeleteLink(item.daoLinkId)}>
                <Icon glyph="fa fa-close"/>
            </Button>}
            {item.childrenCount != null && item.childrenCount > 0 && <Button key="expand" variant="action" onClick={() => handleOpenChange(item.daoLinkUuid, openItem)}>
                {openItem && <Icon glyph="fa fa-chevron-up"/>}
                {!openItem && <Icon glyph="fa fa-chevron-down"/>}
            </Button>}
        </p>);
    }

    const renderMainLinks = (items: Array<DaoLink>) => {
        let count = -1;
        const maxCount = 5;

        return (items.map(item => {
            const openItem = openItems.includes(item.daoLinkUuid);
            count = count + 1;
            let skryt;

            if (!showAllMainLinks) {
                if (count > maxCount) {
                    return;
                } else if (count === maxCount && items.length > maxCount) {
                    return (
                        <div key={'dao-link-div' + item.daoLinkUuid + "dalsi"}>
                            <Row className="napojeni-row" key={'dao-link-row' + item.daoLinkUuid + "dalsi"}>
                                <p>
                                    <Button key="showAll" variant="link" onClick={() => setShowAllMainLinks(!showAllMainLinks)}>
                                        <FormattedMessage {...daoLinkMessages.showMore} values={{ count: items.length - maxCount }} />
                                    </Button>
                                </p>
                            </Row>
                        </div>
                    );
                }
            }

            if (showAllMainLinks && items.length > maxCount && items.length === (count + 1)) {
                skryt = (
                    <div key={'dao-link-div' + item.daoLinkUuid + "skryt"}>
                        <Row className="napojeni-row" key={'dao-link-row' + item.daoLinkUuid + "skryt"}>
                            <p>
                                <Button key="hideAll" variant="link" onClick={() => setShowAllMainLinks(!showAllMainLinks)}>
                                    <FormattedMessage {...daoLinkMessages.hide} />
                                </Button>
                            </p>
                        </Row>
                    </div>
                )
            }

            return (
                <div key={'dao-link-div' + item.daoLinkUuid}>
                    <Row className="napojeni-row" key={'dao-link-row' + item.daoLinkUuid}>
                        {renderLinkDetail(item, true)}
                    </Row>
                    {item.children && openItem && renderChildrenLinks(item)}
                    {skryt}
                </div>
            );
        }));
    }

    const renderChildrenLinks = (item: DaoLink) => {
        let count = -1;

        return (item.children.map(child => {
            const openItem = openItems.includes(child.daoLinkUuid);
            count = count + 1;
            let zobrazitVse;

            if (item.children.length < item.childrenCount && item.children.length === (count + 1)) {
                zobrazitVse = (
                    <div key={'dao-link-div' + item.daoLinkUuid + "zobrazit-vse"}>
                        <Row className="napojeni-row-child" key={'dao-link-row' + item.daoLinkUuid + "zobrazit-vse"}>
                            <p>
                                <Button key="hideAll" variant="link" onClick={() => handleOpenExplorer(item.aipId, null)}>
                                    <FormattedMessage {...daoLinkMessages.showInExplorer} />
                                </Button>
                            </p>
                        </Row>
                    </div>
                )
            }

            return (
                <div className="napojeni-div-child" key={'dao-link-div' + child.daoLinkUuid}>
                    <Row className="napojeni-row-child" key={'dao-link-row' + child.daoLinkUuid}>
                        {renderLinkDetail(child, false)}
                    </Row>
                    {child.children && openItem && renderChildrenLinks(child)}
                    {zobrazitVse}
                </div>
            );
        }));
    }

    const centerPanel = renderMainLinks(daoLinks.data.data.items);

    return (
        <div className="napojeni">
            <p>
                <b><FormattedMessage {...daoLinkMessages.title} /></b>
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
