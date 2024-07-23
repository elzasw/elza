import { 
    Table, 
    TableHeader, 
    TableRow, 
    TableSelectionCell, 
    TableHeaderCell, 
    TableBody, 
    TableCell, 
    TableCellLayout, 
    useTableFeatures, 
    useTableColumnSizing_unstable, 
    TableColumnSizingOptions, 
    useTableSelection, 
    useTableSort, 
    TableColumnDefinition, 
    createTableColumn, 
    TableColumnId,
    TableFeaturePlugin
} from "@fluentui/react-components";
import { FC, useCallback, useState, KeyboardEvent } from "react";
import "./ExplorerTable.scss"
import { formatAipSize } from "components/aip/utils";
import { getFileName } from "../utils";
import { ExplorerMode, isDaoFileFolderVO, useExplorerContext } from "../ExplorerContext";

type Item = {
    fileName?: string;
    label?: string;
    size?: number;
    mimeType?: string;
}

const columns: TableColumnDefinition<Item>[] = [
    createTableColumn<Item>({
      columnId: "name",
      renderHeaderCell: () => <>Název</>,
      renderCell: (item) => <>{item.fileName ? getFileName(item.fileName ): item.label || "-"}</>,
      compare: (a, b) => {
        const nameA = a.fileName || a.label;
        const nameB = b.fileName || b.label;
        return nameA?.localeCompare(nameB)}
    }),
    createTableColumn<Item>({
      columnId: "size",
      renderHeaderCell: () => <>Velikost</>,
      renderCell: (item) => <>{item.size ? formatAipSize(item.size) : "-"}</>,
      compare: (a, b) => b.size - a.size
    }),
    createTableColumn<Item>({
      columnId: "format",
      renderHeaderCell: () => <>Formát</>,
      renderCell: (item) => <>{item.mimeType || "-"}</>,
      compare: (a, b) => a.mimeType?.localeCompare(b.mimeType)
    }),
];

const columnSizes = {
    name: {idealWidth: 300, minWidth: 50},
    size: {idealWidth: 200, minWidth: 50},
    format: {idealWidth: 300, minWidth: 50}
}

const ExplorerTable: FC = () => {
    const {selectedItem, setSelectedItem, mode} = useExplorerContext();
    const [columnSizingOptions] = useState<TableColumnSizingOptions>(columnSizes);

    let items = [];
    if(isDaoFileFolderVO(selectedItem) && 
        !((!selectedItem.childFolders && !selectedItem.childFiles) 
        || (selectedItem.childFolders && selectedItem.childFolders.length == 0))
    ) {
        items =  [...selectedItem.childFolders || [], ...selectedItem.childFiles || []] 
    } else if(selectedItem) {
        items = selectedItem?.parent ? [...selectedItem.parent.childFolders || [], ...selectedItem.parent.childFiles || []] : [];
    }

    const { 
        getRows, 
        columnSizing_unstable, 
        tableRef, 
        sort: { getSortDirection, toggleColumnSort, sort },
        selection: {
            allRowsSelected,
            someRowsSelected,
            toggleAllRows,
            toggleRow,
            isRowSelected,
          },
    } = useTableFeatures(
        { columns, items },
        [
            useTableColumnSizing_unstable({ columnSizingOptions }),
            useTableSort({defaultSortState: { sortColumn: "id", sortDirection: "ascending"}}),
            useTableSelection({
                selectionMode: "multiselect",
                onSelectionChange: (e, data) => {
                    const selectedRows = rows
                        .filter(row => data.selectedItems.has(row.rowId))
                        .map(row => row.item);
                    //TODO: @kasparova action
                }
            })
        ] 
      );
    
    const rows = sort(getRows((row) => {
        const selected = isRowSelected(row.rowId);
        return {
            ...row,
            onClick: (e: MouseEvent) => {
                 //@ts-ignore
                toggleRow(e, row.rowId);
            },
            onKeyDown: (e: KeyboardEvent) => {
                if (e.key === " ") {
                    e.preventDefault();
                    toggleRow(e, row.rowId);
                }
            },
            selected,
            appearance: selected ? ("brand" as const) : ("none" as const),
        };
    }));

    const headerSortProps = (columnId: TableColumnId) => ({
        onClick: (e: MouseEvent) => {
             //@ts-ignore
            toggleColumnSort(e, columnId);
        },
        sortDirection: getSortDirection(columnId),
    });

    const toggleAllKeydown = useCallback(
        (e: KeyboardEvent<HTMLDivElement>) => {
            if (e.key === " ") {
                toggleAllRows(e);
                e.preventDefault();
            }
        },
        [toggleAllRows]
    );

    const handleSelect = (item) => {
        setSelectedItem(item)
    }

    return (
        <Table
            ref={tableRef}
            as="table"
            sortable
            {...columnSizing_unstable.getTableProps()}
            className="explorer-table"
        >
            <TableHeader>
                <TableRow>
                    {mode == ExplorerMode.SELECT && <TableSelectionCell
                        checked={allRowsSelected ? true : someRowsSelected ? "mixed" : false}
                        onClick={toggleAllRows}
                        onKeyDown={toggleAllKeydown}
                        checkboxIndicator={{"aria-label": "Vybrat vše"}}
                        className="header"
                        
                    />}
                    {columns.map((column) => (
                            //@ts-ignore
                        <TableHeaderCell
                            key={column.columnId}
                            {...columnSizing_unstable.getTableHeaderCellProps(column.columnId)}
                            {...headerSortProps(column.columnId)}
                            className="header"
                        >
                            {column.renderHeaderCell()}
                        </TableHeaderCell>
                    ))}
                </TableRow>
            </TableHeader>
            <TableBody>
                {rows.map(({ item, selected, onClick }) => (
                    <TableRow 
                        key={`${item.fileName || item.label}.${item.size}`} 
                        className="table-row"
                    >
                        {mode == ExplorerMode.SELECT && <TableSelectionCell
                            checked={selected}
                            checkboxIndicator={{ "aria-label": "Vybrat" }}
                                //@ts-ignore
                            onClick={onClick}
                        />}

                        {columns.map(col => (
                            <TableCell
                                key={`item.${col.columnId}`} 
                                {...columnSizing_unstable.getTableCellProps(col.columnId)}
                                onClick={() => handleSelect(item)}
                            >
                                <TableCellLayout truncate>
                                    {col.renderCell(item)}
                                </TableCellLayout>
                        </TableCell>
                        ))}
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    );
}

export default ExplorerTable;