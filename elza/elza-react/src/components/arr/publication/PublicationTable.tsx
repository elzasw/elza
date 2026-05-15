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
import { PublicationDetail, PublicationType, PublicationStateInternal } from 'elza-api';
import { colDef, stateMessages } from './utils';
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

// const ALL_DUMMY_DATA: PublicationDetail[] = [
//     { id: 1,  typeId: 1, typeCode: 'PUBLIC',   typeName: 'Veřejný portál',  fundVersionId: 1, state: 'PUBLISHED',     createdByUserId: 1, createdAt: '2024-01-15', preparedAt: '2024-01-16',                          lastFetchedAt: '2024-03-01', publishedAt: '2024-01-17',                             hasDownloadableFile: true  },
//     { id: 2,  typeId: 2, typeCode: 'INTERNAL', typeName: 'Interní systém',  fundVersionId: 1, state: 'PREPARE_ERROR', createdByUserId: 2, createdAt: '2024-02-20',                          errorAt: '2024-02-21',                                                                                  hasDownloadableFile: false, errorMessage: 'Timeout při exportu' },
//     { id: 3,  typeId: 1, typeCode: 'PUBLIC',   typeName: 'Veřejný portál',  fundVersionId: 2, state: 'NEW',           createdByUserId: 3, createdAt: '2024-03-10',                                                                                                                                   hasDownloadableFile: false },
//     { id: 4,  typeId: 3, typeCode: 'ARCHIV',   typeName: 'Archivní portál', fundVersionId: 1, state: 'PUBLISHED',     createdByUserId: 1, createdAt: '2024-03-18', preparedAt: '2024-03-19',                          lastFetchedAt: '2024-04-10', publishedAt: '2024-03-20',                             hasDownloadableFile: true  },
//     { id: 5,  typeId: 1, typeCode: 'PUBLIC',   typeName: 'Veřejný portál',  fundVersionId: 2, state: 'INVALIDATED',   createdByUserId: 2, createdAt: '2024-04-01', preparedAt: '2024-04-02',                          lastFetchedAt: '2024-04-15', publishedAt: '2024-04-03', invalidatedAt: '2024-05-01', hasDownloadableFile: false },
//     { id: 6,  typeId: 2, typeCode: 'INTERNAL', typeName: 'Interní systém',  fundVersionId: 3, state: 'PUBLISHED',     createdByUserId: 3, createdAt: '2024-04-12', preparedAt: '2024-04-13',                          lastFetchedAt: '2024-05-02', publishedAt: '2024-04-14',                             hasDownloadableFile: true  },
//     { id: 7,  typeId: 3, typeCode: 'ARCHIV',   typeName: 'Archivní portál', fundVersionId: 2, state: 'PREPARE_ERROR', createdByUserId: 1, createdAt: '2024-04-25',                          errorAt: '2024-04-26',                                                                                  hasDownloadableFile: false, errorMessage: 'Neplatný formát záznamu' },
//     { id: 8,  typeId: 1, typeCode: 'PUBLIC',   typeName: 'Veřejný portál',  fundVersionId: 1, state: 'PUBLISHED',     createdByUserId: 2, createdAt: '2024-05-03', preparedAt: '2024-05-04',                          lastFetchedAt: '2024-06-01', publishedAt: '2024-05-05',                             hasDownloadableFile: true  },
//     { id: 9,  typeId: 2, typeCode: 'INTERNAL', typeName: 'Interní systém',  fundVersionId: 2, state: 'NEW',           createdByUserId: 3, createdAt: '2024-05-17',                                                                                                                                   hasDownloadableFile: false },
//     { id: 10, typeId: 3, typeCode: 'ARCHIV',   typeName: 'Archivní portál', fundVersionId: 1, state: 'PUBLISHED',     createdByUserId: 1, createdAt: '2024-05-29', preparedAt: '2024-05-30',                          lastFetchedAt: '2024-06-20', publishedAt: '2024-05-31',                             hasDownloadableFile: true  },
// ];

const DEFAULT_PAGE_SIZE = 25;

interface Props {
    fundId: number;
    publicationTypes: PublicationType[];
}

function PublicationTable({ fundId, publicationTypes }: Props) {
    const classes = useTableStyles();
    const { formatMessage } = useIntl();

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
                return <>{item[def.key] ?? '-'}</>;
            },
            compare: (a, b) => String(a[def.key] ?? '').localeCompare(String(b[def.key] ?? '')),
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
                                checkboxIndicator={{ 'aria-label': 'Vybrat vše' }}
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
                                    checkboxIndicator={{ 'aria-label': 'Vybrat' }}
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
                                                    const copyTargets = publicationTypes.filter((type) => (type.active ?? true) && type.id !== item.typeId && type.exportFilterCode === publicationTypes.find((t) => t.id === item.typeId)?.exportFilterCode);
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
