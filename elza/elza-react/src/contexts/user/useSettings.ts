import { useCallback, useLayoutEffect, useSyncExternalStore } from 'react';
import { userDetailsSaveSettings } from 'actions/user/userDetail';
import { getOneSettings, setSettings } from 'components/arr/ArrUtils';
import { useAppThunkDispatch } from 'utils/hooks';
import { EntityType } from './EntityType';
import { SettingsType } from './SettingsType';
import { useUserContext } from './useUserContext';

export function useSettings() {
    const { userDetail } = useUserContext();
    const dispatch = useAppThunkDispatch();
    const settings = userDetail.settings;

    const get = useCallback(
        <T>(settingsType: SettingsType, entityType?: EntityType | null, entityId?: number | null): T | null => {
            const item = getOneSettings(settings, settingsType, entityType ?? null, entityId ?? null);
            if (item.value == null) {
                return null;
            }
            try {
                return JSON.parse(item.value) as T;
            } catch {
                return item.value as T;
            }
        },
        [settings],
    );

    const set = useCallback(
        async <T>(settingsType: SettingsType, value: T, entityType?: EntityType | null, entityId?: number | null) => {
            const item = getOneSettings(settings, settingsType, entityType ?? null, entityId ?? null);
            item.value = typeof value === 'string' ? value : JSON.stringify(value);
            const updated = setSettings(settings, item.id, item);
            await dispatch(userDetailsSaveSettings(updated));
        },
        [settings, dispatch],
    );

    return { get, set, raw: settings };
}

export function useFundStrictMode(fundId: number) {
    const { get, set } = useSettings();

    const value = get<string>(SettingsType.FUND_STRICT_MODE, EntityType.FUND, fundId);
    const parsed = value != null ? value === 'true' : null;

    const setValue = useCallback(
        (strictMode: boolean | null) =>
            set(SettingsType.FUND_STRICT_MODE, strictMode == null ? null : String(strictMode), EntityType.FUND, fundId),
        [set, fundId],
    );

    return [parsed, setValue] as const;
}

export function useFundReadMode(fundId: number) {
    const { get, set } = useSettings();

    const value = get<string>(SettingsType.FUND_READ_MODE, EntityType.FUND, fundId);
    const parsed = value !== 'false';

    const setValue = useCallback(
        (readMode: boolean) => set(SettingsType.FUND_READ_MODE, String(readMode), EntityType.FUND, fundId),
        [set, fundId],
    );

    return [parsed, setValue] as const;
}

export function useFundRightPanel(fundId: number) {
    const { get, set } = useSettings();

    const value = get<Record<string, boolean>>(SettingsType.FUND_RIGHT_PANEL, EntityType.FUND, fundId);

    const setValue = useCallback(
        (panels: Record<string, boolean>) => set(SettingsType.FUND_RIGHT_PANEL, panels, EntityType.FUND, fundId),
        [set, fundId],
    );

    return [value, setValue] as const;
}

export function useFundCenterPanel(fundId: number) {
    const { get, set } = useSettings();

    const value = get<Record<string, boolean>>(SettingsType.FUND_CENTER_PANEL, EntityType.FUND, fundId);

    const setValue = useCallback(
        (panels: Record<string, boolean>) => set(SettingsType.FUND_CENTER_PANEL, panels, EntityType.FUND, fundId),
        [set, fundId],
    );

    return [value, setValue] as const;
}

export function useFundTemplates(fundId: number) {
    const { get, set } = useSettings();

    const value = get<Array<{ name: string }>>(SettingsType.FUND_TEMPLATES, EntityType.FUND, fundId) ?? [];

    const setValue = useCallback(
        (templates: Array<{ name: string }>) => set(SettingsType.FUND_TEMPLATES, templates, EntityType.FUND, fundId),
        [set, fundId],
    );

    return [value, setValue] as const;
}

export function useTextFragments(fundId: number) {
    const { get, set } = useSettings();

    const value = get<Record<string, unknown>>(SettingsType.TEXT_FRAGMENTS, EntityType.FUND, fundId);

    const setValue = useCallback(
        (fragments: Record<string, unknown>) => set(SettingsType.TEXT_FRAGMENTS, fragments, EntityType.FUND, fundId),
        [set, fundId],
    );

    return [value, setValue] as const;
}

export function useSearchNodeFilters(fundId: number) {
    const { get, set } = useSettings();

    const value = get<unknown[]>(SettingsType.SEARCH_NODE_FILTERS, EntityType.FUND, fundId);

    const setValue = useCallback(
        (filters: unknown[]) => set(SettingsType.SEARCH_NODE_FILTERS, filters, EntityType.FUND, fundId),
        [set, fundId],
    );

    return [value, setValue] as const;
}

export type Language = 'cs' | 'en';

export interface UserSettingsData {
    compact?: boolean;
    darkMode?: boolean;
    groupColumns?: number;
    language?: Language;
    showDebugInfo?: boolean;
    showExperimentalFeatures?: boolean;
    aiFullWidth?: boolean;
}

const LOCAL_STORAGE_KEY = 'ELZA-USER-SETTINGS';

const listeners = new Set<() => void>();
let snapshot: UserSettingsData = readFromStorage();

function readFromStorage(): UserSettingsData {
    try {
        const raw = localStorage.getItem(LOCAL_STORAGE_KEY);
        return raw ? JSON.parse(raw) : {};
    } catch {
        return {};
    }
}

function subscribe(listener: () => void) {
    listeners.add(listener);
    return () => listeners.delete(listener);
}

function getSnapshot() {
    return snapshot;
}

function updateSettings(partial: Partial<UserSettingsData>) {
    snapshot = { ...snapshot, ...partial };
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(snapshot));
    listeners.forEach(l => l());
}

export function useUserSettings() {
    const settings = useSyncExternalStore(subscribe, getSnapshot);

    const update = useCallback(
        (partial: Partial<UserSettingsData>) => updateSettings(partial),
        [],
    );

    return { settings, update };
}

export function useTheme() {
    const { settings } = useUserSettings();
    const isDark = !!settings.darkMode;

    useLayoutEffect(() => {
        document.body.classList.remove('dark', 'light');
        document.body.classList.add(isDark ? 'dark' : 'light');
    }, [isDark]);

    return isDark;
}
