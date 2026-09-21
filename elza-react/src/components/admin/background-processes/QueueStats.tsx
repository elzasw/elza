import {makeStyles, mergeClasses, tokens} from '@fluentui/react-components';
import {ReactNode} from 'react';
import {FormattedMessage, MessageDescriptor, useIntl} from 'react-intl';
import {messages, queueTypeMessages} from './messages';
import {QueueInfo} from './types';

const useStyles = makeStyles({
    root: {
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalXXL,
        rowGap: tokens.spacingVerticalS,
        flexGrow: 1,
        minWidth: 0,
    },
    name: {
        fontWeight: tokens.fontWeightSemibold,
        flexShrink: 0,
        minWidth: '9em',
    },
    stats: {
        display: 'flex',
        flexWrap: 'wrap',
        columnGap: tokens.spacingHorizontalXXL,
        rowGap: tokens.spacingVerticalS,
        marginLeft: 'auto',
    },
    stat: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalXXS,
        minWidth: '7em',
    },
    statLabel: {
        fontSize: tokens.fontSizeBase200,
        lineHeight: tokens.lineHeightBase200,
        color: tokens.colorNeutralForeground3,
    },
    statValue: {
        fontWeight: tokens.fontWeightSemibold,
        fontVariantNumeric: 'tabular-nums',
    },
    statValueActive: {
        color: tokens.colorBrandForeground1,
    },
    loadTrack: {
        height: '3px',
        marginTop: tokens.spacingVerticalXXS,
        borderRadius: tokens.borderRadiusSmall,
        backgroundColor: tokens.colorNeutralBackground5,
        overflow: 'hidden',
    },
    loadValue: {
        height: '100%',
        backgroundColor: tokens.colorBrandBackground,
    },
});

interface StatProps {
    label: ReactNode;
    value: ReactNode;
    /** Draws attention to the value - used where a non-zero number means work in progress. */
    highlight?: boolean;
    children?: ReactNode;
}

function Stat({label, value, highlight, children}: StatProps) {
    const styles = useStyles();
    return (
        <div className={styles.stat}>
            <span className={styles.statLabel}>{label}</span>
            <span className={mergeClasses(styles.statValue, highlight && styles.statValueActive)}>{value}</span>
            {children}
        </div>
    );
}

interface Props {
    queue: QueueInfo;
}

/**
 * Summary of one queue, shown in the accordion header: the queue name and the
 * numbers that tell at a glance whether the queue is busy or stuck.
 */
export function QueueStats({queue}: Props) {
    const styles = useStyles();
    const intl = useIntl();

    // The bar is only a visual cue for the percentage printed next to it, so it
    // stays out of the accessibility tree.
    const loadWidth = `${Math.min(Math.max(queue.load, 0), 1) * 100}%`;

    // The server may run a queue type this client does not know yet; its code
    // says more than the name of some other queue would.
    const typeMessage: MessageDescriptor | undefined = queueTypeMessages[queue.type];

    return (
        <div className={styles.root}>
            <span className={styles.name}>
                {typeMessage ? <FormattedMessage {...typeMessage} /> : queue.type}
            </span>
            <div className={styles.stats}>
                <Stat
                    label={<FormattedMessage {...messages.load} />}
                    value={intl.formatNumber(queue.load, {style: 'percent', maximumFractionDigits: 2})}
                >
                    <div className={styles.loadTrack} aria-hidden="true">
                        <div className={styles.loadValue} style={{width: loadWidth}} />
                    </div>
                </Stat>
                <Stat
                    label={<FormattedMessage {...messages.requestPerHour} />}
                    value={intl.formatNumber(queue.requestPerHour)}
                />
                <Stat
                    label={<FormattedMessage {...messages.waitingRequests} />}
                    value={intl.formatNumber(queue.waitingRequests)}
                    highlight={queue.waitingRequests > 0}
                />
                <Stat
                    label={<FormattedMessage {...messages.runningThreadCount} />}
                    value={intl.formatNumber(queue.runningThreadCount)}
                    highlight={queue.runningThreadCount > 0}
                />
                <Stat
                    label={<FormattedMessage {...messages.totalThreadCount} />}
                    value={intl.formatNumber(queue.totalThreadCount)}
                />
            </div>
        </div>
    );
}
