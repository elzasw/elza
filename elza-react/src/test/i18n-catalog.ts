import enCatalog from "../../lang/translated/en.json";

/**
 * Anglický katalog pro testy, ve tvaru, který čeká `IntlProvider`.
 *
 * Záměrně z `lang/translated/` (verzovaný zdroj pravdy), ne z
 * `public/static/res/locale/` - ten je generovaný, gitignorovaný a na čerstvém
 * checkoutu před `locale:compile` neexistuje.
 *
 * Test, který kreslí proti tomuhle katalogu, spadne, když zpráva v katalogu
 * chybí. Ručně psaný seznam zpráv by naopak prošel i ve stavu, kdy id v kódu
 * existuje, ale v katalogu ne - a uživatel by v anglickém UI viděl češtinu.
 */
export const enMessages: Record<string, string> = Object.fromEntries(
    Object.entries(enCatalog as Record<string, { defaultMessage: string }>).map(
        ([id, entry]) => [id, entry.defaultMessage],
    ),
);

/**
 * Ověří, že všechna uvedená id mají anglický překlad.
 *
 * Užitečné tam, kde se zprávy vybírají za běhu (mapy enumů) a render by
 * neprošel všechny větve - pak by chybějící překlad test neshodil.
 */
export function expectTranslated(ids: readonly string[]): void {
    const missing = ids.filter((id) => !(id in enMessages));
    if (missing.length > 0) {
        throw new Error(`Chybí v lang/translated/en.json: ${missing.join(", ")}`);
    }
}
