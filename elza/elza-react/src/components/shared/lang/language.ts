import { Api, serverContextPath } from 'api';
import { getUserSettings } from 'contexts/user/useSettings';

/**
 * UI language of the client.
 *
 * The client ships message catalogs for {@link CLIENT_LANGUAGES}; the server says which languages
 * the installation offers (`GET /api/v1/languages`, `uiEnabled`) and which one is its default
 * (`elza.locale`, passed as `window.defaultLanguage`). A user who has not chosen a language gets
 * the installation's language, not the browser's: many users run a browser in another language
 * than the one they work in.
 *
 * The chosen language travels to the server in the {@link LANGUAGE_COOKIE} cookie, so it reaches
 * also downloads and other plain links; the server returns names of package entities in it.
 */

/** Languages with a message catalog in the client (`static/res/locale/<tag>.json`). */
export const CLIENT_LANGUAGES: readonly string[] = ['cs', 'en'];

/** Cookie read by the server (`PackageTexts.LANGUAGE_COOKIE`). */
export const LANGUAGE_COOKIE = 'elza-lang';

const COOKIE_MAX_AGE_SECONDS = 365 * 24 * 60 * 60;

interface WindowWithLanguage extends Window {
    defaultLanguage?: string;
}

/** Language of a tag the client has a catalog for (`en-GB` -> `en`), or undefined. */
export function clientLanguage(tag: string | undefined | null): string | undefined {
    if (!tag) {
        return undefined;
    }
    const lower = tag.toLowerCase();
    if (CLIENT_LANGUAGES.includes(lower)) {
        return lower;
    }
    const language = lower.split('-')[0];
    return CLIENT_LANGUAGES.includes(language) ? language : undefined;
}

/** Language of the installation, if the client has a catalog for it; else Czech. */
export function defaultLanguage(): string {
    return clientLanguage((window as WindowWithLanguage).defaultLanguage) ?? 'cs';
}

/** The language the UI is shown in: the user's choice, else the installation's language. */
export function effectiveLanguage(chosen: string | undefined): string {
    return clientLanguage(chosen) ?? defaultLanguage();
}

/**
 * The first of the browser's preferred languages that is offered, or undefined.
 *
 * @param offered languages offered by the installation and the client
 */
export function browserLanguage(offered: readonly string[]): string | undefined {
    const preferred = navigator.languages?.length ? navigator.languages : [navigator.language];
    for (const tag of preferred) {
        const language = clientLanguage(tag);
        if (language && offered.includes(language)) {
            return language;
        }
    }
    return undefined;
}

/** Name of a language in the language itself ("čeština", "English"). */
export function nativeLanguageName(tag: string): string {
    try {
        const name = new Intl.DisplayNames([tag], { type: 'language' }).of(tag);
        return name ? name.charAt(0).toLocaleUpperCase(tag) + name.slice(1) : tag;
    } catch {
        return tag;
    }
}

/** Sends the language to the server with every following request. */
export function writeLanguageCookie(language: string) {
    const path = serverContextPath || '/';
    document.cookie = `${LANGUAGE_COOKIE}=${encodeURIComponent(language)}; path=${path}; max-age=${COOKIE_MAX_AGE_SECONDS}; SameSite=Lax`;
}

/** Called once at startup, before the first request. */
export function initLanguage() {
    writeLanguageCookie(effectiveLanguage(getUserSettings().language));
}

let offeredLanguages: Promise<string[]> | undefined;

/**
 * Languages the user can switch to: UI languages of the installation with a client catalog. Loaded
 * once; without the server answer the client catalogs.
 */
export function loadOfferedLanguages(): Promise<string[]> {
    if (!offeredLanguages) {
        offeredLanguages = Api.languages
            .languagesListLanguages({ overrideErrorHandler: true })
            .then((response) => {
                const offered = new Set<string>();
                for (const language of response.data) {
                    const tag = language.uiEnabled ? clientLanguage(language.tag) : undefined;
                    if (tag) {
                        offered.add(tag);
                    }
                }
                return CLIENT_LANGUAGES.filter((tag) => offered.has(tag));
            })
            .catch(() => [...CLIENT_LANGUAGES]);
    }
    return offeredLanguages;
}
