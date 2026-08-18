import { useIntl } from "react-intl";
import { SortingOrder } from "elza-api";
import {FC, useCallback, useEffect, useState, MouseEvent, KeyboardEvent} from 'react';
import { useAppSelector } from 'utils/hooks';
import {StoreHorizontalLoader} from 'components/shared';
import storeFromArea from '../../shared/utils/storeFromArea.jsx';
import { formatAipSize } from './format';
import { formatDateCz } from 'utils/date';
import { findColDefByKey } from './columns';
import './AipTable.scss';
import { useHistory} from 'react-router';
import {urlAip} from '../../constants.tsx';
import { useThunkDispatch } from 'utils/hooks';
import {aipsFetchIfNeeded, aipsFilter, AREA_AIP, AREA_AIPS, setSelectedAips, } from "../../actions/aip/aip.ts";
import {
    MenuCheckedValueChangeData,
    MenuCheckedValueChangeEvent,
    Table,
    TableBody,
    TableCell,
    TableCellLayout,
    TableColumnDefinition,
    TableColumnId,
    TableColumnSizingOptions,
    TableHeader,
    TableHeaderCell,
    TableRow,
    TableSelectionCell,
    useTableColumnSizing_unstable,
    useTableFeatures,
    useTableSelection,
    useTableSort,
    createTableColumn,
} from '@fluentui/react-components';
import { getBoolIcon } from './AipCells';
import { colDef } from './columns';
import { Row } from 'react-bootstrap';
import AipFilterSection from './filter/AipFilterSection.tsx';
import Pagination from 'components/shared/pagination/Pagination.tsx';
import { Aip, AipFilterEntry, Aips } from 'typings/store/index.ts';
import AipDetail from './AipDetail.tsx';
import {AipDetailVO} from "elza-api";

type AipTableProps = {
    onAipSelect?: (id: number) => void;
    filterDisabled?: boolean;
    initialFilters?: AipFilterEntry[];
    hiddenValues?: string[];
    detailOpen?: boolean;
    setDetailOpen?: (open: boolean) => void;
}

/**
 * Řádky seznamu AIP ze store; dokud není načteno, je seznam prázdný.
 */
const getAipRows = (aips: Aips) => {
    if(aips.fetched && aips.rows){
        if(
            aips.filter?.from &&
            aips.filter?.pageSize &&
            aips.filter.from > aips.filter.pageSize - 1
        ){
            return aips.rows;
        }
        return [
            ...aips.rows,
        ];
    }

    return [];
};

