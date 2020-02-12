/**
 * Akce pro storeSuggest
 */
import * as list from './../list'
import * as SharedActions from "./../shared/SharedActions"


export function fetchListIfNeeded(area, apiCall) {
    return (dispatch, getState) => {
        dispatch(SharedActions.initIfNeeded(area, list.SimpleListReducer));

        dispatch(list.SimpleListActions.fetchIfNeeded(area, null, apiCall));
    }
}
