import { Link } from "@fluentui/react-components";
import { UrlFactory } from "actions";
import { downloadFile } from "actions/global/download";
import { useFundFile } from "components/arr/item-form/desc-items/useFundFile";
import { useActiveFund } from "utils/hooks";
import { DataFileRef, DataType } from "elza-api";
import { useAppThunkDispatch } from "utils/hooks";
import { DescItemProps } from "./types";
import { FormattedMessage } from "react-intl";
import { messages as commonMessages } from "components/arr/item-form/desc-items/commonMessages";

export function DescItemFileRef({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.FileRef) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataFileRef;

  const dispatch = useAppThunkDispatch();
  const activeFund = useActiveFund();
  const file = useFundFile(activeFund.id, data?.fileId);

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
        <FormattedMessage {...commonMessages.undefined} />
      ) : (
        <Link appearance="subtle" onClick={handleDownload} inline={true}>
          {file?.name || data.fileId}
        </Link>
      )}
    </div>
  );
}
