import { Button, Input, Textarea } from "@fluentui/react-components";
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
import { useIntl } from "react-intl";
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

export function DescItemCoordinates({ item, nodeId, onChange }: Props) {
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
  const isDisabled = item.undefined || isInherited || item.inhibited;

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
        ref={inputRef}
        disabled={isDisabled}
        style={{ flex: 1, minWidth: "60px" }}
        value={item.undefined ? "Výjimka" : value || ""}
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
                  disabled={isDisabled}
                  appearance="subtle"
                  icon={<MapRegular />}
                  onClick={handleShowInMap}
                ></Button>
              )}
            </PolygonShowInMap>
            {data?.value && (
              <>
                <Button
                  disabled={isDisabled}
                  appearance="subtle"
                  icon={<CopyRegular />}
                  onClick={handleCopyToClipboard}
                ></Button>
                <Button
                  disabled={isDisabled}
                  appearance="subtle"
                  icon={<ArrowExportRegular />}
                  onClick={handleExport}
                ></Button>
              </>
            )}
            {!data?.value && (
              <Button
                disabled={isDisabled}
                appearance="subtle"
                icon={<DocumentAddRegular />}
                onClick={handleImport}
              ></Button>
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
