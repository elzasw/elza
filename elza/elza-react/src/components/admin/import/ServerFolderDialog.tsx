import {
    Button,
    Checkbox,
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
    Tooltip,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    ArrowUpRegular,
    DocumentRegular,
    FolderRegular,
    HomeRegular,
} from '@fluentui/react-icons';
import { Api } from 'api';
import { ImportServerFolderEntry } from 'elza-api';
import React, { useCallback, useEffect, useState } from 'react';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';

const messages = defineMessages({
    title: { id: 'admin.import.serverFolder.title', defaultMessage: 'Import ze složky na serveru' },
    root: { id: 'admin.import.serverFolder.root', defaultMessage: 'Kořen' },
    up: { id: 'admin.import.serverFolder.up', defaultMessage: 'O úroveň výš' },
    colName: { id: 'admin.import.serverFolder.col.name', defaultMessage: 'Název' },
    colSize: { id: 'admin.import.serverFolder.col.size', defaultMessage: 'Velikost' },
    empty: { id: 'admin.import.serverFolder.empty', defaultMessage: 'Složka je prázdná' },
    importSelected: { id: 'admin.import.serverFolder.importSelected', defaultMessage: 'Importovat vybrané' },
    importAll: { id: 'admin.import.serverFolder.importAll', defaultMessage: 'Importovat celou složku' },
    close: { id: 'admin.import.serverFolder.close', defaultMessage: 'Zavřít' },
    loadError: { id: 'admin.import.serverFolder.loadError', defaultMessage: 'Nelze načíst obsah složky: {msg}' },
});

const useStyles = makeStyles({
    breadcrumb: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalXS,
        flexWrap: 'wrap',
        marginBottom: tokens.spacingVerticalS,
    },
    crumb: {
        cursor: 'pointer',
        color: tokens.colorBrandForegroundLink,
    },
    separator: { color: tokens.colorNeutralForeground3 },
    row: { cursor: 'default' },
    dirRow: { cursor: 'pointer' },
    error: { color: tokens.colorPaletteRedForeground1, padding: tokens.spacingVerticalS },
    tableWrap: { maxHeight: '400px', overflow: 'auto' },
    nameCell: { display: 'flex', alignItems: 'center', gap: tokens.spacingHorizontalS },
});

