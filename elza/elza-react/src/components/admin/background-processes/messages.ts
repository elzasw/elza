import {defineMessages} from 'react-intl';

// Klíč se skládal z typu fronty, což statický extraktor nevidí. Množina typů je
// uzavřená (odpovídá typům front na serveru), takže stačí ji vypsat a vybírat.
export const queueTypeMessages = defineMessages({
    NODE: {id: 'admin.bulk.header.title.NODE', defaultMessage: 'Validace JP'},
    BULK: {id: 'admin.bulk.header.title.BULK', defaultMessage: 'Hromadné akce'},
    OUTPUT: {id: 'admin.bulk.header.title.OUTPUT', defaultMessage: 'Výstupy'},
    AP: {id: 'admin.bulk.header.title.AP', defaultMessage: 'Validace AE'},
    EXPORT: {id: 'admin.bulk.header.title.EXPORT', defaultMessage: 'Publikace'},
    AIP: {id: 'admin.bulk.header.title.AIP', defaultMessage: 'Archivní balíčky'},
    BATCH_IMPORT: {id: 'admin.bulk.header.title.BATCH_IMPORT', defaultMessage: 'Dávkový import'},
});

// Ids jsou převzatá z legacy katalogu beze změny - přejmenování id při migraci
// zahodí jeho překlady při dalším locale:merge. Placeholder {0} zůstává:
// ICU bere jako jméno argumentu i číslo.
export const messages = defineMessages({
    load: {id: 'admin.bulk.title.load', defaultMessage: 'Zatížení'},
    requestPerHour: {id: 'admin.bulk.title.requestPerHour', defaultMessage: 'Požadavků za hodinu'},
    waitingRequests: {id: 'admin.bulk.title.waitingRequests', defaultMessage: 'Čekajících požadavků'},
    runningThreadCount: {id: 'admin.bulk.title.runningThreadCount', defaultMessage: 'Běžících vláken'},
    totalThreadCount: {id: 'admin.bulk.title.totalThreadCount', defaultMessage: 'Vláken celkem'},
    runningThreads: {id: 'admin.bulk.title.runningThreads', defaultMessage: 'Běžící vlákna'},
    noRunningThread: {id: 'admin.bulk.title.noRunningThread', defaultMessage: 'Neběží žádné vlákno'},
    beginTime: {id: 'admin.bulk.title.beginTime', defaultMessage: 'Běží od {0}'},
    requestId: {id: 'admin.bulk.title.requestId', defaultMessage: 'ID fronty #{0}'},
    currentId: {id: 'admin.bulk.title.currentId', defaultMessage: 'ID vazby #{0}'},
    queueEmpty: {id: 'admin.bulk.detail.queue.empty', defaultMessage: 'Ve frontě nejsou žádné požadavky'},
    queueContent: {id: 'admin.bulk.detail.queue.title', defaultMessage: 'Obsah fronty'},
    refresh: {id: 'admin.bulk.action.refresh', defaultMessage: 'Obnovit'},
    loadFailed: {id: 'admin.bulk.loadFailed', defaultMessage: 'Stav front se nepodařilo načíst'},
    noQueues: {id: 'admin.bulk.noQueues', defaultMessage: 'Server nehlásí žádné fronty'},
});
