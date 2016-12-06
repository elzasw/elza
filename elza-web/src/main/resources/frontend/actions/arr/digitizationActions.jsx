/**
 * Akce pro digitalizaci.
 */

import * as SimpleListActions from "shared/list/simple/SimpleListActions";
import {WebApi} from 'actions/index.jsx';

export function fetchFundListIfNeeded(versionId) {
    return (dispatch, getState) => {
        dispatch(SimpleListActions.fetchIfNeeded("digitizationRequestList", versionId, (parent, filter) => {
            return WebApi.getDigitizationRequests(versionId)
                .then(json => ({ rows: json, count: 0 }));
        }));
    }
}