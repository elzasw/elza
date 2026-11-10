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
    Badge,
    ToggleButton,
    Table,
    TableBody,
    TableCell,
    TableHeader,
    TableHeaderCell,
    TableRow,
    ProgressBar,
    Tag,
    Tooltip,
    Menu,
    MenuTrigger,
    MenuPopover,
    MenuList,
    MenuItem,
    makeStyles,
    mergeClasses,
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
import { ChangeEvent, ReactElement, ReactNode, useCallback, useEffect, useRef, useState } from 'react';
import { FormattedDate, FormattedMessage, FormattedTime, MessageDescriptor, defineMessages, useIntl } from 'react-intl';
import { useHistory, useParams } from 'react-router';
import { showConfirmDialog } from 'components/shared/dialog';
import { Ribbon } from 'components/index.jsx';
import { useWebsocket } from 'components/shared/web-socket/WebsocketProvider';
import { URL_ADMIN_IMPORT, urlFundTree } from '../../constants';
import { EventType } from 'typings/websocket/EventType';
import { AdminLayout } from '../shared/layout/AdminLayout';
import { useThunkDispatch } from 'utils/hooks';
import { exceedsUploadLimit, useUploadLimits } from 'utils/hooks/useUploadLimits';
import { humanFileSize } from 'components/Utils.jsx';
import { addToastrDanger } from 'components/shared/toastr/ToastrActions';
import { ServerFolderDialog } from '../../components/admin/import/ServerFolderDialog';
import { batchSettingsMessages } from '../../components/admin/import/messages';
import { globalMessages } from 'components/shared/lang';
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
    maxFileSize: { id: 'admin.import.detail.maxFileSize', defaultMessage: 'Maximální velikost souboru: {size}' },
    uploadTooLarge: {
        id: 'admin.import.detail.uploadTooLarge',
        defaultMessage: 'Soubor „{name}“ ({size}) je větší než povolená velikost {limit}',
    },
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
    folderNotConfigured: {
        id: 'admin.import.detail.folderNotConfigured',
        defaultMessage: 'Složka pro import na serveru není nakonfigurovaná, neexistuje nebo je prázdná',
    },
    filesPurged: {
        id: 'admin.import.detail.filesPurged',
        defaultMessage: 'Vstupní soubory dávky byly odstraněny v rámci retence, dávku už nelze spustit',
    },
    openFund: {
        id: 'admin.import.detail.openFund',
        defaultMessage: 'Otevřít archivní soubor',
    },
    removeItem: {
        id: 'admin.import.detail.removeItem',
        defaultMessage: 'Odstranit položku',
    },
    startHint: {
        id: 'admin.import.detail.startHint',
        defaultMessage: 'Importuje dosud nezpracované položky postupně v pořadí; pozastavenou nebo neúspěšnou dávku spustí znovu od první nezpracované položky',
    },
    dryRunHint: {
        id: 'admin.import.detail.dryRunHint',
        defaultMessage: 'Ověří soubory položek bez zápisu do archivních souborů',
    },
    pauseHint: {
        id: 'admin.import.detail.pauseHint',
        defaultMessage: 'Zastaví zpracování po dokončení aktuální položky; dávku lze znovu spustit',
    },
    cancelHint: {
        id: 'admin.import.detail.cancelHint',
        defaultMessage: 'Ukončí dávku natrvalo; zbývající položky se nezpracují, už importovaná data zůstanou',
    },
    removeHint: {
        id: 'admin.import.detail.removeHint',
        defaultMessage: 'Smaže dávku včetně položek; importovaná data v archivních souborech zůstanou',
    },
    filesDeletedAt: {
        id: 'admin.import.detail.filesDeletedAt',
        defaultMessage: 'Soubory odstraněny',
    },
    progress: {
        id: 'admin.import.detail.progress',
        defaultMessage: 'Zpracováno {done} z {total}',
    },
    progressErrors: {
        id: 'admin.import.detail.progressErrors',
        defaultMessage: '{errors, plural, one {# chyba} few {# chyby} other {# chyb}}',
    },
});

