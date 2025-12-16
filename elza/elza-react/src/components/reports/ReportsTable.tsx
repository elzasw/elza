import {
    Table as FluentTable,
    TableBody,
    TableCell,
    TableCellLayout,
    TableColumnDefinition,
    TableHeader,
    TableHeaderCell,
    TableRow,
    createTableColumn,
    useTableFeatures,
    useTableSort,
    // useTableColumnSizing_unstable,
    useArrowNavigationGroup,
    TableColumnId,
} from "@fluentui/react-components";
import { ReportReportData, ReportReportRow, ReportValueType } from "elza-api";
import { FormattedMessage } from "react-intl";

export interface Props {
    reportData: ReportReportData;
}

export function ReportsTable({ reportData }: Props) {
    const columns: TableColumnDefinition<ReportReportRow>[] = reportData.header.map((header, index) =>
        createTableColumn<ReportReportRow>({
            columnId: header,
            renderHeaderCell: () => <><FormattedMessage id={`admin_reports_header_${header}`} defaultMessage={header} /></>,
            renderCell: (item) => {
                if (item.cols[index].valueType === ReportValueType.String) {
                    return <>{(item.cols[index] as any).textValue}</>;
                }
                if (item.cols[index].valueType === ReportValueType.Int) {
                    return <>{(item.cols[index] as any).intValue}</>;
                }
            },
            compare: (a, b) => {
                const aCol: any = a.cols[index];
                const bCol: any = b.cols[index];
                switch (aCol.valueType) {
                    case ReportValueType.Int:
                        return aCol.intValue - bCol.intValue;
                    case ReportValueType.String:
                        return aCol.textValue?.localeCompare(bCol.textValue);
                    default:
                        return 0;
                }
            },
        }),
    );

    const headerSortProps = (columnId: TableColumnId) => ({
        onClick: (e: React.MouseEvent) => {
            toggleColumnSort(e, columnId);
        },
        sortDirection: getSortDirection(columnId),
    });

    const {
        getRows,
        sort: { getSortDirection, toggleColumnSort, sort },
        tableRef,
    } = useTableFeatures(
        {
            columns,
            items: reportData.rows,
        },
        [
            useTableSort({
                defaultSortState: { sortColumn: "file", sortDirection: "ascending" },
            }),
        ],
    );

    const rows = sort(
        getRows((row) => {
            return { ...row };
        }),
    );

    const keyboardNavAttr = useArrowNavigationGroup({ axis: "grid" });

    return (
        <div style={{ overflow: "auto", height: "100%" }}>
            <FluentTable
                {...keyboardNavAttr}
                size="small"
                role="grid"
                sortable
                aria-label="Report table"
                ref={tableRef}
                noNativeElements={false}
                style={{ tableLayout: "auto" }}
            >
                <TableHeader>
                    <TableRow>
                        {columns.map(({ renderHeaderCell, columnId }) => {
                            return (
                                <TableHeaderCell key={columnId} {...headerSortProps(columnId)}>
                                    {renderHeaderCell()}
                                </TableHeaderCell>
                            );
                        })}
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {rows.map(({ item, rowId }) => (
                        <TableRow
                            key={rowId}
                        >
                            {columns.map(({ columnId, renderCell }) => {
                                const value = item[columnId];
                                return (
                                    <TableCell tabIndex={0} role="gridcell">
                                        <TableCellLayout truncate={false}>{renderCell ? renderCell(item) : value}</TableCellLayout>
                                    </TableCell>
                                );
                            })}
                        </TableRow>
                    ))}
                </TableBody>
            </FluentTable>
        </div>
    );
}

export default ReportsTable;
