import { defineMessages } from 'react-intl';
import { AipType, DaQueueDirection } from 'elza-api';

export const messages = defineMessages({
    title: { id: 'admin.daQueue.title', defaultMessage: 'Fronta digitálního archivu' },
    repository: { id: 'admin.daQueue.repository', defaultMessage: 'Digitální archiv' },
    noRepository: {
        id: 'admin.daQueue.noRepository',
        defaultMessage: 'Není nastaven žádný digitální archiv. Založte jej v části Externí systémy.',
    },
    refresh: { id: 'admin.daQueue.refresh', defaultMessage: 'Obnovit' },
    loadFailed: {
        id: 'admin.daQueue.loadFailed',
        defaultMessage: 'Frontu se nepodařilo načíst, zobrazen je poslední známý stav.',
    },
    empty: { id: 'admin.daQueue.empty', defaultMessage: 'Ve frontě není žádný požadavek odpovídající filtru.' },

    presetWaiting: { id: 'admin.daQueue.preset.waiting', defaultMessage: 'Čekající' },
    presetAll: { id: 'admin.daQueue.preset.all', defaultMessage: 'Vše' },
    filterState: { id: 'admin.daQueue.filter.state', defaultMessage: 'Stav' },
    filterDirection: { id: 'admin.daQueue.filter.direction', defaultMessage: 'Směr' },
    filterAnyDirection: { id: 'admin.daQueue.filter.anyDirection', defaultMessage: 'Oba směry' },
    filterAipCode: { id: 'admin.daQueue.filter.aipCode', defaultMessage: 'Kód AIP' },
    filterBatch: { id: 'admin.daQueue.filter.batch', defaultMessage: 'Dávka' },
    filterFailed: { id: 'admin.daQueue.filter.failed', defaultMessage: 'Jen s chybou' },

    colId: { id: 'admin.daQueue.col.id', defaultMessage: 'ID' },
    colAip: { id: 'admin.daQueue.col.aip', defaultMessage: 'AIP' },
    colRequest: { id: 'admin.daQueue.col.request', defaultMessage: 'Požadavek' },
    colState: { id: 'admin.daQueue.col.state', defaultMessage: 'Stav' },
    colBatch: { id: 'admin.daQueue.col.batch', defaultMessage: 'Dávka' },
    colAttempts: { id: 'admin.daQueue.col.attempts', defaultMessage: 'Pokusy' },
    colNextAttempt: { id: 'admin.daQueue.col.nextAttempt', defaultMessage: 'Další pokus' },
    colChanged: { id: 'admin.daQueue.col.changed', defaultMessage: 'Změna stavu' },
    colMessage: { id: 'admin.daQueue.col.message', defaultMessage: 'Zpráva' },
    colRequestedBy: { id: 'admin.daQueue.col.requestedBy', defaultMessage: 'Požádal' },
    colVersion: { id: 'admin.daQueue.col.version', defaultMessage: 'Verze AIP' },

    now: { id: 'admin.daQueue.now', defaultMessage: 'teď' },
    inactive: { id: 'admin.daQueue.inactive', defaultMessage: 'neaktivní' },
    inactiveTitle: {
        id: 'admin.daQueue.inactive.title',
        defaultMessage: 'Požadavek nahradil novější, byl zrušen nebo AIP zneplatněn',
    },
    synchronization: { id: 'admin.daQueue.synchronization', defaultMessage: 'synchronizace' },
    filterByBatch: { id: 'admin.daQueue.filterByBatch', defaultMessage: 'Zobrazit jen tuto dávku' },
    detailClose: { id: 'admin.daQueue.detail.close', defaultMessage: 'Zavřít' },
    detailTitle: { id: 'admin.daQueue.detail.title', defaultMessage: 'Požadavek {id}' },

    pager: {
        id: 'admin.daQueue.pager',
        defaultMessage: '{from}–{to} z {total}',
    },
    previous: { id: 'admin.daQueue.previous', defaultMessage: 'Předchozí' },
    next: { id: 'admin.daQueue.next', defaultMessage: 'Další' },
    selected: {
        id: 'admin.daQueue.selected',
        defaultMessage: '{count, plural, one {Vybrán # požadavek} few {Vybrány # požadavky} other {Vybráno # požadavků}}',
    },

    retryNow: { id: 'admin.daQueue.action.retryNow', defaultMessage: 'Zkusit znovu teď' },
    retryNowTitle: {
        id: 'admin.daQueue.action.retryNow.title',
        defaultMessage: 'Čekající požadavek se vezme hned, bez čekání na další pokus',
    },
    withdraw: { id: 'admin.daQueue.action.withdraw', defaultMessage: 'Zrušit požadavek' },
    withdrawTitle: {
        id: 'admin.daQueue.action.withdraw.title',
        defaultMessage: 'Jen požadavky, které digitální archiv ještě nepřijal',
    },
    repeat: { id: 'admin.daQueue.action.repeat', defaultMessage: 'Opakovat' },
    repeatTitle: {
        id: 'admin.daQueue.action.repeat.title',
        defaultMessage: 'Neúspěšné stažení se vyžádá znovu, neúspěšný export odešle nový změnový balíček',
    },
    actionDone: {
        id: 'admin.daQueue.action.done',
        defaultMessage: '{done, plural, one {Provedeno u # požadavku} few {Provedeno u # požadavků} other {Provedeno u # požadavků}}{skipped, plural, =0 {} one { · # přeskočen} few { · # přeskočeny} other { · # přeskočeno}}',
    },
    actionNothing: {
        id: 'admin.daQueue.action.nothing',
        defaultMessage: 'Akce se na žádný z vybraných požadavků nevztahuje',
    },
});

export const directionMessages = defineMessages({
    [DaQueueDirection.Download]: { id: 'admin.daQueue.direction.DOWNLOAD', defaultMessage: 'Stažení' },
    [DaQueueDirection.Export]: { id: 'admin.daQueue.direction.EXPORT', defaultMessage: 'Export' },
});

export const aipTypeMessages = defineMessages({
    [AipType.PackageInfo]: { id: 'admin.daQueue.aipType.PACKAGE_INFO', defaultMessage: 'PACKAGE-INFO' },
    [AipType.Archdesc]: { id: 'admin.daQueue.aipType.ARCHDESC', defaultMessage: 'archivní popis' },
    [AipType.MetadataBase]: { id: 'admin.daQueue.aipType.METADATA_BASE', defaultMessage: 'metadata' },
    [AipType.AipBase]: { id: 'admin.daQueue.aipType.AIP_BASE', defaultMessage: 'úplný AIP' },
    [AipType.AipRaw]: { id: 'admin.daQueue.aipType.AIP_RAW', defaultMessage: 'nativní AIP' },
});