const useStyles = makeStyles({
    container: { display: 'flex', flexDirection: 'column', gap: tokens.spacingVerticalL, padding: '16px' },
    // The header keeps its natural height, the batch card takes the rest of the panel.
    detail: { display: 'grid', gridTemplateRows: 'auto minmax(0, 1fr)', height: '100%', boxSizing: 'border-box' },
    batchCard: { minHeight: 0 },
    // Settings scroll away with the list, so the table gets the whole card once the user scrolls.
    batchScroll: { flexGrow: 1, minHeight: 0, overflow: 'auto', display: 'flex', flexDirection: 'column', gap: tokens.spacingVerticalM },
    stickyHeader: { position: 'sticky', top: 0, zIndex: 1, backgroundColor: tokens.colorNeutralBackground1 },
    progress: { display: 'flex', flexDirection: 'column', gap: tokens.spacingVerticalXS, flexShrink: 0 },
    stateSummary: { display: 'flex', alignItems: 'center', gap: tokens.spacingHorizontalXS, flexWrap: 'wrap' },
    header: { display: 'flex', alignItems: 'center', gap: tokens.spacingHorizontalM, flexWrap: 'wrap' },
    actions: { display: 'flex', gap: tokens.spacingHorizontalS, flexWrap: 'wrap' },
    settings: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', columnGap: tokens.spacingHorizontalXXL, rowGap: tokens.spacingVerticalS },
    field: { display: 'flex', flexDirection: 'column' },
    rowStart: { gridColumnStart: 1 },
    label: { color: tokens.colorNeutralForeground3, fontSize: tokens.fontSizeBase200 },
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
// The runner is walking the items only in these two states; they are the ones in which an entry
// in the asynchronous queue still refers to the batch. Mirrors BatchState.isRunning() on the server.
const RUNNING: BatchState[] = [BatchState.InProgress, BatchState.TestInProgress];
const canDelete = (state: BatchState) => !RUNNING.includes(state);
const CAN_EDIT_ITEMS: BatchState[] = [BatchState.Preparation, BatchState.TestFinished];
// Item states in their lifecycle order, as shown in the summary above the table.
const ITEM_STATE_ORDER: ItemState[] = [ItemState.Ready, ItemState.Running, ItemState.TestRunFinished, ItemState.Finished, ItemState.Error];
const ITEM_STATE_COLOR: Partial<Record<ItemState, 'danger' | 'success' | 'informative'>> = {
    [ItemState.Error]: 'danger',
    [ItemState.Finished]: 'success',
    [ItemState.TestRunFinished]: 'informative',
};

interface SettingProps {
    label: MessageDescriptor;
    // Starts a new grid row, so the row's settings line up under the first column.
    startsRow?: boolean;
    children: ReactNode;
}

function Setting({ label, startsRow, children }: SettingProps) {
    const styles = useStyles();
    return (
        <div className={mergeClasses(styles.field, startsRow && styles.rowStart)}>
            <span className={styles.label}><FormattedMessage {...label} /></span>
            <span>{children}</span>
        </div>
    );
}

export function AdminImportBatchDetailPage() {
    const styles = useStyles();
    const intl = useIntl();
    const history = useHistory();
    const dispatch = useThunkDispatch();
    const websocket = useWebsocket();
    const uploadLimits = useUploadLimits();
    const maxFileSize = uploadLimits?.maxFileSize ?? -1;
    const { id } = useParams<{ id: string }>();
    const batchId = Number(id);

    const [batch, setBatch] = useState<ImportBatch | null>(null);
    const [items, setItems] = useState<ImportItem[]>([]);
    const [showFolderDialog, setShowFolderDialog] = useState(false);
    const [errorDetail, setErrorDetail] = useState<ImportItemError | null>(null);
    const [stateFilter, setStateFilter] = useState<ItemState | null>(null);
    const [scopeMap, setScopeMap] = useState<Record<number, string>>({});
    const [folderConfigured, setFolderConfigured] = useState<boolean | null>(null);

    const fileInputRef = useRef<HTMLInputElement>(null);
    const zipInputRef = useRef<HTMLInputElement>(null);

    const reload = useCallback(async () => {
        const [batchRes, itemsRes] = await Promise.all([
            Api.importBatches.importBatchGet(batchId),
            Api.importBatches.importBatchListItems(batchId),
        ]);
        setBatch(batchRes.data as ImportBatch);
        setItems(itemsRes.data ?? []);
    }, [batchId]);

    useEffect(() => {
        reload();
    }, [reload]);

    useEffect(() => {
        let cancelled = false;
        WebApi.getAllScopes().then((scopes: Array<{ id: number; name: string }> | undefined) => {
            if (cancelled) return;
            const map: Record<number, string> = {};
            for (const s of scopes ?? []) {
                map[s.id] = s.name;
            }
            setScopeMap(map);
        });
        return () => { cancelled = true; };
    }, []);

    useEffect(() => {
        let cancelled = false;
        Api.importBatches.importBatchListServerFolder(undefined)
            .then(({ data }) => { if (!cancelled) setFolderConfigured((data ?? []).length > 0); })
            .catch(() => { if (!cancelled) setFolderConfigured(false); });
        return () => { cancelled = true; };
    }, []);

    // Item events come once per processed item; coalesce them so a batch of small files
    // does not turn into a request per item.
    const refreshTimer = useRef<number | undefined>(undefined);
    useEffect(() => () => window.clearTimeout(refreshTimer.current), []);

    useEffect(() => {
        const listener = websocket.addListener((msg: { eventType?: string; ids?: number[] }) => {
            if (msg.eventType !== EventType.IMPORT_BATCH_STATE_CHANGE && msg.eventType !== EventType.IMPORT_BATCH_ITEM_CHANGE) return;
            if (!msg.ids?.includes(batchId)) return;
            if (refreshTimer.current != null) return;
            refreshTimer.current = window.setTimeout(() => {
                refreshTimer.current = undefined;
                reload();
            }, 1000);
        });
        return () => websocket.removeListener(listener);
    }, [websocket, batchId, reload]);

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

    // An oversized upload makes the server drop the connection, so it is rejected before sending.
    const fitsUploadLimit = (file: File) => {
        if (!exceedsUploadLimit(file, maxFileSize)) return true;
        dispatch(addToastrDanger(intl.formatMessage(messages.uploadTooLarge, {
            name: file.name,
            size: humanFileSize(file.size),
            limit: humanFileSize(maxFileSize),
        })));
        return false;
    };
    const withSizeHint = (button: ReactElement) => maxFileSize >= 0 ? (
        <Tooltip
            content={intl.formatMessage(messages.maxFileSize, { size: humanFileSize(maxFileSize) })}
            relationship="description"
            withArrow
        >
            {button}
        </Tooltip>
    ) : button;

    const onUploadFile = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        e.target.value = '';
        if (!file || !fitsUploadLimit(file)) return;
        await runAction(() => Api.importBatches.importBatchUploadItem(batchId, file));
    };
    const onUploadZip = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        e.target.value = '';
        if (!file || !fitsUploadLimit(file)) return;
        await runAction(() => Api.importBatches.importBatchUploadZip(batchId, file));
    };
    const onShowError = async (itemId: number) => {
        const { data } = await Api.importBatches.importBatchItemError(itemId);
        setErrorDetail(data);
    };

    if (!batch) {
        return <AdminLayout ribbon={<Ribbon />} centerPanel={<div className={styles.container}><Spinner /></div>} />;
    }

    const state = batch.state;
    const isEdx = batch.importType === BatchImportType.Edx2;
    const edx = isEdx ? (batch as ImportBatchEdx) : null;
    const csv = !isEdx ? (batch as ImportBatchDescCsv) : null;
    const canEditItems = CAN_EDIT_ITEMS.includes(state);
    // The retention sweep dropped the input files, so there is nothing left for a run to read.
    const filesPurged = batch.filesDeletedAt != null;
    const noItems = items.length === 0;
    const running = RUNNING.includes(state);
    const doneState = state === BatchState.TestInProgress ? ItemState.TestRunFinished : ItemState.Finished;
    const doneCount = items.filter(it => it.state === doneState).length;
    const errorCount = items.filter(it => it.state === ItemState.Error).length;
    const stateCounts = ITEM_STATE_ORDER
        .map(s => ({ state: s, count: items.filter(it => it.state === s).length }))
        .filter(c => c.count > 0);
    // A filter on a state that no item is in any more (e.g. all errors re-run) shows the whole list.
    const activeFilter = stateCounts.some(c => c.state === stateFilter) ? stateFilter : null;
    const visibleItems = activeFilter ? items.filter(it => it.state === activeFilter) : items;
    const yesNo = (value?: boolean) => intl.formatMessage(value ? globalMessages.yes : globalMessages.no);
    // Wrapped in a tooltip only when the folder is unavailable, an empty Tooltip still pops up.
    const folderButton = (
        <Button icon={<FolderRegular />} onClick={() => setShowFolderDialog(true)} disabled={folderConfigured !== true}>
            <FormattedMessage {...messages.fromFolder} />
        </Button>
    );

    const content = (
        <>
            <div className={mergeClasses(styles.container, styles.detail)}>
                <div className={styles.header}>
                    <Button appearance="subtle" icon={<ArrowLeftRegular />} onClick={() => history.push(URL_ADMIN_IMPORT)}>
                        <FormattedMessage {...messages.back} />
                    </Button>
                    <h2 style={{ margin: 0 }}>{batch.name}</h2>
                    <Tag size="small">{batch.importType}</Tag>
                    <Tag size="small" appearance="brand">{state}</Tag>
                    <div className={styles.spacer} />
                    <div className={styles.actions}>
                        {CAN_START.includes(state) && (
                            <Tooltip content={intl.formatMessage(filesPurged ? messages.filesPurged : messages.startHint)} relationship="description" withArrow>
                                <Button icon={<PlayRegular />} appearance="primary" onClick={onStart} disabled={filesPurged || noItems}>
                                    <FormattedMessage {...messages.start} />
                                </Button>
                            </Tooltip>
                        )}
                        {CAN_DRY_RUN.includes(state) && (
                            <Tooltip content={intl.formatMessage(filesPurged ? messages.filesPurged : messages.dryRunHint)} relationship="description" withArrow>
                                <Button icon={<BeakerRegular />} onClick={onDryRun} disabled={filesPurged || noItems}>
                                    <FormattedMessage {...messages.dryRun} />
                                </Button>
                            </Tooltip>
                        )}
                        {CAN_PAUSE.includes(state) && (
                            <Tooltip content={intl.formatMessage(messages.pauseHint)} relationship="description" withArrow>
                                <Button icon={<PauseRegular />} onClick={onPause}><FormattedMessage {...messages.pause} /></Button>
                            </Tooltip>
                        )}
                        {CAN_CANCEL.includes(state) && (
                            <Tooltip content={intl.formatMessage(messages.cancelHint)} relationship="description" withArrow>
                                <Button icon={<StopRegular />} onClick={onCancel} disabled={noItems}><FormattedMessage {...messages.cancel} /></Button>
                            </Tooltip>
                        )}
                        {canDelete(state) && (
                            <Tooltip content={intl.formatMessage(messages.removeHint)} relationship="description" withArrow>
                                <Button icon={<DeleteRegular />} onClick={onDelete}><FormattedMessage {...messages.remove} /></Button>
                            </Tooltip>
                        )}
                    </div>
                </div>

                <Card className={styles.batchCard}>
                    <div className={styles.batchScroll}>
                        <h3 style={{ margin: 0 }}><FormattedMessage {...messages.settings} /></h3>
                        <div className={styles.settings}>
                            {edx && (<>
                                <Setting label={batchSettingsMessages.fundImportStrategy}>{edx.fundImportStrategy}</Setting>
                                <Setting label={batchSettingsMessages.fundPairKey}>{edx.fundPairKey ?? '—'}</Setting>
                                <Setting label={batchSettingsMessages.scope}>{edx.scopeId != null ? (scopeMap[edx.scopeId] ?? edx.scopeId) : '—'}</Setting>
                            </>)}
                            {csv && (<>
                                <Setting label={batchSettingsMessages.separator}>{csv.separator ?? '—'}</Setting>
                                <Setting label={batchSettingsMessages.encoding}>{csv.encoding ?? '—'}</Setting>
                            </>)}
                            {filesPurged && (
                                <Setting label={messages.filesDeletedAt}>
                                    <FormattedDate value={batch.filesDeletedAt} />
                                    {' '}
                                    <FormattedTime value={batch.filesDeletedAt} />
                                </Setting>
                            )}
                            {edx && <Setting label={batchSettingsMessages.ignoreRootNodes} startsRow>{yesNo(edx.ignoreRootNodes)}</Setting>}
                            <Setting label={batchSettingsMessages.skipError} startsRow={!edx}>{yesNo(batch.skipError)}</Setting>
                            <Setting label={batchSettingsMessages.keepFiles}>{yesNo(batch.keepFiles)}</Setting>
                        </div>

                        <div className={styles.header}>
                            <h3 style={{ margin: 0 }}><FormattedMessage {...messages.items} /></h3>
                            {items.length > 0 && (
                                <div className={styles.stateSummary}>
                                    <Badge appearance="outline" color="informative">{items.length}</Badge>
                                    {stateCounts.map(c => (
                                        <ToggleButton
                                            key={c.state}
                                            size="small"
                                            shape="circular"
                                            checked={activeFilter === c.state}
                                            onClick={() => setStateFilter(activeFilter === c.state ? null : c.state)}
                                        >
                                            {c.state}
                                            {' '}
                                            <Badge size="small" appearance="tint" color={ITEM_STATE_COLOR[c.state] ?? 'subtle'}>{c.count}</Badge>
                                        </ToggleButton>
                                    ))}
                                </div>
                            )}
                            <div className={styles.spacer} />
                            {canEditItems && (
                                <div className={styles.actions}>
                                    {withSizeHint(
                                        <Button icon={<ArrowUploadRegular />} onClick={() => fileInputRef.current?.click()}>
                                            <FormattedMessage {...messages.upload} />
                                        </Button>
                                    )}
                                    {withSizeHint(
                                        <Button icon={<ArchiveRegular />} onClick={() => zipInputRef.current?.click()}>
                                            <FormattedMessage {...messages.uploadZip} />
                                        </Button>
                                    )}
                                    {folderConfigured === false ? (
                                        <Tooltip content={intl.formatMessage(messages.folderNotConfigured)} relationship="description" withArrow>
                                            {folderButton}
                                        </Tooltip>
                                    ) : folderButton}
                                </div>
                            )}
                            <input ref={fileInputRef} type="file" className={styles.hidden} onChange={onUploadFile} />
                            <input ref={zipInputRef} type="file" accept=".zip,application/zip" className={styles.hidden} onChange={onUploadZip} />
                        </div>
                        {running && items.length > 0 && (
                            <div className={styles.progress}>
                                <ProgressBar value={(doneCount + errorCount) / items.length} />
                                <span className={styles.label}>
                                    <FormattedMessage {...messages.progress} values={{ done: doneCount, total: items.length }} />
                                    {errorCount > 0 && (<>
                                        {' · '}
                                        <FormattedMessage {...messages.progressErrors} values={{ errors: errorCount }} />
                                    </>)}
                                </span>
                            </div>
                        )}
                        {items.length === 0 ? (
                            <div className={styles.empty}><FormattedMessage {...messages.empty} /></div>
                        ) : (
                            <Table>
                                <TableHeader className={styles.stickyHeader}>
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
                                    {visibleItems.map(it => (
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
                    </div>
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
