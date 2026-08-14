import { useState } from "react";
import {
  Button,
  DataGrid,
  DataGridBody,
  DataGridCell,
  DataGridHeader,
  DataGridHeaderCell,
  DataGridProps,
  DataGridRow,
  TableColumnDefinition,
  TableColumnSizingOptions,
  Tooltip,
  createTableColumn,
  makeStyles,
  tokens,
} from "@fluentui/react-components";
import { ArrowDownloadRegular, ArrowSortRegular } from "@fluentui/react-icons";
import { DataJsonTable, DataType } from "elza-api";
import { useIntl, defineMessages } from "react-intl";
import { DescItemProps } from "./types";

interface TableColumn {
  code: string;
  name: string;
  dataType?: string;
  width?: number;
}

interface TableRowValue {
  values: Record<string, string>;
}

interface IndexedRow {
  row: TableRowValue;
  index: number;
}

const ACTIONS_COLUMN_ID = "__actions";

const messages = defineMessages({
  empty: {
    id: "descItem.jsonTable.empty",
    defaultMessage: "Tabulka je prázdná",
  },
  resetOrder: {
    id: "descItem.jsonTable.resetOrder",
    defaultMessage: "Zrušit řazení",
  },
  exportCsv: {
    id: "descItem.jsonTable.exportCsv",
    defaultMessage: "Exportovat CSV",
  },
});

const useStyles = makeStyles({
  root: {
    width: "100%",
  },
  grid: {
    width: "100%",
  },
  cellText: {
    display: "block",
    width: "100%",
    padding: `0 ${tokens.spacingHorizontalXS}`,
    whiteSpace: "pre-wrap",
    wordBreak: "break-word",
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

function parseTable(value: unknown): { rows: TableRowValue[] } {
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

export function DescItemJsonTable({ item, nodeId, typeRef, onExportCsv }: DescItemProps) {
  if (item.data && item.data?.dataType !== DataType.JsonTable && !item.undefined) {
    throw "Incorrect data type";
  }

  const styles = useStyles();
  const { formatMessage } = useIntl();

  const isInherited = item.nodeId !== nodeId;

  const columns = extractColumns(typeRef.viewDefinition);
  const table = parseTable((item.data as DataJsonTable | undefined)?.value);

  const [sortState, setSortState] = useState<NonNullable<DataGridProps["sortState"]>>({
    sortColumn: undefined,
    sortDirection: "ascending",
  });

  const isSorted = sortState.sortColumn != undefined;

  if (item.undefined) {
    return <div>Výjimka</div>;
  }

  if (table.rows.length === 0) {
    return <span className={styles.empty}>{formatMessage(messages.empty)}</span>;
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
      renderCell: ({ row }) => (
        <span className={styles.cellText}>{row.values[column.code] ?? ""}</span>
      ),
    });
  });

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
      renderCell: () => "",
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
    <div className={styles.root} style={{ opacity: isInherited ? 0.5 : undefined }}>
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
  );
}
