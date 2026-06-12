import { Button, Checkbox, Spinner, Tooltip } from "@fluentui/react-components";
import {
    AddRegular,
    DeleteRegular,
    LockClosedRegular,
    LockOpenRegular,
} from "@fluentui/react-icons";
import { DataType, MandatoryType } from "elza-api";
import { forwardRef, useImperativeHandle, useMemo } from "react";
import { defineMessages, useIntl } from "react-intl";
import { modalDialogShow } from "actions/global/modalDialog";
import { DescItemTypeRef } from "typings/store";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useAppThunkDispatch } from "utils/hooks";
import { useUserSettings } from "contexts/user";
import { AddDescItemTypeForm } from "components/arr/node-edit/AddDescItemType";
import { DescItemTypeFields } from "components/arr/node-edit/DescItemTypeFields";
import { FormItemGroup } from "components/arr/node-edit/FormItemGroup";
import { GroupColumns } from "components/arr/node-edit/GroupColumns";
import { buildGroupsForm } from "components/arr/node-edit/utils";
import { FormItem } from "components/arr/node-edit/hooks";
import { EditItem } from "components/arr/node-edit/types";
import {
    MultiFormItem,
    SdoBatchPayload,
    useMultiStructureFormData,
} from "./multiHooks";

const messages = defineMessages({
    lock: { id: "arr.structure.modal.updateMultiple.lock", defaultMessage: "Upravit hodnotu" },
    unlock: { id: "arr.structure.modal.updateMultiple.unlock", defaultMessage: "Ponechat původní hodnotu" },
    delete: { id: "arr.structure.modal.updateMultiple.delete", defaultMessage: "Smazat hodnotu" },
    increment: { id: "arr.structure.modal.increment", defaultMessage: "Inkrementovat" },
    originalValue: { id: "arr.structure.modal.updateMultiple.originalValue", defaultMessage: "Původní hodnota" },
    deletedValue: { id: "arr.structure.modal.updateMultiple.deletedValue", defaultMessage: "Smazaná hodnota" },
    addDescItemTitle: { id: "subNodeForm.descItemType.title.add", defaultMessage: "Přidat prvek popisu" },
    addDescItem: { id: "node_action_addDescItem", defaultMessage: "Přidat prvek popisu" },
});

interface Props {
    fundId: number;
    fundVersionId: number;
    structureObjectId: number;
    structureObjectIds: number[];
}

export type { Props as MultiStructureEditProps };

export interface MultiStructureEditHandle {
    buildPayload: () => SdoBatchPayload;
}

function placeholderItem(itemTypeId: number, dataType: DataType): EditItem {
    return {
        itemTypeId,
        position: 0,
        readOnly: true,
        data: { dataType } as EditItem["data"],
    };
}

