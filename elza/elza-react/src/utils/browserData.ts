import { disableStoreSave } from 'actions/store/storeEx';

export type BrowserDataCategoryKey = 'appState' | 'display' | 'layout' | 'dataGrid' | 'descItemDrafts';

interface BrowserDataCategory {
    key: BrowserDataCategoryKey;
    matches: (storageKey: string) => boolean;
    /** The periodic store save writes this category back, so it has to be stopped before clearing. */
    isWrittenPeriodically?: boolean;
    /**
     * The number of entries says something to the user. Categories holding a fixed set of keys are
     * either present or not, so their count is noise.
     */
    hasMeaningfulCount?: boolean;
    /** Kept out of the settings UI, but still cleared when asked for explicitly. */
    isHidden?: boolean;
}

/**
 * Local storage entries the application owns, grouped so the user can drop them by kind. Anything
 * that does not match a category here belongs to something else on the same origin and is left
 * alone.
 */
const browserDataCategories: BrowserDataCategory[] = [
    {
        key: 'appState',
        matches: (storageKey) => storageKey === 'ELZA-STORE-STATE',
        isWrittenPeriodically: true,
    },
    {
        key: 'display',
        matches: (storageKey) => storageKey === 'ELZA-USER-SETTINGS' || storageKey === 'theme',
    },
    {
        key: 'layout',
        matches: (storageKey) => storageKey === 'arrDaos.leftSize' || storageKey === 'apDetail-globalCollapsed',
        // Covers only the DAO panel width and the entity detail collapse, which is too little to
        // offer as its own choice.
        isHidden: true,
    },
    {
        key: 'dataGrid',
        matches: (storageKey) => storageKey === 'ELZA-DATAGRID-LAST-HIGHLIGHT',
    },
    {
        key: 'descItemDrafts',
        matches: (storageKey) => storageKey.startsWith('descItem-'),
        hasMeaningfulCount: true,
    },
];

export const browserDataCategoryKeys = browserDataCategories.map(({ key }) => key);

/** The categories the settings UI offers; the rest are cleared only through an explicit request. */
export const visibleBrowserDataCategoryKeys = browserDataCategories
    .filter(({ isHidden }) => !isHidden)
    .map(({ key }) => key);

/** Whether the number of stored entries is worth showing next to the category. */
export function hasMeaningfulCount(categoryKey: BrowserDataCategoryKey) {
    return !!browserDataCategories.find(({ key }) => key === categoryKey)?.hasMeaningfulCount;
}

function collectStoredKeys(categoryKeys: BrowserDataCategoryKey[]) {
    const selectedCategories = browserDataCategories.filter(({ key }) => categoryKeys.includes(key));
    const storedKeys: string[] = [];

    for (let index = 0; index < localStorage.length; index++) {
        const storedKey = localStorage.key(index);
        if (storedKey != null && selectedCategories.some(({ matches }) => matches(storedKey))) {
            storedKeys.push(storedKey);
        }
    }

    return storedKeys;
}

/** How many entries each category currently holds, so the user sees what there is to clear. */
export function countBrowserData(): Record<BrowserDataCategoryKey, number> {
    const counts = {} as Record<BrowserDataCategoryKey, number>;

    browserDataCategories.forEach(({ key }) => {
        counts[key] = collectStoredKeys([key]).length;
    });

    return counts;
}

/**
 * Drop the entries of the given categories. The page is left for the caller to reload - the state
 * just cleared is still held in memory by the running application.
 */
export function clearBrowserData(categoryKeys: BrowserDataCategoryKey[]) {
    const clearsPeriodicallyWrittenData = browserDataCategories.some(
        ({ key, isWrittenPeriodically }) => isWrittenPeriodically && categoryKeys.includes(key)
    );

    if (clearsPeriodicallyWrittenData) {
        disableStoreSave();
    }

    collectStoredKeys(categoryKeys).forEach((storedKey) => localStorage.removeItem(storedKey));
}
