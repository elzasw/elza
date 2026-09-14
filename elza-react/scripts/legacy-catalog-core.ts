/**
 * Zjištění, které klíče legacy katalogu (`messages_cs.js`) ještě někdo potřebuje.
 *
 * Katalog se načítá `<script>` tagem v `index.html`, takže se celý posílá
 * každému uživateli při každém načtení stránky - i ty klíče, na které už
 * v kódu nikdo nesahá. Postupnou migrací na react-intl jich zbyla hrstka.
 *
 * Oddělené od I/O, aby to šlo unit-testovat stejně jako ostatní `*-core`.
 */

/** `i18n('klic')`, `i18n("klic", param)` i caret forma `i18n('^klic')`. */
const LITERAL_CALL = /\bi18n\(\s*['"`]\^?([^'"`]+)['"`]/g;

/**
 * Klíče, které se skládají za běhu, takže je ve zdroji staticky nevidíme.
 *
 * Množiny jsou uzavřené a dohledané u každého volajícího - proto se vypisují
 * sem, ne odhadem podle prefixu. Kdyby některé volání přibylo, `collectUsed`
 * ho nenajde a klíč by se z katalogu ztratil; hlídá to test, který kontroluje,
 * že v `src/` nepřibylo skládané volání mimo tenhle seznam.
 */
export const DYNAMIC_KEYS: string[] = [
    // Prázdné: všechna místa, která klíč skládala, jsou převedená na react-intl
    // (uzavřené množiny se tam vypisují staticky a vybírá se přes messageFor).
];

/** Soubory, ve kterých je skládané volání očekávané (viz DYNAMIC_KEYS). */
export const DYNAMIC_CALL_FILES: string[] = [];

/**
 * Zachycený text, který ve skutečnosti není klíč: prefix skládaného klíče
 * (`'arr.x.' + kod`), šablonový literál nebo samotný caret z `i18n('^' + key)`.
 */
function isKeyFragment(captured: string): boolean {
    return captured === "^" || captured.endsWith(".") || captured.includes("${");
}

/** Klíče, na které se ve zdroji odkazuje literálem. */
export function collectUsed(sources: Iterable<string>): Set<string> {
    const used = new Set<string>();
    for (const source of sources) {
        for (const match of source.matchAll(LITERAL_CALL)) {
            if (isKeyFragment(match[1])) continue;
            used.add(match[1]);
        }
    }
    return used;
}

/**
 * Počet volání `i18n(`, u kterých se klíč **skládá** - a `collectUsed` ho tedy
 * nenajde.
 *
 * Skládané je volání, jehož argument obsahuje `+` nebo `${` (konkatenace,
 * šablonový literál), nebo v něm není vůbec žádný literál (klíč přichází
 * proměnnou). Ternář nad dvěma literály (`i18n(cond ? 'a' : 'b')`) skládaný
 * **není** - oba klíče `collectUsed` zachytí.
 */
export function countDynamicCalls(source: string): number {
    let count = 0;
    for (const match of source.matchAll(/\bi18n\(([^)]{0,120})/g)) {
        const arg = match[1];
        const composed = arg.includes("+") || arg.includes("${");
        const hasLiteral = /['"`]/.test(arg);
        if (composed || !hasLiteral) count++;
    }
    return count;
}
