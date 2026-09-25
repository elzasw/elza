import {
    Button,
    Checkbox,
    Label,
    Radio,
    RadioGroup,
    Switch,
    Text,
    TreeItemValue,
} from "@fluentui/react-components";
import { Modal, Col, Row } from "react-bootstrap";
import "./AipAssignmentModal.scss";
import { defineMessages, useIntl } from "react-intl";
import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { AipDetailVO, DaAipActionVO, ExplorerTreeNode } from "elza-api";
import FundTree from "./FundTree";
import { AipPartTree, SelectedPart, selectableDaoIds } from "./AipPartTree";
import { WebApi } from "../../../../actions";
import { Api } from "../../../../api";
import { AREA_AIP, aipFetchIfNeeded, aipsFetchIfNeeded } from "actions/aip/aip";
import { AREA_AIP_STRUCTURE, fetchAipStructureIfNeeded } from "actions/aip/exp";
import { fundTreeFetch } from "actions/arr/fundTree";
import { FUND_TREE_AREA_MAIN } from "actions/constants/ActionTypes";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { useThunkDispatch } from "utils/hooks";
import { useWebsocket } from "components/shared/web-socket/WebsocketProvider";
import { runAipAction } from "../../../aip/AipActionRunner";
import { linkStateMessages } from "../../../aip/messages";
import { addToastrSuccess } from "components/shared/toastr/ToastrActions";

