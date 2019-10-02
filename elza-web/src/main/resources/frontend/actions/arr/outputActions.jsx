/**
 * Output related actions
 */

import * as types from '../constants/ActionTypes.js';

 /**
 * Increase output version. Zvýšení probíhá o 1.
 * @param fundVersionId verze AS
 * @param outputId id výstupu
 * @param outputVersion jaké verze se má povýšit - pokud již bude mít jinou verzi, nebude se zvyšovat
 */
export function outputIncreaseNodeVersion(fundVersionId, outputId, outputVersion) {
    return {
            type: types.OUTPUT_INCREASE_VERSION,
            versionId: fundVersionId,
            outputId,
            outputVersion
    }
}
