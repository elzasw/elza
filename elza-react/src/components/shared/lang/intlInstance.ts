import { createIntl, createIntlCache, type IntlConfig, type IntlShape } from "react-intl";

/**
 * `OnErrorFn` je definovaný v @formatjs/intl, což je jen tranzitivní závislost -
 * odvozujeme ho proto z typu, který react-intl skutečně reexportuje.
 */
type OnErrorFn = IntlConfig["onError"];

/**
 * Jedna instance `intl` pro celou aplikaci.
 *
 * React komponenty si `intl` berou hookem (`useIntl`) nebo přes `<FormattedMessage>`;
 * tenhle modul existuje kvůli kódu, který k hookům přístup nemá - validátory
 * a redux akce. `createAppIntl` vytvoří instanci pro strom a zároveň ji tady
 * uloží, takže non-React volající formátuje **tímtéž objektem** jako strom;
 * nemůže tedy vzniknout druhá konfigurace, která by se rozešla.
 *
 * ## Kdy singleton NEpoužít
 *
 * Zpráva naformátovaná přes `getIntl()` je řetězec zachycený v čase volání -
 * při přepnutí jazyka se sama nepřekreslí. To je v pořádku jen pro krátkodobé
 * hodnoty, které stejně vznikají znovu (validační hlášky se přepočítají při
 * další validaci).
 *
 * Pravidlo: **singleton jen tam, kde hodnota musí být `string` a je krátkodobá.
 * Všude jinde předávej `ReactNode`** - tedy `<FormattedMessage />` - protože ten
 * se po přepnutí jazyka překreslí sám. Toastry, tituly dialogů a popisky
 * v JSX patří do druhé kategorie.
 *
 * Proto se `getIntl` záměrně nereexportuje z barrelu `components/shared`:
 * ať je každé použití v review vidět na plné cestě importu.
 */

const cache = createIntlCache();

let current: IntlShape | undefined;

/**
 * Vytvoří instanci pro `<RawIntlProvider>` a zpřístupní ji i mimo React.
 * Volá se pouze z `LangProvider` (a z testovacích providerů).
 */
export function createAppIntl(
    locale: string,
    messages: Record<string, string>,
    onError: OnErrorFn = defaultOnError(messages),
): IntlShape {
    // Klíč `onError` sem nesmí přijít jako `undefined`: createIntl konfiguraci
    // rozkopíruje přes své výchozí hodnoty, takže explicitní undefined přepíše
    // výchozí handler a první MISSING_TRANSLATION pak spadne na
    // "onError is not a function". Přesně tak umřela přihlašovací stránka v EN.
    current = createIntl({ locale, defaultLocale: "cs", messages, onError }, cache);
    return current;
}

/**
 * Výchozí handler chyb formátování.
 *
 * Katalog se stahuje asynchronně, takže první render v jiném jazyce než `cs`
 * proběhne nad prázdným katalogem a každá zpráva by nahlásila MISSING_TRANSLATION
 * (pro `cs` formatjs hlášení přeskočí, protože se shoduje s defaultLocale).
 * V té chvíli je to očekávaný stav a `defaultMessage` je správný fallback -
 * proto se chybějící překlad mlčky ignoruje **jen dokud je katalog prázdný**.
 * Jakmile je načtený, chybějící klíč je reálná mezera a hlásí se jako všechno
 * ostatní.
 */
function defaultOnError(messages: Record<string, string>): OnErrorFn {
    const catalogLoaded = Object.keys(messages).length > 0;
    return (error) => {
        if (!catalogLoaded && error.code === "MISSING_TRANSLATION") return;
        console.error(error);
    };
}

/**
 * Instance pro moduly bez přístupu k hookům.
 *
 * Nikdy nevrací `undefined`: před prvním renderem (a v testech, které si
 * providera nezakládají) spadne na `cs` s prázdným katalogem, takže se použije
 * `defaultMessage` deskriptoru - tedy český zdrojový text, ne `[klíč]`.
 */
export function getIntl(): IntlShape {
    if (!current) {
        current = createIntl({ locale: "cs", defaultLocale: "cs", messages: {} }, cache);
    }
    return current;
}
