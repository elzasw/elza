import { WebApi } from "actions";
import { ApAccessPointVO } from "api";
import { DataRecordRef, DataType } from "elza-api";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { urlEntity } from "../../../../constants";
import { DescItemProps } from "./types";

export function DescItemRecordRef({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.RecordRef) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const [accessPoint, setAccessPoint] = useState<ApAccessPointVO>();

  const data = item.data as DataRecordRef;

  useEffect(() => {
    if (data?.value) {
      (async () => {
        const _accessPoint = await WebApi.getAccessPoint(data.value);
        setAccessPoint(_accessPoint);
      })();
    }
  }, [data.value]);

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
        <Link to={urlEntity(data.value)}>
          {accessPoint?.name || data.value}
        </Link>
      )}
    </div>
  );
}
