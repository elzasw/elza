import {
    Button,
    Checkbox,
    Label,
    Radio,
    RadioGroup,
    Text,
    TreeItemValue,
} from "@fluentui/react-components";
import { Modal, Col, Row } from "react-bootstrap";
import "./AipAssignmentModal.scss";
import { defineMessages, useIntl } from 'react-intl';
import AipsLogicalContainer, { LogicalTreeNode } from "./AipsLogicalContainer";
import { useEffect, useState } from "react";
import FundTree from "./FundTree";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { AIP_LOGICAL_TREE, AREA_AIPS, fetchAipLogicalTreeIfNeeded } from "actions/aip/aip";
import { useThunkDispatch } from "utils/hooks";
import { useWebsocket } from "components/shared/web-socket/WebsocketProvider";
import { AipConnectBlockedVO, AipDetailVO, AipLinkState, DaAipActionVO } from "elza-api";
import { Api } from "../../../../api";
import AipConnectBlockedPanel from "../../../aip/AipConnectBlockedPanel";
import { runAipAction } from "../../../aip/AipActionRunner";
import { aipsFetchIfNeeded } from "actions/aip/aip";
import { useNodeName } from "components/aip/explorer/levels";
import { fundTreeFetch } from "actions/arr/fundTree";
import { FUND_TREE_AREA_MAIN } from "actions/constants/ActionTypes";

const messages = defineMessages({
    selectedCount: {
        id: "arr.aip.assignment.selectedCount",
        defaultMessage: "{count, plural, one {Vybrán # balíček} few {Vybrány # balíčky} other {Vybráno # balíčků}}",
    },
    showPackages: { id: "arr.aip.assignment.showPackages", defaultMessage: "Zobrazit balíčky vybrané úrovně" },
    hidePackages: { id: "arr.aip.assignment.hidePackages", defaultMessage: "Skrýt balíčky" },
    source: { id: "arr.aip.assignment.source", defaultMessage: "Zdroj - logická struktura balíčků" },
    target: { id: "arr.aip.assignment.target", defaultMessage: "Cíl - archivní soubor" },
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
});

/** Způsob připojení, který uživatel zvolil. */
type Mode = "whole" | "level" | "sublevels" | "structure";

type FundTreeData = { nodes: { id: TreeItemValue; name: string }[]; expandedIds?: Set<TreeItemValue> };

interface Props {
    aips: AipDetailVO[];
    /** Strom archivního souboru při otevření dialogu; po akci se čte aktuální ze store. */
    tree: FundTreeData;
}

export type AipAssignmentModalProps = Props;

/**
 * Hromadné připojení balíčků k archivnímu popisu: vlevo logická struktura balíčků (zdroj),
 * vpravo archivní soubor (cíl), dole volba, co se připojí, a jedno tlačítko Připojit.
 *
 * Celé balíčky se připojují všechny, které dialog dostal; ostatní způsoby pracují s balíčky
 * vybrané úrovně. Vybraný kořen struktury znamená vrchol balíčků - celé balíčky.
 *
 * Po každé akci se dialog načte znovu: plně napojené balíčky z něj zmizí a ve stromu archivního
 * souboru jsou vidět nově vytvořené úrovně.
 */
