import * as types from 'actions/constants/ActionTypes.js';

import {detail, list, utils} from "shared";
import DetailReducer from "shared/detail/DetailReducer";
import SimpleListReducer from "shared/list/simple/SimpleListReducer";
import processAreaStores from "shared/utils/processAreaStores";
import registryList from "stores/app/registry/registryList"
import registryDetail from "stores/app/registry/registryDetail"

const initialState = {
    partyList: SimpleListReducer(undefined, undefined, {filter:{text:null, type:null, itemSpecId: null, scopeId: null, from: 0, excludeInvalid: true}}),
    partyDetail: DetailReducer(),
    registryDetail: registryDetail(),
    preparedRequestList: SimpleListReducer(),   // seznam neodeslaných požadavků - sdíleno pro celou aplikaci
    requestInQueueList: SimpleListReducer(),   // seznam požadavků ve frontě
    regExtSystemList: SimpleListReducer(),   // seznam externích systémů
    extSystemDetail: DetailReducer(),
    extSystemList: SimpleListReducer(),   // seznam externích systémů
    registryList: registryList(undefined, undefined, {filter:{text: null, registryParentId: null, registryTypeId: null, versionId: null, itemSpecId: null, parents: [], typesToRoot: null, scopeId: null, from: 0, excludeInvalid: true}}),
};

export default function app(state = initialState, action) {
    if (action.area && typeof action.area  === "string") {
        return processAreaStores(state, action);
    }

    if (action.type == types.STORE_SAVE) {
        return {
            partyList: SimpleListReducer(state.partyList, action),
            partyDetail: DetailReducer(state.partyDetail, action),
            registryDetail: DetailReducer(state.registryDetail, action)
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

        if (action.registryDetail) {
            newState.registryDetail = DetailReducer(state.registryDetail, {...action.registryDetail, type: types.STORE_LOAD, store: "app"});
        }

        return newState;
    }

    return state;
}
