import { Button, Spinner, Tooltip } from "@fluentui/react-components";
import { AddRegular, EditRegular } from "@fluentui/react-icons";
import { modalDialogShow } from "actions/global/modalDialog";
import { Api } from "api";
import { downloadBlob } from "actions/global/download";
import { MandatoryType, NodeItem } from "elza-api";
import { useMemo } from "react";
import { useIntl } from "react-intl";
import { DescItemTypeRef } from "typings/store";
import { useAppThunkDispatch } from "utils/hooks";
import { useUserSettings } from "contexts/user";
import { AddDescItemTypeForm } from "components/arr/item-form/AddDescItemType";
import { ItemFormBody } from "components/arr/item-form/ItemFormBody";
import { messages } from "components/arr/item-form/messages";
import { useStyles } from "components/arr/item-form/styles";
import { useOutputFormData } from "./hooks";
import { OutputView } from "./OutputView";

interface Props {
    outputId: number;
    readonly?: boolean;
}

export type { Props as OutputEditProps };

export function OutputEdit({ outputId, readonly }: Props) {
    const dispatch = useAppThunkDispatch();
    const { formatMessage } = useIntl();
    const { settings } = useUserSettings();
    const styles = useStyles();

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
        getOutputVersion,
    } = useOutputFormData(outputId);

    async function exportCsv(item: NodeItem) {
        const { data } = await Api.output.outputOutputItemCsvExport(outputId, item.itemObjectId, {
            responseType: "blob",
        });
        downloadBlob(data, `output-${outputId}-${item.itemObjectId}.csv`);
    }

    async function importCsv(item: NodeItem, file: File) {
        await Api.output.outputOutputItemCsvImport(
            outputId,
            getOutputVersion(),
            item.itemTypeId,
            file,
        );
    }

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
                    icon={<EditRegular />}
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

    if (readonly) {
        return (
            <div className={styles.nodeEditForm} style={{ width: "100%" }}>
                <OutputView outputId={outputId} />
            </div>
        );
    }

    return (
        <div className={styles.nodeEditForm} style={{ width: "100%" }}>
            {hasPossibleTypes && (
                <div className={styles.toolbarSticky}>
                    <Button appearance="primary" icon={<AddRegular />} onClick={handleAddDescItemType}>
                        {formatMessage(messages.addDescItem)}
                    </Button>
                </div>
            )}
            <div style={{ padding: settings.compact ? "4px 8px" : "8px" }}>
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
                    exportCsv={exportCsv}
                    importCsv={importCsv}
                    hideCopyButtons={true}
                    renderExtraActions={renderTypeActions}
                />
            </div>
        </div>
    );
}