function AipAssignmentModal({ aips: initialAips, tree: initialTree }: Props) {
    const [logicalTree, setLogicalTree] = useState<{ nodes: LogicalTreeNode[] } | null>(null);
    const [level, setLevel] = useState<LogicalTreeNode | null>(null);
    const [targetNodeId, setTargetNodeId] = useState<TreeItemValue>(initialTree.nodes[0].id);
    const [mode, setMode] = useState<Mode>("whole");
    const [fileplanAsRoot, setFileplanAsRoot] = useState(false);
    const [packagesShown, setPackagesShown] = useState(false);
    const [blocked, setBlocked] = useState<AipConnectBlockedVO[]>([]);
    const structure = useSelector((state: AppState) => storeFromArea(state, AIP_LOGICAL_TREE));
    const dispatch = useThunkDispatch();
    const websocket = useWebsocket();
    const intl = useIntl();
    const nodeName = useNodeName();

    // aktuální stav balíčků ze seznamu - po akci se seznam načte znovu
    const listRows = useSelector((state: AppState) => storeFromArea(state, AREA_AIPS)?.rows) as AipDetailVO[] | undefined;
    const aips = initialAips
        .map(a => listRows?.find(r => r.aipId === a.aipId) ?? a)
        .filter(a => a.linkState !== AipLinkState.FullyLinked);
    // aktuální strom archivního souboru ze store; mimo pořádání (testy) ten, se kterým se dialog otevřel
    const activeFund = useSelector((state: AppState) => state.arrRegion?.funds?.[state.arrRegion.activeIndex ?? -1]);
    const tree = (activeFund?.fundTree?.nodes ? activeFund.fundTree : initialTree) as FundTreeData;

    const allAipIds = aips.map(a => a.aipId);
    const aipKey = allAipIds.join(",");
    const levelAipIds: number[] = level?.value ?? allAipIds;
    const levelViewId = level?.daLeveViewId;
    const aipIds = mode === "whole" ? allAipIds : levelAipIds;
    const targetName = tree.nodes.find(n => n.id == targetNodeId)?.name ?? "";

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
        if (targetNodeId == null || allAipIds.length === 0) {
            setBlocked([]);
            return;
        }
        Api.aips.aipConnectCheck(targetNodeId as number, allAipIds)
            .then(response => setBlocked(response.data.blocked ?? []))
            .catch(() => setBlocked([]));
    }, [targetNodeId, aipKey]);

    /** Po akci: seznam balíčků (a s ním stav napojení) a strom archivního souboru s cílovou úrovní rozbalenou. */
    const reloadAfterAction = () => {
        dispatch(aipsFetchIfNeeded(true));
        if (activeFund?.versionId != null) {
            const expandedIds = { ...(activeFund.fundTree?.expandedIds ?? {}), [targetNodeId as number]: true };
            dispatch(fundTreeFetch(FUND_TREE_AREA_MAIN, activeFund.versionId, null, expandedIds) as never);
        }
    };

    const request = (): Promise<{ data: DaAipActionVO }> => {
        const target = targetNodeId as number;
        switch (mode) {
            case "whole":
                return Api.aips.aipBulkConnectToJp(target, aipIds);
            case "level":
                // kořen struktury je vrchol balíčků - připojí se celé
                return levelViewId != null
                    ? Api.aips.aipBulkConnectLogicToJp(target, aipIds, levelViewId)
                    : Api.aips.aipBulkConnectToJp(target, aipIds);
            case "sublevels":
                return Api.aips.aipBulkCreateSublevels(target, aipIds, levelViewId);
            case "structure":
                return Api.aips.aipBulkImportDescription(target, aipIds, levelViewId == null && fileplanAsRoot,
                                                         levelViewId);
        }
    };

    const handleConnect = () => {
        runAipAction(dispatch, intl, websocket, intl.formatMessage(messages.connect), request as never, reloadAfterAction);
    };

    // napojené AIPy odmítne jen připojení, které se týká celého balíčku nebo úrovně
    const refused = blocked.length > 0 && (mode === "whole" || mode === "level");
    const levelLabel = level ? nodeName(level as never) : "";
    const shownPackages = aips.filter(a => levelAipIds.includes(a.aipId));

    return (
        <Modal.Body className="aip-assignment-body">
            <AipConnectBlockedPanel blocked={blocked} />
            <div className="aip-assignment-selection">
                <Text weight="semibold">
                    {aips.length > 0
                        ? intl.formatMessage(messages.selectedCount, { count: aips.length })
                        : intl.formatMessage(messages.allLinked)}
                </Text>
                <Button appearance="subtle" size="small" onClick={() => setPackagesShown(!packagesShown)}>
                    {intl.formatMessage(packagesShown ? messages.hidePackages : messages.showPackages)}
                </Button>
            </div>
            {packagesShown && (
                <ul className="aip-assignment-packages">
                    {shownPackages.map(a => (
                        <li key={a.aipId}>
                            <span className="code">{a.code}</span>
                            {a.contentType && <span className="content-type">{a.contentType}</span>}
                        </li>
                    ))}
                </ul>
            )}
            <Row className="aip-assignment-trees">
                <Col xs={6} className="d-flex flex-column">
                    <Label weight="semibold">{intl.formatMessage(messages.source)}</Label>
                    {logicalTree && (
                        <AipsLogicalContainer tree={logicalTree} onSelect={setLevel}
                                              selectedNode={logicalTree.nodes[0].UUID} />
                    )}
                </Col>
                <Col xs={6} className="d-flex flex-column">
                    <Label weight="semibold">{intl.formatMessage(messages.target)}</Label>
                    <div className="border flex-grow-1 overflow-auto">
                        <FundTree tree={tree} expandedIds={tree.expandedIds} selectedNode={targetNodeId}
                                  setSelectedNode={setTargetNodeId} />
                    </div>
                </Col>
            </Row>
            <div className="aip-assignment-mode">
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
                    <Checkbox className="aip-assignment-option" checked={fileplanAsRoot}
                              onChange={(_, data) => setFileplanAsRoot(data.checked === true)}
                              label={intl.formatMessage(messages.fileplanAsRoot)} />
                )}
            </div>
            <div className="aip-assignment-footer">
                <Text>{intl.formatMessage(messages.summary, { count: aipIds.length, target: targetName })}</Text>
                <Button appearance="primary" disabled={refused || aipIds.length === 0 || targetNodeId == null}
                        onClick={handleConnect}>
                    {intl.formatMessage(messages.connect)}
                </Button>
            </div>
        </Modal.Body>
    );
}

/** Název způsobu připojení a jednou větou, co udělá. */
function ModeLabel({ text, hint }: { text: string; hint: string }) {
    return (
        <span className="aip-assignment-mode-label">
            <span>{text}</span>
            <Text size={200} className="hint">{hint}</Text>
        </span>
    );
}

export default AipAssignmentModal;
