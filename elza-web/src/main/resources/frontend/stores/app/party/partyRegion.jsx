import * as types from 'actions/constants/ActionTypes.js';

import {panel} from './../arr/panel.jsx'
import {consolidateState} from 'components/Utils.jsx'

const initialState = {
    dirty: false,
    dirtySearch: false,
    isFetchingSearch: false,
    fetchedSearch: false,
    isFetchingDetail: false,
    fetchedDetail: false,
    filterText: "",
    items: [],
    panel: panel(),
    selectedPartyID : null,
    selectedPartyData: null,
    partyTypes: [],
    gregorianCalendarId: 1,         // id gregoriánského kalendáře - TODO: potřeba ho dovypočíst
}

export default function partyRegion(state = initialState, action) {
    switch (action.type) {
        case types.STORE_LOAD:
            if (!action.partyRegion) {
                return state;
            }

            return {
                ...state,
                isFetchingSearch: false,
                fetchedSearch: false,
                isFetchingDetail: false,
                fetchedDetail: false,
                items: [],
                selectedPartyData: null,
                partyTypes: [],
                ...action.partyRegion
            }
        case types.STORE_SAVE:
            const {selectedPartyData, filterText, selectedPartyID} = state;

            var _info
            if (selectedPartyData && selectedPartyData.partyId === selectedPartyID) {
                _info = {name: selectedPartyData.record.record, desc: selectedPartyData.record.characteristics}
            } else {
                _info = null
            }

            return {
                _info,
                filterText,
                selectedPartyID
            }
        case types.PARTY_FIND_PARTY_REQUEST:
            return Object.assign({}, state, {
                isFetchingSearch: true,
            })
        case types.PARTY_FIND_PARTY_RECEIVE:
            return Object.assign({}, state, {
                dirtySearch: false,
                isFetchingSearch: false,
                fetchedSearch: true,
                items: action.items,
                filterText: action.filterText,
            })
        case types.PARTY_DETAIL_REQUEST:
            return Object.assign({}, state, {
                isFetchingDetail: true,
            })
        case types.PARTY_DETAIL_RECEIVE:
            return Object.assign({}, state, {
                dirty: false,
                isFetchingDetail: false,
                fetchedDetail: true,
                selectedPartyData: action.selectedPartyData,
                selectedPartyID: action.selectedPartyID,
            })
        case types.PARTY_DETAIL_CLEAR:
            return Object.assign({}, state, {
                selectedPartyData: null,
                selectedPartyID: null,
            })
        case types.PARTY_UPDATED:
            var isDirty = action.partyId === state.selectedPartyID;
            console.log("partyUpdate", isDirty, state);

            if (isDirty) {
                return Object.assign({}, state, {
                    dirty: isDirty
                })
            }

            return state;

        case types.CHANGE_PARTY_DELETED:
            var isDirty = false;
            var selectedPartyID = state.selectedPartyID == action.partyId ? null : state.selectedPartyID;

            if (state.items) {
                state.items.forEach(item => {
                    if (item.partyId == action.partyId) {
                        isDirty = true;
                    }
                });
            }

            if (isDirty) {
                return Object.assign({}, state, {
                    dirty: isDirty,
                    isFetchingSearch: false,
                    fetchedSearch: false,
                    selectedPartyID: selectedPartyID
                })
            }

            return state;

        case types.PARTY_SELECT:
            var result = {...state};
            result.panel = panel(result.panel, action);
            result.selectedPartyID = action.partyId;
            result.dirty = true;
            result.dirtySearch = true;
            result.filterText = "";
            return consolidateState(state, result);

        case types.PARTY_ARR_RESET:
            var result = {...state};
            result.panel = panel(result.panel, action);
            result.selectedPartyID = null;
            result.selectedPartyData = null;
            result.fetchedSearch = false;
            result.fetchedDetail = false;
            result.filterText = "";
            return consolidateState(state, result);

            case types.CHANGE_REGISTRY_UPDATE:
                if(state.items){
                    var recordIds = {};
                    for(var i = 0; i < action.changedIds.length;i++){
                        recordIds[action.changedIds[i]] = 1;
                    }
                    //projdeme všechny zobrazené osoby a pokud je zobreno aktualizovane heslo,musíme přenačíst seznam
                    for(var i = 0; i < state.items.length;i++){
                        if(recordIds[state.items[i].record.recordId] === 1){
                            return Object.assign({}, state, {
                                dirtySearch: true
                            });
                        }
                    }
                }
            return state;


        default:
            return state
    }
}
