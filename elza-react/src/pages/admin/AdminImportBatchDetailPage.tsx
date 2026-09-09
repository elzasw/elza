import {
    Button,
    Card,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    Spinner,
    Table,
    TableBody,
    TableCell,
    TableHeader,
    TableHeaderCell,
    TableRow,
    Tag,
    Tooltip,
    Menu,
    MenuTrigger,
    MenuPopover,
    MenuList,
    MenuItem,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    ArrowLeftRegular,
    ArrowUploadRegular,
    DeleteRegular,
    ErrorCircleRegular,
    FolderRegular,
    OpenRegular,
    PauseRegular,
    PlayRegular,
    StopRegular,
    ArchiveRegular,
    BeakerRegular,
} from '@fluentui/react-icons';
import { Api } from 'api';
import { BatchImportType, BatchState, ImportBatch, ImportBatchDescCsv, ImportBatchEdx, ImportItem, ImportItemError, ItemState } from 'elza-api';
import { ChangeEvent, useCallback, useEffect, useRef, useState } from 'react';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { useHistory, useParams } from 'react-router';
import { showConfirmDialog } from 'components/shared/dialog';
import { addToastrDanger, addToastrSuccess } from 'components/shared/toastr/ToastrActions';
import { Ribbon } from 'components/index.jsx';
import { useWebsocket } from 'components/shared/web-socket/WebsocketProvider';
import { URL_ADMIN_IMPORT, urlFundTree } from '../../constants';
import { EventType } from 'typings/websocket/EventType';
import { AdminLayout } from '../shared/layout/AdminLayout';
import { useThunkDispatch } from 'utils/hooks';
import { ServerFolderDialog } from '../../components/admin/import/ServerFolderDialog';
import { WebApi } from 'actions/WebApi';

const messages = defineMessages({
    back: { id: 'admin.import.detail.back', defaultMessage: 'Zpět' },
    start: { id: 'admin.import.detail.start', defaultMessage: 'Spustit' },
    dryRun: { id: 'admin.import.detail.dryRun', defaultMessage: 'Testovací běh' },
    pause: { id: 'admin.import.detail.pause', defaultMessage: 'Pozastavit' },
    cancel: { id: 'admin.import.detail.cancel', defaultMessage: 'Přerušit' },
    remove: { id: 'admin.import.detail.remove', defaultMessage: 'Smazat' },
    settings: { id: 'admin.import.detail.settings', defaultMessage: 'Nastavení' },
    items: { id: 'admin.import.detail.items', defaultMessage: 'Položky' },
    upload: { id: 'admin.import.detail.upload', defaultMessage: 'Nahrát soubor' },
    uploadZip: { id: 'admin.import.detail.uploadZip', defaultMessage: 'Nahrát ZIP' },
    fromFolder: { id: 'admin.import.detail.fromFolder', defaultMessage: 'Ze složky na serveru' },
    confirmCancel: { id: 'admin.import.detail.confirmCancel', defaultMessage: 'Opravdu chcete dávku přerušit?' },
    confirmDelete: { id: 'admin.import.detail.confirmDelete', defaultMessage: 'Opravdu chcete dávku smazat?' },
    confirmDeleteItem: { id: 'admin.import.detail.confirmDeleteItem', defaultMessage: 'Opravdu chcete položku odstranit?' },
    empty: { id: 'admin.import.detail.empty', defaultMessage: 'Dávka neobsahuje žádné položky' },
    colName: { id: 'admin.import.detail.col.name', defaultMessage: 'Název' },
    colState: { id: 'admin.import.detail.col.state', defaultMessage: 'Stav' },
    colOrder: { id: 'admin.import.detail.col.order', defaultMessage: 'Pořadí' },
    colNodes: { id: 'admin.import.detail.col.nodes', defaultMessage: 'Vytvořeno JP' },
    colNodesUpdated: { id: 'admin.import.detail.col.nodesUpdated', defaultMessage: 'Aktualizováno JP' },
    colApsCreated: { id: 'admin.import.detail.col.apsCreated', defaultMessage: 'Vytvořeno AE' },
    colApsPaired: { id: 'admin.import.detail.col.apsPaired', defaultMessage: 'Spárováno AE' },
    errorDialogTitle: { id: 'admin.import.detail.errorDialog.title', defaultMessage: 'Detail chyby' },
    errorDialogClose: { id: 'admin.import.detail.errorDialog.close', defaultMessage: 'Zavřít' },
    errorShow: { id: 'admin.import.detail.errorShow', defaultMessage: 'Zobrazit chybu' },
    toastFinished: { id: 'admin.import.detail.toast.finished', defaultMessage: 'Dávka „{name}" byla dokončena' },
    toastFailed: { id: 'admin.import.detail.toast.failed', defaultMessage: 'Dávka „{name}" skončila s chybou' },
    folderNotConfigured: {
        id: 'admin.import.detail.folderNotConfigured',
        defaultMessage: 'Import ze složky na serveru není nakonfigurován (elza.import.batchInputDir)',
    },
    openFund: {
        id: 'admin.import.detail.openFund',
        defaultMessage: 'Otevřít archivní soubor',
    },
    removeItem: {
        id: 'admin.import.detail.removeItem',
        defaultMessage: 'Odstranit položku',
    },
});

