import { Link } from "@fluentui/react-components";
import { UrlFactory, WebApi } from "actions";
import { downloadFile } from "actions/global/download";
import { ArrFileVO } from "components/arr/item-form/desc-items";
import { useActiveFund } from "utils/hooks";
import { DataFileRef, DataType } from "elza-api";
import { useEffect, useState } from "react";
import { useAppThunkDispatch } from "utils/hooks";
import { DescItemProps } from "./types";

export function DescItemFileRef({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.FileRef) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataFileRef;

  const dispatch = useAppThunkDispatch();
  const activeFund = useActiveFund();
  const [file, setFile] = useState<ArrFileVO>();

  useEffect(() => {
    if (data?.fileId) {
      (async () => {
        const file: ArrFileVO = await WebApi.getEditableFundFile(
          activeFund.id,
          data.fileId,
        );
        setFile(file);
      })();
    }
  }, [data?.fileId, item.undefined]);

  function handleDownload() {
    dispatch(downloadFile(UrlFactory.downloadDmsFile(data.fileId)));
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
        <Link appearance="subtle" onClick={handleDownload} inline={true}>
          {file?.name || data.fileId}
        </Link>
      )}
    </div>
  );
}
