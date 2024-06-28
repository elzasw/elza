import { Table, TableHeader, TableRow, TableSelectionCell, TableHeaderCell, TableBody, TableCell, TableCellLayout, useTableFeatures, useTableColumnSizing_unstable, TableColumnSizingOptions, useTableSelection, useTableSort, TableColumnDefinition, createTableColumn, TableColumnId } from "@fluentui/react-components";
import { FC, useCallback, useState, KeyboardEvent } from "react";

type Item = {
    name: string;
    size: number;
    format: string;
}

const columnsDef: TableColumnDefinition<Item>[] = [
    createTableColumn<Item>({
      columnId: "name",
      renderHeaderCell: () => <>Název</>,
    }),
    createTableColumn<Item>({
      columnId: "size",
      renderHeaderCell: () => <>Velikost</>,
    }),
    createTableColumn<Item>({
      columnId: "format",
      renderHeaderCell: () => <>Formát</>,
    }),
];

const columnSizes = {
    name: {idealWidth: 50, minWidth: 20},
    size: {idealWidth: 50, minWidth: 20},
    format: {idealWidth: 50, minWidth: 20}
}


const AipFileTable: FC = () => {
    const [columns, setColumns] = useState<TableColumnDefinition<Item>[]>(columnsDef);
    const items = [
        {name: "Nevim", size: 123456, format: 'xml'},
        {name: "dalsi", size: 794561654, format: 'pdf'},
        {name: "sloykz", size: null, format: null},
    ];

    const [columnSizingOptions] = useState<TableColumnSizingOptions>(columnSizes);
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
            }),
        ] 
      );
    
    const rows = sort(getRows((row) => {
        const selected = isRowSelected(row.rowId);
        return {
            ...row,
            onClick: (e: MouseEvent) => {
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


    return (
        <> 
            <Table
                ref={tableRef}
                as="table"
                sortable
                {...columnSizing_unstable.getTableProps()}
                className="aip-table"
            >
                <TableHeader>
                    <TableRow>
                        <TableSelectionCell
                            checked={allRowsSelected ? true : someRowsSelected ? "mixed" : false}
                            onClick={toggleAllRows}
                            onKeyDown={toggleAllKeydown}
                            checkboxIndicator={{"aria-label": "Vybrat vše"}}
                            className="header"
                            
                        />
                        {columns.map((column) => (
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
                            key={item.name}
                            className="table-row"
                        >
                            <TableSelectionCell
                                checked={selected}
                                checkboxIndicator={{ "aria-label": "Vybrat" }}
                                onClick={onClick}
                            />

                            {columns.map(col => (
                                <TableCell
                                    key={`item[${item.name}].${col.columnId}`} 
                                    {...columnSizing_unstable.getTableCellProps(col.columnId)}
                                    onClick={() => {}}
                                >
                                    <TableCellLayout truncate>
                                        {/* {col.renderCell(item)} */}
                                    </TableCellLayout>
                            </TableCell>
                            ))}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        {/* <Pagination
            onPageChange={handleChangePage}
            from={from}
            pageSize={pageSize}
            totalCount={aips.count}
            onPageSizeChange={handlePageSizeChange}
        /> */}
        </>
    );
}

export default AipFileTable;