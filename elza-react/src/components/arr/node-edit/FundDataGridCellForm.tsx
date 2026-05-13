import { Popover, PopoverSurface, Spinner } from "@fluentui/react-components";
import { copyDescItemType, nocopyDescItemType } from "actions/arr/nodeSetting";
import { WebApi } from "actions";
import { useEffect, useMemo } from "react";
import { useAppThunkDispatch } from "utils/hooks";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useActiveFund, useActiveParent, useNodeFormData } from "./hooks";
import { NodeFormContext } from "./NodeFormContext";
import { DescItemTypeFields } from "./DescItemTypeFields";
import { useStyles } from "./styles";

interface Props {
    fondsVersionId: number;
    nodeId: number;
    nodeVersionId?: number;
    descItemTypeId: number;
    target: HTMLElement;
    onClose: () => void;
}

export function FundDataGridCellForm({ fondsVersionId, nodeId, nodeVersionId, descItemTypeId, target, onClose }: Props) {
    const dispatch = useAppThunkDispatch();
    const activeParent = useActiveParent();
    const activeFund = useActiveFund();
    const styles = useStyles();

    const itemTypeRefs = useAppSelector(({ refTables }) => refTables.descItemTypes.itemsMap);
    const groupRefs = useAppSelector(({ refTables }) => refTables.groups.data);
    const nodeSetting = useAppSelector(({ arrRegion }) =>
        arrRegion.nodeSettings.nodes.find(({ id }) => id === activeParent?.id),
    );

    const nodeFormData = useNodeFormData(fondsVersionId, nodeId, nodeVersionId);
    const {
        formItems,
        forcedFormItems,
        addedFormItems,
        itemTypes,
        addEmptyDescItem,
        deleteDescItem,
        createDescItem: createDescItemBase,
        updateDescItem: updateDescItemBase,
    } = nodeFormData;

    async function createDescItem(item: any, localId: string) {
        await createDescItemBase(item, localId);
        const isSingleItem = !descItemTypeEntry || descItemTypeEntry.descItems.length <= 1;
        const hasData = item.data?.dataId != null;
        if (isSingleItem && hasData) {
            onClose();
        }
    }

    async function updateDescItem(item: any, localId?: string) {
        await updateDescItemBase(item, localId);
        const isSingleItem = !descItemTypeEntry || descItemTypeEntry.descItems.length <= 1;
        const hasData = item.data?.dataId != null;
        if (isSingleItem && hasData) {
            onClose();
        }
    }

    const descItemTypeEntry = useMemo(() => {
        if (!formItems || !groupRefs || !itemTypeRefs) { return null; }

        const typeRef = itemTypeRefs[descItemTypeId];
        const typeForm = itemTypes?.find(({ itemTypeId }) => itemTypeId === descItemTypeId);
        if (!typeRef || !typeForm) { return null; }

        let typeWidth: number | undefined;
        for (const id of groupRefs.ids) {
            const group = groupRefs[id];
            const found = group.itemTypes.find((itemType) => itemType.id === descItemTypeId);
            if (found) { typeWidth = found.width; break; }
        }

        const allItems = [...formItems, ...forcedFormItems, ...addedFormItems];
        const descItems = allItems.filter(({ item }) => item.itemTypeId === descItemTypeId);

        return { typeRef, typeForm, typeWidth, descItems };
    }, [formItems, forcedFormItems, addedFormItems, itemTypes, groupRefs, itemTypeRefs, descItemTypeId]);

    useEffect(() => {
        if (descItemTypeEntry && descItemTypeEntry.descItems.length === 0) {
            addEmptyDescItem(descItemTypeId);
        }
    }, [descItemTypeEntry]);

    async function handleCopyFromPrev(typeId: number) {
        await WebApi.copyOlderSiblingAttribute(activeFund.versionId, nodeId, nodeVersionId, typeId);
    }

    async function handleCopyToggle(typeId: number) {
        const copy = nodeSetting?.descItemTypeCopyIds?.includes(typeId);
        if (!copy) {
            dispatch(copyDescItemType(activeParent.id, typeId));
        } else {
            dispatch(nocopyDescItemType(activeParent.id, typeId));
        }
    }

    const isFirstNode =
        activeParent.childNodes.findIndex((node: any) => node.id === nodeId) === 0;

    return (
        <Popover
            open
            onOpenChange={(_e, data) => { if (!data.open) { setTimeout(onClose, 0); } }}
            positioning={{
                target,
                position: "below",
                align: "start",
                offset: { mainAxis: -target.getBoundingClientRect().height },
                // flipBoundary: "viewport",
                // overflowBoundary: "viewport"
            }}
        >
            <PopoverSurface className={styles.fundDataGridPopover}>
                {!descItemTypeEntry ? <Spinner /> : (
                    <NodeFormContext.Provider value={nodeFormData}>
                        <DescItemTypeFields
                            typeRef={descItemTypeEntry.typeRef}
                            typeForm={descItemTypeEntry.typeForm}
                            typeWidth={descItemTypeEntry.typeWidth}
                            descItems={descItemTypeEntry.descItems}
                            fondsVersionId={fondsVersionId}
                            nodeId={nodeId}
                            nodeVersionId={nodeVersionId}
                            nodeSetting={nodeSetting}
                            isFirstNode={isFirstNode}
                            handleCopyFromPrev={handleCopyFromPrev}
                            handleCopyToggle={handleCopyToggle}
                            addEmptyDescItem={addEmptyDescItem}
                            deleteDescItem={deleteDescItem}
                            createDescItem={createDescItem}
                            updateDescItem={updateDescItem}
                            hideCopyButtons
                        />
                    </NodeFormContext.Provider>
                )}
            </PopoverSurface>
        </Popover>
    );
}
