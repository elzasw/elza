import {
  Combobox,
  Option,
  OptionOnSelectData,
  SelectionEvents,
} from "@fluentui/react-components";
import { WebApi } from "actions";
import { DataFileRef, DataType, NodeItem } from "elza-api";
import { useEffect, useRef, useState } from "react";
import { useDebouncedEffect } from "utils/hooks/hooks";
import { useIntl } from "react-intl";
import { messages as commonMessages } from "./commonMessages";
import { useStyles } from "./styles";
import { useActiveFund } from "../hooks";
import { DescItemProps } from "./types";

interface Props extends DescItemProps {
  onChange: (item: NodeItemFileRef) => Promise<void>;
}

interface NodeItemFileRef extends NodeItem {
  data: DataFileRef;
}

export interface ArrFileVO {
  ["@class"]: string;
  id: number;
  content: unknown | null;
  name: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  pagesCount: unknown | null;
  file: unknown | null;
  fundId: number;
  editable: boolean;
  generatePdf: boolean;
}

export function DescItemFileRef({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
  compact,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.FileRef && !item.undefined) {
    throw "Incorrect data type";
  }

  const { formatMessage } = useIntl();
  const styles = useStyles();
  const activeFund = useActiveFund();

  const [query, setQuery] = useState<string>(
    item.undefined ? formatMessage(commonMessages.undefined) : "",
  );
  const [files, setFiles] = useState<ArrFileVO[]>([]);
  const [file, setFile] = useState<ArrFileVO>();

  const fieldRef = useRef<HTMLInputElement>(null);

  const data = item.data as DataFileRef;

  const handleFileSelect = (_e: SelectionEvents, data: OptionOnSelectData) => {
    setQuery(data.optionText);
    onChange({
      ...item,
      data: {
        ...item.data,
        fileId: parseInt(data.optionValue),
      },
    });
  };

  function handleBlur() {
    setQuery(file?.name || "");
  }

  useEffect(() => {
    if (data?.fileId) {
      (async () => {
        const file: ArrFileVO = await WebApi.getEditableFundFile(
          activeFund.id,
          data.fileId,
        );
        setQuery(file.name);
        setFile(file);
      })();
    } else if (item.undefined) {
      setQuery(formatMessage(commonMessages.undefined));
    } else {
      setQuery("");
    }
  }, [data?.fileId, item.undefined]);

  useDebouncedEffect(() => {
    if (!item.undefined && item.nodeId === nodeId) {
      (async () => {
        const { rows } = await WebApi.findFundFiles(
          activeFund.id,
          file && query === file?.name ? "" : query,
        );
        setFiles(rows);
      })();
    }
  }, 300, [query, item.undefined, item.nodeId, nodeId]);

  const isInherited = item.nodeId != nodeId;
  const isDisabled =
    item.undefined ||
    isInherited ||
    item.inhibited ||
    item.readOnly ||
    _isDisabled;

  return (
    <Combobox
      size={compact ? "small" : "medium"}
      title={query}
      value={query}
      onChange={(e) => setQuery(e.target.value)}
      onOptionSelect={handleFileSelect}
      onOpenChange={(_e, open) => {
        if (open) {
          fieldRef.current?.setSelectionRange(0, query?.length || 0);
        }
      }}
      onBlur={handleBlur}
      className={styles.comboboxNoMinWidth}
      input={{
        ref: fieldRef,
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
      {files.map(({ name, id }) => {
        return (
          <Option text={name} value={id.toString()}>
            <div>
              <div>{name}</div>
            </div>
          </Option>
        );
      })}
    </Combobox>
  );
}
