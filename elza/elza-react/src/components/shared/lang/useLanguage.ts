import { useCallback, useEffect, useState } from 'react';
import { useUserSettings } from 'contexts/user';
import { effectiveLanguage, loadOfferedLanguages, writeLanguageCookie } from './language';

/**
 * UI language of the user and the languages to switch to.
 *
 * Switching reloads the application: names from rules packages come from the server in the
 * language of the request and are held in many places of the client state, so a fresh start is
 * the one way to have all of them in the new language. Choosing the language already shown only
 * records the choice.
 */
export function useLanguage() {
    const { settings, update } = useUserSettings();
    const [offered, setOffered] = useState<string[]>([]);

    useEffect(() => {
        let cancelled = false;
        loadOfferedLanguages().then((languages) => {
            if (!cancelled) {
                setOffered(languages);
            }
        });
        return () => {
            cancelled = true;
        };
    }, []);

    const current = effectiveLanguage(settings.language);

    const switchLanguage = useCallback(
        (language: string) => {
            update({ language });
            if (language !== current) {
                writeLanguageCookie(language);
                window.location.reload();
            }
        },
        [update, current],
    );

    return {
        /** Language the UI is shown in. */
        current,
        /** Language the user chose explicitly, if any. */
        chosen: settings.language,
        /** Languages to switch to; empty until loaded. */
        offered,
        switchLanguage,
    };
}
