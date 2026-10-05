import {
    Button,
    Checkbox,
    Label,
    Radio,
    RadioGroup,
    Text,
} from "@fluentui/react-components";
import "./AipConnectPanel.scss";
import { defineMessages, useIntl } from 'react-intl';
import AipsLogicalContainer, { LogicalTreeNode } from "./AipsLogicalContainer";
import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { AIP_LOGICAL_TREE, aipsFetchIfNeeded, fetchAipLogicalTreeIfNeeded } from "actions/aip/aip";
import { useThunkDispatch } from "utils/hooks";
import { useWebsocket } from "components/shared/web-socket/WebsocketProvider";
import { AipDetailVO, AipLinkState, DaAipActionVO } from "elza-api";
import { Api } from "../../../../api";
import { WebApi } from "../../../../actions";
import AipConnectBlockedPanel from "../../../aip/AipConnectBlockedPanel";
import { runAipAction } from "../../../aip/AipActionRunner";
import { useNodeName } from "components/aip/explorer/levels";
import { AipTargetTree, TargetNode, expandTarget, useAipTarget } from "./AipTargetTree";
import { ModeLabel, connectMessages, useConnectCheck } from "./connectShared";

const messages = defineMessages({
    selectedCount: {
        id: "arr.aip.assignment.selectedCount",
        defaultMessage: "{count, plural, one {Vybrán # balíček} few {Vybrány # balíčky} other {Vybráno # balíčků}}",
    },
    showPackages: { id: "arr.aip.assignment.showPackages", defaultMessage: "Zobrazit balíčky vybrané úrovně" },
    hidePackages: { id: "arr.aip.assignment.hidePackages", defaultMessage: "Skrýt balíčky" },
    source: { id: "arr.aip.assignment.source", defaultMessage: "Zdroj - logická struktura balíčků" },
    modeWhole: { id: "arr.aip.assignment.mode.whole", defaultMessage: "Celé balíčky ({count})" },
    modeWholeHint: {
        id: "arr.aip.assignment.mode.whole.hint",
        defaultMessage: "Připojí celé balíčky k vybrané jednotce popisu, bez ohledu na jejich strukturu.",
    },
    modeLevel: { id: "arr.aip.assignment.mode.level", defaultMessage: "Vybranou úroveň „{level}“" },
    modeLevelHint: {
        id: "arr.aip.assignment.mode.level.hint",
        defaultMessage: "Připojí vybranou úroveň balíčků k vybrané jednotce popisu; nižší úrovně jsou připojeny s ní.",
    },
    needsLevel: {
        id: "arr.aip.assignment.needsLevel",
        defaultMessage: "Vyberte ve struktuře úroveň pod kořenem - kořen jsou celé balíčky.",
    },
    summary: {
        id: "arr.aip.assignment.summary",
        defaultMessage: "{count, plural, one {# balíček} few {# balíčky} other {# balíčků}} → „{target}“",
    },
    allLinked: { id: "arr.aip.assignment.allLinked", defaultMessage: "Všechny balíčky jsou plně připojeny." },
    noPackages: { id: "arr.aip.assignment.noPackages", defaultMessage: "Žádný z požadovaných balíčků nebyl nalezen." },
    loading: { id: "arr.aip.assignment.loading", defaultMessage: "Načítání balíčků…" },
});

/** Způsob připojení, který uživatel zvolil. */
type Mode = "whole" | "level" | "sublevels" | "structure";

interface Props {
    /** Balíčky, které uživatel vybral v seznamu (nebo zobrazené, když nevybral žádný). */
    aipIds: number[];
    /** Archivní soubor nelze měnit (režim čtení, uzavřená verze) - připojit nelze. */
    readOnly?: boolean;
}

export type AipBulkConnectPanelProps = Props;

/**
 * Hromadné připojení balíčků k archivnímu popisu jako stránka: vlevo logická struktura balíčků
 * (zdroj), uprostřed strom archivního souboru (cíl), vpravo volba, co se připojí, a jedno
 * tlačítko Připojit.
 *
 * Celé balíčky se připojují všechny, které stránka dostala; ostatní způsoby pracují s balíčky
 * vybrané úrovně. Vybraný kořen struktury znamená vrchol balíčků - celé balíčky.
 *
 * Po každé akci se balíčky načtou znovu (plně napojené ze stránky zmizí, ve stromu archivního
 * souboru jsou vidět nově vytvořené úrovně) a způsob připojení se zruší - další akce je nová volba.
 */
