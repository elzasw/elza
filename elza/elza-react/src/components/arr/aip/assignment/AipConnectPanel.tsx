import {
    Button,
    Checkbox,
    Label,
    Radio,
    RadioGroup,
    Switch,
    Text,
} from "@fluentui/react-components";
import "./AipConnectPanel.scss";
import { defineMessages, useIntl } from "react-intl";
import { useEffect, useMemo, useState } from "react";
import { useSelector } from "react-redux";
import { AipDetailVO, DaAipActionVO, ExplorerTreeNode } from "elza-api";
import FundTreeDaos from "../../FundTreeDaos";
import { AipPartTree, SelectedPart, linkedFiles, packageFiles, selectableDaoIds } from "./AipPartTree";
import { WebApi } from "../../../../actions";
import { Api } from "../../../../api";
import { AREA_AIP, aipFetchIfNeeded, aipsFetchIfNeeded } from "actions/aip/aip";
import { AREA_AIP_STRUCTURE, fetchAipStructureIfNeeded } from "actions/aip/exp";
import { fundTreeNodeExpand, fundTreeSelectNode } from "actions/arr/fundTree";
import { FUND_TREE_AREA_AIP } from "actions/constants/ActionTypes";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { useThunkDispatch } from "utils/hooks";
import { useWebsocket } from "components/shared/web-socket/WebsocketProvider";
import { runAipAction } from "../../../aip/AipActionRunner";
import { linkStateMessages } from "../../../aip/messages";
import { addToastrSuccess } from "components/shared/toastr/ToastrActions";

