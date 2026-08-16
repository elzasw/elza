import { WebApi } from "actions";
import { AnonymousStructure } from "components/arr/item-form/desc-items/AnonymousStructure";
import { useActiveFund } from "utils/hooks";
import { DataStructureRef, DataType } from "elza-api";
import { useEffect, useMemo, useState } from "react";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { DescItemProps } from "./types";

export function DescItemStructured({ item, typeRef, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.Structured) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const { id: fundId, versionId: fundVersionId } = useActiveFund();
  const data = item.data as DataStructureRef;

  const structureTypes = useAppSelector(
    ({ refTables }) =>
      refTables.structureTypes.data?.find(
        ({ versionId }) => versionId === fundVersionId,
      )?.data || [],
  );

  const [structure, setStructure] = useState<any>(); // TODO add types

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
          fundVersionId,
          data.structuredObjectId,
        );
        setStructure(_structure);
      })();
    }
  }, [fundVersionId, data.structuredObjectId]);

  if (structureType.anonymous) {
    return (
      <AnonymousStructure
        data={data}
        structureType={structureType}
        readOnly={true}
      />
    );
  }

  return (
    <div
      style={{
        textDecoration: item.inhibited ? "line-through" : undefined,
        opacity: isInherited ? 0.5 : undefined,
      }}
    >
      {item.undefined ? (
        "Výjimka"
      ) : (
        <>
          {structure?.value}
          {structure?.complement && (
            <>
              &nbsp;
              <span style={{ opacity: 0.6 }}>{structure?.complement}</span>
            </>
          )}
        </>
      )}
    </div>
  );
}
