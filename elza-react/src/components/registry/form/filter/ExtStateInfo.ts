import {ExtAsyncQueueState} from "../../../../api/ExtAsyncQueueState";

export function getValues(): ExtAsyncQueueState[] {
    return [ExtAsyncQueueState.UPDATE,
        ExtAsyncQueueState.UPDATE_DEFERRED,
        ExtAsyncQueueState.IMPORT_NEW,
        ExtAsyncQueueState.IMPORT_OK,
        ExtAsyncQueueState.EXPORT_NEW,
        ExtAsyncQueueState.EXPORT_NEED_CONFIRM,
        ExtAsyncQueueState.EXPORT_OK,
        ExtAsyncQueueState.EXPORT_CANCELLED,
        ExtAsyncQueueState.ERROR]
}

export function getName(state: ExtAsyncQueueState): string {
    switch (state) {
        case ExtAsyncQueueState.UPDATE:
            return 'K aktualizaci';
        case ExtAsyncQueueState.UPDATE_DEFERRED:
            return 'Odloženo (čeká na náhradu)';
        case ExtAsyncQueueState.EXPORT_NEW:
            return 'K odeslání';
        case ExtAsyncQueueState.EXPORT_NEED_CONFIRM:
            return 'Čeká na potvrzení';
        case ExtAsyncQueueState.IMPORT_NEW:
            return 'Ke stažení';
        case ExtAsyncQueueState.IMPORT_OK:
            return 'Aktualizováno/Staženo';
        case ExtAsyncQueueState.EXPORT_OK:
            return 'Odesláno';
        case ExtAsyncQueueState.EXPORT_CANCELLED:
            return 'Zrušeno uživatelem';
        case ExtAsyncQueueState.ERROR:
            return 'Chyba';
        default:
            return 'Neznámý stav ' + state;
    }
}
