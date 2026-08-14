import { useEffect, useRef, useState } from "react";
import {
  Button,
  DataGrid,
  DataGridBody,
  DataGridCell,
  DataGridHeader,
  DataGridHeaderCell,
  DataGridProps,
  DataGridRow,
  Input,
  TableColumnDefinition,
  TableColumnSizingOptions,
  Tooltip,
  createTableColumn,
  makeStyles,
  mergeClasses,
  tokens,
} from "@fluentui/react-components";
import {
  AddRegular,
  ArrowDownloadRegular,
  ArrowSortRegular,
  ArrowUploadRegular,
  DeleteRegular,
} from "@fluentui/react-icons";
import { DataJsonTable, DataType, NodeItem } from "elza-api";
import { useIntl, defineMessages } from "react-intl";
import { DescItemProps } from "./types";
import { messages as commonMessages } from "./commonMessages";

interface Props extends DescItemProps {
  onChange: (item: NodeItem) => Promise<void>;
}

interface TableColumn {
  code: string;
  name: string;
  dataType?: string;
  width?: number;
}

interface TableRowValue {
  values: Record<string, string>;
}

interface JsonTable {
  rows: TableRowValue[];
}

/** Row paired with its position in the canonical (unsorted) data, so edits survive visual sorting. */
interface IndexedRow {
  row: TableRowValue;
  index: number;
}

const ACTIONS_COLUMN_ID = "__actions";

const messages = defineMessages({
  addRow: {
    id: "descItem.jsonTable.addRow",
    defaultMessage: "Přidat řádek",
  },
  removeRow: {
    id: "descItem.jsonTable.removeRow",
    defaultMessage: "Odebrat řádek",
  },
  empty: {
    id: "descItem.jsonTable.empty",
    defaultMessage: "Tabulka je prázdná",
  },
  resetOrder: {
    id: "descItem.jsonTable.resetOrder",
    defaultMessage: "Zrušit řazení",
  },
  importCsv: {
    id: "descItem.jsonTable.importCsv",
    defaultMessage: "Importovat CSV",
  },
  exportCsv: {
    id: "descItem.jsonTable.exportCsv",
    defaultMessage: "Exportovat CSV",
  },
});

const useStyles = makeStyles({
  root: {
    display: "flex",
    flexDirection: "column",
    alignItems: "flex-start",
    rowGap: tokens.spacingVerticalXS,
    flex: 1,
    minWidth: 0,
  },
  grid: {
    width: "100%",
  },
  toolbar: {
    display: "flex",
    flexWrap: "wrap",
    columnGap: tokens.spacingHorizontalXS,
  },
  cellInput: {
    width: "100%",
  },
  cellInputIdle: {
    backgroundColor: "transparent",
    border: "none",
    "&::before": {
      border: "none",
    },
    "&::after": {
      display: "none",
    },
  },
  empty: {
    color: tokens.colorNeutralForeground3,
    fontStyle: "italic",
  },
});

function extractColumns(viewDefinition: unknown): TableColumn[] {
  if (Array.isArray(viewDefinition)) {
    return viewDefinition as TableColumn[];
  }
  const tableColumns = (viewDefinition as { tableColumns?: unknown })?.tableColumns;
  return Array.isArray(tableColumns) ? (tableColumns as TableColumn[]) : [];
}

function parseTable(value: unknown): JsonTable {
  if (typeof value !== "string" || value.length === 0) {
    return { rows: [] };
  }
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed?.rows) ? parsed : { rows: [] };
  } catch {
    return { rows: [] };
  }
}

function compareCell(a: string, b: string, isNumeric: boolean): number {
  if (isNumeric) {
    const numA = Number(a);
    const numB = Number(b);
    const bothNumbers = !Number.isNaN(numA) && !Number.isNaN(numB);
    if (bothNumbers) {
      return numA - numB;
    }
  }
  return a.localeCompare(b);
}

interface CellProps {
  value: string;
  disabled: boolean;
  onCommit: (value: string) => void;
}

