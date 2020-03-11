import StoreUtils from './StoreUtils';
import {indexById} from 'stores/app/utils.jsx';

/**
 * Provede zpracování store, který může obsahovat area a vrátí upraený store.
 * Musí být voláno jen s platnou areou.
 * @param state state
 * @param action akce
 * @return {Object} upravený store
 */
export default function processAreaStores(state, action) {
    const areaItems = action.area.split(".");

    // Variabilita zpracování area i do fund store na základě předaných dat
    if (areaItems[0].startsWith("fund[")) {
        const versionId = areaItems[0].substring("fund[".length, areaItems[0].indexOf("]"));
        const index = indexById(state.funds, versionId, "versionId");
        if (index !== null) {
            const newAction = {
                ...action,
                area: areaItems.slice(1).join(".")
            };
            return {
                ...state,
                funds: [
                    ...state.funds.slice(0, index),
                    StoreUtils.processConcreteStore(state.funds[index], newAction),
                    ...state.funds.slice(index + 1)
                ]
            }
        } else {
            return state;
        }
    } else if (state[areaItems[0]]) {   // standardní zpracování
        const newAction = {
            ...action,
            area: areaItems.slice(1).join(".")
        };
        return StoreUtils.processStore(areaItems[0], state, newAction);
    } else {
        return state;
    }
}
