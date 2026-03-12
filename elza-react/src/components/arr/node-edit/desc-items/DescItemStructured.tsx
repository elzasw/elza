import {
  Button,
  Combobox,
  Option,
  OptionOnSelectData,
  SelectionEvents,
  Tooltip,
} from "@fluentui/react-components";
import { DocumentAddRegular } from "@fluentui/react-icons";
import { WebApi } from "actions";
import { DataStructureRef, DataType, NodeItem } from "elza-api";
import { ChangeEvent, useCallback, useEffect, useMemo, useState } from "react";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useActiveFund } from "../hooks";
import { AnonymousStructure } from "./AnonymousStructure";
import { DescItemProps } from "./types";
import { i18n } from "components";
import AddStructureDataForm from "components/arr/structure/AddStructureDataForm";
import { modalDialogHide, modalDialogShow } from "actions/global/modalDialog";
import { useAppThunkDispatch } from "utils/hooks";
import { structureTypeInvalidate } from "actions/arr/structureType";
import DescItemFactory from "components/arr/nodeForm/DescItemFactory";
import { FormattedMessage, defineMessages } from "react-intl";

interface Props extends DescItemProps {
  onChange: (item: NodeItemStructureRef) => Promise<void>;
}

interface NodeItemStructureRef extends NodeItem {
  data: DataStructureRef;
}

const messages = defineMessages({
  addNewStructure: {
    id: "desc_item_structure_action_addNewStructure",
    defaultMessage: "Přidat",
  },
});

