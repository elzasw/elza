/**
 * Úprava parametrů zprávy do tvaru, který ICU přijme.
 *
 * ICU na poli hodnot spadne a `null`/`undefined` nesnese. Pole proto spojíme
 * čárkou a prázdné hodnoty zahodíme - jednou tady, ne na každém volajícím
 * místě.
 *
 * Číslo je platné jméno argumentu, takže `{0}` i `{name}` fungují stejně.
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
