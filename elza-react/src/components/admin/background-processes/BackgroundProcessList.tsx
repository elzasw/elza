import {
    Accordion,
    AccordionHeader,
    AccordionItem,
    AccordionPanel,
    Button,
    MessageBar,
    MessageBarBody,
    Spinner,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {ArrowClockwiseRegular} from '@fluentui/react-icons';
import {WebApi} from 'actions/index.jsx';
import {FormattedMessage} from 'react-intl';
import {messages} from './messages';
import {QueueDetail} from './QueueDetail';
import {QueueStats} from './QueueStats';
import {QueueInfo, compareQueueType} from './types';
import {usePolledData} from './usePolledData';

/** How often the state of all queues is reloaded. */
const LIST_REFRESH_MS = 10000;

const useStyles = makeStyles({
    page: {
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        minHeight: 0,
    },
    toolbar: {
        flexShrink: 0,
        display: 'flex',
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalM,
        padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalL}`,
    },
    errorBar: {
        flexShrink: 0,
        margin: `0 ${tokens.spacingHorizontalL} ${tokens.spacingVerticalS}`,
    },
    // The scrollbar belongs to the list alone, so the toolbar stays in place.
    scroll: {
        flexGrow: 1,
        minHeight: 0,
        overflowY: 'auto',
        padding: `0 ${tokens.spacingHorizontalL} ${tokens.spacingVerticalL}`,
    },
    item: {
        borderBottom: `${tokens.strokeWidthThin} solid ${tokens.colorNeutralStroke2}`,
    },
    empty: {
        color: tokens.colorNeutralForeground3,
    },
});

function fetchQueues(): Promise<QueueInfo[]> {
    return WebApi.getAsyncRequestInfo();
}

/**
 * State of the server's asynchronous request queues.
 *
 * Every queue is one collapsible item: the header carries the numbers worth
 * watching continuously, the panel the detail worth loading only on demand.
 */
export function BackgroundProcessList() {
    const styles = useStyles();
    const {data, failed, refresh} = usePolledData(fetchQueues, LIST_REFRESH_MS);

    const queues = data === undefined ? [] : [...data].sort((a, b) => compareQueueType(a.type, b.type));

    return (
        <div className={styles.page}>
            <div className={styles.toolbar}>
                <Button appearance="subtle" icon={<ArrowClockwiseRegular />} onClick={refresh}>
                    <FormattedMessage {...messages.refresh} />
                </Button>
            </div>
            {failed && (
                <MessageBar className={styles.errorBar} intent="error">
                    <MessageBarBody>
                        <FormattedMessage {...messages.loadFailed} />
                    </MessageBarBody>
                </MessageBar>
            )}
            <div className={styles.scroll}>
                {data === undefined ? (
                    <Spinner />
                ) : queues.length === 0 ? (
                    <div className={styles.empty}>
                        <FormattedMessage {...messages.noQueues} />
                    </div>
                ) : (
                    <Accordion collapsible multiple>
                        {queues.map(queue => (
                            <AccordionItem className={styles.item} key={queue.type} value={queue.type}>
                                <AccordionHeader expandIconPosition="end">
                                    <QueueStats queue={queue} />
                                </AccordionHeader>
                                <AccordionPanel>
                                    <QueueDetail queue={queue} />
                                </AccordionPanel>
                            </AccordionItem>
                        ))}
                    </Accordion>
                )}
            </div>
        </div>
    );
}
