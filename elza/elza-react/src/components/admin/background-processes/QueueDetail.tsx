import {Badge, Spinner, makeStyles, tokens} from '@fluentui/react-components';
import {WebApi} from 'actions/index.jsx';
import {FormattedMessage, useIntl} from 'react-intl';
import {localUTCToDateTime} from 'shared/utils/commons';
import {messages} from './messages';
import {QueueFundStats, QueueInfo, QueueType} from './types';
import {usePolledData} from './usePolledData';

/** How often the queue contents are reloaded while the panel is open. */
const DETAIL_REFRESH_MS = 10000;

const useStyles = makeStyles({
    root: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalL,
        paddingBottom: tokens.spacingVerticalM,
    },
    section: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalXS,
    },
    sectionTitle: {
        fontWeight: tokens.fontWeightSemibold,
        color: tokens.colorNeutralForeground2,
    },
    empty: {
        color: tokens.colorNeutralForeground3,
    },
    threadRow: {
        display: 'flex',
        flexWrap: 'wrap',
        columnGap: tokens.spacingHorizontalXXL,
        rowGap: tokens.spacingVerticalXXS,
        padding: `${tokens.spacingVerticalXS} 0`,
        borderBottom: `${tokens.strokeWidthThin} solid ${tokens.colorNeutralStroke2}`,
        fontVariantNumeric: 'tabular-nums',
    },
    fundRow: {
        display: 'flex',
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalM,
        padding: `${tokens.spacingVerticalXS} 0`,
        borderBottom: `${tokens.strokeWidthThin} solid ${tokens.colorNeutralStroke2}`,
    },
    fundName: {
        flexGrow: 1,
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
    },
});

function fetchQueueFunds(type: QueueType): Promise<QueueFundStats[]> {
    return WebApi.getAsyncRequestDetail(type);
}

function RunningThreads({queue}: {queue: QueueInfo}) {
    const styles = useStyles();
    const intl = useIntl();

    if (queue.currentThreads.length === 0) {
        return (
            <div className={styles.empty}>
                <FormattedMessage {...messages.noRunningThread} />
            </div>
        );
    }

    return (
        <>
            {queue.currentThreads.map((thread, index) => {
                const beginTime = localUTCToDateTime(thread.beginTime);
                return (
                    <div className={styles.threadRow} key={thread.requestId ?? index}>
                        <span>
                            <FormattedMessage
                                {...messages.beginTime}
                                values={{
                                    0: beginTime
                                        ? intl.formatDate(beginTime, {dateStyle: 'short', timeStyle: 'short'})
                                        : thread.beginTime,
                                }}
                            />
                        </span>
                        <span>
                            <FormattedMessage {...messages.requestId} values={{0: thread.requestId}} />
                        </span>
                        <span>
                            <FormattedMessage {...messages.currentId} values={{0: thread.currentId}} />
                        </span>
                    </div>
                );
            })}
        </>
    );
}

function QueueContent({type}: {type: QueueType}) {
    const styles = useStyles();
    const {data} = usePolledData(() => fetchQueueFunds(type), DETAIL_REFRESH_MS);

    if (data === undefined) {
        return <Spinner size="tiny" />;
    }

    const funds = data.filter(row => row.fund != null);
    if (funds.length === 0) {
        return (
            <div className={styles.empty}>
                <FormattedMessage {...messages.queueEmpty} />
            </div>
        );
    }

    return (
        <>
            {funds.map(row => (
                <div className={styles.fundRow} key={row.fundVersionId ?? row.fund?.id}>
                    <span className={styles.fundName} title={`${row.fund?.name} (id: ${row.fund?.id})`}>
                        {row.fund?.name}
                    </span>
                    <Badge appearance="tint" color="informative">
                        {row.requestCount}
                    </Badge>
                </div>
            ))}
        </>
    );
}

/**
 * Contents of an expanded queue.
 *
 * The queue contents are loaded here rather than in the list, so only the queues
 * the administrator actually opened are polled. The panel is unmounted while
 * collapsed, which stops that polling.
 */
export function QueueDetail({queue}: {queue: QueueInfo}) {
    const styles = useStyles();

    return (
        <div className={styles.root}>
            <div className={styles.section}>
                <span className={styles.sectionTitle}>
                    <FormattedMessage {...messages.runningThreads} />
                </span>
                <RunningThreads queue={queue} />
            </div>
            <div className={styles.section}>
                <span className={styles.sectionTitle}>
                    <FormattedMessage {...messages.queueContent} />
                </span>
                <QueueContent type={queue.type} />
            </div>
        </div>
    );
}
