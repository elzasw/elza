import {ExtAsyncQueueState} from "../../../../api/ExtAsyncQueueState";

export function getValues(): ExtAsyncQueueState[] {
    return [ExtAsyncQueueState.NEW, ExtAsyncQueueState.RUNNING, ExtAsyncQueueState.ERROR, ExtAsyncQueueState.OK]
}

export function getName(state: ExtAsyncQueueState): string {
    switch (state) {
        case ExtAsyncQueueState.OK:
            return 'Zpracovaný OK';
        case ExtAsyncQueueState.ERROR:
            return 'Zpracovaný chyba';
        case ExtAsyncQueueState.NEW:
            return 'Nový';
        case ExtAsyncQueueState.RUNNING:
            return 'Zpracovávaný';
        default:
            return 'Neznámý stav ' + state;
    }
}
