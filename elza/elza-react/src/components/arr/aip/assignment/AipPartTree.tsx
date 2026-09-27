import {
    Checkbox,
    FlatTree,
    FlatTreeItem,
    HeadlessFlatTreeItemProps,
    Text,
    Tooltip,
    TreeItemLayout,
    useHeadlessFlatTree_unstable,
    TreeItemValue,
} from "@fluentui/react-components";
import { AddSquare16Regular, LinkRegular, SubtractSquare16Regular } from "@fluentui/react-icons";
import { AipLevelType, ExplorerTreeNode, ExplorerTreeNodeFile, LinkedNodeVO } from "elza-api";
import { useMemo, useState } from "react";
import { defineMessages, useIntl } from "react-intl";
import { useNodeName } from "components/aip/explorer/levels";
import { formatAipSize } from "components/aip/format";
import "./Tree.scss";

const messages = defineMessages({
    linkedTo: { id: "arr.aip.parts.linkedTo", defaultMessage: "Připojeno k: {nodes}" },
    treeLabel: { id: "arr.aip.parts.tree", defaultMessage: "Struktura balíčku" },
    files: {
        id: "arr.aip.parts.files",
        defaultMessage: "{count, plural, one {# soubor} few {# soubory} other {# souborů}}",
    },
    linkedFiles: { id: "arr.aip.parts.linkedFiles", defaultMessage: "připojeno {count}" },
});

/** Druh části balíčku, kterou lze připojit. */
export type PartKind = "level" | "representation" | "file";

/** Část balíčku, kterou uživatel vybral. */
export interface SelectedPart {
    daoId: number;
    kind: PartKind;
    label: string;
}

/** Řádek stromu - úroveň, složka nebo soubor balíčku. */
interface PartItem {
    value: string;
    parentValue?: string;
    label: string;
    /** Jen skutečné části balíčku - úrovně, reprezentace a soubory; složka reprezentace ne. */
    part?: SelectedPart;
    /** Velikost souboru, u složky souhrn skrytých i zobrazených souborů pod ní. */
    size?: number;
    /** Počet souborů pod složkou; u souboru chybí. */
    fileCount?: number;
    /** Kolik ze souborů pod složkou je připojeno (samo nebo s některou nadřízenou částí). */
    linkedFileCount?: number;
    /** Kam jsou soubory pod složkou připojeny. */
    linkedFileNodes?: LinkedNodeVO[];
    /** Kam je část připojena; u souboru i s nadřízenou částí, která ho zahrnuje. */
    linkedNodes: LinkedNodeVO[];
    branch: boolean;
    /** Otevřená při prvním zobrazení - úrovně logické struktury ano, reprezentace ne. */
    openByDefault: boolean;
}

interface Props {
    structure: ExplorerTreeNode;
    selected: SelectedPart[];
    onChange: (selected: SelectedPart[]) => void;
    /** Zobrazit i soubory a komponenty; jinak jen úrovně a reprezentace se souhrnem souborů. */
    showFiles: boolean;
}

export type AipPartTreeProps = Props;

type AnyNode = ExplorerTreeNode | ExplorerTreeNodeFile;

/** Soubory pod složkou, každý jednou (týž soubor může viset na více místech). */
function collectFiles(folder: ExplorerTreeNode, into = new Map<number, number>()): Map<number, number> {
    folder.childFiles?.forEach(f => into.set(f.daoId, f.size ?? 0));
    folder.childFolders?.forEach(child => collectFiles(child, into));
    return into;
}

/** Připojené uzly bez opakování - týž uzel může přijít z více míst. */
const uniqueNodes = (nodes: LinkedNodeVO[]) =>
    nodes.filter((n, i) => nodes.findIndex(o => o.nodeId === n.nodeId) === i);

/**
 * Připojené soubory balíčku a kam jsou připojeny. Soubor je připojený, je-li připojen sám nebo
 * některá část, pod kterou leží - připojená úroveň nebo reprezentace zahrnuje vše pod sebou.
 * Týž soubor leží v reprezentaci i v logické struktuře; připojení kterékoli cesty platí pro obě.
 */
export function linkedFiles(structure: ExplorerTreeNode): Map<number, LinkedNodeVO[]> {
    const result = new Map<number, LinkedNodeVO[]>();
    const walk = (folder: ExplorerTreeNode, inherited: LinkedNodeVO[]) => {
        if (folder.levelType === AipLevelType.Metadata) {
            return;
        }
        const links = [...inherited, ...(folder.linkedNodes ?? [])];
        folder.childFiles?.forEach(file => {
            const fileLinks = [...(result.get(file.daoId) ?? []), ...links, ...(file.linkedNodes ?? [])];
            if (fileLinks.length > 0) {
                result.set(file.daoId, uniqueNodes(fileLinks));
            }
        });
        folder.childFolders?.forEach(child => walk(child, links));
    };
    walk(structure, []);
    return result;
}

