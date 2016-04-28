/**
 * Akce pro hromadné akce
 */

import * as types from 'actions/constants/ActionTypes';
import {WebApi} from 'actions'

export function isFundActionsAction(action) {
    switch (action) {
        case types.FUND_ACTIONS_ACTION_SELECT:
        case types.FUND_ACTIONS_ACTION_DETAIL_REQUEST:
        case types.FUND_ACTIONS_ACTION_DETAIL_RECEIVE:
            return true;
        default:
            return false;
    }
}