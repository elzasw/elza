import { CoordinatesDisplay } from "components/shared/coordinates";
import { DataCoordinates, DataType } from "elza-api";
import { DescItemProps } from "./types";

export function DescItemCoordinates({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.Coordinates) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataCoordinates;

  return (
    <div>
      {item.undefined ? (
        "Výjimka"
      ) : (
        <CoordinatesDisplay
          value={data.value}
          arrangement={true}
          isUndefined={item.undefined}
          isInherited={isInherited}
          isInhibited={item.inhibited}
        />
      )}
    </div>
  );
}
