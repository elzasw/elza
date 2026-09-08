import { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Api } from 'api';
import { BatchImportType, BatchState } from 'elza-api';
import { useWebsocket } from 'components/shared/web-socket/WebsocketProvider';
import { useThunkDispatch } from 'utils/hooks';
import { EventType } from 'typings/websocket/EventType';
import { fundsFilter } from 'actions/fund/fund';
import { AppState } from 'typings/store';

export function FundListImportListener(): null {
    const websocket = useWebsocket();
    const dispatch = useThunkDispatch();
    const filter = useSelector((s: AppState) => (s.fundRegion as { filter: unknown }).filter);

    useEffect(() => {
        const listener = websocket.addListener(async (msg: { eventType?: string; ids?: number[] }) => {
            if (msg.eventType !== EventType.IMPORT_BATCH_STATE_CHANGE) return;
            const ids = msg.ids ?? [];
            for (const id of ids) {
                try {
                    const { data } = await Api.importBatches.importBatchGet(id);
                    if (data.state === BatchState.Finished && data.importType === BatchImportType.Edx2) {
                        dispatch(fundsFilter(filter));
                        return;
                    }
                } catch {
                    // ignore – batch might have been deleted
                }
            }
        });
        return () => websocket.removeListener(listener);
    }, [websocket, dispatch, filter]);

    return null;
}
