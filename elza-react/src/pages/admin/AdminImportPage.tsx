import { Button, Dropdown, Option, Spinner, Table, TableBody, TableCell, TableHeader, TableHeaderCell, TableRow, Tag, makeStyles, tokens } from '@fluentui/react-components';
import { AddRegular, ChevronLeftRegular, ChevronRightRegular } from '@fluentui/react-icons';
import { Api } from 'api';
import { ImportBatch, BatchState } from 'elza-api';
import { useCallback, useEffect, useState } from 'react';
import { FormattedDate, FormattedMessage, FormattedTime, defineMessages } from 'react-intl';
import { useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import * as perms from 'actions/user/Permission.jsx';
import { Ribbon } from 'components/index.jsx';
import { useWebsocket } from 'components/shared/web-socket/WebsocketProvider';
import { AppState } from 'typings/store';
import { EventType } from 'typings/websocket/EventType';
import { urlAdminImport } from '../../constants';
import { CreateBatchDialog } from '../../components/admin/import/CreateBatchDialog';
import { AdminLayout } from '../shared/layout/AdminLayout';

const PAGE_SIZES = [10, 25, 50];

const messages = defineMessages({
    title: {
        id: 'admin.import.title',
        defaultMessage: 'Importní dávky',
    },
    addEdx: {
        id: 'admin.import.action.addEdx',
        defaultMessage: 'Nová EDX2 dávka',
    },
    addCsv: {
        id: 'admin.import.action.addCsv',
        defaultMessage: 'Nová CSV dávka',
    },
    empty: {
        id: 'admin.import.empty',
        defaultMessage: 'Žádné importní dávky',
    },
    colName: {
        id: 'admin.import.col.name',
        defaultMessage: 'Název',
    },
    colType: {
        id: 'admin.import.col.type',
        defaultMessage: 'Druh',
    },
    colState: {
        id: 'admin.import.col.state',
        defaultMessage: 'Stav',
    },
    colCreatedAt: {
        id: 'admin.import.col.createdAt',
        defaultMessage: 'Založeno',
    },
    colExecutedAt: {
        id: 'admin.import.col.executedAt',
        defaultMessage: 'Spuštěno',
    },
    colFinishedAt: {
        id: 'admin.import.col.finishedAt',
        defaultMessage: 'Dokončeno',
    },
    prev: {
        id: 'admin.import.page.prev',
        defaultMessage: 'Předchozí',
    },
    next: {
        id: 'admin.import.page.next',
        defaultMessage: 'Další',
    },
    pageInfo: {
        id: 'admin.import.page.info',
        defaultMessage: 'Strana {page} z {total}',
    },
    pageSize: {
        id: 'admin.import.page.size',
        defaultMessage: 'Záznamů na stránku',
    },
});

const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
        padding: '16px',
    },
    toolbar: {
        display: 'flex',
        gap: '8px',
    },
    empty: {
        padding: '24px',
        color: tokens.colorNeutralForeground3,
        textAlign: 'center',
    },
    pagination: {
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        padding: '8px 4px',
        flexWrap: 'wrap',
    },
    pageSizeDropdown: { minWidth: '80px' },
});

const STATE_APPEARANCE: Record<BatchState, 'brand' | 'outline' | 'filled'> = {
    [BatchState.Preparation]: 'outline',
    [BatchState.InProgress]: 'brand',
    [BatchState.TestInProgress]: 'brand',
    [BatchState.TestFinished]: 'outline',
    [BatchState.Paused]: 'outline',
    [BatchState.Failed]: 'filled',
    [BatchState.Finished]: 'filled',
    [BatchState.Cancelled]: 'filled',
};

