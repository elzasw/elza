import { defineMessages } from 'react-intl';

// Batch settings labels shared by the create dialog and the batch detail.
export const batchSettingsMessages = defineMessages({
    fundImportStrategy: {
        id: 'admin.import.dialog.fundImportStrategy',
        defaultMessage: 'Strategie importu fondu',
    },
    fundPairKey: {
        id: 'admin.import.dialog.fundPairKey',
        defaultMessage: 'Klíč pro párování',
    },
    ignoreRootNodes: {
        id: 'admin.import.dialog.ignoreRootNodes',
        defaultMessage: 'Ignorovat kořenové úrovně',
    },
    scope: {
        id: 'admin.import.dialog.scope',
        defaultMessage: 'Oblast entit',
    },
    separator: {
        id: 'admin.import.dialog.separator',
        defaultMessage: 'Oddělovač',
    },
    encoding: {
        id: 'admin.import.dialog.encoding',
        defaultMessage: 'Kódování',
    },
    skipError: {
        id: 'admin.import.dialog.skipError',
        defaultMessage: 'Pokračovat po chybě položky',
    },
    keepFiles: {
        id: 'admin.import.dialog.keepFiles',
        defaultMessage: 'Zachovat vstupní soubory',
    },
});