/**
 * Vždy vykreslený input, který bez fokusu vypadá jako prostý text. Díky tomu je každá buňka
 * trvale v pořadí tabulátoru a přepínání mezi buňkami zajišťuje nativní chování prohlížeče.
 */
function JsonTableCell({ value, disabled, onCommit }: CellProps) {
  const styles = useStyles();
  const [draft, setDraft] = useState(value);
  const [focused, setFocused] = useState(false);

  // Keep the draft in sync with committed value while not editing (e.g. after a server refresh).
  useEffect(() => {
    if (!focused) {
      setDraft(value);
    }
  }, [value, focused]);

  return (
    <Input
      appearance={focused ? "outline" : "underline"}
      size="small"
      disabled={disabled}
      className={mergeClasses(styles.cellInput, !focused && styles.cellInputIdle)}
      value={draft}
      onChange={(_event, data) => setDraft(data.value)}
      onFocus={() => setFocused(true)}
      onBlur={() => {
        setFocused(false);
        onCommit(draft);
      }}
      onKeyDown={(event) => {
        if (event.key === "Enter") {
          event.currentTarget.blur();
        } else if (event.key === "Escape") {
          setDraft(value);
          event.currentTarget.blur();
        }
      }}
    />
  );
}

export function DescItemJsonTable({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
  typeRef,
  onExportCsv,
  onImportCsv,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.JsonTable && !item.undefined) {
    throw "Incorrect data type";
  }

  const styles = useStyles();
  const { formatMessage } = useIntl();

  const isInherited = item.nodeId !== nodeId;
  const isDisabled =
    item.undefined || isInherited || item.inhibited || item.readOnly || _isDisabled;

  const columns = extractColumns(typeRef.viewDefinition);
  const table = parseTable((item.data as DataJsonTable | undefined)?.value);

  const [sortState, setSortState] = useState<NonNullable<DataGridProps["sortState"]>>({
    sortColumn: undefined,
    sortDirection: "ascending",
  });
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isSorted = sortState.sortColumn != undefined;

  async function commit(rows: TableRowValue[]) {
    const data: DataJsonTable = {
      ...(item.data as DataJsonTable),
      dataType: DataType.JsonTable,
      value: JSON.stringify({ rows }),
    };
    await onChange({ ...item, data });
  }

  async function commitCell(index: number, code: string, value: string) {
    const current = table.rows[index].values[code] ?? "";
    if (value === current) {
      return;
    }
    const rows = table.rows.map((tableRow, rowIndex) => {
      if (rowIndex !== index) {
        return tableRow;
      }
      const values = { ...tableRow.values };
      // Empty value is stored as an absent key; the server rejects an empty string for
      // typed columns (e.g. INTEGER "must be a whole number").
      if (value === "") {
        delete values[code];
      } else {
        values[code] = value;
      }
      return { ...tableRow, values };
    });
    await commit(rows);
  }

  async function handleAddRow() {
    await commit([...table.rows, { values: {} }]);
  }

  async function handleRemoveRow(index: number) {
    await commit(table.rows.filter((_row, rowIndex) => rowIndex !== index));
  }

  if (item.undefined) {
    return (
      <Input disabled value={formatMessage(commonMessages.undefined)} style={{ flex: 1 }} />
    );
  }

  const gridColumns: TableColumnDefinition<IndexedRow>[] = columns.map((column) => {
    const isNumeric = column.dataType === "INTEGER";
    return createTableColumn<IndexedRow>({
      columnId: column.code,
      compare: (a, b) =>
        compareCell(
          a.row.values[column.code] ?? "",
          b.row.values[column.code] ?? "",
          isNumeric,
        ),
      renderHeaderCell: () => column.name,
      renderCell: ({ row, index }) => (
        <JsonTableCell
          value={row.values[column.code] ?? ""}
          disabled={isDisabled}
          onCommit={(value) => commitCell(index, column.code, value)}
        />
      ),
    });
  });

  // The actions column carries the reset-order button (any mode) and per-row delete (edit mode),
  // so it is present whenever the table has rows — sorting is available read-only too.
  gridColumns.push(
    createTableColumn<IndexedRow>({
      columnId: ACTIONS_COLUMN_ID,
      renderHeaderCell: () =>
        isSorted ? (
          <Tooltip relationship="label" content={formatMessage(messages.resetOrder)}>
            <Button
              appearance="subtle"
              size="small"
              icon={<ArrowSortRegular />}
              onClick={() =>
                setSortState({ sortColumn: undefined, sortDirection: "ascending" })
              }
            />
          </Tooltip>
        ) : (
          ""
        ),
      renderCell: ({ index }) =>
        isDisabled ? (
          ""
        ) : (
          <Tooltip relationship="label" content={formatMessage(messages.removeRow)}>
            <Button
              appearance="subtle"
              size="small"
              icon={<DeleteRegular />}
              onClick={() => handleRemoveRow(index)}
            />
          </Tooltip>
        ),
    }),
  );

  const items: IndexedRow[] = table.rows.map((row, index) => ({ row, index }));

  // DataGrid lays columns out on a CSS grid and cannot size to content on its own, so derive an
  // ideal width per column from the longest header/cell text (clamped to a sensible range).
  const columnSizingOptions: TableColumnSizingOptions = {
    [ACTIONS_COLUMN_ID]: { minWidth: 36, idealWidth: 36 },
  };
  columns.forEach((column) => {
    const longest = table.rows.reduce(
      (max, tableRow) => Math.max(max, (tableRow.values[column.code] ?? "").length),
      column.name.length,
    );
    const idealWidth = Math.min(400, Math.max(80, longest * 8 + 24));
    columnSizingOptions[column.code] = { minWidth: 48, idealWidth };
  });

  return (
    <div className={styles.root}>
      {table.rows.length === 0 ? (
        <span className={styles.empty}>{formatMessage(messages.empty)}</span>
      ) : (
        <DataGrid
          className={styles.grid}
          items={items}
          columns={gridColumns}
          sortable
          sortState={sortState}
          onSortChange={(_event, nextSortState) => setSortState(nextSortState)}
          getRowId={(indexedRow: IndexedRow) => indexedRow.index}
          focusMode="none"
          resizableColumns
          columnSizingOptions={columnSizingOptions}
          size="small"
        >
          <DataGridHeader>
            <DataGridRow>
              {({ renderHeaderCell }) => (
                <DataGridHeaderCell>{renderHeaderCell()}</DataGridHeaderCell>
              )}
            </DataGridRow>
          </DataGridHeader>
          <DataGridBody<IndexedRow>>
            {({ item: indexedRow, rowId }) => (
              <DataGridRow<IndexedRow> key={rowId}>
                {({ renderCell }) => (
                  <DataGridCell focusMode="none">{renderCell(indexedRow)}</DataGridCell>
                )}
              </DataGridRow>
            )}
          </DataGridBody>
        </DataGrid>
      )}
      <div className={styles.toolbar}>
        {!isDisabled && (
          <Button appearance="subtle" size="small" icon={<AddRegular />} onClick={handleAddRow}>
            {formatMessage(messages.addRow)}
          </Button>
        )}
        {!isDisabled && onImportCsv && (
          <>
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv,text/csv"
              style={{ display: "none" }}
              onChange={async (event) => {
                const file = event.target.files?.[0];
                event.target.value = "";
                if (file) {
                  await onImportCsv(item, file);
                }
              }}
            />
            <Button
              appearance="subtle"
              size="small"
              icon={<ArrowUploadRegular />}
              onClick={() => fileInputRef.current?.click()}
            >
              {formatMessage(messages.importCsv)}
            </Button>
          </>
        )}
        {onExportCsv && (
          <Button
            appearance="subtle"
            size="small"
            icon={<ArrowDownloadRegular />}
            onClick={() => onExportCsv(item)}
          >
            {formatMessage(messages.exportCsv)}
          </Button>
        )}
      </div>
    </div>
  );
}
