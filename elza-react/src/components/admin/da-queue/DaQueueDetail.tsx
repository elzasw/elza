import {
    Button,
    DrawerBody,
    DrawerHeader,
    DrawerHeaderTitle,
    InlineDrawer,
    Spinner,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { DismissRegular, ErrorCircleRegular } from '@fluentui/react-icons';
import { DaQueueItemVO } from 'elza-api';
import { ReactNode, useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { Link as RouterLink } from 'react-router-dom';
import { urlAip } from '../../../constants';
import { queueStateMessages } from '../../aip/messages';
import { formatDateTime, formatNextAttempt } from './format';
import { aipTypeMessages, directionMessages, messages } from './messages';

const useStyles = makeStyles({
    list: {
        display: 'grid',
        gridTemplateColumns: 'max-content 1fr',
        columnGap: tokens.spacingHorizontalM,
        rowGap: tokens.spacingVerticalXS,
        margin: 0,
    },
    term: {
        color: tokens.colorNeutralForeground3,
    },
    value: {
        margin: 0,
        overflowWrap: 'anywhere',
    },
    message: {
        margin: 0,
        whiteSpace: 'pre-wrap',
        overflowWrap: 'anywhere',
    },
    actions: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-start',
        rowGap: tokens.spacingVerticalXS,
        marginTop: tokens.spacingVerticalL,
    },
    hint: {
        color: tokens.colorNeutralForeground3,
    },
});

interface Props {
    item: DaQueueItemVO | undefined;
    onClose: () => void;
    /** Ends the request as failed; offered only for a download that keeps failing. */
    onMarkFailed?: () => Promise<void>;
}

export type DaQueueDetailProps = Props;

/** Everything known about one request, the message in full. */
export function DaQueueDetail({ item, onClose, onMarkFailed }: Props) {
    const styles = useStyles();
    const intl = useIntl();
    // the server may wait for an exchange with the DA in progress before it answers
    const [marking, setMarking] = useState(false);

    const markFailed = async () => {
        if (!onMarkFailed) {
            return;
        }
        setMarking(true);
        try {
            await onMarkFailed();
        } finally {
            setMarking(false);
        }
    };

    const row = (label: ReactNode, value: ReactNode) => (
        <>
            <dt className={styles.term}>{label}</dt>
            <dd className={styles.value}>{value}</dd>
        </>
    );

    return (
        <InlineDrawer position="end" separator open={item != null} style={{ width: '420px' }}>
            {item && (
                <>
                    <DrawerHeader>
                        <DrawerHeaderTitle
                            action={
                                <Button
                                    appearance="subtle"
                                    aria-label={intl.formatMessage(messages.detailClose)}
                                    icon={<DismissRegular />}
                                    onClick={onClose}
                                />
                            }
                        >
                            <FormattedMessage {...messages.detailTitle} values={{ id: item.id }} />
                        </DrawerHeaderTitle>
                    </DrawerHeader>
                    <DrawerBody>
                        <dl className={styles.list}>
                            {row(<FormattedMessage {...messages.colAip} />,
                                item.aipId != null
                                    ? <RouterLink to={urlAip(item.aipId)}>{item.aipCode}</RouterLink>
                                    : item.aipCode)}
                            {row(<FormattedMessage {...messages.colVersion} />, item.aipVersion)}
                            {row(<FormattedMessage {...messages.colRequest} />,
                                <>
                                    <FormattedMessage {...directionMessages[item.direction]} />
                                    {item.aipType && <> · <FormattedMessage {...aipTypeMessages[item.aipType]} /></>}
                                </>)}
                            {row(<FormattedMessage {...messages.colState} />,
                                <>
                                    <FormattedMessage {...queueStateMessages[item.state]} />
                                    {!item.active && <> · <FormattedMessage {...messages.inactive} /></>}
                                </>)}
                            {row(<FormattedMessage {...messages.colBatch} />, item.batchId)}
                            {row(<FormattedMessage {...messages.colAttempts} />, item.attemptCount)}
                            {item.failingSince && row(<FormattedMessage {...messages.colFailingSince} />,
                                formatDateTime(intl, item.failingSince))}
                            {item.active && row(<FormattedMessage {...messages.colNextAttempt} />,
                                formatNextAttempt(intl, item.nextAttemptAt))}
                            {row(<FormattedMessage {...messages.colChanged} />, formatDateTime(intl, item.stateDate))}
                            {row(<FormattedMessage {...messages.colRequestedBy} />,
                                item.requestedBy ?? intl.formatMessage(messages.synchronization))}
                        </dl>
                        {item.stateMessage && (
                            <p className={styles.message}>{item.stateMessage}</p>
                        )}
                        {onMarkFailed && (
                            <div className={styles.actions}>
                                <Button icon={marking ? <Spinner size="tiny" /> : <ErrorCircleRegular />}
                                        disabled={marking} onClick={markFailed}>
                                    <FormattedMessage {...messages.markFailed} />
                                </Button>
                                <span className={styles.hint}><FormattedMessage {...messages.markFailedTitle} /></span>
                            </div>
                        )}
                    </DrawerBody>
                </>
            )}
        </InlineDrawer>
    );
}
