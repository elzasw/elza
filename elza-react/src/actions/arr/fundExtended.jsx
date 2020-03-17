/**
 * Akce pro záložky otevřených stromů AS. - Pouze z důvodu circular dependency
 */
import * as types from 'actions/constants/ActionTypes.js';

/**
 * Zapnutí/vypnutí rozšířeného zobrazení stromu AS.
 * {boolean} enable zapnout nebo vypnout rozšířené zobrazení?
 */
export function fundExtendedView(enable) {
    return {
        type: types.FUND_EXTENDED_VIEW,
        enable,
    };
}
