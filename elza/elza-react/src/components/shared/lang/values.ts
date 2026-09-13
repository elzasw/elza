/**
 * Převod parametrů zprávy z legacy tvaru do ICU.
 *
 * Starý helper (`components/i18n`) uměl navíc dvě věci, které ICU nemá:
 * spojoval pole hodnot čárkou a tiše přijímal `null`/`undefined`. ICU na poli
 * spadne. Převádíme proto jednou tady, ne na každém volajícím místě.
 *
 * Placeholdery samotné převádět netřeba: ICU bere jako jméno argumentu i číslo,
 * takže legacy `{0}` i `{name}` fungují beze změny textu zprávy.
 */
export function icuValues(
    properties?: Record<string, unknown> | null,
): Record<string, string | number> {
    const result: Record<string, string | number> = {};
    for (const [key, value] of Object.entries(properties ?? {})) {
        if (Array.isArray(value)) {
            result[key] = value.join(", ");
        } else if (typeof value === "number") {
            result[key] = value;
        } else {
            result[key] = value == null ? "" : String(value);
        }
    }
    return result;
}
