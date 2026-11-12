import { FundImportStrategy, FundPairKey } from 'elza-api';
import { MessageDescriptor, defineMessages } from 'react-intl';

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

// Explanations of the batch settings shown under the fields of the create dialog.
export const batchSettingsHints = defineMessages({
    fundPairKey: {
        id: 'admin.import.dialog.fundPairKeyHint',
        defaultMessage: 'Podle čeho se hledá existující archivní soubor; povinné u strategií s párováním.',
    },
    ignoreRootNodes: {
        id: 'admin.import.dialog.ignoreRootNodesHint',
        defaultMessage: 'Při importu do existujícího archivního souboru se vynechají kořenové úrovně ze souboru a jejich potomci se vloží pod kořen archivního souboru.',
    },
    scope: {
        id: 'admin.import.dialog.scopeHint',
        defaultMessage: 'Oblast, do které se zakládají a v níž se párují archivní entity ze souboru.',
    },
    separator: {
        id: 'admin.import.dialog.separatorHint',
        defaultMessage: 'Musí odpovídat CSV souboru; český Excel ukládá CSV se středníkem.',
    },
    encoding: {
        id: 'admin.import.dialog.encodingHint',
        defaultMessage: 'Musí odpovídat CSV souboru; český Excel ukládá CSV v kódování windows-1250.',
    },
    skipError: {
        id: 'admin.import.dialog.skipErrorHint',
        defaultMessage: 'Při chybě položky se dávka nezastaví a zpracuje i další položky.',
    },
    keepFiles: {
        id: 'admin.import.dialog.keepFilesHint',
        defaultMessage: 'Nahrané soubory se po dokončení dávky neodstraní ani po uplynutí doby uchování.',
    },
});

const valueMessages = defineMessages({
    strategyAlwaysNew: {
        id: 'admin.import.strategy.alwaysNew',
        defaultMessage: 'Vždy založit nový',
    },
    strategyAlwaysNewHint: {
        id: 'admin.import.strategy.alwaysNewHint',
        defaultMessage: 'Každá položka založí nový archivní soubor.',
    },
    strategyAlwaysPair: {
        id: 'admin.import.strategy.alwaysPair',
        defaultMessage: 'Jen do existujícího',
    },
    strategyAlwaysPairHint: {
        id: 'admin.import.strategy.alwaysPairHint',
        defaultMessage: 'Položka se naimportuje do existujícího archivního souboru nalezeného podle klíče pro párování; pokud žádný nenajde, skončí chybou.',
    },
    strategyPairAndCreate: {
        id: 'admin.import.strategy.pairAndCreate',
        defaultMessage: 'Spárovat, jinak založit',
    },
    strategyPairAndCreateHint: {
        id: 'admin.import.strategy.pairAndCreateHint',
        defaultMessage: 'Položka se naimportuje do existujícího archivního souboru nalezeného podle klíče pro párování; pokud žádný nenajde, založí nový.',
    },
    pairKeyInstitutionAndFundNumber: {
        id: 'admin.import.pairKey.institutionAndFundNumber',
        defaultMessage: 'Instituce a číslo fondu',
    },
});

export const FUND_IMPORT_STRATEGY_MESSAGE: Record<FundImportStrategy, MessageDescriptor> = {
    [FundImportStrategy.AlwaysNew]: valueMessages.strategyAlwaysNew,
    [FundImportStrategy.AlwaysPair]: valueMessages.strategyAlwaysPair,
    [FundImportStrategy.PairAndCreate]: valueMessages.strategyPairAndCreate,
};

export const FUND_IMPORT_STRATEGY_HINT: Record<FundImportStrategy, MessageDescriptor> = {
    [FundImportStrategy.AlwaysNew]: valueMessages.strategyAlwaysNewHint,
    [FundImportStrategy.AlwaysPair]: valueMessages.strategyAlwaysPairHint,
    [FundImportStrategy.PairAndCreate]: valueMessages.strategyPairAndCreateHint,
};

export const FUND_PAIR_KEY_MESSAGE: Record<FundPairKey, MessageDescriptor> = {
    [FundPairKey.InstitutionAndFundNumber]: valueMessages.pairKeyInstitutionAndFundNumber,
};
