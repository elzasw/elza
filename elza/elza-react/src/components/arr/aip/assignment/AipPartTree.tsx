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
    size?: number;
    linkedNodes: LinkedNodeVO[];
    branch: boolean;
}

interface Props {
    structure: ExplorerTreeNode;
    selected: SelectedPart[];
    onChange: (selected: SelectedPart[]) => void;
}

export type AipPartTreeProps = Props;

type AnyNode = ExplorerTreeNode | ExplorerTreeNodeFile;

/**
 * Sestaví řádky stromu ze struktury balíčku. Metadata se nenabízejí - popisují balíček, nejsou
 * tím, co se pořádá. Složky uvnitř reprezentace nejsou samostatnými částmi balíčku (nesou
 * digitální entitu celé reprezentace), proto je nelze vybrat; vybrat lze reprezentaci, úroveň
 * logické struktury a soubor.
 */
function buildItems(structure: ExplorerTreeNode, nodeName: (node: never) => string): PartItem[] {
    const items: PartItem[] = [];

    const add = (node: AnyNode, parentValue: string | undefined, kind: PartKind | undefined, isFile: boolean) => {
        const value = `${parentValue ?? ""}/${node.uuid ?? node.daoId}`;
        const folder = node as ExplorerTreeNode;
        const file = node as ExplorerTreeNodeFile;
        const hasChildren = !isFile && ((folder.childFolders?.length ?? 0) + (folder.childFiles?.length ?? 0)) > 0;
        const label = nodeName(node as never);
        items.push({
            value,
            parentValue,
            label,
            part: kind != null && node.daoId != null ? { daoId: node.daoId, kind, label } : undefined,
            size: isFile ? file.size : undefined,
            linkedNodes: node.linkedNodes ?? [],
            branch: hasChildren,
        });
        return value;
    };

    const walkFolder = (folder: ExplorerTreeNode, parentValue: string, section: AipLevelType, depth: number) => {
        // v reprezentacích je části balíčku jen reprezentace sama (první úroveň) a soubory
        const kind: PartKind | undefined = section === AipLevelType.LogicalStructure ? "level"
            : depth === 0 ? "representation" : undefined;
        const value = add(folder, parentValue, kind, false);
        folder.childFolders?.forEach(child => walkFolder(child, value, section, depth + 1));
        folder.childFiles?.forEach(file => add(file, value, "file", true));
    };

    for (const section of structure.childFolders ?? []) {
        if (section.levelType === AipLevelType.Metadata) {
            continue;
        }
        const sectionValue = add(section, undefined, undefined, false);
        section.childFolders?.forEach(folder => walkFolder(folder, sectionValue, section.levelType!, 0));
        section.childFiles?.forEach(file => add(file, sectionValue, "file", true));
    }
    return items;
}

/**
 * Struktura jednoho balíčku - úrovně logické struktury, reprezentace a jejich soubory. Části,
 * které lze připojit, mají zaškrtávátko; už připojené části nesou značku s tím, kam jsou
 * připojeny.
 */
export function AipPartTree({ structure, selected, onChange }: Props) {
    const intl = useIntl();
    const nodeName = useNodeName();
    const items = useMemo(() => buildItems(structure, nodeName as never), [structure, nodeName]);
    const [openItems, setOpenItems] = useState<Set<TreeItemValue>>(
        () => new Set(items.filter(i => i.branch).map(i => i.value)));

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
        <FlatTree {...flatTree.getTreeProps()} aria-label={intl.formatMessage(messages.treeLabel)} className="tree">
            {Array.from(flatTree.items(), flatItem => {
                const item = byValue.get(flatItem.value as string)!;
                const treeItemProps = flatItem.getTreeItemProps();
                const linkedTo = item.linkedNodes.map(n => n.name).join(", ");
                return (
                    <FlatTreeItem {...treeItemProps} key={item.value}>
                        <TreeItemLayout
                            expandIcon={item.branch
                                ? openItems.has(item.value) ? <SubtractSquare16Regular /> : <AddSquare16Regular />
                                : undefined}

                            aside={<>
                                {item.size != null && <Text size={200}>{formatAipSize(item.size)}</Text>}
                                {item.linkedNodes.length > 0 &&
                                    <Tooltip content={intl.formatMessage(messages.linkedTo, { nodes: linkedTo })}
                                             relationship="description">
                                        <LinkRegular aria-label={intl.formatMessage(messages.linkedTo, { nodes: linkedTo })} />
                                    </Tooltip>}
                            </>}
                        >
                            {/* the checkbox is part of the content: iconBefore is hidden from assistive technologies */}
                            {item.part
                                ? <Checkbox checked={isSelected(item.part)} label={item.label}
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