const useStyles = makeStyles({
    container: { display: 'flex', flexDirection: 'column', gap: tokens.spacingVerticalL, padding: '16px' },
    header: { display: 'flex', alignItems: 'center', gap: tokens.spacingHorizontalM, flexWrap: 'wrap' },
    actions: { display: 'flex', gap: tokens.spacingHorizontalS, flexWrap: 'wrap' },
    settings: { display: 'grid', gridTemplateColumns: 'auto 1fr', columnGap: tokens.spacingHorizontalL, rowGap: tokens.spacingVerticalS, padding: tokens.spacingVerticalM },
    label: { color: tokens.colorNeutralForeground3 },
    spacer: { flex: 1 },
    empty: { padding: tokens.spacingVerticalM, color: tokens.colorNeutralForeground3, textAlign: 'center' },
    hidden: { display: 'none' },
    errorSummary: { margin: 0, fontWeight: tokens.fontWeightSemibold },
    errorDetail: {
        whiteSpace: 'pre-wrap',
        fontFamily: 'monospace',
        fontSize: tokens.fontSizeBase200,
        maxHeight: '400px',
        overflow: 'auto',
        backgroundColor: tokens.colorNeutralBackground3,
        padding: tokens.spacingVerticalS,
        marginTop: tokens.spacingVerticalM,
    },
});

const CAN_START: BatchState[] = [BatchState.Preparation, BatchState.TestFinished, BatchState.Paused, BatchState.Failed];
const CAN_DRY_RUN: BatchState[] = [BatchState.Preparation, BatchState.TestFinished];
const CAN_PAUSE: BatchState[] = [BatchState.InProgress];
const CAN_CANCEL: BatchState[] = [BatchState.Preparation, BatchState.InProgress, BatchState.TestInProgress, BatchState.TestFinished, BatchState.Paused, BatchState.Failed];
const CAN_DELETE: BatchState[] = [BatchState.Preparation, BatchState.Finished, BatchState.Cancelled];
const CAN_EDIT_ITEMS: BatchState[] = [BatchState.Preparation, BatchState.TestFinished];

