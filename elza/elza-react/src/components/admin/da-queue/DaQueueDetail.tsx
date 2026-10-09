import {
    Button,
    DrawerBody,
    DrawerHeader,
    DrawerHeaderTitle,
    InlineDrawer,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { DismissRegular } from '@fluentui/react-icons';
import { DaQueueItemVO } from 'elza-api';
import { ReactNode } from 'react';
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
});

interface Props {
    item: DaQueueItemVO | undefined;
    onClose: () => void;
}

/** Everything known about one request, the message in full. */
export function DaQueueDetail({ item, onClose }: Props) {
    const styles = useStyles();
    const intl = useIntl();

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
                            {item.active && row(<FormattedMessage {...messages.colNextAttempt} />,
                                formatNextAttempt(intl, item.nextAttemptAt))}
                            {row(<FormattedMessage {...messages.colChanged} />, formatDateTime(intl, item.stateDate))}
                            {row(<FormattedMessage {...messages.colRequestedBy} />,
                                item.requestedBy ?? intl.formatMessage(messages.synchronization))}
                        </dl>
                        {item.stateMessage && (
                            <p className={styles.message}>{item.stateMessage}</p>
                        )}
                    </DrawerBody>
                </>
            )}
        </InlineDrawer>
    );
}
