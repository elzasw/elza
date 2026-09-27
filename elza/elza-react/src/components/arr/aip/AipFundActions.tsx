import { Toolbar, ToolbarButton, Tooltip } from "@fluentui/react-components";
import { LinkAddRegular, LinkMultipleRegular, LinkRegular } from "@fluentui/react-icons";
import { defineMessages, useIntl } from "react-intl";
import { useSelector } from "react-redux";
import { daoMessages } from "components/arr/daoMessages";
import { useHistory } from "react-router-dom";
import { AREA_AIP, AREA_AIPS, AREA_SELECTED_AIPS, aipsFetchIfNeeded } from "actions/aip/aip";
import { modalDialogHide, modalDialogShow } from "actions/global/modalDialog";
import { useThunkDispatch } from "utils/hooks";
import { useWebsocket } from "components/shared/web-socket/WebsocketProvider";
import FundNodesSelectForm from "components/arr/FundNodesSelectForm";
import { Api } from "../../../api";
import { runAipAction } from "../../aip/AipActionRunner";
import { storeFromArea } from "shared/utils";
import { AipDetailVO, AipLinkState } from "elza-api";
import { addToastrWarning } from "components/shared/toastr/ToastrActions";
import { AppState } from "typings/store";
import { getFundVersion, urlFundAipConnect, urlFundAipExplorer } from "../../../constants";
import type { Fund } from "typings/store";

const messages = defineMessages({
    connectSelected: { id: "arr.aip.actions.connectSelected", defaultMessage: "Připojit vybrané ({count})" },
    connectShown: { id: "arr.aip.actions.connectShown", defaultMessage: "Připojit zobrazené ({count})" },
    connectSelectedHint: {
        id: "arr.aip.actions.connectSelected.hint",
        defaultMessage: "Připojit vybrané balíčky k archivnímu popisu nebo z nich převzít popis",
    },
    connectShownHint: {
        id: "arr.aip.actions.connectShown.hint",
        defaultMessage: "Nic není vybráno - připojit balíčky zobrazené na této stránce seznamu",
    },
    connectWhole: { id: "arr.aip.actions.connectWhole", defaultMessage: "Připojit celé k JP…" },
    connectWholeHint: {
        id: "arr.aip.actions.connectWhole.hint",
        defaultMessage: "Rychle připojit {count, plural, one {# balíček} few {# balíčky} other {# balíčků}} celé k jednotce popisu vybrané v dialogu - bez procházení jejich struktury",
    },
    connectWholeTitle: {
        id: "arr.aip.actions.connectWhole.title",
        defaultMessage: "Připojit {count, plural, one {# balíček} few {# balíčky} other {# balíčků}} celé k jednotce popisu",
    },
    connectWholeSkipped: {
        id: "arr.aip.actions.connectWhole.skipped",
        defaultMessage: "{count, plural, one {# balíček je už připojen a vynechá se} few {# balíčky jsou už připojeny a vynechají se} other {# balíčků je už připojeno a vynechá se}}",
    },
    connectOneHint: {
        id: "arr.aip.actions.connectOne.hint",
        defaultMessage: "Připojit jeden balíček nebo jeho vybrané části",
    },
    connectOneDisabled: {
        id: "arr.aip.actions.connectOne.disabled",
        defaultMessage: "Vyberte v seznamu právě jeden balíček",
    },
});

interface Props {
    /** Archivní soubor stránky - připojuje se k jeho popisu. */
    fund: { id: number };
    /** Archivní soubor nelze měnit - režim čtení nebo uzavřená verze. */
    readMode: boolean;
}

export type AipFundActionsProps = Props;

/**
 * Akce nad seznamem balíčků archivního souboru, zobrazené v liště nad seznamem.
 *
 * Hromadné připojení pracuje s vybranými balíčky, a když není nic vybráno, s balíčky zobrazenými
 * na stránce seznamu - popisek říká, o které jde a kolik jich je; otevře stránku připojení.
 * Připojení jednotlivě pracuje s jedním balíčkem: jediným vybraným, jinak s tím, který je
 * otevřený v detailu - otevře jeho průzkumník na kartě připojení.
 *
 * Nejčastější případ - celé balíčky k jedné jednotce popisu - má rychlou cestu: výběr jednotky
 * v dialogu a hned připojení, bez odchodu ze seznamu. Plně napojené balíčky se nenabízejí a ty,
 * které server celé nepřipojí (už napojené), se vynechají s upozorněním.
 */
