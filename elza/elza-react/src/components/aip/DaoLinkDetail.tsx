import "./DaoLinkDetail.scss";
import { AREA_DAO_LINKS, daoLinksFetchIfNeeded } from "actions/aip/aip";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { useEffect, useState } from "react";
import { useThunkDispatch } from "../../utils/hooks";
import { Button } from "react-bootstrap";
import { Link } from "react-router-dom";
import { Icon } from "../shared";
import { FormattedMessage, defineMessages, useIntl } from "react-intl";
import { globalMessages } from "components/shared/lang";

// Id je převzaté z legacy katalogu beze změny. ConfirmForm chce řetězce,
// proto formatMessage a ne FormattedMessage.
const messages = defineMessages({
    deleteLink: { id: "arr.aip.dao.link.delete", defaultMessage: "Opravdu chcete smazat napojení?" },
});
import { Api } from "../../api";
import { modalDialogHide, modalDialogShow } from "../../actions/global/modalDialog";
import { getFundVersion, urlAipExplorer, urlFundAipExplorer } from "../../constants";
import type { Fund } from "typings/store";
import { daoTypeMessages, levelMessages } from "./messages";
import { AipLevelType, DaDaoType, DaoLink, DaoViewRequestVO } from "elza-api";
import CrossTabHelper, { CrossTabEventType, getThisLayout } from "../CrossTabHelper";
import { WebApi } from "../../actions";
import ConfirmForm from "../shared/form/ConfirmForm";
import { daoLinkMessages } from "./messages";

/** Kolik napojení se vypíše, než se zbytek schová za "a N dalších…". */
const MAX_VISIBLE_LINKS = 5;

type DaoLinkDetailProps = {
    nodeId: number;
    /**
     * In a closed version nothing can be unlinked - but what the unit hangs on is worth reading
     * there just as much, so the block only drops the actions that would change the data.
     */
    readOnly?: boolean;
}

const DaoLinkDetail = ({nodeId, readOnly = false}: DaoLinkDetailProps) => {
    const intl = useIntl();
    const daoLinks = useSelector((state: AppState) => storeFromArea(state, AREA_DAO_LINKS));
    const dispatch = useThunkDispatch();
    const [collapsed, setCollapsed] = useState<boolean>(false);
    const [openItems, setOpenItems] = useState<string[]>([]);
    const [showAllMainLinks, setShowAllMainLinks] = useState<boolean>();
    const fund = useSelector((state: AppState) =>
        state.arrRegion?.funds?.[state.arrRegion.activeIndex ?? -1]) as Fund | undefined;

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

    /**
     * Průzkumník balíčku jako stránka archivního souboru, na kartě struktury s napojenou částí
     * vybranou. Je to odkaz, takže jde otevřít i v nové záložce prohlížeče.
     */
    const explorerUrl = (aipId: number, daoCode?: string) => fund
        ? urlFundAipExplorer(fund.id, aipId, getFundVersion(fund), "structure", daoCode)
        : urlAipExplorer(aipId);

    const handleOpenChange = (value: string, close: boolean) => {
        setOpenItems(prev => close ? prev.filter(item => item !== value)
                                   : prev.includes(value) ? prev : [...prev, value]);
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
        return null;
    }

    /**
     * One link on one line: what it attaches, the name it attaches, how much hangs below it and
     * the actions - in that order and in one row, so a list of links reads as a column.
     */
    const renderLink = (item: DaoLink, canDelete: boolean) => {
        const openItem = openItems.includes(item.daoLinkUuid);
        const hasChildren = item.childrenCount != null && item.childrenCount > 0;
        // Napojení bez typu zůstává u původního znění - popisuje celý balíček.
        const label = item.path
            ?? intl.formatMessage((item.daoType && daoTypeMessages[item.daoType])
                                  || levelMessages[AipLevelType.Package]) + ":";

        return (
            <div className="dao-link">
                <span className="dao-link-label" title={label}>{label}</span>
                <Link className="btn btn-link dao-link-name" to={explorerUrl(item.aipId, item.daoCode)}
                      title={intl.formatMessage(daoLinkMessages.openInExplorer)}>
                    {item.name}
                </Link>
                {hasChildren &&
                    <span className="dao-link-count">
                        <FormattedMessage {...daoLinkMessages.components}
                                          values={{ count: item.childrenCount }} />
                    </span>}
                <span className="dao-link-actions">
                    {item.daoType === DaDaoType.File &&
                        <Button variant="action" title={intl.formatMessage(daoLinkMessages.showComponent)}
                                onClick={() => handleOpenComponent(item.daoId)}>
                            <Icon glyph="fa-eye"/>
                        </Button>}
                    {canDelete && item.daoLinkId &&
                        <Button variant="action" title={intl.formatMessage(globalMessages.delete)}
                                onClick={() => handleDeleteLink(item.daoLinkId)}>
                            <Icon glyph="fa fa-close"/>
                        </Button>}
                    {hasChildren &&
                        <Button variant="action"
                                title={intl.formatMessage(openItem ? daoLinkMessages.collapseItem
                                                                   : daoLinkMessages.expandItem)}
                                onClick={() => handleOpenChange(item.daoLinkUuid, openItem)}>
                            <Icon glyph={openItem ? "fa fa-chevron-up" : "fa fa-chevron-down"}/>
                        </Button>}
                </span>
            </div>
        );
    }

    const renderLinkTree = (item: DaoLink, canDelete: boolean) => (
        <div key={item.daoLinkUuid}>
            {renderLink(item, canDelete)}
            {item.children && openItems.includes(item.daoLinkUuid) && renderChildrenLinks(item)}
        </div>
    );

    /**
     * Components of one link. The server sends only a page of them, so a link whose components do
     * not all fit offers the explorer, which is where the rest of the package is browsed.
     */
    const renderChildrenLinks = (item: DaoLink) => (
        <div className="dao-link-children">
            {item.children.map(child => renderLinkTree(child, false))}
            {item.children.length < item.childrenCount &&
                <div className="dao-link-more">
                    <Link className="btn btn-link" to={explorerUrl(item.aipId, item.daoCode)}>
                        <FormattedMessage {...daoLinkMessages.showInExplorer} />
                    </Link>
                </div>}
        </div>
    );

    const items: Array<DaoLink> = daoLinks.data.data.items;
    const visibleItems = showAllMainLinks ? items : items.slice(0, MAX_VISIBLE_LINKS);

    return (
        <div className="dao-links">
            <button type="button" className="dao-links-header"
                    aria-expanded={!collapsed}
                    title={intl.formatMessage(collapsed ? daoLinkMessages.showLinks
                                                        : daoLinkMessages.hideLinks)}
                    onClick={() => setCollapsed(!collapsed)}>
                <Icon glyph={collapsed ? "fa fa-chevron-down" : "fa fa-chevron-up"}/>
                <FormattedMessage {...daoLinkMessages.title} />
            </button>
            {!collapsed &&
                <div className="dao-links-list">
                    {visibleItems.map(item => renderLinkTree(item, !readOnly))}
                    {items.length > MAX_VISIBLE_LINKS &&
                        <div className="dao-link-more">
                            <Button variant="link" onClick={() => setShowAllMainLinks(!showAllMainLinks)}>
                                {showAllMainLinks
                                    ? <FormattedMessage {...daoLinkMessages.hide} />
                                    : <FormattedMessage {...daoLinkMessages.showMore}
                                                        values={{ count: items.length - MAX_VISIBLE_LINKS }} />}
                            </Button>
                        </div>}
                </div>}
        </div>
    );
}


export default DaoLinkDetail;
