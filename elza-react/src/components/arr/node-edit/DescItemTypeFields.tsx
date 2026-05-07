import { Button } from "@fluentui/react-components";
import { AddRegular } from "@fluentui/react-icons";
import { FormItemType } from "elza-api";
import { DescItemTypeRef } from "typings/store";
import { useUserSettings } from "contexts/user";
import { useStyles } from "./styles";
import { DraggableList } from "./DraggableList";
import { DescItemTypeHeader } from "./DescItemTypeHeader";
import { DescItemField } from "./desc-items";
import { DescItemInfo } from "./NodeDebugInfo";
import { FormItem } from "./hooks";

interface DescItem {
    item: FormItem["item"];
    localId: string;
}

interface Props {
    typeRef: DescItemTypeRef;
    typeForm: FormItemType;
    typeWidth: number;
    descItems: DescItem[];
    fondsVersionId: number;
    nodeId: number;
    nodeVersionId: number;
    nodeSetting?: any;
    isFirstNode: boolean;
    handleCopyFromPrev: (descItemTypeId: number) => void;
    handleCopyToggle: (descItemTypeId: number) => void;
    addEmptyDescItem: (typeId: number, specId?: number, position?: number) => void;
    deleteDescItem: (item: any, localId: string) => Promise<void>;
    createDescItem: (item: any, localId: string) => Promise<any>;
    updateDescItem: (item: any, localId?: string) => void | Promise<void>;
    hideCopyButtons?: boolean;
}

export function DescItemTypeFields({
    typeRef,
    typeForm,
    typeWidth,
    descItems,
    fondsVersionId,
    nodeId,
    nodeVersionId,
    nodeSetting,
    isFirstNode,
    handleCopyFromPrev,
    handleCopyToggle,
    addEmptyDescItem,
    deleteDescItem,
    createDescItem,
    updateDescItem,
    hideCopyButtons = false,
}: Props) {
    const { settings } = useUserSettings();
    const compact = settings.compact;
    const styles = useStyles();

    function handleChangeOrder(index: number, newIndex: number) {
        const item = descItems[index].item;
        let newPosition = descItems[newIndex]?.item.position;

        if (!newPosition) {
            newPosition = descItems[descItems.length - 1].item.position + 1;
        }

        if (newPosition > item.position) {
            newPosition = newPosition - 1;
        }
        updateDescItem({ ...item, position: newPosition });
    }

    const sortedDescItems = [...descItems].sort(
        ({ item: { position: positionA } }, { item: { position: positionB } }) =>
            positionA - positionB,
    );

    const lastItem = descItems[descItems.length - 1];
    const showAddButton =
        typeForm.repeatable &&
        ((lastItem?.item.data?.dataId != undefined && !lastItem?.item.undefined) ||
            typeRef.useSpecification);

    return (
        <DescItemTypeHeader
            typeForm={typeForm}
            typeRef={typeRef}
            typeWidth={typeWidth}
            nodeSettings={nodeSetting}
            handleCopyFromPrev={handleCopyFromPrev}
            handleCopyToggle={handleCopyToggle}
            canCopyFromPrev={!isFirstNode}
            hideCopyButtons={hideCopyButtons}
        >
            <DraggableList
                canPlaceBeforeItem={(index) => descItems[index].item.position > 0}
                isItemDraggable={(index) => {
                    const isDraggable =
                        descItems[index].item.position > 0 &&
                        (descItems[index].item.data?.dataId != undefined ||
                            descItems[index].item.undefined);
                    return isDraggable;
                }}
                onChangeOrder={handleChangeOrder}
            >
                {sortedDescItems.map(({ item, localId }) => (
                    <div key={localId} style={{ container: "desc-item-container" }}>
                        <div>
                            <DescItemField
                                typeRef={typeRef}
                                typeForm={typeForm}
                                item={item}
                                fondsVersionId={fondsVersionId}
                                nodeId={nodeId}
                                nodeVersionId={nodeVersionId}
                                typeWidth={typeWidth}
                                onDelete={(item) => deleteDescItem(item, localId)}
                                onCreate={(item) => createDescItem(item, localId)}
                                onUpdate={(item) => Promise.resolve(updateDescItem(item, localId))}
                            />
                        </div>
                        <DescItemInfo item={item} typeForm={typeForm} localId={localId} nodeId={nodeId} />
                    </div>
                ))}
            </DraggableList>
            {showAddButton && (
                <Button
                    className={styles.addDescItemButton}
                    size={compact ? "small" : "medium"}
                    icon={<AddRegular />}
                    onClick={() => {
                        const nextPosition =
                            lastItem.item.position > 0 ? lastItem.item.position + 1 : 1;
                        addEmptyDescItem(typeRef.id, undefined, nextPosition);
                    }}
                    tabIndex={-1}
                >
                    {typeRef.shortcut}
                </Button>
            )}
        </DescItemTypeHeader>
    );
}