/** Soubory balíčku bez metadat, každý jednou. */
export function packageFiles(structure: ExplorerTreeNode): Map<number, number> {
    const files = new Map<number, number>();
    structure.childFolders?.filter(s => s.levelType !== AipLevelType.Metadata).forEach(s => collectFiles(s, files));
    structure.childFiles?.forEach(f => files.set(f.daoId, f.size ?? 0));
    return files;
}

/**
 * Komponenta - úroveň logické struktury, která leží pod jinou úrovní a nese už jen soubory.
 * Rozpoznává se podle tvaru, ne podle typu úrovně: typy se liší podle profilu balíčku.
 */
const isComponent = (folder: ExplorerTreeNode, depth: number) =>
    depth > 0 && (folder.childFolders?.length ?? 0) === 0;

/**
 * Sestaví řádky stromu ze struktury balíčku. Metadata se nenabízejí - popisují balíček, nejsou
 * tím, co se pořádá. Složky uvnitř reprezentace nejsou samostatnými částmi balíčku (nesou
 * digitální entitu celé reprezentace), proto je nelze vybrat; vybrat lze reprezentaci, úroveň
 * logické struktury a soubor.
 *
 * Bez souborů zůstanou jen úrovně (bez komponent) a reprezentace, každá s počtem a velikostí
 * souborů pod ní. Se soubory se komponenta s jediným souborem zobrazí jako ten soubor, aby
 * stejná věc nebyla ve stromu dvakrát pod sebou.
 */
export function buildItems(structure: ExplorerTreeNode, nodeName: (node: never) => string,
                           showFiles: boolean): PartItem[] {
    const items: PartItem[] = [];
    const linked = linkedFiles(structure);

    const add = (node: AnyNode, parentValue: string | undefined, kind: PartKind | undefined,
                 extra: Partial<PartItem>, key = node.uuid ?? node.daoId) => {
        const value = `${parentValue ?? ""}/${key}`;
        const label = nodeName(node as never);
        items.push({
            value,
            parentValue,
            label,
            part: kind != null && node.daoId != null ? { daoId: node.daoId, kind, label } : undefined,
            linkedNodes: node.linkedNodes ?? [],
            branch: false,
            openByDefault: false,
            ...extra,
        });
        return items[items.length - 1];
    };

    const summary = (folder: ExplorerTreeNode): Partial<PartItem> => {
        const files = collectFiles(folder);
        let size = 0;
        const nodes: LinkedNodeVO[] = [];
        let linkedFileCount = 0;
        files.forEach((s, daoId) => {
            size += s;
            const fileLinks = linked.get(daoId);
            if (fileLinks) {
                linkedFileCount++;
                nodes.push(...fileLinks);
            }
        });
        return files.size > 0
            ? { fileCount: files.size, size, linkedFileCount, linkedFileNodes: uniqueNodes(nodes) }
            : {};
    };

    const addFile = (file: ExplorerTreeNodeFile, parentValue: string) =>
        add(file, parentValue, "file", { size: file.size, linkedNodes: linked.get(file.daoId) ?? [] });

    const walkFolder = (folder: ExplorerTreeNode, parent: PartItem, section: AipLevelType, depth: number) => {
        const logical = section === AipLevelType.LogicalStructure;
        if (logical && isComponent(folder, depth)) {
            if (!showFiles) {
                return;
            }
            const only = folder.childFiles?.length === 1 ? folder.childFiles[0] : undefined;
            if (only) {
                // komponenta a její soubor jako jeden řádek; značka připojení platí pro obojí
                parent.branch = true;
                addFile(only, parent.value);
                return;
            }
        }
        // v reprezentacích je části balíčku jen reprezentace sama (první úroveň) a soubory
        const kind: PartKind | undefined = logical ? "level" : depth === 0 ? "representation" : undefined;
        if (!showFiles && !logical && depth > 0) {
            return;
        }
        parent.branch = true;
        const item = add(folder, parent.value, kind, { ...summary(folder), openByDefault: logical });
        folder.childFolders?.forEach(child => walkFolder(child, item, section, depth + 1));
        if (showFiles) {
            folder.childFiles?.forEach(file => { item.branch = true; addFile(file, item.value); });
        }
    };

    for (const section of structure.childFolders ?? []) {
        if (section.levelType === AipLevelType.Metadata) {
            continue;
        }
        const logical = section.levelType === AipLevelType.LogicalStructure;
        const sectionItem = add(section, undefined, undefined, { openByDefault: logical });
        section.childFolders?.forEach(folder => walkFolder(folder, sectionItem, section.levelType!, 0));
        if (showFiles) {
            section.childFiles?.forEach(file => { sectionItem.branch = true; addFile(file, sectionItem.value); });
        }
    }
    return items;
}

