import {ExtAsyncQueueState} from "../../../../api/ExtAsyncQueueState";

export function getValues(): ExtAsyncQueueState[] {
    return [ExtAsyncQueueState.EXPORT_NEW, ExtAsyncQueueState.IMPORT_NEW, ExtAsyncQueueState.UPDATE, ExtAsyncQueueState.ERROR, ExtAsyncQueueState.OK]
}

export function getName(state: ExtAsyncQueueState): string {
    switch (state) {
        case ExtAsyncQueueState.EXPORT_NEW:
            return 'Nový v ELZA';
        case ExtAsyncQueueState.IMPORT_NEW:
            return 'Nový v CAM';
        case ExtAsyncQueueState.UPDATE:
            return 'Aktualizace';
        case ExtAsyncQueueState.ERROR:
            return 'Chyba';
        case ExtAsyncQueueState.OK:
            return 'Odesláno';
        default:
            return 'Neznámý stav ' + state;
    }
}
