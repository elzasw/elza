import {
    Button,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    Spinner,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { useEffect, useState } from 'react';
import { defineMessages, useIntl } from 'react-intl';
import { ExtHistory, ExtParticipantRole, ExtRevision } from 'elza-api';
import { Api } from 'api/api';
import { ChevronLeftRegular, ChevronRightRegular } from '@fluentui/react-icons';

const DEFAULT_PAGE_SIZE = 25;

const useStyles = makeStyles({
    surface: {
        width: '750px',
        maxWidth: '95vw',
    },
    contentFixed: {
        height: '500px',
        overflowY: 'auto',
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
    },
    contentFlex: {
        maxHeight: '500px',
        overflowY: 'auto',
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
    },
    incomplete: {
        color: tokens.colorStatusWarningForeground1,
        fontSize: tokens.fontSizeBase200,
    },
    revision: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXXS,
        paddingBottom: tokens.spacingVerticalS,
        borderBottom: `1px solid ${tokens.colorNeutralStroke2}`,
        ':last-child': { borderBottom: 'none' },
    },
    line1: {
        fontWeight: tokens.fontWeightSemibold,
    },
    line2: {
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorNeutralForeground2,
    },
});

const messages = defineMessages({
    title:      { id: 'ap.binding.history.title',      defaultMessage: 'Historie revizí v externím systému' },
    revision:   { id: 'ap.binding.history.revision',   defaultMessage: 'Revize {id}, {date}, odeslal: {sender}' },
    approval:   { id: 'ap.binding.history.approval',   defaultMessage: 'Schválil: {names}' },
    author:     { id: 'ap.binding.history.author',     defaultMessage: 'Autor: {names}' },
    incomplete: { id: 'ap.binding.history.incomplete', defaultMessage: 'Historie je neúplná – pro úplnou historii nahlédněte do zdrojového systému.' },
    close:      { id: 'ap.binding.history.close',      defaultMessage: 'Zavřít' },
    noHistory:  { id: 'ap.binding.history.empty',      defaultMessage: 'Žádná historie není k dispozici.' },
    unknown:    { id: 'ap.binding.history.unknown',    defaultMessage: 'neznámý' },
});

interface Props {
    bindingId: number;
    open: boolean;
    onClose: () => void;
    testHistory?: ExtHistory;
}

const formatDateTime = (value?: string) => value ? new Date(value).toLocaleString() : '—';

export function BindingHistoryDialog({ bindingId, open, onClose, testHistory }: Props) {
    const classes = useStyles();
    const { formatMessage } = useIntl();
    const [history, setHistory] = useState<ExtHistory | null>(testHistory ?? null);
    const [loading, setLoading] = useState(false);
    const [from, setFrom] = useState(0);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    useEffect(() => {
        if (!open || testHistory) { return; }
        setLoading(true);
        Api.accesspointInternal.accessPointBindingGetBindingHistory(bindingId, from, pageSize)
            .then(({ data }) => setHistory(data))
            .finally(() => setLoading(false));
    }, [open, bindingId, from, pageSize]);

    const renderRevision = (rev: ExtRevision) => {
        const approvals = rev.participants.filter((p) => p.role === ExtParticipantRole.Approval);
        const authors = rev.participants.filter((p) => p.role === ExtParticipantRole.Author);

        const approvalNames = approvals.map((p) => `${p.name}, ${formatDateTime(p.lastChange)}`).join('; ');
        const authorNames = authors.map((p) => `${p.name}, ${formatDateTime(p.lastChange)}`).join('; ');

        return (
            <div key={rev.bindingStateId} className={classes.revision}>
                <div className={classes.line1}>
                    {formatMessage(messages.revision, {
                        id: rev.extRevision ?? '—',
                        date: formatDateTime(rev.extCreatedAt ?? rev.createChangeAt),
                        sender: rev.sender ?? formatMessage(messages.unknown),
                    })}
                </div>
                {approvals.length > 0 && (
                    <div className={classes.line2}>
                        {formatMessage(messages.approval, { names: approvalNames })}
                    </div>
                )}
                {authors.length > 0 && (
                    <div className={classes.line2}>
                        {formatMessage(messages.author, { names: authorNames })}
                    </div>
                )}
            </div>
        );
    };

    return (
        <Dialog open={open} onOpenChange={(_, data) => { if (!data.open) { onClose(); } }}>
            <DialogSurface className={classes.surface}>
                <DialogBody>
                    <DialogTitle>{formatMessage(messages.title)}</DialogTitle>
                    <DialogContent className={(history?.totalCount ?? 0) > pageSize ? classes.contentFixed : classes.contentFlex}>
                        {loading && <Spinner size="small" />}
                        {!loading && history && (
                            <>
                                {history.incomplete && (
                                    <div className={classes.incomplete}>{formatMessage(messages.incomplete)}</div>
                                )}
                                {history.revisions.length === 0
                                    ? <span>{formatMessage(messages.noHistory)}</span>
                                    : history.revisions.map(renderRevision)
                                }
                            </>
                        )}
                    </DialogContent>
                    {history && history.totalCount > pageSize && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px', padding: '4px 0' }}>
                            <Button
                                icon={<ChevronLeftRegular />}
                                appearance="subtle"
                                disabled={from === 0}
                                onClick={() => setFrom(Math.max(0, from - pageSize))}
                            />
                            <span>{Math.floor(from / pageSize) + 1}/{Math.ceil(history.totalCount / pageSize)}</span>
                            <Button
                                icon={<ChevronRightRegular />}
                                appearance="subtle"
                                disabled={from + pageSize >= history.totalCount}
                                onClick={() => setFrom(from + pageSize)}
                            />
                        </div>
                    )}
                    <DialogActions>
                        <Button onClick={onClose}>{formatMessage(messages.close)}</Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}
