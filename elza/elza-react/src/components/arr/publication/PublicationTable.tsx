import { useCallback, useEffect, useState, MouseEvent, KeyboardEvent } from 'react';
import {
    Menu,
    MenuButton,
    MenuItem,
    MenuList,
    MenuPopover,
    MenuTrigger,
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
    Tooltip,
    useTableColumnSizing_unstable,
    useTableFeatures,
    useTableSelection,
    useTableSort,
    createTableColumn,
    mergeClasses,
} from '@fluentui/react-components';
import { MoreHorizontalRegular, ArrowDownloadRegular, CopyRegular, DeleteRegular } from '@fluentui/react-icons';
import { defineMessages, FormattedMessage, useIntl } from 'react-intl';
import { tableMessages } from "components/shared/lang/tableMessages";
import { PublicationDetail, PublicationType, PublicationStateInternal } from 'elza-api';
import { colDef } from './columns';
import { stateMessages } from './messages';
import { useCanUsePublicationType } from './hooks';
import PublicationToolbar from './filter/PublicationToolbar';
import Pagination from 'components/shared/pagination/Pagination';
import { Api } from 'api/api';
import { downloadBlob } from 'actions/global/download';
import { useTableStyles } from './styles';

const messages = defineMessages({
    menuDownload: { id: 'publication.table.menu.download', defaultMessage: 'Stáhnout' },
    menuCopy:     { id: 'publication.table.menu.copy',     defaultMessage: 'Kopírovat do...' },
    menuDelete:   { id: 'publication.table.menu.delete',   defaultMessage: 'Odstranit' },
});

const DEFAULT_PAGE_SIZE = 25;

/**
 * Resolve the displayable string for a column. Object-typed columns
 * (e.g. `createdBy: UserRef`) supply a {@link colDef.getValue} extractor;
 * scalar columns fall back to direct property access.
 */
const cellValue = (def: typeof colDef[number], item: PublicationDetail): string => {
    const raw = def.getValue ? def.getValue(item) : item[def.key];
    return raw == null ? '' : String(raw);
};

interface Props {
    fundId: number;
    publicationTypes: PublicationType[];
}

