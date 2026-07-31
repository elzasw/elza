import { Button, Spinner, Tooltip } from "@fluentui/react-components";
import { AddRegular, CalculatorRegular, CalculatorMultipleRegular } from "@fluentui/react-icons";
import { modalDialogShow } from "actions/global/modalDialog";
import { MandatoryType } from "elza-api";
import { useMemo } from "react";
import { useIntl } from "react-intl";
import { DescItemTypeRef } from "typings/store";
import { useAppThunkDispatch } from "utils/hooks";
import { useUserSettings } from "contexts/user";
import { AddDescItemTypeForm } from "components/arr/item-form/AddDescItemType";
import { ItemFormBody } from "components/arr/item-form/ItemFormBody";
import { messages } from "components/arr/item-form/messages";
import { useOutputFormData } from "./hooks";

interface Props {
    outputId: number;
}

export type { Props as OutputEditProps };

export function OutputEdit({ outputId }: Props) {
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
        switchCalculating,
    } = useOutputFormData(outputId);

    const allItems = useMemo(
        () => [...formItems, ...forcedFormItems, ...addedFormItems],
        [formItems, forcedFormItems, addedFormItems],
    );

    // Calculable item types get an auto/manual toggle. `calSt` true = manual (user fills the
    // values), false = automatic (server computes them and the fields are read-only).
    function renderTypeActions(typeRef: DescItemTypeRef) {
        const typeForm = itemTypes.find(({ itemTypeId }) => itemTypeId === typeRef.id);
        if (!typeForm?.cal) {
            return null;
        }
        const isManual = !!typeForm.calSt;
        return (
            <Tooltip
                relationship="label"
                appearance="inverted"
                content={formatMessage(
                    isManual ? messages.calculateSwitchToAuto : messages.calculateSwitchToManual,
                )}
            >
                <Button
                    size="small"
                    appearance={isManual ? "primary" : "subtle"}
                    icon={isManual ? <CalculatorMultipleRegular /> : <CalculatorRegular />}
                    onClick={() => switchCalculating(typeRef.id, !isManual)}
                    tabIndex={-1}
                />
            </Tooltip>
        );
    }

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
        <div style={{ width: "100%" }}>
            {hasPossibleTypes && (
                <div style={{ margin: "4px 0 0 4px" }}>
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
                addEmptyDescItem={addEmptyItem}
                deleteDescItem={deleteItem}
                createDescItem={createItem}
                updateDescItem={updateItem}
                hideCopyButtons={true}
                renderExtraActions={renderTypeActions}
            />
        </div>
    );
}
