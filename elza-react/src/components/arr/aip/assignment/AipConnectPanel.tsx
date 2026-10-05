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
import { AipTargetTree, TargetNode, expandTarget, useAipTarget } from "./AipTargetTree";
import { AipPartTree, SelectedPart, linkedFiles, packageFiles, selectableDaoIds } from "./AipPartTree";
import { ModeLabel, connectMessages, useConnectCheck } from "./connectShared";
import { WebApi } from "../../../../actions";
import { Api } from "../../../../api";
import { AREA_AIP, aipFetchIfNeeded, aipsFetchIfNeeded } from "actions/aip/aip";
import { AREA_AIP_STRUCTURE, fetchAipStructureIfNeeded } from "actions/aip/exp";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { useThunkDispatch } from "utils/hooks";
import { useWebsocket } from "components/shared/web-socket/WebsocketProvider";
import AipConnectBlockedPanel from "../../../aip/AipConnectBlockedPanel";
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
    needsParts: { id: "arr.aip.single.needsParts", defaultMessage: "Vyberte ve struktuře balíčku jednu nebo více částí." },
    needsLevel: {
        id: "arr.aip.single.needsLevel",
        defaultMessage: "Vyberte jednu úroveň logické struktury, nebo nic - pak se použije celý balíček.",
    },
    connected: { id: "arr.aip.single.connected", defaultMessage: "Připojeno" },
});

/** Způsob připojení, který uživatel zvolil. */
type Mode = "whole" | "parts" | "newLevels" | "sublevels" | "structure";

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
 * případně do nové JP. Způsoby, které výběr nedovoluje, jsou nedostupné a řeknou proč.
 *
 * Po akci se způsob připojení zruší: výběr částí se vyprázdní, a kdyby způsob zůstal, další
 * stisk tlačítka by tentýž způsob použil na celý balíček.
 */
function AipConnectPanel({ aipId }: Props) {
    const intl = useIntl();
    const dispatch = useThunkDispatch();
    const websocket = useWebsocket();
    const [selected, setSelected] = useState<SelectedPart[]>([]);
    const [mode, setMode] = useState<Mode | null>("whole");
    const [withoutLower, setWithoutLower] = useState(false);
    const [fileplanAsRoot, setFileplanAsRoot] = useState(false);
    const [showFiles, setShowFiles] = useState(false);
    const [busy, setBusy] = useState(false);
    const [reloads, setReloads] = useState(0);

    // store balíčku i jeho struktury je společný - dokud se nenačte tento balíček, drží předchozí
    const aipStore = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const structureStore = useSelector((state: AppState) => storeFromArea(state, AREA_AIP_STRUCTURE));
    const aip = aipStore?.id === aipId ? aipStore.data as AipDetailVO | undefined : undefined;
    const structure = structureStore?.id === aipId ? structureStore.data as ExplorerTreeNode | undefined : undefined;
    const { target } = useAipTarget();
    const blocked = useConnectCheck(target?.id, [aipId], reloads);

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
    // napojený balíček server celý znovu nepřipojí
    const refused = blocked.length > 0 && mode === "whole";
    const canConnect = mode != null && allowed[mode] && !refused && !busy && target != null && aip != null;

    /**
     * Po akci: struktura balíčku (značky připojení), balíček a seznam balíčků. Stromy archivního
     * souboru obnoví události ze serveru; cíl se rozbalí, aby byly vidět nově vytvořené JP.
     */
    const reloadAfterAction = (targetNode: TargetNode, createsLevels: boolean) => {
        dispatch(fetchAipStructureIfNeeded(aipId, true));
        dispatch(aipFetchIfNeeded(aipId, true));
        dispatch(aipsFetchIfNeeded(true));
        if (createsLevels) {
            dispatch(expandTarget(targetNode) as never);
        }
        setSelected([]);
        setMode(null);
        setReloads(r => r + 1);
    };

    const handleConnect = () => {
        if (!target || mode == null || !canConnect) {
            return;
        }
        const targetNode = target;
        const createsLevels = mode === "newLevels" || mode === "sublevels" || mode === "structure";

        /** Připojení na pozadí jako akce nad AIPy - s dialogem průběhu. */
        const runAction = (request: () => Promise<{ data: DaAipActionVO }>) =>
            runAipAction(dispatch, intl, websocket, intl.formatMessage(connectMessages.connect), request as never,
                         () => reloadAfterAction(targetNode, createsLevels));

        /** Přímé připojení částí - hned hotové. */
        const runDirect = (request: () => Promise<unknown>) =>
            request().then(() => {
                dispatch(addToastrSuccess(intl.formatMessage(messages.connected)));
                reloadAfterAction(targetNode, createsLevels);
            });

        const nodeId = targetNode.id;
        const run = (): Promise<unknown> => {
            switch (mode) {
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
        // dvojí stisk by např. vytvořil JP dvakrát; chybu ohlásí společné zpracování chyb volání
        setBusy(true);
        run().catch(() => undefined).finally(() => setBusy(false));
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
                    {aip?.code && <Text className="code">{aip.code}</Text>}
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
                    <Radio value="whole" label={<ModeLabel text={intl.formatMessage(messages.modeWhole)}
                                                           hint={intl.formatMessage(messages.modeWholeHint)} />} />
                    <Radio value="parts" disabled={!allowed.parts}
                           label={<ModeLabel text={intl.formatMessage(messages.modeParts, { count: selected.length })}
                                             hint={hintFor("parts", intl.formatMessage(messages.modePartsHint))} />} />
                    {mode === "parts" && (
                        <Checkbox className="aip-connect-option" checked={withoutLower}
                                  onChange={(_, data) => setWithoutLower(data.checked === true)}
                                  label={intl.formatMessage(messages.withoutLower)} />
                    )}
                    <Radio value="newLevels" disabled={!allowed.newLevels}
                           label={<ModeLabel text={intl.formatMessage(messages.modeNewLevels)}
                                             hint={hintFor("newLevels", intl.formatMessage(messages.modeNewLevelsHint))} />} />
                    <Radio value="sublevels" disabled={!allowed.sublevels}
                           label={<ModeLabel text={intl.formatMessage(connectMessages.modeSublevels)}
                                             hint={hintFor("sublevels", intl.formatMessage(connectMessages.modeSublevelsHint))} />} />
                    <Radio value="structure" disabled={!allowed.structure}
                           label={<ModeLabel text={intl.formatMessage(connectMessages.modeStructure)}
                                             hint={hintFor("structure", intl.formatMessage(connectMessages.modeStructureHint))} />} />
                </RadioGroup>
                {mode === "structure" && level == null && (
                    <Checkbox className="aip-connect-option" checked={fileplanAsRoot}
                              onChange={(_, data) => setFileplanAsRoot(data.checked === true)}
                              label={intl.formatMessage(connectMessages.fileplanAsRoot)} />
                )}
                <div className="aip-connect-footer">
                    <Text>{!target ? intl.formatMessage(connectMessages.noTarget)
                        : mode == null ? intl.formatMessage(connectMessages.chooseMode)
                        : `${aip?.code ?? ""} → „${target.name}“`}</Text>
                    <Button appearance="primary" disabled={!canConnect} onClick={handleConnect}>
                        {intl.formatMessage(connectMessages.connect)}
                    </Button>
                </div>
            </div>
        </div>
    );
}

export default AipConnectPanel;
