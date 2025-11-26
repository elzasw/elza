import {
  Combobox,
  Option,
  OptionOnSelectData,
  SelectionEvents,
} from "@fluentui/react-components";
import { WebApi } from "actions";
import { DataStructureRef, DataType, NodeItem } from "elza-api";
import { ChangeEvent, useCallback, useEffect, useMemo, useState } from "react";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useActiveFund } from "../hooks";
import { AnonymousStructure } from "./AnonymousStructure";
import { DescItemProps } from "./types";

interface Props extends DescItemProps {
  onChange: (item: NodeItemStructureRef) => Promise<void>;
}

interface NodeItemStructureRef extends NodeItem {
  data: DataStructureRef;
}

export function DescItemStructured({
  item,
  typeRef,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
}: Props) {
  if (item.data?.dataType !== DataType.Structured) {
    throw "Incorrect data type";
  }

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
    await onChange({
      ...item,
      data: {
        ...data,
        structuredObjectId: parseInt(_data.optionValue),
      },
    });
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
    item.undefined || isInherited || item.inhibited || _isDisabled;

  return (
    <div style={{ display: "flex", flex: 1, position: "relative" }}>
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
            }}
            input={{
              style: {
                minWidth: "30px",
                textDecoration: item.inhibited ? "line-through" : undefined,
                flex: 1,
                flexBasis: `${(query || "").length + 3}ch`,
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
              right: "40px",
              height: "90%",
              display: "flex",
              alignItems: "center",
              opacity: 0.5,
              background: "var(--shade-0)",
            }}
          >
            {structure?.complement}
          </div>
        </>
      )}
    </div>
  );
}