export function AdminImportPage() {
    const styles = useStyles();
    const userDetail = useSelector(({ userDetail }: AppState) => userDetail);
    const isAdmin = userDetail.hasOne(perms.ADMIN);

    const [batches, setBatches] = useState<ImportBatch[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [creating, setCreating] = useState<'EDX' | 'CSV' | null>(null);
    const [page, setPage] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [totalCount, setTotalCount] = useState(0);
    const history = useHistory();
    const websocket = useWebsocket();

    const reload = useCallback(async () => {
        setIsLoading(true);
        try {
            const { data } = await Api.importBatches.importBatchList(page * pageSize, pageSize);
            setBatches(data.items ?? []);
            setTotalCount(data.totalCount ?? 0);
        } finally {
            setIsLoading(false);
        }
    }, [page, pageSize]);

    const onChangePageSize = (n: number) => {
        setPageSize(n);
        setPage(0);
    };

    useEffect(() => {
        reload();
    }, [reload]);

    useEffect(() => {
        const listener = websocket.addListener((msg: { eventType?: string }) => {
            if (msg.eventType === EventType.IMPORT_BATCH_STATE_CHANGE) reload();
        });
        return () => websocket.removeListener(listener);
    }, [websocket, reload]);


    const openCreated = (batchId: number) => {
        setCreating(null);
        history.push(urlAdminImport(batchId));
    };

    const content = (
        <div className={styles.container}>
            {isAdmin && (
                <div className={styles.toolbar}>
                    <Button appearance="primary" icon={<AddRegular />} onClick={() => setCreating('EDX')}>
                        <FormattedMessage {...messages.addEdx} />
                    </Button>
                    <Button icon={<AddRegular />} onClick={() => setCreating('CSV')}>
                        <FormattedMessage {...messages.addCsv} />
                    </Button>
                </div>
            )}

            {isLoading && <Spinner />}

            {!isLoading && batches.length === 0 && (
                <div className={styles.empty}>
                    <FormattedMessage {...messages.empty} />
                </div>
            )}

            {creating && (
                <CreateBatchDialog
                    kind={creating}
                    open
                    onClose={() => setCreating(null)}
                    onCreated={openCreated}
                />
            )}

            {!isLoading && batches.length > 0 && (
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.colName} />
                            </TableHeaderCell>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.colType} />
                            </TableHeaderCell>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.colState} />
                            </TableHeaderCell>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.colExecutedAt} />
                            </TableHeaderCell>
                            <TableHeaderCell>
                                <FormattedMessage {...messages.colFinishedAt} />
                            </TableHeaderCell>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {batches.map(batch => (
                            <TableRow key={batch.batchId} onClick={() => history.push(urlAdminImport(batch.batchId))} style={{ cursor: 'pointer' }}>
                                <TableCell>{batch.name}</TableCell>
                                <TableCell>{batch.importType}</TableCell>
                                <TableCell>
                                    <Tag size="small" appearance={STATE_APPEARANCE[batch.state]}>
                                        {batch.state}
                                    </Tag>
                                </TableCell>
                                <TableCell>
                                    {batch.executedAt && (
                                        <>
                                            <FormattedDate value={batch.executedAt} />
                                            {' '}
                                            <FormattedTime value={batch.executedAt} />
                                        </>
                                    )}
                                </TableCell>
                                <TableCell>
                                    {(batch.state === BatchState.Finished
                                        || batch.state === BatchState.Cancelled
                                        || batch.state === BatchState.Failed) && batch.lastStateChangeAt && (
                                        <>
                                            <FormattedDate value={batch.lastStateChangeAt} />
                                            {' '}
                                            <FormattedTime value={batch.lastStateChangeAt} />
                                        </>
                                    )}
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            )}

            {!isLoading && totalCount > 0 && (() => {
                const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
                return (
                    <div className={styles.pagination}>
                        <Button
                            size="small"
                            icon={<ChevronLeftRegular />}
                            disabled={page === 0}
                            onClick={() => setPage(p => p - 1)}
                        >
                            <FormattedMessage {...messages.prev} />
                        </Button>
                        <span>
                            <FormattedMessage {...messages.pageInfo} values={{ page: page + 1, total: totalPages }} />
                        </span>
                        <Button
                            size="small"
                            icon={<ChevronRightRegular />}
                            iconPosition="after"
                            disabled={page + 1 >= totalPages}
                            onClick={() => setPage(p => p + 1)}
                        >
                            <FormattedMessage {...messages.next} />
                        </Button>
                        <span style={{ flex: 1 }} />
                        <span><FormattedMessage {...messages.pageSize} />:</span>
                        <Dropdown
                            className={styles.pageSizeDropdown}
                            selectedOptions={[String(pageSize)]}
                            value={String(pageSize)}
                            onOptionSelect={(_e, data) => data.optionValue && onChangePageSize(parseInt(data.optionValue))}
                        >
                            {PAGE_SIZES.map(n => (
                                <Option key={n} value={String(n)} text={String(n)}>{String(n)}</Option>
                            ))}
                        </Dropdown>
                    </div>
                );
            })()}
        </div>
    );

    return <AdminLayout ribbon={<Ribbon />} centerPanel={content} />;
}
