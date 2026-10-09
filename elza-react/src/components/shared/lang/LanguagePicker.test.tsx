import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest';

import { fireEvent, renderWithProviders, screen } from 'test/test-utils';
import { getUserSettings } from 'contexts/user/useSettings';
import { LANGUAGE_COOKIE, browserLanguage, effectiveLanguage } from './language';
import { LanguagePicker } from './LanguagePicker';

/**
 * Language switch of the login dialog: the installation offers Czech and English (German is known
 * but not a UI language), the installation's language is Czech and the browser prefers English.
 */

const originalLocation = window.location;
const reload = vi.fn();

beforeAll(() => {
    // the user settings are stored in localStorage, which the test environment does not provide
    const storage = new Map<string, string>();
    vi.stubGlobal('localStorage', {
        getItem: (key: string) => storage.get(key) ?? null,
        setItem: (key: string, value: string) => storage.set(key, value),
        removeItem: (key: string) => storage.delete(key),
    });
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['en-US', 'en']);
    Object.defineProperty(window, 'location', { configurable: true, value: { ...originalLocation, reload } });
});

afterAll(() => {
    Object.defineProperty(window, 'location', { configurable: true, value: originalLocation });
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
});

describe('language', () => {
    it('falls back to the installation language and strips the region', () => {
        expect(effectiveLanguage(undefined)).toBe('cs');
        expect(effectiveLanguage('de')).toBe('cs');
        expect(effectiveLanguage('en-GB')).toBe('en');
        expect(browserLanguage(['cs', 'en'])).toBe('en');
        expect(browserLanguage(['cs'])).toBeUndefined();
    });
});

describe('LanguagePicker', () => {
    it('offers the browser language by name until the user chooses, then only an icon', async () => {
        const { unmount } = renderWithProviders(<LanguagePicker />);

        const suggestion = await screen.findByRole('button', { name: 'English' });
        expect(screen.queryByRole('button', { name: 'Jazyk' })).toBeNull();

        fireEvent.click(suggestion);
        expect(getUserSettings().language).toBe('en');
        expect(document.cookie).toContain(`${LANGUAGE_COOKIE}=en`);
        expect(reload).toHaveBeenCalledTimes(1);
        unmount();

        // the choice is made: no suggestion any more, the menu stays reachable through the icon
        renderWithProviders(<LanguagePicker />);
        expect(await screen.findByRole('button', { name: 'Jazyk' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'English' })).toBeNull();
    });
});
