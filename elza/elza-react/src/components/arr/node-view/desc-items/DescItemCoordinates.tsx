import { CoordinatesDisplay } from "components/shared/coordinates";
import { DataCoordinates, DataType } from "elza-api";
import { DescItemProps } from "./types";
import { FormattedMessage } from "react-intl";
import { messages as commonMessages } from "components/arr/item-form/desc-items/commonMessages";

export function DescItemCoordinates({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.Coordinates) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataCoordinates;

  return (
    <div>
      {item.undefined ? (
        <FormattedMessage {...commonMessages.undefined} />
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