export function DescItemStructured({
  item,
  typeRef,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.Structured && !item.undefined) {
    throw "Incorrect data type";
  }

  const dispatch = useAppThunkDispatch();

  const { id: fundId, versionId: fundVersionId } = useActiveFund();
  const structureTypes = useAppSelector(
    ({ refTables }) =>
      refTables.structureTypes.data?.find(
        ({ versionId }) => versionId === fundVersionId,
      )?.data || [],
  );

  const data = item.data as DataStructureRef;
  const [structure, setStructure] = useState<any>();
  const [structures, setStructures] = useState<any[]>([]);
  const [query, setQuery] = useState("");

  const structureType = useMemo(() => {
    if (typeRef?.structureTypeId != undefined) {
      return structureTypes.find(({ id }) => id === typeRef.structureTypeId);
    }
    return undefined;
  }, [typeRef?.structureTypeId, structureTypes]);

  useEffect(() => {
    if (data.structuredObjectId) {
      (async () => {
        const _structure = await WebApi.getStructureData(
          fundId,
          data.structuredObjectId,
        );
        setStructure(_structure);
        setQuery(_structure.value);
      })();
    }
  }, [fundId, data.structuredObjectId]);

  const loadStructures = useCallback(
    async (_query: string) => {
      if (structureType?.code && !structureType.anonymous) {
        const _structures = await WebApi.findStructureData(
          fundVersionId,
          structureType?.code,
          _query === structure?.value ? "" : _query,
        );
        setStructures(_structures.rows);
      }
    },
    [
      fundVersionId,
      structureType?.code,
      structureType?.anonymous,
      structure?.value,
    ],
  );

  async function handleQueryChange(e: ChangeEvent<HTMLInputElement>) {
    const _query = e.currentTarget.value;
    loadStructures(_query);
    setQuery(_query);
  }

  async function handleSelect(_e: SelectionEvents, _data: OptionOnSelectData) {
    setQuery(_data.optionText);
    setStructure(
      structures.find(({ id }) => id === parseInt(_data.optionValue)),
    );

    const id = parseInt(_data.optionValue);
    if (!isNaN(id)) {
      return await handleChange(parseInt(_data.optionValue));
    }
  }

  async function handleChange(id: number) {
    return await onChange({
      ...item,
      data: {
        ...data,
        structuredObjectId: id,
      },
    });
  }

  function addNewStructure() {
    WebApi.createStructureData(fundVersionId, structureType.code, query).then(
      (structureData) => {
        dispatch(
          modalDialogShow(
            this,
            i18n("arr.structure.modal.add.title", structureType.code),
            <AddStructureDataForm
              //@ts-expect-error TODO fix wrong types (missing fundId)
              fundId={fundId}
              fundVersionId={fundVersionId}
              structureData={structureData}
              descItemFactory={DescItemFactory}
              onSubmit={() => {
                WebApi.confirmStructureData(
                  fundVersionId,
                  structureData.id,
                ).then((structure) => {
                  // TODO add types
                  handleChange(structure.id);
                });
              }}
              onSubmitSuccess={() => {
                dispatch(modalDialogHide());
                dispatch(structureTypeInvalidate());
              }}
            />,
            "",
            () => {
              WebApi.deleteStructureData(fundVersionId, structureData.id);
            },
          ),
        );
      },
    );
  }

  useEffect(() => {
    loadStructures(query);
  }, [query, loadStructures]);

  async function handleCreateAnonymousStructure(_structureObjectId: number) {
    await onChange({
      ...item,
      data: {
        ...data,
        structuredObjectId: _structureObjectId,
      },
    });
  }

  if (structureType.anonymous) {
    return (
      <AnonymousStructure
        data={data}
        structureType={structureType}
        onCreate={handleCreateAnonymousStructure}
      />
    );
  }

  const isInherited = item.nodeId != nodeId;
  const isDisabled =
    item.undefined ||
    isInherited ||
    item.inhibited ||
    item.readOnly ||
    _isDisabled;

  return (
    <div
      style={{
        position: "relative",
        display: "inline-flex",
        flex: 1,
        alignItems: "center",
      }}
    >
      {!structureType.anonymous && (
        // <Input
        //   style={{ flex: 1, minWidth: "60px" }}
        //   value={data.structuredObjectId?.toString()}
        // />
        <>
          <Combobox
            title={query}
            value={`${query}`}
            onChange={handleQueryChange}
            onOptionSelect={handleSelect}
            // onOpenChange={(_e, open) => {
            //   if (open) {
            //     fieldRef.current?.setSelectionRange(0, query?.length || 0);
            //   }
            // }}
            // onBlur={handleBlur}
            style={{
              minWidth: "unset",
              flex: 1,
              flexGrow: 5,
              // paddingLeft: "80px",
              paddingRight: "37px",
            }}
            input={{
              style: {
                minWidth: "30px",
                textDecoration: item.inhibited ? "line-through" : undefined,
                flex: 1,
                flexBasis: `${(query || "").length + 3}ch`,
                zIndex: 1,
              },
            }}
            listbox={{ style: { maxHeight: "400px", minWidth: "400px" } }}
            disabled={isDisabled}
          >
            {structures.map(({ value, complement, id }) => {
              return (
                <Option text={value} value={id.toString()}>
                  <div>
                    <span>{value}</span>
                    &nbsp;
                    <span style={{ opacity: 0.5 }}>{complement}</span>
                  </div>
                </Option>
              );
            })}
          </Combobox>
          <div
            style={{
              position: "absolute",
              right: "70px",
              height: "90%",
              display: "flex",
              alignItems: "center",
              opacity: 0.5,
              background: "var(--shade-0)",
              pointerEvents: "none",
              zIndex: 0,
            }}
          >
            {structure?.complement}
          </div>
          <div
            style={{
              position: "absolute",
              right: "1px",
            }}
          >
            <Tooltip
              relationship="label"
              appearance="inverted"
              content={<FormattedMessage {...messages.addNewStructure} />}
            >
              <Button
                style={{ height: "29px" }}
                appearance="subtle"
                icon={<DocumentAddRegular />}
                onClick={addNewStructure}
                tabIndex={-1}
              />
            </Tooltip>
          </div>
        </>
      )}
    </div>
  );
}
