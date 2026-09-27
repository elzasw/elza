import { Toolbar, ToolbarButton, Tooltip } from "@fluentui/react-components";
import { LinkMultipleRegular, LinkRegular } from "@fluentui/react-icons";
import { defineMessages, useIntl } from "react-intl";
import { useSelector } from "react-redux";
import { daoMessages } from "components/arr/daoMessages";
import { useHistory } from "react-router-dom";
import { AREA_AIP, AREA_AIPS, AREA_SELECTED_AIPS } from "actions/aip/aip";
import { storeFromArea } from "shared/utils";
import { AipDetailVO } from "elza-api";
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
    readMode: boolean;
}

export type AipFundActionsProps = Props;

/**
 * Akce nad seznamem balíčků archivního souboru, zobrazené v liště nad seznamem.
 *
 * Hromadné připojení pracuje s vybranými balíčky, a když není nic vybráno, s balíčky zobrazenými
 * na stránce seznamu - popisek říká, o které jde a kolik jich je; otevře stránku připojení. Připojení jednotlivě pracuje
 * s jedním balíčkem: jediným vybraným, jinak s tím, který je otevřený v detailu - otevře jeho
 * průzkumník na kartě připojení.
 */
export function AipFundActions({ fund, readMode }: Props) {
    const intl = useIntl();
    const history = useHistory();
    const selectedAips = useSelector((state: AppState) => storeFromArea(state, AREA_SELECTED_AIPS));
    const aips = useSelector((state: AppState) => storeFromArea(state, AREA_AIPS));
    const openAip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));

    const selected: AipDetailVO[] = selectedAips?.rows ?? [];
    const shown: AipDetailVO[] = aips?.rows ?? [];
    const bulkTargets = selected.length > 0 ? selected : shown;
    const singleAipId: number | undefined = selected.length === 1
        ? selected[0].aipId
        : selected.length === 0 ? openAip?.id : undefined;

    const version = getFundVersion(fund as unknown as Fund);

    const handleConnectBulk = () => {
        history.push(urlFundAipConnect(fund.id, bulkTargets.map(a => a.aipId), version));
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
