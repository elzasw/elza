import { MessageDescriptor, defineMessages } from 'react-intl';
import { PasteSkipReason } from './pasteRules';

export const clipboardMessages = defineMessages({
    copied: {
        id: 'arr.node.clipboard.copied',
        defaultMessage:
            '{count, plural, one {Zkopírována # hodnota} few {Zkopírovány # hodnoty} other {Zkopírováno # hodnot}}',
    },
    copyFailed: {
        id: 'arr.node.clipboard.copyFailed',
        defaultMessage: 'Hodnoty se nepodařilo zkopírovat',
    },
    clipboardTotal: {
        id: 'arr.node.clipboard.total',
        defaultMessage:
            '{count, plural, one {Celkem zkopírována # hodnota} few {Celkem zkopírovány # hodnoty} other {Celkem zkopírováno # hodnot}}',
    },
    removeItem: {
        id: 'arr.node.clipboard.removeItem',
        defaultMessage: 'Odebrat ze zkopírovaných',
    },
    clear: {
        id: 'arr.node.clipboard.clear',
        defaultMessage: 'Vymazat vše',
    },
    tableRows: {
        id: 'arr.node.clipboard.tableRows',
        defaultMessage: '{count, plural, one {Tabulka, # řádek} few {Tabulka, # řádky} other {Tabulka, # řádků}}',
    },
    pasted: {
        id: 'arr.node.clipboard.pasted',
        defaultMessage: '{count, plural, one {Vložena # hodnota} few {Vloženy # hodnoty} other {Vloženo # hodnot}}',
    },
    pasteFailed: {
        id: 'arr.node.clipboard.pasteFailed',
        defaultMessage: 'Hodnoty se nepodařilo vložit',
    },
    duplicates: {
        id: 'arr.node.clipboard.duplicates',
        defaultMessage:
            '{count, plural, one {# hodnotu již JP obsahuje} few {# hodnoty již JP obsahuje} other {# hodnot již JP obsahuje}}',
    },
    reportLine: {
        id: 'arr.node.clipboard.reportLine',
        defaultMessage: '{label}: {reason}',
    },
    truncated: {
        id: 'arr.node.clipboard.truncated',
        defaultMessage: 'vloženo, zkráceno na {max} znaků',
    },
});

export const skipReasonMessages: Record<PasteSkipReason, MessageDescriptor> = defineMessages({
    calculated: {
        id: 'arr.node.clipboard.skip.calculated',
        defaultMessage: 'nevloženo, prvek se počítá automaticky',
    },
    otherFund: {
        id: 'arr.node.clipboard.skip.otherFund',
        defaultMessage: 'nevloženo, odkazuje do jiného archivního souboru',
    },
    notUndefinable: {
        id: 'arr.node.clipboard.skip.notUndefinable',
        defaultMessage: 'nevloženo, prvek nepovoluje výjimku',
    },
    undefinedNextToValue: {
        id: 'arr.node.clipboard.skip.undefinedNextToValue',
        defaultMessage: 'nevloženo, výjimku nelze přidat k existující hodnotě',
    },
    specExists: {
        id: 'arr.node.clipboard.skip.specExists',
        defaultMessage: 'nevloženo, JP už má hodnotu s touto specifikací',
    },
    wouldRewriteUndefined: {
        id: 'arr.node.clipboard.skip.wouldRewriteUndefined',
        defaultMessage: 'nevloženo, hodnota by přepsala výjimku',
    },
    notRepeatable: {
        id: 'arr.node.clipboard.skip.notRepeatable',
        defaultMessage: 'nevloženo, prvek nepovoluje více hodnot',
    },
});
