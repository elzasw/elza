import StoreUtils from "./StoreUtils";

/**
 * Provede zpracování store, který může obsahovat area a vrátí upraený store.
 * Musí být voláno jen s platnou areou.
 * @param state state
 * @param action akce
 * @return upravený store
 */
export default function processAreaStores(state, action) {
    const areaItems = action.area.split(".");
    if (state[areaItems[0]]) {
        const newAction = {
            ...action,
            area: areaItems.slice(1).join(".")
        };
        return StoreUtils.processStore(areaItems[0], state, newAction);
    } else {
        return state;
    }
}
