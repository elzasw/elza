import * as types from 'actions/constants/ActionTypes.js';

import {detail, list, utils} from "shared";
import DetailReducer from "shared/detail/DetailReducer";
import SimpleListReducer from "shared/list/simple/SimpleListReducer";
import processAreaStores from "shared/utils/processAreaStores";

function getPartyListDataKey() { return this.filter.type+"-"+this.filter.text };

const initialState = {
    partyList: SimpleListReducer(undefined, undefined, {getPartyListDataKey, filter:{text:null, type:null}}),
    partyDetail: DetailReducer(),
    preparedDigitizationRequestList: SimpleListReducer(),   // seznam neodeslaných požadavků na digitalizaci - sdíleno pro celou aplikaci
    requestInQueueList: SimpleListReducer(),   // seznam požadavků ve frontě
    regExtSystemList: DetailReducer(),   // seznam externích systémů
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
        const newState = {...state};
        if (action.partyDetail) {
            newState.partyDetail = DetailReducer(state.partyDetail, {...action.partyDetail, type: types.STORE_LOAD, store: "app"})
        }

        if (action.partyList) {
            newState.partyList = SimpleListReducer(state.partyList, {...action.partyList, type: types.STORE_LOAD, store: "app"});
        }

        return newState;
    }

    return state;
}
