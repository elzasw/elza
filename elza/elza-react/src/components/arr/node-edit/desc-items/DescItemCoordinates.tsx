import { Button, Input, Textarea, Tooltip } from "@fluentui/react-components";
import {
  DocumentAddRegular,
  MapRegular,
  CopyRegular,
  ArrowExportRegular,
} from "@fluentui/react-icons";
import { DataCoordinates, DataType, NodeItem } from "elza-api";
import { DescItemProps } from "./types";
import { PolygonShowInMap } from "components/PolygonShowInMap";
import { useValueManager } from "./utils";
import { EditStateDisplay } from "./EditStateDisplay";
import { ConflictValue } from "./ConflictValue";
import { useAppThunkDispatch } from "utils/hooks";
import { globalMessages } from "components/shared/lang";
import {
  addToastrDanger,
  addToastrInfo,
} from "components/shared/toastr/ToastrActions";
import { FormattedMessage, defineMessages, useIntl } from "react-intl";
import { messages as commonMessages } from "./commonMessages";
import { modalDialogHide, modalDialogShow } from "actions/global/modalDialog";
import { i18n } from "components";
import { ExportCoordinateModal } from "components/shared/coordinates";
import ImportCoordinateModal from "components/registry/Detail/coordinate/ImportCoordinateModal";
import { WebApi } from "actions";
import { useRef } from "react";

interface Props extends DescItemProps {
  onChange: (item: NodeItemCoordinates) => Promise<void>;
}

interface NodeItemCoordinates extends NodeItem {
  data: DataCoordinates;
}

const messages = defineMessages({
  showInMap: {
    id: "desc_item_coordinates_action_showInMap",
    defaultMessage: "Zobrazit v mapě",
  },
  export: {
    id: "desc_item_coordinates_export",
    defaultMessage: "Exportovat",
  },
  import: {
    id: "desc_item_coordinates_import",
    defaultMessage: "Importovat",
  },
});

export function DescItemCoordinates({
  item,
  nodeId,
  onChange,
  isDisabled: _isDisabled,
  compact,
}: Props) {
  if (
    item.data &&
    item.data.dataType !== DataType.Coordinates &&
    !item.undefined
  ) {
    throw "Incorrect data type";
  }

  const data = item.data as DataCoordinates;

  const inputRef = useRef<HTMLInputElement>(null);
  const dispatch = useAppThunkDispatch();
  const { formatMessage } = useIntl();

  const {
    value,
    setValue,
    isDirty,
    conflictValue,
    initialValue,
    resetConflict,
    finishChange,
  } = useValueManager<string>(data?.value, item);

  const isInherited = item.nodeId !== nodeId;
  const isDisabled =
    item.undefined ||
    isInherited ||
    item.inhibited ||
    item.readOnly ||
    _isDisabled;

  async function handleSave(_value: string) {
    await onChange({
      ...item,
      data: {
        ...item.data,
        value: _value,
      },
    });
  }

  async function handleChange(force?: boolean) {
    if (value && initialValue !== value && (!conflictValue || force)) {
      await handleSave(value);
      finishChange();
    }
  }

  async function resolveConflict(resetValue?: boolean) {
    if (!resetValue) {
      await handleChange(true);
    }
    resetConflict();
  }

  const handleCopyToClipboard = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(value);
      dispatch(
        addToastrInfo(
          formatMessage({ ...globalMessages.copyToClipboardFinished }),
        ),
      );
    } else {
      dispatch(
        addToastrDanger(
          formatMessage({ ...globalMessages.copyToClipboardUnavailable }),
        ),
      );
    }
  };

  const handleExport = () =>
    dispatch(
      modalDialogShow(
        undefined,
        i18n("ap.coordinate.export.title"),
        <ExportCoordinateModal
          onClose={() => dispatch(modalDialogHide())}
          itemId={item.id}
          arrangement={true}
        />,
      ),
    );

  function handleImport() {
    dispatch(
      modalDialogShow(
        undefined,
        i18n("ap.coordinate.import.title"),
        <ImportCoordinateModal
          onSubmit={async (formData) => {
            try {
              const fieldValue = await WebApi.importApCoordinates(
                formData.file,
                formData.format,
              );
              setValue(fieldValue);
              setTimeout(() => {
                inputRef.current?.focus(); // TODO find better solution
              }, 0);
            } catch (e) {
              //notification.error({message: 'Nepodařilo se importovat souřadnice'});
            }
          }}
          onSubmitSuccess={(result, dispatch) => dispatch(modalDialogHide())}
        />,
      ),
    );
  }

  function handleInputChange({
    currentTarget,
  }: React.ChangeEvent<HTMLInputElement>) {
    setValue(currentTarget.value);
  }

  console.log("#dic - value", item.id, value);

  return (
    <div
      style={{
        display: "flex",
        flex: 1,
        position: "relative",
        flexDirection: "column",
      }}
    >
      <Input
        size={compact ? "small" : "medium"}
        ref={inputRef}
        disabled={isDisabled}
        style={{ flex: 1, minWidth: "60px" }}
        value={item.undefined ? formatMessage(commonMessages.undefined) : value || ""}
        onChange={handleInputChange}
        onBlur={() => handleChange()}
        contentAfter={
          <>
            <PolygonShowInMap
              polygon={data?.value}
              showInEditor={true}
              onEditorSave={handleSave}
            >
              {({ handleShowInMap }) => (
                <Button
                  size={compact ? "small" : "medium"}
                  disabled={isDisabled}
                  appearance="subtle"
                  icon={<MapRegular />}
                  onClick={handleShowInMap}
                  tabIndex={-1}
                />
              )}
            </PolygonShowInMap>
            {data?.value && (
              <>
                <Tooltip
                  relationship="label"
                  appearance="inverted"
                  content={
                    <FormattedMessage {...globalMessages.copyToClipboard} />
                  }
                >
                  <Button
                    size={compact ? "small" : "medium"}
                    disabled={isDisabled}
                    appearance="subtle"
                    icon={<CopyRegular />}
                    onClick={handleCopyToClipboard}
                    tabIndex={-1}
                  />
                </Tooltip>
                <Tooltip
                  relationship="label"
                  appearance="inverted"
                  content={<FormattedMessage {...messages.export} />}
                >
                  <Button
                    size={compact ? "small" : "medium"}
                    disabled={isDisabled}
                    appearance="subtle"
                    icon={<ArrowExportRegular />}
                    onClick={handleExport}
                    tabIndex={-1}
                  />
                </Tooltip>
              </>
            )}
            {!data?.value && (
              <Tooltip
                relationship="label"
                appearance="inverted"
                content={<FormattedMessage {...messages.import} />}
              >
                <Button
                  size={compact ? "small" : "medium"}
                  disabled={isDisabled}
                  appearance="subtle"
                  icon={<DocumentAddRegular />}
                  onClick={handleImport}
                  tabIndex={-1}
                />
              </Tooltip>
            )}
          </>
        }
      />
      <ConflictValue
        value={value?.toString()}
        conflictValue={conflictValue?.toString()}
        isDirty={isDirty}
        onResolve={resolveConflict}
      >
        {(conflictValue) => (
          <Textarea
            size={compact ? "small" : "medium"}
            style={{ borderColor: "var(--color-red)" }}
            value={conflictValue}
            readOnly={true}
          />
        )}
      </ConflictValue>
      {isDirty && <EditStateDisplay />}
    </div>
  );
}
