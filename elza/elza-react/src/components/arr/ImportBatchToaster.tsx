import { useEffect } from 'react';
import { defineMessages, useIntl } from 'react-intl';
import { Api } from 'api';
import { BatchState } from 'elza-api';
import { useWebsocket } from 'components/shared/web-socket/WebsocketProvider';
import { useThunkDispatch } from 'utils/hooks';
import { EventType } from 'typings/websocket/EventType';
import { addToastrDanger, addToastrSuccess } from 'components/shared/toastr/ToastrActions';
import { consumeImportBatch, isImportBatchPending } from 'utils/pendingImportBatches';

const messages = defineMessages({
    finished: {
        id: 'import.toast.batch.finished',
        defaultMessage: 'Import „{name}" byl dokončen',
    },
    failed: {
        id: 'import.toast.batch.failed',
        defaultMessage: 'Import „{name}" skončil s chybou',
    },
    failedDetail: {
        id: 'import.toast.batch.failed.detail',
        defaultMessage: 'Podrobnosti najdete v části Administrace → Import.',
    },
});

export function ImportBatchToaster(): null {
    const websocket = useWebsocket();
    const dispatch = useThunkDispatch();
    const intl = useIntl();

    useEffect(() => {
        const listener = websocket.addListener(async (msg: { eventType?: string; ids?: number[] }) => {
            if (msg.eventType !== EventType.IMPORT_BATCH_STATE_CHANGE) return;
            const ids = msg.ids ?? [];
            for (const id of ids) {
                if (!isImportBatchPending(id)) continue;
                try {
                    const { data } = await Api.importBatches.importBatchGet(id);
                    const state = data.state;
                    if (state === BatchState.Finished) {
                        consumeImportBatch(id);
                        dispatch(addToastrSuccess(intl.formatMessage(messages.finished, { name: data.name })));
                    } else if (state === BatchState.Failed || state === BatchState.Cancelled) {
                        consumeImportBatch(id);
                        dispatch(addToastrDanger(
                                intl.formatMessage(messages.failed, { name: data.name }),
                                intl.formatMessage(messages.failedDetail)));
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