export function AipFundActions({ fund, readMode }: Props) {
    const intl = useIntl();
    const history = useHistory();
    const dispatch = useThunkDispatch();
    const websocket = useWebsocket();
    const selectedAips = useSelector((state: AppState) => storeFromArea(state, AREA_SELECTED_AIPS));
    const aips = useSelector((state: AppState) => storeFromArea(state, AREA_AIPS));
    const openAip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));

    const selected: AipDetailVO[] = selectedAips?.rows ?? [];
    const shown: AipDetailVO[] = aips?.rows ?? [];
    const bulkTargets = selected.length > 0 ? selected : shown;
    const wholeTargets = bulkTargets.filter(a => a.linkState !== AipLinkState.FullyLinked);
    const singleAipId: number | undefined = selected.length === 1
        ? selected[0].aipId
        : selected.length === 0 ? openAip?.id : undefined;

    const version = getFundVersion(fund as unknown as Fund);

    const handleConnectBulk = () => {
        history.push(urlFundAipConnect(fund.id, bulkTargets.map(a => a.aipId), version));
    };

    const handleConnectWhole = () => {
        const aipIds = wholeTargets.map(a => a.aipId);
        const title = intl.formatMessage(messages.connectWholeTitle, { count: aipIds.length });

        const connect = async (nodeId: number) => {
            dispatch(modalDialogHide());
            // napojené balíčky server celé nepřipojí - vynechají se hned, ne až chybou akce
            const blocked = (await Api.aips.aipConnectCheck(nodeId, aipIds)).data.blocked ?? [];
            const allowed = aipIds.filter(id => !blocked.some(b => b.aipId === id));
            if (blocked.length > 0) {
                dispatch(addToastrWarning(intl.formatMessage(messages.connectWholeSkipped, { count: blocked.length })));
            }
            if (allowed.length > 0) {
                await runAipAction(dispatch, intl, websocket, title,
                                   () => Api.aips.aipBulkConnectToJp(nodeId, allowed),
                                   () => dispatch(aipsFetchIfNeeded(true)));
            }
        };

        dispatch(modalDialogShow(null, title,
            <FundNodesSelectForm multipleSelection={false} onSubmitForm={(nodeId: number) => { connect(nodeId); }} />));
    };

    const handleConnectOne = () => {
        if (singleAipId == null) {
            return;
        }
        history.push(urlFundAipExplorer(fund.id, singleAipId, version, "connect"));
    };

    const bulkLabel = selected.length > 0
        ? intl.formatMessage(messages.connectSelected, { count: selected.length })
        : intl.formatMessage(messages.connectShown, { count: shown.length });
    const bulkHint = intl.formatMessage(selected.length > 0 ? messages.connectSelectedHint : messages.connectShownHint);
    const oneHint = intl.formatMessage(singleAipId != null ? messages.connectOneHint : messages.connectOneDisabled);

    return (
        <Toolbar size="small" aria-label={intl.formatMessage(daoMessages.aipAssignmentBulk)}>
            <Tooltip content={bulkHint} relationship="description">
                <ToolbarButton appearance="primary" icon={<LinkMultipleRegular />}
                               disabledFocusable={readMode || bulkTargets.length === 0}
                               onClick={handleConnectBulk}>
                    {bulkLabel}
                </ToolbarButton>
            </Tooltip>
            <Tooltip content={intl.formatMessage(messages.connectWholeHint, { count: wholeTargets.length })}
                     relationship="description">
                <ToolbarButton icon={<LinkAddRegular />}
                               disabledFocusable={readMode || wholeTargets.length === 0}
                               onClick={handleConnectWhole}>
                    {intl.formatMessage(messages.connectWhole)}
                </ToolbarButton>
            </Tooltip>
            <Tooltip content={oneHint} relationship="description">
                <ToolbarButton icon={<LinkRegular />}
                               disabledFocusable={readMode || singleAipId == null}
                               onClick={handleConnectOne}>
                    {intl.formatMessage(daoMessages.aipAssignmentIndividually)}
                </ToolbarButton>
            </Tooltip>
        </Toolbar>
    );
}
