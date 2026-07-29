import { Button, useFocusFinders } from "@fluentui/react-components";
import { AddRegular } from "@fluentui/react-icons";
import { FormItemType } from "elza-api";
import { ReactNode, useEffect, useRef } from "react";
import { DescItemTypeRef } from "typings/store";
import { useUserSettings } from "contexts/user";
import { useStyles } from "./styles";
import { DraggableList } from "./DraggableList";
import { DescItemTypeHeader } from "./DescItemTypeHeader";
import { DescItemField } from "./desc-items";
import { DescItemInfo } from "./NodeDebugInfo";
import { FormItem } from "./formItems";

interface DescItem {
    item: FormItem["item"];
    localId: string;
    forcedDisplayString?: string;
}

interface Props {
    typeRef: DescItemTypeRef;
    typeForm?: FormItemType;
    typeWidth: number;
    descItems: DescItem[];
    fondsVersionId?: number;
    nodeId?: number;
    nodeVersionId?: number;
    nodeSetting?: any;
    isFirstNode: boolean;
    isAnonymousStructured?: boolean;
    handleCopyFromPrev: (descItemTypeId: number) => void;
    handleCopyToggle: (descItemTypeId: number) => void;
    getOpenInDataGridHref?: (descItemTypeId: number) => string;
    onOpenInDataGrid?: (descItemTypeId: number) => void;
    addEmptyDescItem: (typeId: number, specId?: number, position?: number) => string | void;
    deleteDescItem: (item: any, localId: string) => Promise<void>;
    createDescItem: (item: any, localId: string) => Promise<any>;
    updateDescItem: (item: any, localId?: string) => void | Promise<void>;
    autoFocusLocalId?: string;
    onAutoFocusTaken?: () => void;
    autoFocusOnOpen?: boolean;
    hideCopyButtons?: boolean;
    renderExtraActions?: (typeRef: DescItemTypeRef) => ReactNode;
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
    isAnonymousStructured = false,
    handleCopyFromPrev,
    handleCopyToggle,
    getOpenInDataGridHref,
    onOpenInDataGrid,
    addEmptyDescItem,
    deleteDescItem,
    createDescItem,
    updateDescItem,
    autoFocusLocalId,
    onAutoFocusTaken,
    autoFocusOnOpen = false,
    hideCopyButtons = false,
    renderExtraActions,
}: Props) {
    const { settings } = useUserSettings();
    const compact = settings.compact;
    const styles = useStyles();
    const { findFirstFocusable } = useFocusFinders();

    // Row containers keyed by localId; populated via ref callbacks so focusing a newly
    // added field doesn't depend on re-renders.
    const rowRefs = useRef(new Map<string, HTMLDivElement>());
    const addButtonRef = useRef<HTMLButtonElement>(null);
    const autoFocusOnOpenDone = useRef(false);

    // Focus a freshly added field once it has mounted in this instance. The target
    // localId is owned by NodeEdit so both add paths work (per-type "+" button and the
    // "add item type" modal, which add to different DescItemTypeFields instances).
    useEffect(() => {
        if (!autoFocusLocalId) {
            return;
        }
        const row = rowRefs.current.get(autoFocusLocalId);
        if (!row) {
            return;
        }
        findFirstFocusable(row)?.focus();
        onAutoFocusTaken?.();
    }, [autoFocusLocalId, findFirstFocusable, onAutoFocusTaken]);

    function handleChangeOrder(index: number, newIndex: number) {
        const item = descItems[index].item;
        let newPosition = descItems[newIndex]?.item.position;

        if (newPosition == null) {
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

    const lastItem = sortedDescItems[sortedDescItems.length - 1];
    const hasNoItems = sortedDescItems.length === 0;

    const repeatableWithoutEmptyItem =
        typeForm?.repeatable &&
        ((lastItem?.item.data?.dataId != undefined && !lastItem?.item.undefined) ||
            typeRef.useSpecification);

    // Anonymous structured fields have no auto-added empty placeholder, so they need an
    // explicit "+" whenever another value may still be added: always when repeatable,
    // and while still empty when not.
    const anonymousStructuredNeedsButton =
        isAnonymousStructured && (typeForm?.repeatable || hasNoItems);

    const showAddButton = repeatableWithoutEmptyItem || anonymousStructuredNeedsButton;

    const lastEditableLocalId = [...sortedDescItems].reverse().find(
        ({ item, forcedDisplayString }) =>
            !item.undefined && !item.inhibited && item.nodeId == nodeId && forcedDisplayString == undefined,
    )?.localId;

    // Move focus into the popover once, when opened as an edit form. Repeatable types focus
    // the add button (ready to append a value); everything else focuses the last editable
    // field, falling back to the add button when no field is editable. The guard locks only
    // after focus actually lands, so the effect can re-run as async rows (e.g. an auto-added
    // empty field) mount instead of prematurely settling on the add button.
    const focusAddButtonOnOpen = showAddButton && typeForm?.repeatable === true;
    useEffect(() => {
        if (!autoFocusOnOpen || autoFocusOnOpenDone.current) {
            return;
        }
        let focusable: HTMLElement | null = null;
        if (focusAddButtonOnOpen) {
            focusable = addButtonRef.current;
        } else {
            const row = lastEditableLocalId ? rowRefs.current.get(lastEditableLocalId) : undefined;
            focusable = row ? findFirstFocusable(row) ?? null : null;
            if (!focusable && showAddButton) {
                focusable = addButtonRef.current;
            }
        }
        if (!focusable) {
            return;
        }
        focusable.focus();
        autoFocusOnOpenDone.current = true;
    }, [autoFocusOnOpen, focusAddButtonOnOpen, lastEditableLocalId, showAddButton, findFirstFocusable]);

    return (
        <DescItemTypeHeader
            typeForm={typeForm}
            typeRef={typeRef}
            typeWidth={typeWidth}
            nodeSettings={nodeSetting}
            handleCopyFromPrev={handleCopyFromPrev}
            handleCopyToggle={handleCopyToggle}
            getOpenInDataGridHref={getOpenInDataGridHref}
            onOpenInDataGrid={onOpenInDataGrid}
            canCopyFromPrev={!isFirstNode}
            hideCopyButtons={hideCopyButtons}
            extraActions={renderExtraActions?.(typeRef)}
        >
            <DraggableList
                canPlaceBeforeItem={(index) => descItems[index].item.nodeId == nodeId}
                isItemDraggable={(index) => {
                    const isDraggable =
                        descItems[index].item.nodeId == nodeId &&
                        (descItems[index].item.data?.dataId != undefined ||
                            descItems[index].item.undefined);
                    return isDraggable;
                }}
                onChangeOrder={handleChangeOrder}
            >
                {sortedDescItems.map(({ item, localId, forcedDisplayString }) => (
                    <div
                        key={localId}
                        ref={(node) => {
                            if (node) {
                                rowRefs.current.set(localId, node);
                            } else {
                                rowRefs.current.delete(localId);
                            }
                        }}
                        style={{ container: "desc-item-container" }}
                    >

                        <div>
                            <DescItemField
                                typeRef={typeRef}
                                typeForm={typeForm}
                                item={item}
                                forcedDisplayString={forcedDisplayString}
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
                    ref={addButtonRef}
                    className={styles.addDescItemButton}
                    size={compact ? "small" : "medium"}
                    icon={<AddRegular />}
                    onClick={() => {
                        const nextPosition =
                            lastItem?.item.position > 0 ? lastItem.item.position + 1 : 1;
                        addEmptyDescItem(typeRef.id, undefined, nextPosition);
                    }}
                    tabIndex={autoFocusOnOpen ? 0 : -1}
                >
                    {typeRef.shortcut}
                </Button>
            )}
        </DescItemTypeHeader>
    );
}
