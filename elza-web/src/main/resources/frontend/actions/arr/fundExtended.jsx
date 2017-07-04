/**
 * Akce pro záložky otevřených stromů AS. - Pouze z důvodu circular dependency
 */



/**
 * Zapnutí/vypnutí rozšířeného zobrazení stromu AS.
 * {boolean} enable zapnout nebo vypnout rozšířené zobrazení?
 */
export function fundExtendedView(enable) {
    return {
        type: types.FUND_EXTENDED_VIEW,
        enable
    }
}
