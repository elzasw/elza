import {ExtAsyncQueueState} from "../../../../api/ExtAsyncQueueState";

export function getValues(): ExtAsyncQueueState[] {
    return [ExtAsyncQueueState.NEW, ExtAsyncQueueState.ERROR, ExtAsyncQueueState.OK]
}

export function getName(state: ExtAsyncQueueState): string {
    switch (state) {
        case ExtAsyncQueueState.OK:
            return 'Odeslaný';
        case ExtAsyncQueueState.ERROR:
            return 'Chyba';
        case ExtAsyncQueueState.NEW:
            return 'Nový';
        default:
            return 'Neznámý stav ' + state;
    }
}
