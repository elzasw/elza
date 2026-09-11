import { useEffect } from 'react';
import { defineMessages, useIntl } from 'react-intl';
import { Api } from 'api';
import { useWebsocket } from 'components/shared/web-socket/WebsocketProvider';
import { useThunkDispatch } from 'utils/hooks';
import { EventType } from 'typings/websocket/EventType';
import { addToastrInfo } from 'components/shared/toastr/ToastrActions';
import { consumeImportBatch, isImportBatchPending, onImportBatchTracked } from 'utils/pendingImportBatches';
import { notifyBatchOutcome } from './importBatchOutcome';

const messages = defineMessages({
    enqueued: {
        id: 'import.toast.enqueued',
        defaultMessage: 'Import zahájen',
    },
    enqueuedDetail: {
        id: 'import.toast.enqueued.detail',
        defaultMessage: 'Podrobnosti najdete v části Administrace → Import.',
    },
});

export function ImportBatchToaster(): null {
    const websocket = useWebsocket();
    const dispatch = useThunkDispatch();
    const intl = useIntl();

    useEffect(() => {
        return onImportBatchTracked(() => {
            dispatch(addToastrInfo(
                    intl.formatMessage(messages.enqueued),
                    intl.formatMessage(messages.enqueuedDetail)));
        });
    }, [dispatch, intl]);

    useEffect(() => {
        const listener = websocket.addListener(async (msg: { eventType?: string; ids?: number[] }) => {
            if (msg.eventType !== EventType.IMPORT_BATCH_STATE_CHANGE) return;
            const ids = msg.ids ?? [];
            for (const id of ids) {
                if (!isImportBatchPending(id)) continue;
                try {
                    const { data } = await Api.importBatches.importBatchGet(id);
                    if (notifyBatchOutcome(dispatch, intl, data)) {
                        consumeImportBatch(id);
                    }
                } catch {
                    consumeImportBatch(id);
                }
            }
        });
        return () => websocket.removeListener(listener);
    }, [websocket, dispatch, intl]);

    return null;
}