function PublicationTable({ fundId, publicationTypes }: Props) {
    const classes = useTableStyles();
    const { formatMessage } = useIntl();
    const canPublishToType = useCanUsePublicationType(fundId);

    const [from, setFrom] = useState(0);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
    const [items, setItems] = useState<PublicationDetail[]>([]);
    const [totalCount, setTotalCount] = useState(0);
    const [refreshToken, setRefreshToken] = useState(0);
    useEffect(() => {
        (async () => {
            const {data} = await Api.publication.fundPublicationListFundPublications(fundId, undefined, from, pageSize);
            setItems(data.items);
            setTotalCount(data.totalCount);
        })();
    }, [fundId, from, pageSize, refreshToken]);

    const columnsDef: TableColumnDefinition<PublicationDetail>[] = colDef.map((def) =>
        createTableColumn<PublicationDetail>({
            columnId: def.key,
            renderHeaderCell: () => <>{formatMessage(def.message)}</>,
            renderCell: (item: PublicationDetail) => {
                if (def.key === 'state' && item.state) {
                    const stateMsg = stateMessages[item.state];
                    return <>{stateMsg ? formatMessage(stateMsg) : item.state}</>;
                }
                if (def.type === 'date') {
                    const raw = item[def.key] as string | undefined;
                    if (!raw) { return <>-</>; }
                    const date = new Date(raw);
                    const dateStr = date.toLocaleDateString();
                    const dateTimeStr = date.toLocaleString();
                    return (
                        <Tooltip content={dateTimeStr} relationship="label" appearance="inverted">
                            <span>{dateStr}</span>
                        </Tooltip>
                    );
                }
                return <>{cellValue(def, item) || '-'}</>;
            },
            compare: (a, b) => cellValue(def, a).localeCompare(cellValue(def, b)),
        })
    );

    const defMap = colDef.reduce<Record<string, { label: string; minWidth: number; idealWidth: number }>>((acc, item) => {
        acc[item.key] = { label: formatMessage(item.message), minWidth: item.minWidth, idealWidth: item.idealWidth };
        return acc;
    }, {});

    const [columns, setColumns] = useState<TableColumnDefinition<PublicationDetail>[]>(columnsDef);
    const [columnSizingOptions] = useState<TableColumnSizingOptions>(
        colDef.reduce<TableColumnSizingOptions>((acc, item) => {
            acc[item.key] = { minWidth: item.minWidth, idealWidth: item.idealWidth };
            return acc;
        }, {})
    );

    const {
        getRows,
        columnSizing_unstable,
        tableRef,
        sort: { getSortDirection, toggleColumnSort, sort },
        selection: { allRowsSelected, someRowsSelected, toggleAllRows, toggleRow, isRowSelected },
    } = useTableFeatures(
        { columns, items },
        [
            useTableColumnSizing_unstable({ columnSizingOptions }),
            useTableSort({ defaultSortState: { sortColumn: 'id', sortDirection: 'ascending' } }),
            useTableSelection({ selectionMode: 'multiselect' }),
        ]
    );

    const rows = sort(getRows((row) => {
        const selected = isRowSelected(row.rowId);
        return {
            ...row,
            onClick: (e: MouseEvent) => toggleRow(e, row.rowId),
            onKeyDown: (e: KeyboardEvent) => {
                if (e.key === ' ') {
                    e.preventDefault();
                    toggleRow(e, row.rowId);
                }
            },
            selected,
            appearance: selected ? 'brand' : 'none',
        };
    }));

    const headerSortProps = (columnId: TableColumnId) => ({
        onClick: (e: MouseEvent) => toggleColumnSort(e, columnId),
        sortDirection: getSortDirection(columnId),
    });

    const toggleAllKeydown = useCallback(
        (e: KeyboardEvent<HTMLDivElement>) => {
            if (e.key === ' ') {
                toggleAllRows(e);
                e.preventDefault();
            }
        },
        [toggleAllRows]
    );

    const handleToggleColumns = (e: MenuCheckedValueChangeEvent, data: MenuCheckedValueChangeData) => {
        setColumns(
            columnsDef.filter((col) =>
                data.checkedItems.some((checked) => checked === defMap[String(col.columnId)]?.label)
            )
        );
    };

    const handleDownload = async (item: PublicationDetail) => {
        const { data } = await Api.publication.fundPublicationDownloadFundPublication(fundId, item.id, { responseType: 'blob' });
        downloadBlob(data, `publication-${item.id}.xml`);
    };

    const handleCopy = async (item: PublicationDetail, targetPublicationTypeId: number) => {
        await Api.publication.fundPublicationCopyFundPublication(fundId, item.id, { targetPublicationTypeId });
        setRefreshToken((t) => t + 1);
    };

    const handleDelete = async (item: PublicationDetail) => {
        await Api.publication.fundPublicationInvalidateFundPublication(fundId, item.id);
        setRefreshToken((t) => t + 1);
    };

    const typeIdsWithNew = new Set(
        items.filter((item) => item.state === PublicationStateInternal.New).map((item) => item.typeId)
    );

    return (
        <div className={classes.root}>
            <PublicationToolbar
                columns={columns.map((col) => defMap[String(col.columnId)]?.label)}
                onColsChange={handleToggleColumns}
                onPublish={() => setRefreshToken((t) => t + 1)}
                publicationTypes={publicationTypes}
                disabledTypeIds={typeIdsWithNew}
                fundId={fundId}
            />
            <div className={classes.tableWrapper}>
                <Table
                    ref={tableRef}
                    as="table"
                    sortable
                    {...columnSizing_unstable.getTableProps()}
                >
                    <TableHeader>
                        <TableRow>
                            <TableSelectionCell
                                checked={allRowsSelected ? true : someRowsSelected ? 'mixed' : false}
                                onClick={toggleAllRows}
                                onKeyDown={toggleAllKeydown}
                                checkboxIndicator={{ 'aria-label': formatMessage(tableMessages.selectAll) }}
                                className={classes.header}
                                hidden
                            />
                            {columns.map((column) => (
                                <TableHeaderCell
                                    key={column.columnId}
                                    {...columnSizing_unstable.getTableHeaderCellProps(column.columnId)}
                                    {...headerSortProps(column.columnId)}
                                    className={classes.header}
                                >
                                    {column.renderHeaderCell()}
                                </TableHeaderCell>
                            ))}
                            <th className={mergeClasses(classes.header, classes.actionCol)} />
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {rows.map(({ item, selected, onClick, onKeyDown }) => (
                            <TableRow
                                key={item.id}
                                className={classes.tableRow}
                                onClick={onClick}
                                onKeyDown={onKeyDown}
                                aria-selected={selected}
                            >
                                <TableSelectionCell
                                    checked={selected}
                                    checkboxIndicator={{ 'aria-label': formatMessage(tableMessages.select) }}
                                    hidden
                                />
                                {columns.map((col) => (
                                    <TableCell
                                        key={`${item.id}.${col.columnId}`}
                                        {...columnSizing_unstable.getTableCellProps(col.columnId)}
                                    >
                                        <TableCellLayout truncate>
                                            {col.renderCell(item)}
                                        </TableCellLayout>
                                    </TableCell>
                                ))}
                                <TableCell className={classes.actionCol} onClick={(e) => e.stopPropagation()}>
                                    <Menu>
                                        <MenuTrigger disableButtonEnhancement>
                                            <MenuButton
                                                appearance="subtle"
                                                size="small"
                                                icon={<MoreHorizontalRegular />}
                                                menuIcon={null}
                                            />
                                        </MenuTrigger>
                                        <MenuPopover>
                                            <MenuList>
                                                <MenuItem disabled={!item.hasDownloadableFile} icon={<ArrowDownloadRegular />} onClick={() => handleDownload(item)}>
                                                    <FormattedMessage {...messages.menuDownload} />
                                                </MenuItem>
                                                {(() => {
                                                    const copyTargets = item.hasDownloadableFile
                                                        ? publicationTypes.filter((type) => (type.active ?? true) && canPublishToType(type) && type.id !== item.typeId && type.exportFilterCode === publicationTypes.find((t) => t.id === item.typeId)?.exportFilterCode)
                                                        : [];
                                                    return copyTargets.length === 0
                                                        ? <MenuItem icon={<CopyRegular />} disabled><FormattedMessage {...messages.menuCopy} /></MenuItem>
                                                        : (
                                                            <Menu>
                                                                <MenuTrigger disableButtonEnhancement>
                                                                    <MenuItem icon={<CopyRegular />}><FormattedMessage {...messages.menuCopy} /></MenuItem>
                                                                </MenuTrigger>
                                                                <MenuPopover>
                                                                    <MenuList>
                                                                        {copyTargets.map((type) => (
                                                                            <MenuItem key={type.id} onClick={() => handleCopy(item, type.id!)}>
                                                                                {type.name}
                                                                            </MenuItem>
                                                                        ))}
                                                                    </MenuList>
                                                                </MenuPopover>
                                                            </Menu>
                                                        );
                                                })()}
                                                <MenuItem icon={<DeleteRegular />} onClick={() => handleDelete(item)}>
                                                    <FormattedMessage {...messages.menuDelete} />
                                                </MenuItem>
                                            </MenuList>
                                        </MenuPopover>
                                    </Menu>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </div>
            <Pagination
                from={from}
                pageSize={pageSize}
                totalCount={totalCount}
                onPageChange={(nextFrom) => setFrom(nextFrom)}
                onPageSizeChange={(newPageSize) => { setPageSize(newPageSize); setFrom(0); }}
            />
        </div>
    );
};

export default PublicationTable;
