import { Button, Spinner } from "@fluentui/react-components";
import { AddRegular } from "@fluentui/react-icons";
import { modalDialogShow } from "actions/global/modalDialog";
import { MandatoryType } from "elza-api";
import { ReactNode, useMemo } from "react";
import { useIntl } from "react-intl";
import { DescItemTypeRef } from "typings/store";
import { useAppThunkDispatch } from "utils/hooks";
import { useUserSettings } from "contexts/user";
import { AddDescItemTypeForm } from "components/arr/item-form/AddDescItemType";
import { ItemFormBody } from "components/arr/item-form/ItemFormBody";
import { messages } from "components/arr/item-form/messages";
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

    function handleAddDescItemType() {
        dispatch(
            modalDialogShow(null, formatMessage(messages.addDescItemTitle), ({ onClose }) => (
                <AddDescItemTypeForm
                    itemTypes={itemTypes}
                    descItems={allItems.map(({ item }) => item)}
                    onSubmit={(typeRefs) => {
                        typeRefs.forEach((typeRef) => addEmptyItem(typeRef.id));
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
            <ItemFormBody
                formItems={formItems}
                forcedFormItems={forcedFormItems}
                addedFormItems={addedFormItems}
                itemTypes={itemTypes}
                columnCount={settings.groupColumns || 1}
                plain={plain}
                addEmptyDescItem={addEmptyItem}
                deleteDescItem={deleteItem}
                createDescItem={createItem}
                updateDescItem={updateItem}
                hideCopyButtons={true}
                renderExtraActions={renderExtraActions}
            />
        </div>
    );
}
