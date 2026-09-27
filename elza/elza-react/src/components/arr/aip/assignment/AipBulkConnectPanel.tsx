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
import { AIP_LOGICAL_TREE, fetchAipLogicalTreeIfNeeded } from "actions/aip/aip";
import { useThunkDispatch } from "utils/hooks";
import { useWebsocket } from "components/shared/web-socket/WebsocketProvider";
import { AipConnectBlockedVO, AipDetailVO, AipLinkState, DaAipActionVO } from "elza-api";
import { Api } from "../../../../api";
import { WebApi } from "../../../../actions";
import AipConnectBlockedPanel from "../../../aip/AipConnectBlockedPanel";
import { runAipAction } from "../../../aip/AipActionRunner";
import { aipsFetchIfNeeded } from "actions/aip/aip";
import { useNodeName } from "components/aip/explorer/levels";
import { AipTargetTree, TargetNode, expandTarget, useAipTarget } from "./AipTargetTree";

const messages = defineMessages({
    selectedCount: {
        id: "arr.aip.assignment.selectedCount",
        defaultMessage: "{count, plural, one {Vybrán # balíček} few {Vybrány # balíčky} other {Vybráno # balíčků}}",
    },
    showPackages: { id: "arr.aip.assignment.showPackages", defaultMessage: "Zobrazit balíčky vybrané úrovně" },
    hidePackages: { id: "arr.aip.assignment.hidePackages", defaultMessage: "Skrýt balíčky" },
    source: { id: "arr.aip.assignment.source", defaultMessage: "Zdroj - logická struktura balíčků" },
    target: { id: "arr.aip.assignment.target", defaultMessage: "Cíl - archivní soubor" },
    noTarget: { id: "arr.aip.single.noTarget", defaultMessage: "Vyberte ve stromu jednotku popisu, ke které se připojí." },
    what: { id: "arr.aip.assignment.what", defaultMessage: "Co připojit" },
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
    modeSublevels: { id: "arr.aip.assignment.mode.sublevels", defaultMessage: "Úrovně pod vybranou úrovní" },
    modeSublevelsHint: {
        id: "arr.aip.assignment.mode.sublevels.hint",
        defaultMessage: "Pro každou úroveň přímo pod vybranou úrovní vytvoří pod vybranou jednotkou popisu podúroveň a připojí k ní její část balíčků.",
    },
    modeStructure: { id: "arr.aip.assignment.mode.structure", defaultMessage: "Převzít strukturu a popis" },
    modeStructureHint: {
        id: "arr.aip.assignment.mode.structure.hint",
        defaultMessage: "Vytvoří úrovně pod vybranou úrovní až po dokumenty, s prvky popisu z balíčků, a připojí k nim soubory.",
    },
    fileplanAsRoot: { id: "arr.aip.assignment.fileplanAsRoot", defaultMessage: "Spisový plán jako kořenová série" },
    summary: {
        id: "arr.aip.assignment.summary",
        defaultMessage: "{count, plural, one {# balíček} few {# balíčky} other {# balíčků}} → „{target}“",
    },
    connect: { id: "arr.aip.assignment.connect", defaultMessage: "Připojit" },
    allLinked: { id: "arr.aip.assignment.allLinked", defaultMessage: "Všechny balíčky jsou plně připojeny." },
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
 * Po každé akci se balíčky načtou znovu: plně napojené ze stránky zmizí a ve stromu archivního
 * souboru jsou vidět nově vytvořené úrovně.
 */
function AipBulkConnectPanel({ aipIds: requestedIds, readOnly = false }: Props) {
    const [details, setDetails] = useState<AipDetailVO[] | null>(null);
    const [reloads, setReloads] = useState(0);
    const [logicalTree, setLogicalTree] = useState<{ nodes: LogicalTreeNode[] } | null>(null);
    const [level, setLevel] = useState<LogicalTreeNode | null>(null);
    const [mode, setMode] = useState<Mode>("whole");
    const [fileplanAsRoot, setFileplanAsRoot] = useState(false);
    const [packagesShown, setPackagesShown] = useState(false);
    const [blocked, setBlocked] = useState<AipConnectBlockedVO[]>([]);
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
    const levelAipIds: number[] = level?.value ?? allAipIds;
    const levelViewId = level?.daLeveViewId;
    const aipIds = mode === "whole" ? allAipIds : levelAipIds;

    useEffect(() => {
        if (allAipIds.length > 0) {
            dispatch(fetchAipLogicalTreeIfNeeded(allAipIds, true));
        }
    }, [aipKey]);

    useEffect(() => {
        if (allAipIds.length === 0) {
            setLogicalTree(null);
        } else if (structure.data) {
            setLogicalTree(structure.data);
        }
    }, [structure, aipKey]);

    /** Připojení už napojený AIP odmítne; uživatel to má vědět dřív, než potvrdí. */
    useEffect(() => {
        if (target == null || allAipIds.length === 0) {
            setBlocked([]);
            return;
        }
        Api.aips.aipConnectCheck(target.id, allAipIds)
            .then(response => setBlocked(response.data.blocked ?? []))
            .catch(() => setBlocked([]));
    }, [target?.id, aipKey]);

    /** Po akci: balíčky (a s nimi stav napojení), seznam balíčků a cíl rozbalený na nové úrovně. */
    const reloadAfterAction = (targetNode: TargetNode, createsLevels: boolean) => {
        setReloads(r => r + 1);
        dispatch(aipsFetchIfNeeded(true));
        if (createsLevels) {
            dispatch(expandTarget(targetNode) as never);
        }
    };

    const handleConnect = () => {
        if (!target) {
            return;
        }
        const targetNode = target;
        const nodeId = targetNode.id;
        const request = (): Promise<{ data: DaAipActionVO }> => {
            switch (mode) {
                case "whole":
                    return Api.aips.aipBulkConnectToJp(nodeId, aipIds);
                case "level":
                    // kořen struktury je vrchol balíčků - připojí se celé
                    return levelViewId != null
                        ? Api.aips.aipBulkConnectLogicToJp(nodeId, aipIds, levelViewId)
                        : Api.aips.aipBulkConnectToJp(nodeId, aipIds);
                case "sublevels":
                    return Api.aips.aipBulkCreateSublevels(nodeId, aipIds, levelViewId);
                case "structure":
                    return Api.aips.aipBulkImportDescription(nodeId, aipIds, levelViewId == null && fileplanAsRoot,
                                                             levelViewId);
            }
        };
        const createsLevels = mode === "sublevels" || mode === "structure";
        runAipAction(dispatch, intl, websocket, intl.formatMessage(messages.connect), request as never,
                     () => reloadAfterAction(targetNode, createsLevels));
    };

    // napojené AIPy odmítne jen připojení, které se týká celého balíčku nebo úrovně
    const refused = blocked.length > 0 && (mode === "whole" || mode === "level");
    const levelLabel = level ? nodeName(level as never) : "";
    const shownPackages = aips.filter(a => levelAipIds.includes(a.aipId));

    return (
        <div className="aip-connect">
            <div className="aip-connect-column">
                <div className="aip-connect-header">
                    <Label weight="semibold">{intl.formatMessage(messages.source)}</Label>
                </div>
                <div className="aip-connect-state">
                    <Text weight="semibold">
                        {details == null ? intl.formatMessage(messages.loading)
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
                    {logicalTree && (
                        <AipsLogicalContainer tree={logicalTree} onSelect={setLevel}
                                              selectedNode={logicalTree.nodes[0].UUID} />
                    )}
                </div>
            </div>
            <div className="aip-connect-column">
                <div className="aip-connect-header">
                    <Label weight="semibold">{intl.formatMessage(messages.target)}</Label>
                </div>
                <div className="aip-connect-tree">
                    <AipTargetTree />
                </div>
            </div>
            <div className="aip-connect-column aip-connect-options">
                <AipConnectBlockedPanel blocked={blocked} />
                <Label weight="semibold">{intl.formatMessage(messages.what)}</Label>
                <RadioGroup value={mode} onChange={(_, data) => setMode(data.value as Mode)}>
                    <Radio value="whole" label={<ModeLabel text={intl.formatMessage(messages.modeWhole, { count: aips.length })}
                                                           hint={intl.formatMessage(messages.modeWholeHint)} />} />
                    <Radio value="level" label={<ModeLabel text={intl.formatMessage(messages.modeLevel, { level: levelLabel })}
                                                           hint={intl.formatMessage(messages.modeLevelHint)} />} />
                    <Radio value="sublevels" label={<ModeLabel text={intl.formatMessage(messages.modeSublevels)}
                                                               hint={intl.formatMessage(messages.modeSublevelsHint)} />} />
                    <Radio value="structure" label={<ModeLabel text={intl.formatMessage(messages.modeStructure)}
                                                               hint={intl.formatMessage(messages.modeStructureHint)} />} />
                </RadioGroup>
                {mode === "structure" && levelViewId == null && (
                    <Checkbox className="aip-connect-option" checked={fileplanAsRoot}
                              onChange={(_, data) => setFileplanAsRoot(data.checked === true)}
                              label={intl.formatMessage(messages.fileplanAsRoot)} />
                )}
                <div className="aip-connect-footer">
                    <Text>{target
                        ? intl.formatMessage(messages.summary, { count: aipIds.length, target: target.name })
                        : intl.formatMessage(messages.noTarget)}</Text>
                    <Button appearance="primary" disabled={readOnly || refused || aipIds.length === 0 || !target}
                            onClick={handleConnect}>
                        {intl.formatMessage(messages.connect)}
                    </Button>
                </div>
            </div>
        </div>
    );
}

/** Název způsobu připojení a jednou větou, co udělá. */
function ModeLabel({ text, hint }: { text: string; hint: string }) {
    return (
        <span className="aip-connect-mode-label">
            <span>{text}</span>
            <Text size={200} className="hint">{hint}</Text>
        </span>
    );
}

export default AipBulkConnectPanel;