function formatSize(bytes: number | undefined): string {
    if (bytes == null) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
    return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`;
}

interface Props {
    batchId: number;
    open: boolean;
    onClose: () => void;
    onImported: () => void;
}

export function ServerFolderDialog({ batchId, open, onClose, onImported }: Props) {
    const styles = useStyles();
    const intl = useIntl();

    const [path, setPath] = useState('');
    const [entries, setEntries] = useState<ImportServerFolderEntry[]>([]);
    const [selected, setSelected] = useState<Set<string>>(new Set());
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    const load = useCallback(async (nextPath: string) => {
        setLoading(true);
        setError(null);
        try {
            const { data } = await Api.importBatches.importBatchListServerFolder(nextPath || undefined);
            setEntries(data ?? []);
            setSelected(new Set());
            setPath(nextPath);
        } catch (e: unknown) {
            const msg = e instanceof Error ? e.message : String(e);
            setError(intl.formatMessage(messages.loadError, { msg }));
            setEntries([]);
        } finally {
            setLoading(false);
        }
    }, [intl]);

    useEffect(() => {
        if (open) load('');
    }, [open, load]);

    const segments = path.split('/').filter(Boolean);

    const goTo = (i: number) => {
        const next = segments.slice(0, i + 1).join('/');
        load(next);
    };
    const goUp = () => {
        if (segments.length === 0) return;
        load(segments.slice(0, -1).join('/'));
    };
    const enterDir = (name: string) => {
        load(path ? `${path}/${name}` : name);
    };

    const toggle = (name: string) => {
        const next = new Set(selected);
        if (next.has(name)) next.delete(name);
        else next.add(name);
        setSelected(next);
    };

    const fileEntries = entries.filter(e => !e.directory);
    const allFilesSelected = fileEntries.length > 0 && fileEntries.every(e => selected.has(e.name));

    const toggleAll = () => {
        if (allFilesSelected) setSelected(new Set());
        else setSelected(new Set(fileEntries.map(e => e.name)));
    };

    const submit = async (files: string[] | undefined) => {
        setSubmitting(true);
        try {
            await Api.importBatches.importBatchImportFolder(batchId, {
                path,
                files,
            });
            onImported();
            onClose();
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={(_, data) => !data.open && onClose()}>
            <DialogSurface>
                <DialogBody>
                    <DialogTitle><FormattedMessage {...messages.title} /></DialogTitle>
                    <DialogContent>
                        <div className={styles.breadcrumb}>
                            <Tooltip content={intl.formatMessage(messages.root)} relationship="label">
                                <Button
                                    size="small"
                                    appearance="subtle"
                                    icon={<HomeRegular />}
                                    onClick={() => load('')}
                                />
                            </Tooltip>
                            {segments.map((seg, i) => (
                                <span key={i}>
                                    <span className={styles.separator}>/</span>
                                    <span className={styles.crumb} onClick={() => goTo(i)}>{seg}</span>
                                </span>
                            ))}
                            <span style={{ flex: 1 }} />
                            {segments.length > 0 && (
                                <Button size="small" icon={<ArrowUpRegular />} onClick={goUp}>
                                    <FormattedMessage {...messages.up} />
                                </Button>
                            )}
                        </div>

                        {loading && <Spinner size="small" />}
                        {error && <div className={styles.error}>{error}</div>}

                        {!loading && !error && entries.length === 0 && (
                            <div className={styles.error}><FormattedMessage {...messages.empty} /></div>
                        )}

                        {!loading && !error && entries.length > 0 && (
                            <div className={styles.tableWrap}>
                                <Table size="small">
                                    <TableHeader>
                                        <TableRow>
                                            <TableHeaderCell style={{ width: '32px' }}>
                                                {fileEntries.length > 0 && (
                                                    <Checkbox
                                                        checked={allFilesSelected}
                                                        onChange={toggleAll}
                                                    />
                                                )}
                                            </TableHeaderCell>
                                            <TableHeaderCell><FormattedMessage {...messages.colName} /></TableHeaderCell>
                                            <TableHeaderCell><FormattedMessage {...messages.colSize} /></TableHeaderCell>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {entries.map(e => (
                                            <TableRow
                                                key={e.name}
                                                className={e.directory ? styles.dirRow : styles.row}
                                                onClick={e.directory ? () => enterDir(e.name) : undefined}
                                            >
                                                <TableCell onClick={(ev: React.MouseEvent) => ev.stopPropagation()}>
                                                    {!e.directory && (
                                                        <Checkbox
                                                            checked={selected.has(e.name)}
                                                            onChange={() => toggle(e.name)}
                                                        />
                                                    )}
                                                </TableCell>
                                                <TableCell>
                                                    <span className={styles.nameCell}>
                                                        {e.directory ? <FolderRegular /> : <DocumentRegular />}
                                                        {e.name}
                                                    </span>
                                                </TableCell>
                                                <TableCell>{formatSize(e.size)}</TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </div>
                        )}
                    </DialogContent>
                    <DialogActions>
                        <Button
                            appearance="primary"
                            disabled={submitting || loading || selected.size === 0}
                            onClick={() => submit(Array.from(selected))}
                        >
                            <FormattedMessage {...messages.importSelected} /> ({selected.size})
                        </Button>
                        <Button
                            disabled={submitting || loading || fileEntries.length === 0}
                            onClick={() => submit(undefined)}
                        >
                            <FormattedMessage {...messages.importAll} />
                        </Button>
                        <Button appearance="secondary" onClick={onClose}>
                            <FormattedMessage {...messages.close} />
                        </Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}

export type ServerFolderDialogProps = Props;
