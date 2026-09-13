import type { MessageDescriptor } from "react-intl";

/**
 * Mapa zpráv, ze které se vybírá podle hodnoty známé až za běhu.
 *
 * Statický extraktor (`locale:extract`) vidí jen literály v `defineMessages`.
 * Skládat id za běhu (``i18n(`stats.${key}.title`)``) proto znamená, že klíč
 * v katalogu nikdy nevznikne. Řešení je vždycky stejné: **celou množinu klíčů
 * vypsat staticky přes `defineMessages`; dynamický je jen výběr z ní.**
 */
export type MessageMap = Readonly<Record<string, MessageDescriptor>>;

/**
 * Vybere deskriptor podle hodnoty, kterou extraktor nevidí (enum ze serveru,
 * kód oprávnění).
 *
 * U uzavřených množin, kde TypeScript zná všechny klíče, je lepší indexovat
 * mapu přímo - chybějící položka je pak chyba překladu. Tahle funkce je pro
 * případy, kdy server může poslat hodnotu, o které klient ještě neví: místo
 * pádu nebo `[klíč]` vrátí `fallback` a v dev režimu upozorní do konzole.
 */
export function messageFor(
    map: MessageMap,
    key: string | null | undefined,
    fallback: MessageDescriptor,
): MessageDescriptor {
    if (key != null) {
        const descriptor = map[key];
        if (descriptor) {
            return descriptor;
        }
    }
    if (process.env.NODE_ENV !== "production") {
        console.warn(`i18n: chybí zpráva pro '${key}', použit fallback '${String(fallback.id)}'`);
    }
    return fallback;
}