export const MultiStructureEdit = forwardRef<MultiStructureEditHandle, Props>(
    function MultiStructureEdit(
        { fundId, fundVersionId, structureObjectId, structureObjectIds },
        ref,
    ) {
        const { formatMessage } = useIntl();
        const { settings } = useUserSettings();
        const dispatch = useAppThunkDispatch();

        const itemTypeRefs = useAppSelector(({ refTables }) => refTables.descItemTypes.itemsMap);
        const dataTypeRefs = useAppSelector(({ refTables }) => refTables.rulDataTypes.itemsMap);
        const groupRefs = useAppSelector(({ refTables }) => refTables.groups.data);

        const {
            itemTypes,
            allItemTypes,
            visibleTypeIds,
            isLoading,
            formItemsByType,
            getTypeMode,
            isAutoincremented,
            setModified,
            addType,
            setLocked,
            toggleDeleted,
            toggleAutoincrement,
            addEmptyItem,
            updateItem,
            deleteItem,
            buildPayload,
        } = useMultiStructureFormData(fundId, fundVersionId, structureObjectId, structureObjectIds);

        useImperativeHandle(ref, () => ({ buildPayload }), [buildPayload]);

        const itemsForType = useMemo(() => {
            const map: Record<number, MultiFormItem[]> = {};
            itemTypes.forEach(({ itemTypeId }) => {
                const mode = getTypeMode(itemTypeId);
                if (mode === "modified") {
                    map[itemTypeId] = formItemsByType[itemTypeId] ?? [];
                } else {
                    const label = formatMessage(
                        mode === "deleted" ? messages.deletedValue : messages.originalValue,
                    );
                    const dataType = dataTypeRefs[itemTypeRefs[itemTypeId]?.dataTypeId]?.code as DataType;
                    map[itemTypeId] = [
                        {
                            localId: `placeholder-${itemTypeId}`,
                            item: placeholderItem(itemTypeId, dataType),
                            forcedDisplayString: label,
                        },
                    ];
                }
            });
            return map;
        }, [itemTypes, formItemsByType, getTypeMode, formatMessage, itemTypeRefs, dataTypeRefs]);

        const allItems: FormItem[] = useMemo(
            () => Object.values(itemsForType).flat(),
            [itemsForType],
        );

        const groups = useMemo(
            () => buildGroupsForm(allItems, itemTypes, groupRefs, itemTypeRefs),
            [allItems, itemTypes, groupRefs, itemTypeRefs],
        );

        function renderTypeActions(typeRef: DescItemTypeRef) {
            const typeId = typeRef.id;
            const mode = getTypeMode(typeId);
            const isModified = mode === "modified";
            const isDeleted = mode === "deleted";
            const isInt = dataTypeRefs[typeRef.dataTypeId]?.code === DataType.Int;

            return (
                <>
                    <Tooltip
                        relationship="label"
                        appearance="inverted"
                        content={formatMessage(isModified ? messages.unlock : messages.lock)}
                    >
                        <Button
                            size="small"
                            appearance={isModified ? "primary" : "subtle"}
                            icon={isModified ? <LockOpenRegular /> : <LockClosedRegular />}
                            onClick={() => (isModified ? setLocked(typeId) : setModified(typeId))}
                            tabIndex={-1}
                        />
                    </Tooltip>
                    <Tooltip relationship="label" appearance="inverted" content={formatMessage(messages.delete)}>
                        <Button
                            size="small"
                            appearance={isDeleted ? "primary" : "subtle"}
                            icon={<DeleteRegular />}
                            onClick={() => toggleDeleted(typeId)}
                            tabIndex={-1}
                        />
                    </Tooltip>
                    {isModified && isInt && (
                        <Checkbox
                            checked={isAutoincremented(typeId)}
                            onChange={() => toggleAutoincrement(typeId)}
                            label={formatMessage(messages.increment)}
                        />
                    )}
                </>
            );
        }

        function handleAddDescItemType() {
            dispatch(
                modalDialogShow(null, formatMessage(messages.addDescItemTitle), ({ onClose }) => (
                    <AddDescItemTypeForm
                        itemTypes={allItemTypes}
                        descItems={visibleTypeIds.map((itemTypeId) => ({ itemTypeId }))}
                        onSubmit={(typeRef) => {
                            addType(typeRef.id);
                            onClose();
                        }}
                        onClose={onClose}
                    />
                )),
            );
        }

        if (isLoading) {
            return <Spinner />;
        }

        const hasAddableTypes = allItemTypes.some(
            ({ itemTypeId, type }) => type === MandatoryType.Possible && !visibleTypeIds.includes(itemTypeId),
        );

        return (
            <div>
                {hasAddableTypes && (
                    <div style={{ marginLeft: "4px" }}>
                        <Button appearance="primary" icon={<AddRegular />} onClick={handleAddDescItemType}>
                            {formatMessage(messages.addDescItem)}
                        </Button>
                    </div>
                )}
                <GroupColumns groups={groups} columnCount={settings.groupColumns || 1}>
                {({ group, descItemTypes }) => (
                    <FormItemGroup key={group.code} group={group} plain={true}>
                        {descItemTypes.map(({ typeRef, typeForm, typeWidth, descItems }) => {
                            const isModified = getTypeMode(typeRef.id) === "modified";
                            return (
                                <DescItemTypeFields
                                    key={typeRef.id}
                                    typeRef={typeRef}
                                    typeForm={typeForm}
                                    typeWidth={typeWidth}
                                    descItems={descItems}
                                    nodeSetting={undefined}
                                    isFirstNode={true}
                                    handleCopyFromPrev={() => {}}
                                    handleCopyToggle={() => {}}
                                    addEmptyDescItem={() => isModified && addEmptyItem(typeRef.id)}
                                    deleteDescItem={async (_item, localId) => deleteItem(typeRef.id, localId)}
                                    createDescItem={async (item, localId) => {
                                        updateItem(typeRef.id, localId, item);
                                        return {};
                                    }}
                                    updateDescItem={(item, localId) => updateItem(typeRef.id, localId ?? "", item)}
                                    hideCopyButtons={true}
                                    renderExtraActions={renderTypeActions}
                                />
                            );
                        })}
                    </FormItemGroup>
                )}
                </GroupColumns>
            </div>
        );
    },
);