const messages = defineMessages({
    source: { id: "arr.aip.single.source", defaultMessage: "Zdroj - struktura balíčku" },
    showFiles: { id: "arr.aip.single.showFiles", defaultMessage: "Zobrazit soubory" },
    target: { id: "arr.aip.assignment.target", defaultMessage: "Cíl - archivní soubor" },
    what: { id: "arr.aip.assignment.what", defaultMessage: "Co připojit" },
    selected: {
        id: "arr.aip.single.selected",
        defaultMessage: "{count, plural, =0 {nic nevybráno} one {vybrána # část} few {vybrány # části} other {vybráno # částí}}",
    },
    modeWhole: { id: "arr.aip.single.mode.whole", defaultMessage: "Celý balíček" },
    modeWholeHint: {
        id: "arr.aip.single.mode.whole.hint",
        defaultMessage: "Připojí celý balíček k vybrané jednotce popisu.",
    },
    modeParts: { id: "arr.aip.single.mode.parts", defaultMessage: "Vybrané části ({count})" },
    modePartsHint: {
        id: "arr.aip.single.mode.parts.hint",
        defaultMessage: "Připojí vybrané části k vybrané jednotce popisu, každou včetně jejích nižších částí.",
    },
    withoutLower: { id: "arr.aip.single.withoutLower", defaultMessage: "bez nižších částí" },
    modeNewLevels: { id: "arr.aip.single.mode.newLevels", defaultMessage: "Vybrané části, každou do nové JP" },
    modeNewLevelsHint: {
        id: "arr.aip.single.mode.newLevels.hint",
        defaultMessage: "Pro každou vybranou část vytvoří pod vybranou jednotkou popisu novou JP a připojí k ní tuto část.",
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
    needsParts: { id: "arr.aip.single.needsParts", defaultMessage: "Vyberte ve struktuře balíčku jednu nebo více částí." },
    needsLevel: {
        id: "arr.aip.single.needsLevel",
        defaultMessage: "Vyberte jednu úroveň logické struktury, nebo nic - pak se použije celý balíček.",
    },
    fileplanAsRoot: { id: "arr.aip.assignment.fileplanAsRoot", defaultMessage: "Spisový plán jako kořenová série" },
    connect: { id: "arr.aip.assignment.connect", defaultMessage: "Připojit" },
    connected: { id: "arr.aip.single.connected", defaultMessage: "Připojeno" },
});

/** Způsob připojení, který uživatel zvolil. */
type Mode = "whole" | "parts" | "newLevels" | "sublevels" | "structure";

type FundTreeData = { nodes: { id: TreeItemValue; name: string }[]; expandedIds?: Set<TreeItemValue> };

interface Props {
    aipId: number;
    /** Strom archivního souboru při otevření dialogu; po akci se čte aktuální ze store. */
    tree: FundTreeData;
}

export type AipIndividualAssignmentModalProps = Props;

/**
 * Připojení jednoho balíčku k archivnímu popisu: vlevo struktura balíčku (zdroj), vpravo
 * archivní soubor (cíl), dole volba, co se připojí, a jedno tlačítko Připojit.
 *
 * Proti hromadnému připojení lze připojit libovolné části balíčku - úrovně, reprezentace
 * i soubory - a každou z nich případně do nové JP. Způsoby, které výběr nedovoluje, jsou
 * nedostupné a řeknou proč. Po každé akci se dialog načte znovu.
 */
function AipIndividualAssignmentModal({ aipId, tree: initialTree }: Props) {
    const intl = useIntl();
    const dispatch = useThunkDispatch();
    const websocket = useWebsocket();
    const [targetNodeId, setTargetNodeId] = useState<TreeItemValue>(initialTree.nodes[0].id);
    const [selected, setSelected] = useState<SelectedPart[]>([]);
    const [mode, setMode] = useState<Mode>("whole");
    const [withoutLower, setWithoutLower] = useState(false);
    const [fileplanAsRoot, setFileplanAsRoot] = useState(false);
    const [showFiles, setShowFiles] = useState(false);

    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP))?.data as AipDetailVO | undefined;
    const structure = useSelector((state: AppState) => storeFromArea(state, AREA_AIP_STRUCTURE))?.data as
        ExplorerTreeNode | undefined;
    const activeFund = useSelector((state: AppState) => state.arrRegion?.funds?.[state.arrRegion.activeIndex ?? -1]);
    const tree = (activeFund?.fundTree?.nodes ? activeFund.fundTree : initialTree) as FundTreeData;
    const targetName = tree.nodes.find(n => n.id == targetNodeId)?.name ?? "";

    useEffect(() => {
        dispatch(aipFetchIfNeeded(aipId));
        dispatch(fetchAipStructureIfNeeded(aipId, true));
    }, [aipId]);

    /** Po skrytí souborů nezůstane vybráno nic, co není vidět. */
    const toggleShowFiles = (show: boolean) => {
        setShowFiles(show);
        if (!show && structure) {
            const visible = selectableDaoIds(structure, false);
            setSelected(sel => sel.filter(s => visible.has(s.daoId)));
        }
    };

    const daoIds = selected.map(s => s.daoId);
    // úroveň, pod kterou se úrovně vytvoří nebo převezme popis; nic vybráno = celý balíček
    const level = selected.length === 1 && selected[0].kind === "level" ? selected[0] : undefined;
    const levelModeAllowed = selected.length === 0 || level != null;
    const partsAllowed = selected.length > 0;

    const allowed: Record<Mode, boolean> = {
        whole: true,
        parts: partsAllowed,
        newLevels: partsAllowed,
        sublevels: levelModeAllowed,
        structure: levelModeAllowed,
    };
    const effectiveMode: Mode = allowed[mode] ? mode : "whole";

    /** Po akci: struktura balíčku (značky připojení), seznam AIP a strom archivního souboru. */
    const reloadAfterAction = () => {
        dispatch(fetchAipStructureIfNeeded(aipId, true));
        dispatch(aipFetchIfNeeded(aipId, true));
        dispatch(aipsFetchIfNeeded(true));
        if (activeFund?.versionId != null) {
            const expandedIds = { ...(activeFund.fundTree?.expandedIds ?? {}), [targetNodeId as number]: true };
            dispatch(fundTreeFetch(FUND_TREE_AREA_MAIN, activeFund.versionId, null, expandedIds) as never);
        }
        setSelected([]);
    };

    /** Připojení na pozadí jako akce nad AIPy - s dialogem průběhu. */
    const runAction = (request: () => Promise<{ data: DaAipActionVO }>) =>
        runAipAction(dispatch, intl, websocket, intl.formatMessage(messages.connect), request as never,
                     reloadAfterAction);

    /** Přímé připojení částí - hned hotové. */
    const runDirect = (request: () => Promise<unknown>) =>
        request().then(() => {
            dispatch(addToastrSuccess(intl.formatMessage(messages.connected)));
            reloadAfterAction();
        });

    const handleConnect = () => {
        const target = targetNodeId as number;
        switch (effectiveMode) {
            case "whole":
                return runAction(() => Api.aips.aipBulkConnectToJp(target, [aipId]));
            case "parts":
                return runDirect(() => withoutLower
                    ? WebApi.connectSelectedToJp(target, aipId, daoIds)
                    : WebApi.connectAipPartToJp(target, aipId, daoIds));
            case "newLevels":
                return runDirect(() => WebApi.createJpFromSelectedAip(target, aipId, daoIds));
            case "sublevels":
                return runAction(() => Api.aips.aipBulkCreateSublevels(target, [aipId], undefined, level?.daoId));
            case "structure":
                return runAction(() => Api.aips.aipBulkImportDescription(target, [aipId],
                    level == null && fileplanAsRoot, undefined, level?.daoId));
        }
    };

    const hintFor = (m: Mode, hint: string) => allowed[m] ? hint
        : intl.formatMessage(m === "parts" || m === "newLevels" ? messages.needsParts : messages.needsLevel);

    return (
        <Modal.Body className="aip-assignment-body">
            <div className="aip-assignment-selection">
                <Text weight="semibold">{aip?.code}</Text>
                {aip?.contentType && <Text>{aip.contentType}</Text>}
                {aip?.linkState && <Text>{intl.formatMessage(linkStateMessages[aip.linkState])}</Text>}
                <Text>{intl.formatMessage(messages.selected, { count: selected.length })}</Text>
            </div>
            <Row className="aip-assignment-trees">
                <Col xs={6} className="d-flex flex-column">
                    <div className="aip-assignment-tree-header">
                        <Label weight="semibold">{intl.formatMessage(messages.source)}</Label>
                        <Switch checked={showFiles} label={intl.formatMessage(messages.showFiles)}
                                onChange={(_, data) => toggleShowFiles(data.checked)} />
                    </div>
                    <div className="border flex-grow-1 overflow-auto">
                        {structure && <AipPartTree structure={structure} selected={selected} onChange={setSelected}
                                                   showFiles={showFiles} />}
                    </div>
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
                <RadioGroup value={effectiveMode} onChange={(_, data) => setMode(data.value as Mode)}>
                    <Radio value="whole" label={<ModeLabel text={intl.formatMessage(messages.modeWhole)}
                                                           hint={intl.formatMessage(messages.modeWholeHint)} />} />
                    <Radio value="parts" disabled={!allowed.parts}
                           label={<ModeLabel text={intl.formatMessage(messages.modeParts, { count: selected.length })}
                                             hint={hintFor("parts", intl.formatMessage(messages.modePartsHint))} />} />
                    {effectiveMode === "parts" && (
                        <Checkbox className="aip-assignment-option" checked={withoutLower}
                                  onChange={(_, data) => setWithoutLower(data.checked === true)}
                                  label={intl.formatMessage(messages.withoutLower)} />
                    )}
                    <Radio value="newLevels" disabled={!allowed.newLevels}
                           label={<ModeLabel text={intl.formatMessage(messages.modeNewLevels)}
                                             hint={hintFor("newLevels", intl.formatMessage(messages.modeNewLevelsHint))} />} />
                    <Radio value="sublevels" disabled={!allowed.sublevels}
                           label={<ModeLabel text={intl.formatMessage(messages.modeSublevels)}
                                             hint={hintFor("sublevels", intl.formatMessage(messages.modeSublevelsHint))} />} />
                    <Radio value="structure" disabled={!allowed.structure}
                           label={<ModeLabel text={intl.formatMessage(messages.modeStructure)}
                                             hint={hintFor("structure", intl.formatMessage(messages.modeStructureHint))} />} />
                </RadioGroup>
                {effectiveMode === "structure" && level == null && (
                    <Checkbox className="aip-assignment-option" checked={fileplanAsRoot}
                              onChange={(_, data) => setFileplanAsRoot(data.checked === true)}
                              label={intl.formatMessage(messages.fileplanAsRoot)} />
                )}
            </div>
            <div className="aip-assignment-footer">
                <Text>„{targetName}“</Text>
                <Button appearance="primary" disabled={targetNodeId == null} onClick={handleConnect}>
                    {intl.formatMessage(messages.connect)}
                </Button>
            </div>
        </Modal.Body>
    );
}

/** Název způsobu připojení a jednou větou, co udělá (nebo proč teď nejde). */
function ModeLabel({ text, hint }: { text: string; hint: string }) {
    return (
        <span className="aip-assignment-mode-label">
            <span>{text}</span>
            <Text size={200} className="hint">{hint}</Text>
        </span>
    );
}

export default AipIndividualAssignmentModal;
