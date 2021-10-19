import {ExtAsyncQueueState} from "../../../../api/ExtAsyncQueueState";

export function getValues(): ExtAsyncQueueState[] {
    return [ExtAsyncQueueState.UPDATE, 
        ExtAsyncQueueState.IMPORT_NEW, 
        ExtAsyncQueueState.IMPORT_OK, 
        ExtAsyncQueueState.EXPORT_NEW, 
        ExtAsyncQueueState.EXPORT_OK, 
        ExtAsyncQueueState.ERROR]
}

export function getName(state: ExtAsyncQueueState): string {
    switch (state) {
        case ExtAsyncQueueState.UPDATE:
            return 'Aktualizováno';
        case ExtAsyncQueueState.EXPORT_NEW:
            return 'K odeslání';
        case ExtAsyncQueueState.IMPORT_NEW:
            return 'Ke stažení';
        case ExtAsyncQueueState.IMPORT_OK:
            return 'Staženo';
        case ExtAsyncQueueState.EXPORT_OK:
            return 'Odesláno';
        case ExtAsyncQueueState.ERROR:
            return 'Chyba';
        default:
            return 'Neznámý stav ' + state;
    }
}
