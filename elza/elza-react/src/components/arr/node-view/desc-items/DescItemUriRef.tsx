import { DataType, DataUriRef } from "elza-api";
import { Link } from "react-router-dom";
import { urlNode } from "../../../../constants";
import { DescItemProps } from "./types";

export function DescItemUriRef({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.UriRef) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataUriRef;

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
          {data.nodeId ? (
            <Link to={urlNode(data.nodeId)}>{data.description}</Link>
          ) : (
            <a href={data.value}>{data.description}</a>
          )}
        </>
      )}
    </div>
  );
}