const messages = defineMessages({
    source: { id: "arr.aip.single.source", defaultMessage: "Zdroj - struktura balíčku" },
    linkedFiles: {
        id: "arr.aip.single.linkedFiles",
        defaultMessage: "připojeno {linked} z {count, plural, one {# souboru} other {# souborů}}",
    },
    showFiles: { id: "arr.aip.single.showFiles", defaultMessage: "Zobrazit soubory" },
    target: { id: "arr.aip.assignment.target", defaultMessage: "Cíl - archivní soubor" },
    noTarget: { id: "arr.aip.single.noTarget", defaultMessage: "Vyberte ve stromu jednotku popisu, ke které se připojí." },
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

/** Uzel stromu archivního souboru - jen to, co panel čte. */
type TreeNode = { id: number; name: string };

/** Strom archivního souboru v oblasti připojování balíčků, jak ho drží store. */
type AipFundTree = { nodes: TreeNode[]; selectedId: number | null; expandedIds: Record<number, boolean> };

/** Aktivní archivní soubor - jen to, co panel čte. */
type ActiveFund = { id: number; versionId: number; fundTreeAip: AipFundTree };

interface Props {
    aipId: number;
}

export type AipConnectPanelProps = Props;

/**
 * Připojení jednoho balíčku k archivnímu popisu jako karta průzkumníku balíčku: vlevo struktura
 * balíčku (zdroj), uprostřed strom archivního souboru (cíl), vpravo volba, co se připojí,
 * a jedno tlačítko Připojit.
 *
 * Lze připojit libovolné části balíčku - úrovně, reprezentace i soubory - a každou z nich
 * případně do nové JP. Způsoby, které výběr nedovoluje, jsou nedostupné a řeknou proč. Strom
 * archivního souboru má vlastní oblast, takže výběr v něm neovlivní strom pořádání.
 */
function AipConnectPanel({ aipId }: Props) {
    const intl = useIntl();
    const dispatch = useThunkDispatch();
    const websocket = useWebsocket();
    const [selected, setSelected] = useState<SelectedPart[]>([]);
    const [mode, setMode] = useState<Mode>("whole");
    const [withoutLower, setWithoutLower] = useState(false);
    const [fileplanAsRoot, setFileplanAsRoot] = useState(false);
    const [showFiles, setShowFiles] = useState(false);

    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP))?.data as AipDetailVO | undefined;
    const structure = useSelector((state: AppState) => storeFromArea(state, AREA_AIP_STRUCTURE))?.data as
        ExplorerTreeNode | undefined;
    const fund = useSelector((state: AppState) =>
        state.arrRegion?.funds?.[state.arrRegion.activeIndex ?? -1]) as unknown as ActiveFund | undefined;
    const tree = fund?.fundTreeAip;
    const target = tree?.nodes.find(n => n.id === tree.selectedId);

    useEffect(() => {
        dispatch(aipFetchIfNeeded(aipId));
        dispatch(fetchAipStructureIfNeeded(aipId, true));
    }, [aipId]);

    // cíl je na začátku kořen archivního souboru - nejčastěji se připojuje celý balíček k němu
    const root = tree?.nodes[0];
    useEffect(() => {
        if (fund && root && tree?.selectedId == null) {
            dispatch(fundTreeSelectNode(FUND_TREE_AREA_AIP, fund.versionId, root.id, false, false) as never);
        }
    }, [root?.id]);

    /** Po skrytí souborů nezůstane vybráno nic, co není vidět. */
    const toggleShowFiles = (show: boolean) => {
        setShowFiles(show);
        if (!show && structure) {
            const visible = selectableDaoIds(structure, false);
            setSelected(sel => sel.filter(s => visible.has(s.daoId)));
        }
    };

    const daoIds = selected.map(s => s.daoId);
    const fileCounts = useMemo(() => {
        if (!structure) {
            return undefined;
        }
        const linked = linkedFiles(structure);
        const files = Array.from(packageFiles(structure).keys());
        return { count: files.length, linked: files.filter(id => linked.has(id)).length };
    }, [structure]);
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

    /**
     * Po akci: struktura balíčku (značky připojení), balíček a seznam balíčků. Stromy archivního
     * souboru obnoví události ze serveru; cíl se rozbalí, aby byly vidět nově vytvořené JP.
     */
    const reloadAfterAction = (targetNode: TreeNode, createsLevels: boolean) => {
        dispatch(fetchAipStructureIfNeeded(aipId, true));
        dispatch(aipFetchIfNeeded(aipId, true));
        dispatch(aipsFetchIfNeeded(true));
        if (createsLevels) {
            dispatch(fundTreeNodeExpand(FUND_TREE_AREA_AIP, targetNode) as never);
        }
        setSelected([]);
    };

    const handleConnect = () => {
        if (!target) {
            return;
        }
        const targetNode = target;
        const createsLevels = effectiveMode === "newLevels" || effectiveMode === "sublevels"
            || effectiveMode === "structure";

        /** Připojení na pozadí jako akce nad AIPy - s dialogem průběhu. */
        const runAction = (request: () => Promise<{ data: DaAipActionVO }>) =>
            runAipAction(dispatch, intl, websocket, intl.formatMessage(messages.connect), request as never,
                         () => reloadAfterAction(targetNode, createsLevels));

        /** Přímé připojení částí - hned hotové. */
        const runDirect = (request: () => Promise<unknown>) =>
            request().then(() => {
                dispatch(addToastrSuccess(intl.formatMessage(messages.connected)));
                reloadAfterAction(targetNode, createsLevels);
            });

        const nodeId = targetNode.id;
        switch (effectiveMode) {
            case "whole":
                return runAction(() => Api.aips.aipBulkConnectToJp(nodeId, [aipId]));
            case "parts":
                return runDirect(() => withoutLower
                    ? WebApi.connectSelectedToJp(nodeId, aipId, daoIds)
                    : WebApi.connectAipPartToJp(nodeId, aipId, daoIds));
            case "newLevels":
                return runDirect(() => WebApi.createJpFromSelectedAip(nodeId, aipId, daoIds));
            case "sublevels":
                return runAction(() => Api.aips.aipBulkCreateSublevels(nodeId, [aipId], undefined, level?.daoId));
            case "structure":
                return runAction(() => Api.aips.aipBulkImportDescription(nodeId, [aipId],
                    level == null && fileplanAsRoot, undefined, level?.daoId));
        }
    };

    const hintFor = (m: Mode, hint: string) => allowed[m] ? hint
        : intl.formatMessage(m === "parts" || m === "newLevels" ? messages.needsParts : messages.needsLevel);

    return (
        <div className="aip-connect">
            <div className="aip-connect-column">
                <div className="aip-connect-header">
                    <Label weight="semibold">{intl.formatMessage(messages.source)}</Label>
                    <Switch checked={showFiles} label={intl.formatMessage(messages.showFiles)}
                            onChange={(_, data) => toggleShowFiles(data.checked)} />
                </div>
                <div className="aip-connect-state">
                    {aip?.linkState && <Text>{intl.formatMessage(linkStateMessages[aip.linkState])}</Text>}
                    {fileCounts && fileCounts.linked > 0 && fileCounts.linked < fileCounts.count &&
                        <Text>({intl.formatMessage(messages.linkedFiles, fileCounts)})</Text>}
                    <Text>· {intl.formatMessage(messages.selected, { count: selected.length })}</Text>
                </div>
                <div className="aip-connect-scroll border">
                    {structure && <AipPartTree structure={structure} selected={selected} onChange={setSelected}
                                               showFiles={showFiles} />}
                </div>
            </div>
            <div className="aip-connect-column">
                <div className="aip-connect-header">
                    <Label weight="semibold">{intl.formatMessage(messages.target)}</Label>
                </div>
                <div className="aip-connect-tree">
                    {fund && tree && <FundTreeDaos fund={fund} versionId={fund.versionId} area={FUND_TREE_AREA_AIP}
                                                   {...tree} />}
                </div>
            </div>
            <div className="aip-connect-column aip-connect-options">
                <Label weight="semibold">{intl.formatMessage(messages.what)}</Label>
                <RadioGroup value={effectiveMode} onChange={(_, data) => setMode(data.value as Mode)}>
                    <Radio value="whole" label={<ModeLabel text={intl.formatMessage(messages.modeWhole)}
                                                           hint={intl.formatMessage(messages.modeWholeHint)} />} />
                    <Radio value="parts" disabled={!allowed.parts}
                           label={<ModeLabel text={intl.formatMessage(messages.modeParts, { count: selected.length })}
                                             hint={hintFor("parts", intl.formatMessage(messages.modePartsHint))} />} />
                    {effectiveMode === "parts" && (
                        <Checkbox className="aip-connect-option" checked={withoutLower}
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
                    <Checkbox className="aip-connect-option" checked={fileplanAsRoot}
                              onChange={(_, data) => setFileplanAsRoot(data.checked === true)}
                              label={intl.formatMessage(messages.fileplanAsRoot)} />
                )}
                <div className="aip-connect-footer">
                    <Text>{target ? `„${target.name}“` : intl.formatMessage(messages.noTarget)}</Text>
                    <Button appearance="primary" disabled={!target} onClick={handleConnect}>
                        {intl.formatMessage(messages.connect)}
                    </Button>
                </div>
            </div>
        </div>
    );
}

/** Název způsobu připojení a jednou větou, co udělá (nebo proč teď nejde). */
function ModeLabel({ text, hint }: { text: string; hint: string }) {
    return (
        <span className="aip-connect-mode-label">
            <span>{text}</span>
            <Text size={200} className="hint">{hint}</Text>
        </span>
    );
}

export default AipConnectPanel;