/** Identifikátory částí, které lze ve stromu vybrat při daném zobrazení. */
export function selectableDaoIds(structure: ExplorerTreeNode, showFiles: boolean): Set<number> {
    return new Set(buildItems(structure, () => "", showFiles).flatMap(i => i.part ? [i.part.daoId] : []));
}

/**
 * Struktura jednoho balíčku - úrovně logické struktury, reprezentace a případně jejich soubory.
 * Části, které lze připojit, mají zaškrtávátko; už připojené části nesou značku s tím, kam jsou
 * připojeny.
 */
export function AipPartTree({ structure, selected, onChange, showFiles }: Props) {
    const intl = useIntl();
    const nodeName = useNodeName();
    const items = useMemo(() => buildItems(structure, nodeName as never, showFiles), [structure, nodeName, showFiles]);
    // výchozí otevření podle úplného stromu, aby se po zapnutí souborů neměnilo, co je otevřené
    const [openItems, setOpenItems] = useState<Set<TreeItemValue>>(() => new Set(
        buildItems(structure, nodeName as never, true).filter(i => i.branch && i.openByDefault).map(i => i.value)));

    const headlessItems: HeadlessFlatTreeItemProps[] = useMemo(() => items.map(({ value, parentValue, branch }) =>
        ({ value, parentValue, itemType: branch ? "branch" : "leaf" })), [items]);
    const flatTree = useHeadlessFlatTree_unstable(
        headlessItems,
        { openItems, onOpenChange: (_, data) => setOpenItems(data.openItems) },
    );
    const byValue = useMemo(() => new Map(items.map(i => [i.value, i])), [items]);
    const isSelected = (part: SelectedPart) => selected.some(s => s.daoId === part.daoId);

    const toggle = (part: SelectedPart, checked: boolean) => {
        // soubor je ve stromu dvakrát (v reprezentaci i v logické struktuře), vybírá se jednou
        onChange(checked
            ? [...selected.filter(s => s.daoId !== part.daoId), part]
            : selected.filter(s => s.daoId !== part.daoId));
    };

    return (
        <FlatTree {...flatTree.getTreeProps()} aria-label={intl.formatMessage(messages.treeLabel)}
                  className="tree aip-part-tree" size="small">
            {Array.from(flatTree.items(), flatItem => {
                const item = byValue.get(flatItem.value as string)!;
                const treeItemProps = flatItem.getTreeItemProps();
                const linkedTo = item.linkedNodes.map(n => n.name).join(", ");
                const filesLinkedTo = (item.linkedFileNodes ?? []).map(n => n.name).join(", ");
                const sizeText = item.fileCount != null
                    ? `${intl.formatMessage(messages.files, { count: item.fileCount })} · ${formatAipSize(item.size ?? 0)}`
                    : item.size != null ? formatAipSize(item.size) : undefined;
                return (
                    <FlatTreeItem {...treeItemProps} key={item.value}>
                        <TreeItemLayout
                            expandIcon={item.branch
                                ? openItems.has(item.value) ? <SubtractSquare16Regular /> : <AddSquare16Regular />
                                : undefined}

                            aside={<>
                                {sizeText && <Text size={200} className="size">{sizeText}</Text>}
                                {!item.linkedNodes.length && (item.linkedFileCount ?? 0) > 0 &&
                                    <Tooltip content={intl.formatMessage(messages.linkedTo, { nodes: filesLinkedTo })}
                                             relationship="description">
                                        <Text size={200} className="linked-files">
                                            {intl.formatMessage(messages.linkedFiles, { count: item.linkedFileCount })}
                                        </Text>
                                    </Tooltip>}
                                {item.linkedNodes.length > 0 &&
                                    <Tooltip content={intl.formatMessage(messages.linkedTo, { nodes: linkedTo })}
                                             relationship="description">
                                        <LinkRegular aria-label={intl.formatMessage(messages.linkedTo, { nodes: linkedTo })} />
                                    </Tooltip>}
                            </>}
                        >
                            {/* the checkbox is part of the content: iconBefore is hidden from assistive technologies */}
                            {item.part
                                ? <Checkbox checked={isSelected(item.part)} label={item.label} size="medium"
                                            onClick={e => e.stopPropagation()}
                                            onChange={(_, data) => toggle(item.part!, data.checked === true)} />
                                : item.label}
                        </TreeItemLayout>
                    </FlatTreeItem>
                );
            })}
        </FlatTree>
    );
}
