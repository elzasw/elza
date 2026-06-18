import { Button, Spinner } from "@fluentui/react-components";
import { AddRegular } from "@fluentui/react-icons";
import { modalDialogShow } from "actions/global/modalDialog";
import { MandatoryType } from "elza-api";
import { ReactNode, useMemo } from "react";
import { useIntl } from "react-intl";
import { DescItemTypeRef } from "typings/store";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useAppThunkDispatch } from "utils/hooks";
import { useUserSettings } from "contexts/user";
import { AddDescItemTypeForm } from "components/arr/node-edit/AddDescItemType";
import { DescItemTypeFields } from "components/arr/node-edit/DescItemTypeFields";
import { FormItemGroup } from "components/arr/node-edit/FormItemGroup";
import { GroupColumns } from "components/arr/node-edit/GroupColumns";
import { buildGroupsForm } from "components/arr/node-edit/utils";
import { messages } from "components/arr/node-edit/messages";
import { useStructureFormData } from "./hooks";

interface Props {
    fundId: number;
    fundVersionId: number;
    structureObjectId: number;
    plain?: boolean;
    confirmOnCreate?: boolean;
    renderExtraActions?: (typeRef: DescItemTypeRef) => ReactNode;
}

export type { Props as StructureEditProps };

export function StructureEdit({ fundId, fundVersionId, structureObjectId, plain = false, confirmOnCreate = false, renderExtraActions }: Props) {
    const dispatch = useAppThunkDispatch();
    const { formatMessage } = useIntl();
    const { settings } = useUserSettings();

    const itemTypeRefs = useAppSelector(({ refTables }) => refTables.descItemTypes.itemsMap);
    const groupRefs = useAppSelector(({ refTables }) => refTables.groups.data);

    const {
        formItems,
        forcedFormItems,
        addedFormItems,
        itemTypes,
        isLoading,
        addEmptyItem,
        createItem,
        updateItem,
        deleteItem,
    } = useStructureFormData(fundId, fundVersionId, structureObjectId, { confirmOnCreate });

    const allItems = useMemo(
        () => [...formItems, ...forcedFormItems, ...addedFormItems],
        [formItems, forcedFormItems, addedFormItems],
    );

    const groups = useMemo(
        () => buildGroupsForm(allItems, itemTypes, groupRefs, itemTypeRefs),
        [allItems, itemTypes, groupRefs, itemTypeRefs],
    );

    function handleAddDescItemType() {
        dispatch(
            modalDialogShow(null, formatMessage(messages.addDescItemTitle), ({ onClose }) => (
                <AddDescItemTypeForm
                    itemTypes={itemTypes}
                    descItems={allItems.map(({ item }) => item)}
                    onSubmit={(typeRef) => {
                        addEmptyItem(typeRef.id);
                        onClose();
                    }}
                    onClose={onClose}
                />
            )),
        );
    }

    const hasPossibleTypes = itemTypes.some(({ type }) => type === MandatoryType.Possible);

    if (isLoading) {
        return <Spinner />;
    }

    return (
        <div style={{width: '100%'}}>
            {hasPossibleTypes && (
                <div style={{margin: '4px 0 0 4px'}}>
                    <Button appearance="primary" icon={<AddRegular />} onClick={handleAddDescItemType}>
                        {formatMessage(messages.addDescItem)}
                    </Button>
                </div>
            )}
            <GroupColumns groups={groups} columnCount={settings.groupColumns || 1}>
                {({ group, descItemTypes }) => (
                    <FormItemGroup key={group.code} group={group} plain={plain}>
                        {descItemTypes.map(({ typeRef, typeForm, typeWidth, descItems }) => (
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
                                addEmptyDescItem={addEmptyItem}
                                deleteDescItem={deleteItem}
                                createDescItem={createItem}
                                updateDescItem={updateItem}
                                hideCopyButtons={true}
                                renderExtraActions={renderExtraActions}
                            />
                        ))}
                    </FormItemGroup>
                )}
            </GroupColumns>
        </div>
    );
}