export function AdminImportBatchDetailPage() {
    const styles = useStyles();
    const intl = useIntl();
    const history = useHistory();
    const dispatch = useThunkDispatch();
    const websocket = useWebsocket();
    const { id } = useParams<{ id: string }>();
    const batchId = Number(id);

    const [batch, setBatch] = useState<ImportBatch | null>(null);
    const [items, setItems] = useState<ImportItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [showFolderDialog, setShowFolderDialog] = useState(false);
    const [errorDetail, setErrorDetail] = useState<ImportItemError | null>(null);
    const [scopeMap, setScopeMap] = useState<Record<number, string>>({});
    const [folderConfigured, setFolderConfigured] = useState<boolean | null>(null);

    const fileInputRef = useRef<HTMLInputElement>(null);
    const zipInputRef = useRef<HTMLInputElement>(null);
    const prevStateRef = useRef<BatchState | null>(null);

    const reload = useCallback(async () => {
        setLoading(true);
        try {
            const [batchRes, itemsRes, scopes] = await Promise.all([
                Api.importBatches.importBatchGet(batchId),
                Api.importBatches.importBatchListItems(batchId),
                WebApi.getAllScopes(),
            ]);
            setBatch(batchRes.data as ImportBatch);
            setItems(itemsRes.data ?? []);
            const map: Record<number, string> = {};
            for (const s of (scopes ?? []) as Array<{ id: number; name: string }>) {
                map[s.id] = s.name;
            }
            setScopeMap(map);
        } finally {
            setLoading(false);
        }
    }, [batchId]);

    useEffect(() => {
        reload();
    }, [reload]);

    useEffect(() => {
        let cancelled = false;
        Api.importBatches.importBatchListServerFolder(undefined)
            .then(({ data }) => { if (!cancelled) setFolderConfigured((data ?? []).length > 0); })
            .catch(() => { if (!cancelled) setFolderConfigured(false); });
        return () => { cancelled = true; };
    }, []);

    useEffect(() => {
        const listener = websocket.addListener((msg: { eventType?: string; ids?: number[] }) => {
            if (msg.eventType !== EventType.IMPORT_BATCH_STATE_CHANGE) return;
            if (!msg.ids?.includes(batchId)) return;
            reload();
        });
        return () => websocket.removeListener(listener);
    }, [websocket, batchId, reload]);

    useEffect(() => {
        if (!batch) return;
        const prev = prevStateRef.current;
        const now = batch.state;
        if (prev != null && prev !== now) {
            if (now === BatchState.Failed) {
                dispatch(addToastrDanger(intl.formatMessage(messages.toastFailed, { name: batch.name })));
            } else if (now === BatchState.Finished) {
                dispatch(addToastrSuccess(intl.formatMessage(messages.toastFinished, { name: batch.name })));
            }
        }
        prevStateRef.current = now;
    }, [batch, dispatch, intl]);

    const runAction = async (action: () => Promise<unknown>) => {
        await action();
        await reload();
    };

    const confirm = async (msg: { id: string; defaultMessage: string }) =>
        dispatch(showConfirmDialog(intl.formatMessage(msg))) as unknown as Promise<boolean>;

    const onStart = () => runAction(() => Api.importBatches.importBatchStart(batchId));
    const onDryRun = () => runAction(() => Api.importBatches.importBatchDryRun(batchId));
    const onPause = () => runAction(() => Api.importBatches.importBatchPause(batchId));
    const onCancel = async () => {
        if (await confirm(messages.confirmCancel)) {
            await runAction(() => Api.importBatches.importBatchCancel(batchId));
        }
    };
    const onDelete = async () => {
        if (await confirm(messages.confirmDelete)) {
            await Api.importBatches.importBatchRemove(batchId);
            history.push(URL_ADMIN_IMPORT);
        }
    };
    const onRemoveItem = async (itemId: number) => {
        if (await confirm(messages.confirmDeleteItem)) {
            await runAction(() => Api.importBatches.importBatchRemoveItem(itemId));
        }
    };

    const onUploadFile = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        e.target.value = '';
        if (!file) return;
        await runAction(() => Api.importBatches.importBatchUploadItem(batchId, file));
    };
    const onUploadZip = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        e.target.value = '';
        if (!file) return;
        await runAction(() => Api.importBatches.importBatchUploadZip(batchId, file));
    };
    const onShowError = async (itemId: number) => {
        const { data } = await Api.importBatches.importBatchItemError(itemId);
        setErrorDetail(data);
    };

    if (loading || !batch) {
        return <AdminLayout ribbon={<Ribbon />} centerPanel={<div className={styles.container}><Spinner /></div>} />;
    }

    const state = batch.state;
    const isEdx = batch.importType === BatchImportType.Edx2;
    const edx = isEdx ? (batch as ImportBatchEdx) : null;
    const csv = !isEdx ? (batch as ImportBatchDescCsv) : null;
    const canEditItems = CAN_EDIT_ITEMS.includes(state);

    const content = (
        <>
            <div className={styles.container}>
                <div className={styles.header}>
                    <Button appearance="subtle" icon={<ArrowLeftRegular />} onClick={() => history.push(URL_ADMIN_IMPORT)}>
                        <FormattedMessage {...messages.back} />
                    </Button>
                    <h2 style={{ margin: 0 }}>{batch.name}</h2>
                    <Tag size="small" appearance="brand">{state}</Tag>
                    <div className={styles.spacer} />
                    <div className={styles.actions}>
                        {CAN_START.includes(state) && <Button icon={<PlayRegular />} appearance="primary" onClick={onStart}><FormattedMessage {...messages.start} /></Button>}
                        {CAN_DRY_RUN.includes(state) && <Button icon={<BeakerRegular />} onClick={onDryRun}><FormattedMessage {...messages.dryRun} /></Button>}
                        {CAN_PAUSE.includes(state) && <Button icon={<PauseRegular />} onClick={onPause}><FormattedMessage {...messages.pause} /></Button>}
                        {CAN_CANCEL.includes(state) && <Button icon={<StopRegular />} onClick={onCancel}><FormattedMessage {...messages.cancel} /></Button>}
                        {CAN_DELETE.includes(state) && <Button icon={<DeleteRegular />} onClick={onDelete}><FormattedMessage {...messages.remove} /></Button>}
                    </div>
                </div>

                <Card>
                    <h3><FormattedMessage {...messages.settings} /></h3>
                    <div className={styles.settings}>
                        <span className={styles.label}>importType</span><span>{batch.importType}</span>
                        <span className={styles.label}>skipError</span><span>{String(batch.skipError)}</span>
                        <span className={styles.label}>keepFiles</span><span>{String(batch.keepFiles)}</span>
                        {edx && (<>
                            <span className={styles.label}>fundImportStrategy</span><span>{edx.fundImportStrategy}</span>
                            <span className={styles.label}>fundPairKey</span><span>{edx.fundPairKey ?? '—'}</span>
                            <span className={styles.label}>ignoreRootNodes</span><span>{String(edx.ignoreRootNodes)}</span>
                            <span className={styles.label}>scope</span><span>{edx.scopeId != null ? (scopeMap[edx.scopeId] ?? edx.scopeId) : '—'}</span>
                        </>)}
                        {csv && (<>
                            <span className={styles.label}>separator</span><span>{csv.separator ?? '—'}</span>
                            <span className={styles.label}>encoding</span><span>{csv.encoding ?? '—'}</span>
                        </>)}
                    </div>
                </Card>

                <Card>
                    <div className={styles.header}>
                        <h3 style={{ margin: 0 }}><FormattedMessage {...messages.items} /></h3>
                        <div className={styles.spacer} />
                        {canEditItems && (
                            <div className={styles.actions}>
                                <Button icon={<ArrowUploadRegular />} onClick={() => fileInputRef.current?.click()}>
                                    <FormattedMessage {...messages.upload} />
                                </Button>
                                <Button icon={<ArchiveRegular />} onClick={() => zipInputRef.current?.click()}>
                                    <FormattedMessage {...messages.uploadZip} />
                                </Button>
                                <Tooltip
                                    content={folderConfigured === false ? intl.formatMessage(messages.folderNotConfigured) : ''}
                                    relationship="label"
                                    withArrow
                                >
                                    <Button
                                        icon={<FolderRegular />}
                                        onClick={() => setShowFolderDialog(true)}
                                        disabled={folderConfigured !== true}
                                    >
                                        <FormattedMessage {...messages.fromFolder} />
                                    </Button>
                                </Tooltip>
                            </div>
                        )}
                        <input ref={fileInputRef} type="file" className={styles.hidden} onChange={onUploadFile} />
                        <input ref={zipInputRef} type="file" accept=".zip,application/zip" className={styles.hidden} onChange={onUploadZip} />
                    </div>
                    {items.length === 0 ? (
                        <div className={styles.empty}><FormattedMessage {...messages.empty} /></div>
                    ) : (
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHeaderCell><FormattedMessage {...messages.colOrder} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colName} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colState} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colNodes} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colNodesUpdated} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colApsCreated} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colApsPaired} /></TableHeaderCell>
                                    <TableHeaderCell />
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {items.map(it => (
                                    <TableRow key={it.itemId}>
                                        <TableCell>{it.execOrder}</TableCell>
                                        <TableCell>{it.itemName}</TableCell>
                                        <TableCell>
                                            <Tag size="small">{it.state}</Tag>
                                            {it.state === ItemState.Error && (
                                                <Button
                                                    appearance="subtle"
                                                    size="small"
                                                    icon={<ErrorCircleRegular />}
                                                    title={intl.formatMessage(messages.errorShow)}
                                                    onClick={() => onShowError(it.itemId)}
                                                />
                                            )}
                                        </TableCell>
                                        <TableCell>{it.nodesCreated ?? 0}</TableCell>
                                        <TableCell>{it.nodesUpdated ?? 0}</TableCell>
                                        <TableCell>{it.apsCreated ?? 0}</TableCell>
                                        <TableCell>{it.apsPaired ?? 0}</TableCell>
                                        <TableCell>
                                            {it.state === ItemState.Finished && (() => {
                                                const funds = (it.resultFundIds && it.resultFundIds.length > 0)
                                                    ? it.resultFundIds
                                                    : (it.fundId != null ? [it.fundId] : []);
                                                if (funds.length === 0) return null;
                                                if (funds.length === 1) {
                                                    return (
                                                        <Button
                                                            appearance="subtle"
                                                            icon={<OpenRegular />}
                                                            title={intl.formatMessage(messages.openFund)}
                                                            onClick={() => history.push(urlFundTree(funds[0]))}
                                                        />
                                                    );
                                                }
                                                return (
                                                    <Menu>
                                                        <MenuTrigger disableButtonEnhancement>
                                                            <Button
                                                                appearance="subtle"
                                                                icon={<OpenRegular />}
                                                                title={intl.formatMessage(messages.openFund)}
                                                            />
                                                        </MenuTrigger>
                                                        <MenuPopover>
                                                            <MenuList>
                                                                {funds.map(fid => (
                                                                    <MenuItem
                                                                        key={fid}
                                                                        onClick={() => history.push(urlFundTree(fid))}
                                                                    >
                                                                        {`AS #${fid}`}
                                                                    </MenuItem>
                                                                ))}
                                                            </MenuList>
                                                        </MenuPopover>
                                                    </Menu>
                                                );
                                            })()}
                                            {((canEditItems && it.state !== ItemState.Finished) || it.state === ItemState.Error) && (
                                                <Button
                                                    appearance="subtle"
                                                    icon={<DeleteRegular />}
                                                    title={intl.formatMessage(messages.removeItem)}
                                                    onClick={() => onRemoveItem(it.itemId)}
                                                />
                                            )}
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    )}
                </Card>
            </div>

            <Dialog open={errorDetail !== null} onOpenChange={(_, data) => { if (!data.open) setErrorDetail(null); }}>
                <DialogSurface>
                    <DialogBody>
                        <DialogTitle><FormattedMessage {...messages.errorDialogTitle} /></DialogTitle>
                        <DialogContent>
                            {errorDetail && (
                                <>
                                    <p className={styles.errorSummary}>{errorDetail.error}</p>
                                    {errorDetail.errorDetail && (
                                        <pre className={styles.errorDetail}>{errorDetail.errorDetail}</pre>
                                    )}
                                </>
                            )}
                        </DialogContent>
                        <DialogActions>
                            <Button appearance="primary" onClick={() => setErrorDetail(null)}>
                                <FormattedMessage {...messages.errorDialogClose} />
                            </Button>
                        </DialogActions>
                    </DialogBody>
                </DialogSurface>
            </Dialog>

            <ServerFolderDialog
                batchId={batchId}
                open={showFolderDialog}
                onClose={() => setShowFolderDialog(false)}
                onImported={reload}
            />
        </>
    );

    return <AdminLayout ribbon={<Ribbon />} centerPanel={content} />;
}
