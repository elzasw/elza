import {
    Button,
    Checkbox,
    Dropdown,
    Input,
    MessageBar,
    MessageBarBody,
    Option,
    Spinner,
    Tab,
    TabList,
    Table,
    TableBody,
    TableCell,
    TableCellLayout,
    TableHeader,
    TableHeaderCell,
    TableRow,
    TableSelectionCell,
    Tooltip,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    ArrowClockwiseRegular,
    ArrowRepeatAllRegular,
    DismissCircleRegular,
    PlayRegular,
} from '@fluentui/react-icons';
import { Api } from 'api';
import { DaQueueActionResult, DaQueueDirection, DaQueueItemPage, DaQueueItemVO, QueueItemState } from 'elza-api';
import { MouseEvent, ReactNode, useEffect, useMemo, useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { useDispatch } from 'react-redux';
import { queueStateMessages } from '../../aip/messages';
import { addToastrSuccess, addToastrWarning } from '../../shared/toastr/ToastrActions';
import { usePolledData } from '../background-processes/usePolledData';
import { DaQueueDetail } from './DaQueueDetail';
import { formatDateTime, formatNextAttempt } from './format';
import { aipTypeMessages, directionMessages, messages } from './messages';

/** How often the shown page is reloaded. */
const REFRESH_MS = 5000;

/** Typing into a text filter reloads the list only after this pause. */
export const TYPING_DELAY_MS = 400;

export const PAGE_SIZE = 50;

/** Requests waiting for the queue, delivered to the DA or not - "Zkusit znovu teď". */
const WAITING: QueueItemState[] = [
    QueueItemState.ImportNew, QueueItemState.Update, QueueItemState.DownloadRequested,
    QueueItemState.ExportNew, QueueItemState.ExportSent,
];
/** Requests the DA has not received - the only ones that can be cancelled. */
const UNDELIVERED: QueueItemState[] = [QueueItemState.ImportNew, QueueItemState.Update, QueueItemState.ExportNew];
const FAILED: QueueItemState[] = [QueueItemState.ImportError, QueueItemState.ExportError];

const STATES: QueueItemState[] = [
    QueueItemState.ImportNew, QueueItemState.Update, QueueItemState.DownloadRequested, QueueItemState.ImportOk,
    QueueItemState.ImportError, QueueItemState.ExportNew, QueueItemState.ExportSent, QueueItemState.ExportOk,
    QueueItemState.ExportError,
];

export const canRetryNow = (item: DaQueueItemVO) => item.active && WAITING.includes(item.state);
export const canWithdraw = (item: DaQueueItemVO) => item.active && UNDELIVERED.includes(item.state);
export const canRepeat = (item: DaQueueItemVO) => item.active && FAILED.includes(item.state);

const useStyles = makeStyles({
    page: {
        display: 'flex',
        height: '100%',
        minHeight: 0,
    },
    main: {
        flexGrow: 1,
        minWidth: 0,
        display: 'flex',
        flexDirection: 'column',
    },
    bar: {
        flexShrink: 0,
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalM,
        rowGap: tokens.spacingVerticalS,
        padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalL}`,
    },
    spacer: {
        flexGrow: 1,
    },
    errorBar: {
        flexShrink: 0,
        margin: `0 ${tokens.spacingHorizontalL} ${tokens.spacingVerticalS}`,
    },
    // The scrollbar belongs to the table alone, so the filters and the pager stay in place.
    scroll: {
        flexGrow: 1,
        minHeight: 0,
        overflow: 'auto',
        padding: `0 ${tokens.spacingHorizontalL}`,
    },
    header: {
        position: 'sticky',
        top: 0,
        zIndex: 1,
        backgroundColor: tokens.colorNeutralBackground1,
    },
    message: {
        maxWidth: '360px',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
    },
    inactive: {
        color: tokens.colorNeutralForeground3,
    },
    empty: {
        padding: tokens.spacingHorizontalL,
        color: tokens.colorNeutralForeground3,
    },
});

export interface DaQueueFilter {
    all: boolean;
    states: QueueItemState[];
    direction?: DaQueueDirection;
    aipCode: string;
    batchId: string;
    failedOnly: boolean;
}

export const INITIAL_FILTER: DaQueueFilter = {
    all: false, states: [], aipCode: '', batchId: '', failedOnly: false,
};

function load(repositoryId: number, filter: DaQueueFilter, from: number): Promise<DaQueueItemPage> {
    return Api.externalSystems.externalSystemDaQueue(
        repositoryId,
        filter.all,
        filter.states.length > 0 ? filter.states : undefined,
        filter.direction,
        filter.aipCode.trim() || undefined,
        filter.batchId.trim() || undefined,
        filter.failedOnly,
        from,
        PAGE_SIZE,
    ).then(({ data }) => data);
}

interface Props {
    repositoryId: number;
    /** Selector of the repository, shown at the start of the bar. */
    repositorySelector?: ReactNode;
}

/**
 * Requests of the queue of one digital archive, newest first. Opens with the waiting ones; the
 * selected rows can be retried now, cancelled (only before the DA has them) or repeated after a
 * failure.
 */
export function DaQueueList({ repositoryId, repositorySelector }: Props) {
    const styles = useStyles();
    const intl = useIntl();
    const dispatch = useDispatch();

    const [filter, setFilter] = useState<DaQueueFilter>(INITIAL_FILTER);
    const [from, setFrom] = useState(0);
    const [selected, setSelected] = useState<Set<number>>(new Set());
    const [detailId, setDetailId] = useState<number>();
    // what is typed into the text filters; it reaches the filter after a pause in typing
    const [typed, setTyped] = useState({ aipCode: '', batchId: '' });

    const { data, failed, refresh } = usePolledData(() => load(repositoryId, filter, from), REFRESH_MS);
    const items = useMemo(() => data?.items ?? [], [data]);
    const total = data?.totalCount ?? 0;

    const change = (patch: Partial<DaQueueFilter>) => {
        setFilter(current => ({ ...current, ...patch }));
        setFrom(0);
        setSelected(new Set());
        // the new filter must not wait for the next interval
        setTimeout(refresh);
    };

    useEffect(() => {
        if (typed.aipCode === filter.aipCode && typed.batchId === filter.batchId) {
            return undefined;
        }
        const timer = setTimeout(() => change({ aipCode: typed.aipCode, batchId: typed.batchId }), TYPING_DELAY_MS);
        return () => clearTimeout(timer);
    }, [typed]);

    const selectedItems = items.filter(item => selected.has(item.id));
    const toggle = (id: number) => setSelected(current => {
        const next = new Set(current);
        if (next.has(id)) {
            next.delete(id);
        } else {
            next.add(id);
        }
        return next;
    });
    const allSelected = items.length > 0 && items.every(item => selected.has(item.id));
    const someSelected = items.some(item => selected.has(item.id));
    const toggleAll = () => setSelected(allSelected ? new Set() : new Set(items.map(item => item.id)));

    const run = async (action: (ids: number[]) => Promise<{ data: DaQueueActionResult }>) => {
        const { data: result } = await action(selectedItems.map(item => item.id));
        dispatch(result.done > 0
            ? addToastrSuccess(intl.formatMessage(messages.actionDone, { done: result.done, skipped: result.skipped }))
            : addToastrWarning(intl.formatMessage(messages.actionNothing)));
        setSelected(new Set());
        refresh();
    };

    const page = (target: number) => {
        setFrom(target);
        setSelected(new Set());
        setTimeout(refresh);
    };

    const detail = items.find(item => item.id === detailId);

    return (
        <div className={styles.page}>
            <div className={styles.main}>
                <div className={styles.bar}>
                    {repositorySelector}
                    <TabList
                        selectedValue={filter.all ? 'all' : 'waiting'}
                        onTabSelect={(_, { value }) => change({ all: value === 'all' })}
                    >
                        <Tab value="waiting"><FormattedMessage {...messages.presetWaiting} /></Tab>
                        <Tab value="all"><FormattedMessage {...messages.presetAll} /></Tab>
                    </TabList>
                    <Dropdown
                        multiselect
                        aria-label={intl.formatMessage(messages.filterState)}
                        placeholder={intl.formatMessage(messages.filterState)}
                        selectedOptions={filter.states}
                        value={filter.states.map(state => intl.formatMessage(queueStateMessages[state])).join(', ')}
                        onOptionSelect={(_, { selectedOptions }) => change({ states: selectedOptions as QueueItemState[] })}
                    >
                        {STATES.map(state => (
                            <Option key={state} value={state}>{intl.formatMessage(queueStateMessages[state])}</Option>
                        ))}
                    </Dropdown>
                    <Dropdown
                        aria-label={intl.formatMessage(messages.filterDirection)}
                        selectedOptions={[filter.direction ?? '']}
                        value={filter.direction
                            ? intl.formatMessage(directionMessages[filter.direction])
                            : intl.formatMessage(messages.filterAnyDirection)}
                        onOptionSelect={(_, { optionValue }) =>
                            change({ direction: (optionValue || undefined) as DaQueueDirection | undefined })}
                    >
                        <Option value="">{intl.formatMessage(messages.filterAnyDirection)}</Option>
                        <Option value={DaQueueDirection.Download}>{intl.formatMessage(directionMessages[DaQueueDirection.Download])}</Option>
                        <Option value={DaQueueDirection.Export}>{intl.formatMessage(directionMessages[DaQueueDirection.Export])}</Option>
                    </Dropdown>
                    <Input
                        aria-label={intl.formatMessage(messages.filterAipCode)}
                        placeholder={intl.formatMessage(messages.filterAipCode)}
                        value={typed.aipCode}
                        onChange={(_, { value }) => setTyped(current => ({ ...current, aipCode: value }))}
                    />
                    <Input
                        aria-label={intl.formatMessage(messages.filterBatch)}
                        placeholder={intl.formatMessage(messages.filterBatch)}
                        value={typed.batchId}
                        onChange={(_, { value }) => setTyped(current => ({ ...current, batchId: value }))}
                    />
                    <Checkbox
                        label={intl.formatMessage(messages.filterFailed)}
                        checked={filter.failedOnly}
                        onChange={(_, { checked }) => change({ failedOnly: checked === true })}
                    />
                    <div className={styles.spacer} />
                    <Button appearance="subtle" icon={<ArrowClockwiseRegular />} onClick={refresh}>
                        <FormattedMessage {...messages.refresh} />
                    </Button>
                </div>
                <div className={styles.bar}>
                    <Tooltip content={intl.formatMessage(messages.retryNowTitle)} relationship="description">
                        <Button icon={<PlayRegular />} disabled={!selectedItems.some(canRetryNow)}
                                onClick={() => run(ids => Api.externalSystems.externalSystemDaQueueRetryNow(repositoryId, ids))}>
                            <FormattedMessage {...messages.retryNow} />
                        </Button>
                    </Tooltip>
                    <Tooltip content={intl.formatMessage(messages.withdrawTitle)} relationship="description">
                        <Button icon={<DismissCircleRegular />} disabled={!selectedItems.some(canWithdraw)}
                                onClick={() => run(ids => Api.externalSystems.externalSystemDaQueueWithdraw(repositoryId, ids))}>
                            <FormattedMessage {...messages.withdraw} />
                        </Button>
                    </Tooltip>
                    <Tooltip content={intl.formatMessage(messages.repeatTitle)} relationship="description">
                        <Button icon={<ArrowRepeatAllRegular />} disabled={!selectedItems.some(canRepeat)}
                                onClick={() => run(ids => Api.externalSystems.externalSystemDaQueueRepeat(repositoryId, ids))}>
                            <FormattedMessage {...messages.repeat} />
                        </Button>
                    </Tooltip>
                    {selectedItems.length > 0 && (
                        <FormattedMessage {...messages.selected} values={{ count: selectedItems.length }} />
                    )}
                    <div className={styles.spacer} />
                    {total > 0 && (
                        <FormattedMessage {...messages.pager}
                                          values={{ from: from + 1, to: Math.min(from + PAGE_SIZE, total), total }} />
                    )}
                    <Button appearance="subtle" disabled={from === 0} onClick={() => page(Math.max(from - PAGE_SIZE, 0))}>
                        <FormattedMessage {...messages.previous} />
                    </Button>
                    <Button appearance="subtle" disabled={from + PAGE_SIZE >= total} onClick={() => page(from + PAGE_SIZE)}>
                        <FormattedMessage {...messages.next} />
                    </Button>
                </div>
                {failed && (
                    <MessageBar className={styles.errorBar} intent="error">
                        <MessageBarBody><FormattedMessage {...messages.loadFailed} /></MessageBarBody>
                    </MessageBar>
                )}
                <div className={styles.scroll}>
                    {data === undefined ? (
                        <Spinner />
                    ) : items.length === 0 ? (
                        <div className={styles.empty}><FormattedMessage {...messages.empty} /></div>
                    ) : (
                        <Table size="small" aria-label={intl.formatMessage(messages.title)}>
                            <TableHeader className={styles.header}>
                                <TableRow>
                                    <TableSelectionCell
                                        checked={allSelected ? true : someSelected ? 'mixed' : false}
                                        onClick={toggleAll}
                                    />
                                    <TableHeaderCell><FormattedMessage {...messages.colId} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colAip} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colRequest} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colState} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colBatch} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colAttempts} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colNextAttempt} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colChanged} /></TableHeaderCell>
                                    <TableHeaderCell><FormattedMessage {...messages.colMessage} /></TableHeaderCell>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {items.map(item => (
                                    <TableRow
                                        key={item.id}
                                        className={item.active ? undefined : styles.inactive}
                                        aria-selected={selected.has(item.id)}
                                        appearance={item.id === detailId ? 'brand' : 'none'}
                                        onClick={() => setDetailId(item.id)}
                                    >
                                        <TableSelectionCell
                                            checked={selected.has(item.id)}
                                            onClick={(e: MouseEvent) => {
                                                e.stopPropagation();
                                                toggle(item.id);
                                            }}
                                        />
                                        <TableCell>{item.id}</TableCell>
                                        <TableCell>{item.aipCode}</TableCell>
                                        <TableCell>
                                            <FormattedMessage {...directionMessages[item.direction]} />
                                            {item.aipType && <> · <FormattedMessage {...aipTypeMessages[item.aipType]} /></>}
                                        </TableCell>
                                        <TableCell>
                                            <FormattedMessage {...queueStateMessages[item.state]} />
                                            {!item.active && (
                                                <span title={intl.formatMessage(messages.inactiveTitle)}>
                                                    {' · '}<FormattedMessage {...messages.inactive} />
                                                </span>
                                            )}
                                        </TableCell>
                                        <TableCell>
                                            {item.batchId && (
                                                <Button appearance="transparent" size="small"
                                                        title={intl.formatMessage(messages.filterByBatch)}
                                                        onClick={(e: MouseEvent) => {
                                                            e.stopPropagation();
                                                            const batchId = item.batchId ?? '';
                                                            setTyped(current => ({ ...current, batchId }));
                                                            change({ batchId, all: true });
                                                        }}>
                                                    {item.batchId}
                                                </Button>
                                            )}
                                        </TableCell>
                                        <TableCell>{item.attemptCount > 0 ? item.attemptCount : ''}</TableCell>
                                        <TableCell>
                                            {item.active && WAITING.includes(item.state)
                                                ? formatNextAttempt(intl, item.nextAttemptAt) : ''}
                                        </TableCell>
                                        <TableCell>{formatDateTime(intl, item.stateDate)}</TableCell>
                                        <TableCell>
                                            <TableCellLayout truncate className={styles.message}>
                                                {item.stateMessage}
                                            </TableCellLayout>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    )}
                </div>
            </div>
            <DaQueueDetail item={detail} onClose={() => setDetailId(undefined)} />
        </div>
    );
}