const AipTable: FC<AipTableProps> = ({onAipSelect, filterDisabled, initialFilters, hiddenValues, detailOpen, setDetailOpen}) => {
    const aips = useAppSelector(state => storeFromArea(state, AREA_AIPS) as Aips);
    const aip = useAppSelector(state => storeFromArea(state, AREA_AIP) as Aip);
    const {from, pageSize} = aips.filter;
    const dispatch = useThunkDispatch();
    const items = getAipRows(aips);
    const history = useHistory();
    const {formatMessage} = useIntl();

    const columnsDef: TableColumnDefinition<AipDetailVO>[] = colDef.map((def) =>
        createTableColumn<AipDetailVO>({
            columnId: def.key,
            renderHeaderCell: () => <>{formatMessage(def.message)}</>,
            renderCell: (item: AipDetailVO) => <>{getContent(item, def.key)}</>,
            compare: (a, b) => {
                switch(def.valueType) {
                    case "number": return a[def.key] - b[def.key];
                    case "bool": return Number(a[def.key]) - Number(b[def.key]);
                    default: return a[def.key]?.localeCompare(b[def.key]);
                }
            },
        })
    );

    const [columns, setColumns] = useState<TableColumnDefinition<AipDetailVO>[]>(columnsDef);

    const formatUnitDate = (unitdateFrom: string, unitdateTo: string) => {
        return formatDateCz(new Date(unitdateFrom)) + " - " + (unitdateTo ? formatDateCz(new Date(unitdateTo)) : "?");
    }

    const getContent =(item: AipDetailVO, key: string) => {
        switch(key) {
            case "code": return <span className='link-like'>{item.code}</span>
            case "aipSize": return formatAipSize(item[key]);
            case "unitdateFrom":  return item.unitdateFrom ? formatUnitDate(item.unitdateFrom, item.unitdateTo): "-";
            case "fund.name": return item.fund ? item.fund.name : "-";
            case "institution.name": return item.institution.name;
            default:
                return findColDefByKey(key)?.valueType == "bool" ? getBoolIcon(item[key]) : item[key] ? item[key] : "-" ; // Sorry xD
        }
    }


    useEffect(() => {
        dispatch(aipsFetchIfNeeded());

        if(hiddenValues) {
            const res = columns.filter(col => !hiddenValues.includes(col.columnId.toString()));
            setColumns(res);
        }
    },[
        aips.filter.from,
        aips.filter.pageSize,
        aips.filter.filters,
        dispatch,
    ]);

    const toggleColumns = (e: MenuCheckedValueChangeEvent, data: MenuCheckedValueChangeData) => {
        setColumns(
            columnsDef.filter((col) =>
                data.checkedItems.some((checked) => checked == def[col.columnId].label
            ))
        );
    };

    const handleSelect = (id) =>  {
        setDetailOpen(true);
        history.push(urlAip(id))
    };

    const handleChangePage = (nextFrom: number) => nextFrom !== from && dispatch(aipsFilter(aips.filter.filters, nextFrom, aips.filter.pageSize))

    const def = colDef.reduce((acc, item) => {
        const key = item.key;
        acc[key] = {
            label: formatMessage(item.message),
            minWidth: item.minWidth,
            idealWidth: item.idealWidth
        };
        return acc;
    }, {});


    const [columnSizingOptions, setColumnSizingOptions] = useState<TableColumnSizingOptions>(def);
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
                    dispatch(setSelectedAips(selectedRows));
                }
            }),
        ]
      );

    const rows = getRows((row) => {
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
            appearance: selected ? "brand": "none",
        };
    });

    const headerSortProps = (columnId: TableColumnId) => ({
        onClick: (e: MouseEvent) => {
            toggleColumnSort(e, columnId);
            const field = colDef.find(def => def.key === columnId)?.field;
            if (field) {
                const descending = getSortDirection(columnId) === "ascending";
                dispatch(aipsFilter(aips.filter.filters, 0, aips.filter.pageSize,
                    [{field, order: descending ? SortingOrder.Desc : SortingOrder.Asc}]));
            }
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

    const handlePageSizeChange = (pageSize:number) => {
        dispatch(aipsFilter(aips.filter.filters, 0, pageSize));
    }

    return (
        <Row className='aip-table'>
            <AipDetail
                open={detailOpen}
                onClose={() => setDetailOpen(false)}
                onOpen={() => setDetailOpen(true)}
            />
            <StoreHorizontalLoader store={aips} />
            {aips.fetched && (
                <>
                    <AipFilterSection
                        columns={columns.map(item => def[item.columnId]?.name)}
                        onColsChange={toggleColumns}
                        filterDisabled={filterDisabled}
                        initialFilters={initialFilters}
                        hiddenValues={hiddenValues}
                    />
                    <Table
                        ref={tableRef}
                        as="table"
                        sortable
                        {...columnSizing_unstable.getTableProps()}
                        className="aip-table-body"
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
                            {rows.map(({ item, selected, onClick }) =>  {
                                const isDetailShown = aip?.data?.aipId == item.aipId;
                                return (
                                <TableRow
                                    key={item.code}
                                    className="table-row"
                                    style={{backgroundColor: isDetailShown ? "#ddd": undefined}}
                                >
                                    <TableSelectionCell
                                        checked={selected}
                                        checkboxIndicator={{ "aria-label": "Vybrat" }}
                                        onClick={onClick}
                                    />

                                    {columns.map(col => (
                                        <TableCell
                                            key={`item[${item.code}].${col.columnId}`}
                                            {...columnSizing_unstable.getTableCellProps(col.columnId)}
                                            onClick={() => onAipSelect ? onAipSelect(item.aipId) : handleSelect(item.aipId)}
                                        >
                                            <TableCellLayout truncate>
                                                {col.renderCell(item)}
                                            </TableCellLayout>
                                    </TableCell>
                                    ))}
                                </TableRow>
                            )}
                            )}
                        </TableBody>
                    </Table>
                    <Pagination
                        onPageChange={handleChangePage}
                        from={from}
                        pageSize={pageSize}
                        totalCount={aips.count}
                        onPageSizeChange={handlePageSizeChange}
                    />
                </>
            )
        }
    </Row>
    );
}

export default AipTable;
