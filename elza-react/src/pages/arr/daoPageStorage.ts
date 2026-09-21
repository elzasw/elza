import { DaoPageUrlState, normalizeDaoPageState } from './daoPageUrl';

/**
 * Where the DAO page was left, so returning to it through the ribbon lands on the same
 * tab and directory instead of the default one. Kept per fund — a directory only means
 * something inside the repositories of the fund it was browsed in.
 *
 * Local storage can be missing or refuse to write (private windows, disabled site data),
 * and what it holds may come from an older release, so every access is guarded and the
 * stored value is normalized before use.
 */
const storageKey = (fundId: number) => `ELZA-DAO-PAGE-${fundId}`;

export const loadDaoPageState = (fundId: number): DaoPageUrlState | undefined => {
    try {
        const stored = localStorage.getItem(storageKey(fundId));
        return stored == null ? undefined : normalizeDaoPageState(JSON.parse(stored));
    } catch (e) {
        console.warn('Failed to read the stored DAO page state', e);
        return undefined;
    }
};

export const saveDaoPageState = (fundId: number, state: DaoPageUrlState): void => {
    try {
        localStorage.setItem(storageKey(fundId), JSON.stringify(state));
    } catch (e) {
        console.warn('Failed to store the DAO page state', e);
    }
};
