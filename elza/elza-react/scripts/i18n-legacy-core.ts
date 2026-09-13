/**
 * Čistá logika ratchetu nad legacy voláními `i18n(`.
 *
 * Oddělené od I/O, aby to šlo unit-testovat stejně jako `locale-check-core`.
 */

/** Počty volání per soubor, cesty relativní ke kořeni elza-react, lomítka dopředná. */
export type LegacyBaseline = Record<string, number>;

/**
 * Spočítá volání legacy helperu ve zdrojovém textu.
 *
 * Záměrně naivní - stejně jako `grep` započítá i zakomentovaná volání. Je to
 * snadno vysvětlitelné a chybuje směrem k nadpočtu, což je u shrink-only
 * pojistky bezpečný směr.
 *
 * `\b` před `i18n` zabrání započítání `existsI18n(` a podobných.
 */
export function countCalls(source: string): number {
    return source.match(/\bi18n\(/g)?.length ?? 0;
}

export type LegacyComparison = {
    /** Soubory, kde volání přibyla (nebo soubor v baseline vůbec není). */
    added: Array<{ file: string; baseline: number; actual: number }>;
    /** Soubory, kde volání ubyla - baseline jde zmenšit. */
    improved: Array<{ file: string; baseline: number; actual: number }>;
    total: number;
};

export function compareLegacy(counts: LegacyBaseline, baseline: LegacyBaseline): LegacyComparison {
    const added: LegacyComparison["added"] = [];
    const improved: LegacyComparison["improved"] = [];

    for (const [file, actual] of Object.entries(counts)) {
        const allowed = baseline[file] ?? 0;
        if (actual > allowed) {
            added.push({ file, baseline: allowed, actual });
        } else if (actual < allowed) {
            improved.push({ file, baseline: allowed, actual });
        }
    }

    // Soubor, který v baseline je, ale už neexistuje (nebo je čistý), se také
    // počítá jako zlepšení - jinak by baseline nešla zmenšit po smazání souboru.
    for (const [file, allowed] of Object.entries(baseline)) {
        if (!(file in counts) && allowed > 0) {
            improved.push({ file, baseline: allowed, actual: 0 });
        }
    }

    return { added, improved, total: countTotal(counts) };
}

export function countTotal(baseline: LegacyBaseline): number {
    return Object.values(baseline).reduce((sum, n) => sum + n, 0);
}