function AipBulkConnectPanel({ aipIds: requestedIds, readOnly = false }: Props) {
    const [details, setDetails] = useState<AipDetailVO[] | null>(null);
    const [reloads, setReloads] = useState(0);
    const [logicalTree, setLogicalTree] = useState<{ nodes: LogicalTreeNode[] } | null>(null);
    const [level, setLevel] = useState<LogicalTreeNode | null>(null);
    const [mode, setMode] = useState<Mode | null>("whole");
    const [fileplanAsRoot, setFileplanAsRoot] = useState(false);
    const [packagesShown, setPackagesShown] = useState(false);
    const [busy, setBusy] = useState(false);
    const structure = useSelector((state: AppState) => storeFromArea(state, AIP_LOGICAL_TREE));
    const { target } = useAipTarget();
    const dispatch = useThunkDispatch();
    const websocket = useWebsocket();
    const intl = useIntl();
    const nodeName = useNodeName();

    // stav balíčků ze serveru - stránka se dá otevřít i znovu načíst jen z adresy
    const requestedKey = requestedIds.join(",");
    useEffect(() => {
        let current = true;
        Promise.all(requestedIds.map(id => WebApi.getAip(id).catch(() => null)))
            .then(result => current && setDetails(result.filter((a): a is AipDetailVO => a != null)));
        return () => { current = false; };
    }, [requestedKey, reloads]);

    const aips = (details ?? []).filter(a => a.linkState !== AipLinkState.FullyLinked);
    const allAipIds = aips.map(a => a.aipId);
    const aipKey = allAipIds.join(",");
    // úroveň zastupuje balíčky, které mezitím mohly zmizet (plně napojené) - počítá se jen se zbylými
    const levelAipIds: number[] = level ? level.value.filter(id => allAipIds.includes(id)) : allAipIds;
    const levelViewId = level?.daLeveViewId;
    const aipIds = mode === "whole" ? allAipIds : levelAipIds;
    const blocked = useConnectCheck(target?.id, allAipIds, details);

    useEffect(() => {
        if (allAipIds.length > 0) {
            dispatch(fetchAipLogicalTreeIfNeeded(allAipIds, true));
        }
    }, [aipKey]);

    useEffect(() => {
        // struktura jiné sady balíčků (před znovunačtením) se nepoužije
        const forThese = (structure.id as number[] | undefined)?.join(",") === aipKey;
        if (allAipIds.length === 0) {
            setLogicalTree(null);
            setLevel(null);
        } else if (structure.data && forThese) {
            setLogicalTree(structure.data);
        }
    }, [structure, aipKey]);

    /** Po akci: balíčky (a s nimi stav napojení), seznam balíčků a cíl rozbalený na nové úrovně. */
    const reloadAfterAction = (targetNode: TargetNode, createsLevels: boolean) => {
        setReloads(r => r + 1);
        setMode(null);
        dispatch(aipsFetchIfNeeded(true));
        if (createsLevels) {
            dispatch(expandTarget(targetNode) as never);
        }
    };

    const allowed: Record<Mode, boolean> = {
        whole: true,
        level: levelViewId != null,
        sublevels: true,
        structure: true,
    };
    // napojené AIPy odmítne jen připojení, které se týká celého balíčku nebo úrovně - a jen ty, které jdou do akce
    const refused = (mode === "whole" || mode === "level") && blocked.some(b => aipIds.includes(b.aipId));
    const canConnect = !readOnly && !busy && !refused && mode != null && allowed[mode]
        && aipIds.length > 0 && target != null;

    const handleConnect = () => {
        if (!target || mode == null || !canConnect) {
            return;
        }
        const targetNode = target;
        const nodeId = targetNode.id;
        const request = (): Promise<{ data: DaAipActionVO }> => {
            switch (mode) {
                case "whole":
                    return Api.aips.aipBulkConnectToJp(nodeId, aipIds);
                case "level":
                    return Api.aips.aipBulkConnectLogicToJp(nodeId, aipIds, levelViewId!);
                case "sublevels":
                    return Api.aips.aipBulkCreateSublevels(nodeId, aipIds, levelViewId);
                case "structure":
                    return Api.aips.aipBulkImportDescription(nodeId, aipIds, levelViewId == null && fileplanAsRoot,
                                                             levelViewId);
            }
        };
        const createsLevels = mode === "sublevels" || mode === "structure";
        setBusy(true);
        runAipAction(dispatch, intl, websocket, intl.formatMessage(connectMessages.connect), request as never,
                     () => reloadAfterAction(targetNode, createsLevels))
            .catch(() => undefined)
            .finally(() => setBusy(false));
    };

    const levelLabel = level ? nodeName(level as never) : "";
    const shownPackages = aips.filter(a => levelAipIds.includes(a.aipId));
    const footer = readOnly ? intl.formatMessage(connectMessages.readOnly)
        : !target ? intl.formatMessage(connectMessages.noTarget)
        : mode == null ? intl.formatMessage(connectMessages.chooseMode)
        : intl.formatMessage(messages.summary, { count: aipIds.length, target: target.name });

    return (
        <div className="aip-connect">
            <div className="aip-connect-column">
                <div className="aip-connect-header">
                    <Label weight="semibold">{intl.formatMessage(messages.source)}</Label>
                </div>
                <div className="aip-connect-state">
                    <Text weight="semibold">
                        {details == null ? intl.formatMessage(messages.loading)
                            : details.length === 0 ? intl.formatMessage(messages.noPackages)
                            : aips.length > 0 ? intl.formatMessage(messages.selectedCount, { count: aips.length })
                            : intl.formatMessage(messages.allLinked)}
                    </Text>
                    {aips.length > 0 && (
                        <Button appearance="subtle" size="small" onClick={() => setPackagesShown(!packagesShown)}>
                            {intl.formatMessage(packagesShown ? messages.hidePackages : messages.showPackages)}
                        </Button>
                    )}
                </div>
                {packagesShown && (
                    <ul className="aip-connect-packages">
                        {shownPackages.map(a => (
                            <li key={a.aipId}>
                                <span className="code">{a.code}</span>
                                {a.contentType && <span className="content-type">{a.contentType}</span>}
                            </li>
                        ))}
                    </ul>
                )}
                <div className="aip-connect-scroll">
                    {logicalTree && logicalTree.nodes.length > 0 && (
                        <AipsLogicalContainer tree={logicalTree} onSelect={setLevel}
                                              initialNode={logicalTree.nodes[0].UUID} />
                    )}
                </div>
            </div>
            <div className="aip-connect-column">
                <div className="aip-connect-header">
                    <Label weight="semibold">{intl.formatMessage(connectMessages.target)}</Label>
                </div>
                <div className="aip-connect-tree">
                    <AipTargetTree />
                </div>
            </div>
            <div className="aip-connect-column aip-connect-options">
                <AipConnectBlockedPanel blocked={blocked} />
                <Label weight="semibold">{intl.formatMessage(connectMessages.what)}</Label>
                <RadioGroup value={mode ?? ""} onChange={(_, data) => setMode(data.value as Mode)}>
                    <Radio value="whole" label={<ModeLabel text={intl.formatMessage(messages.modeWhole, { count: aips.length })}
                                                           hint={intl.formatMessage(messages.modeWholeHint)} />} />
                    <Radio value="level" disabled={!allowed.level}
                           label={<ModeLabel text={intl.formatMessage(messages.modeLevel, { level: levelLabel })}
                                             hint={intl.formatMessage(allowed.level ? messages.modeLevelHint
                                                                                    : messages.needsLevel)} />} />
                    <Radio value="sublevels" label={<ModeLabel text={intl.formatMessage(connectMessages.modeSublevels)}
                                                               hint={intl.formatMessage(connectMessages.modeSublevelsHint)} />} />
                    <Radio value="structure" label={<ModeLabel text={intl.formatMessage(connectMessages.modeStructure)}
                                                               hint={intl.formatMessage(connectMessages.modeStructureHint)} />} />
                </RadioGroup>
                {mode === "structure" && levelViewId == null && (
                    <Checkbox className="aip-connect-option" checked={fileplanAsRoot}
                              onChange={(_, data) => setFileplanAsRoot(data.checked === true)}
                              label={intl.formatMessage(connectMessages.fileplanAsRoot)} />
                )}
                <div className="aip-connect-footer">
                    <Text>{footer}</Text>
                    <Button appearance="primary" disabled={!canConnect} onClick={handleConnect}>
                        {intl.formatMessage(connectMessages.connect)}
                    </Button>
                </div>
            </div>
        </div>
    );
}

export default AipBulkConnectPanel;
