import * as types from 'actions/constants/ActionTypes.js';

import {detail, list, utils} from "shared";
import DetailReducer from "shared/detail/DetailReducer";
import SimpleListReducer from "shared/list/simple/SimpleListReducer";
import processAreaStores from "shared/utils/processAreaStores";
function getDataKey() { return this.filter.type+"-"+this.filter.text };
const initialState = {
    partyList: SimpleListReducer(undefined, undefined, {getDataKey, filter:{text:null, type:null}}),
    partyDetail: DetailReducer(),
};

export default function app(state = initialState, action) {
    if (action.area && typeof action.area  === "string") {
        return processAreaStores(state, action);
    }

    if (action.type == types.STORE_SAVE) {
        return {
            partyList: SimpleListReducer(state.partyList, action),
            partyDetail: DetailReducer(state.partyDetail, action)
        }
    }

    if (action.type == types.STORE_LOAD && action.store == "app") {
        return {
            partyList: SimpleListReducer(state.partyList, action.partyList),
            partyDetail: DetailReducer(state.partyDetail, action.partyDetail)
        }
    }

    return state;
}
