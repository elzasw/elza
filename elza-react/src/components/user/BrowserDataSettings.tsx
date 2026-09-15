import { useState } from 'react';
import { Button, Checkbox, Text, makeStyles, tokens } from '@fluentui/react-components';
import { defineMessages, useIntl } from 'react-intl';
import { useConfirmModal } from 'components/shared/dialog/useConfirmModal';
import {
    BrowserDataCategoryKey,
    clearBrowserData,
    countBrowserData,
    hasMeaningfulCount,
    visibleBrowserDataCategoryKeys,
} from 'utils/browserData';

const messages = defineMessages({
    browserDataHint: {
        id: 'userSettings.display.browserDataHint',
        defaultMessage:
            'Vyberte, co se má z tohoto prohlížeče smazat. Data na serveru zůstanou nezměněná a stránka se po smazání znovu načte.',
    },
    browserDataAppState: {
        id: 'userSettings.display.browserData.appState',
        defaultMessage: 'Stav aplikace (otevřené fondy, rozdělení panelů)',
    },
    browserDataDisplay: {
        id: 'userSettings.display.browserData.display',
        defaultMessage: 'Nastavení zobrazení (tmavý režim, jazyk, ladící informace)',
    },
    browserDataLayout: {
        id: 'userSettings.display.browserData.layout',
        defaultMessage: 'Rozvržení detailů (šířky a sbalení panelů)',
    },
    browserDataDataGrid: {
        id: 'userSettings.display.browserData.dataGrid',
        defaultMessage: 'Stav tabulky (naposledy vybraná jednotka popisu)',
    },
    browserDataDescItemDrafts: {
        id: 'userSettings.display.browserData.descItemDrafts',
        defaultMessage: 'Rozpracované hodnoty prvků popisu, které ještě nebyly uloženy',
    },
    clearBrowserData: {
        id: 'userSettings.display.clearBrowserData',
        defaultMessage: 'Vymazat vybraná data',
    },
    clearBrowserDataConfirm: {
        id: 'userSettings.display.clearBrowserDataConfirm',
        defaultMessage:
            'Opravdu smazat vybraná data z tohoto prohlížeče? Smaže se: {categories}. Data na serveru zůstanou nezměněná. Stránka se poté znovu načte.',
    },
    clearBrowserDataSubmit: {
        id: 'userSettings.display.clearBrowserDataSubmit',
        defaultMessage: 'Smazat',
    },
});

const browserDataMessages: Record<BrowserDataCategoryKey, typeof messages.browserDataAppState> = {
    appState: messages.browserDataAppState,
    display: messages.browserDataDisplay,
    layout: messages.browserDataLayout,
    dataGrid: messages.browserDataDataGrid,
    descItemDrafts: messages.browserDataDescItemDrafts,
};

const useStyles = makeStyles({
    root: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXS,
        paddingTop: tokens.spacingVerticalM,
        paddingBottom: tokens.spacingVerticalM,
    },
    hint: {
        color: tokens.colorNeutralForeground3,
        marginBottom: tokens.spacingVerticalXS,
    },
    clearButton: {
        alignSelf: 'flex-start',
        marginTop: tokens.spacingVerticalS,
        color: tokens.colorPaletteRedForeground1,
    },
});

export function BrowserDataSettings() {
    const { formatMessage } = useIntl();
    const confirm = useConfirmModal();
    const styles = useStyles();

    // Counted once per mount: nothing else writes local storage while the dialog is up.
    const [browserDataCounts] = useState(countBrowserData);
    const [selectedCategories, setSelectedCategories] = useState<BrowserDataCategoryKey[]>(() =>
        visibleBrowserDataCategoryKeys.filter((categoryKey) => browserDataCounts[categoryKey] > 0)
    );

    const hasSelectedCategory = selectedCategories.length > 0;

    function toggleCategory(categoryKey: BrowserDataCategoryKey, selected: boolean) {
        setSelectedCategories((previousCategories) =>
            selected
                ? [...previousCategories, categoryKey]
                : previousCategories.filter((previousCategory) => previousCategory !== categoryKey)
        );
    }

    async function handleClearBrowserData() {
        const selectedLabels = selectedCategories
            .map((categoryKey) => formatMessage(browserDataMessages[categoryKey]))
            .join(', ');

        const confirmed = await confirm({
            title: formatMessage(messages.clearBrowserData),
            message: formatMessage(messages.clearBrowserDataConfirm, { categories: selectedLabels }),
            confirmLabel: formatMessage(messages.clearBrowserDataSubmit),
            destructive: true,
        });

        if (confirmed) {
            clearBrowserData(selectedCategories);
            window.location.reload();
        }
    }

    return (
        <div className={styles.root}>
            <Text size={200} className={styles.hint}>
                {formatMessage(messages.browserDataHint)}
            </Text>
            {visibleBrowserDataCategoryKeys.map((categoryKey) => {
                const storedCount = browserDataCounts[categoryKey];
                const label = formatMessage(browserDataMessages[categoryKey]);
                return (
                    <Checkbox
                        key={categoryKey}
                        label={hasMeaningfulCount(categoryKey) ? `${label} (${storedCount})` : label}
                        checked={selectedCategories.includes(categoryKey)}
                        disabled={storedCount === 0}
                        onChange={(_event, data) => toggleCategory(categoryKey, !!data.checked)}
                    />
                );
            })}
            <Button className={styles.clearButton} disabled={!hasSelectedCategory} onClick={handleClearBrowserData}>
                {formatMessage(messages.clearBrowserData)}
            </Button>
        </div>
    );
}
