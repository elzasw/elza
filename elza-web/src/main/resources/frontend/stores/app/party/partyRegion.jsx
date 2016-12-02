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
    itemsCount: 0,
    panel: panel(),
    selectedPartyID : null,
    selectedPartyData: null,
    partyTypes: [],
    gregorianCalendarId: 1,         // id gregoriánského kalendáře - TODO: potřeba ho dovypočíst
}

export default function partyRegion(state = initialState, action) {
    switch (action.type) {
        //case types.LOGOUT:
        case types.LOGIN_SUCCESS: {
            if (action.reset) {
                return initialState;
            }
            return state;
        }
        case types.STORE_LOAD:{
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
                itemsCount: 0,
                selectedPartyData: null,
                partyTypes: [],
                ...action.partyRegion
            };
        }
        case types.STORE_SAVE:{
            const {selectedPartyData, filterText, selectedPartyID} = state;

            let _info
            if (selectedPartyData && selectedPartyData.partyId === selectedPartyID) {
                _info = {name: selectedPartyData.record.record, desc: selectedPartyData.record.characteristics}
            } else {
                _info = null
            }

            return {
                _info,
                filterText,
                selectedPartyID
            };
        }
        case types.PARTY_FIND_PARTY_REQUEST:
            return {
                ...state,
                isFetchingSearch: true,
            };
        case types.PARTY_FIND_PARTY_RECEIVE:
            return {
                ...state,
                dirtySearch: false,
                isFetchingSearch: false,
                fetchedSearch: true,
                items: action.items,
                itemsCount: action.itemsCount,
                filterText: action.filterText,
            };
        case types.PARTY_DETAIL_REQUEST:
            return {
                ...state,
                isFetchingDetail: true,
            };
        case types.PARTY_DETAIL_RECEIVE:
            return {
                ...state,
                dirty: false,
                isFetchingDetail: false,
                fetchedDetail: true,
                selectedPartyData: action.selectedPartyData,
                selectedPartyID: action.selectedPartyID,
            };
        case types.PARTY_DETAIL_CLEAR:
            return {
                ...state,
                dirty: false,
                isFetchingDetail: false,
                fetchedDetail: false,
                selectedPartyData: null,
                selectedPartyID: null,
            };

        case types.PARTY_CREATED: {
            return {
                ...state,
                dirtySearch: true
            }
        }

        case types.PARTY_UPDATED:{
            if (action.partyId === state.selectedPartyID) {
                return {
                    ...state,
                    dirty: true
                }
            }

            return state;
        }
        case types.CHANGE_PARTY_DELETED:{
            if (state.items) {
                for (let item of state.items) {
                    if (item.partyId == action.partyId) {
                        const selectedPartyID = state.selectedPartyID == action.partyId ? null : state.selectedPartyID;
                        const selectedPartyData = state.selectedPartyID == action.partyId ? null : state.selectedPartyData;
                        return {
                            ...state,
                            dirty: true,
                            isFetchingSearch: false,
                            fetchedSearch: false,
                            selectedPartyID,
                            selectedPartyData
                        }
                    }
                }
            }

            return state;
        }
        case types.PARTY_SELECT:{
            const result = {
                ...state,
                panel: panel(state.panel, action),
                selectedPartyID: action.partyId,
                dirty: true,
                dirtySearch: true,
                filterText: "",
            };
            return consolidateState(state, result);
        }
        case types.PARTY_ARR_RESET:{
            const result = {
                ...state,
                panel: panel(state.panel, action),
                selectedPartyID: null,
                selectedPartyData: null,
                fetchedSearch: false,
                fetchedDetail: false,
                filterText: "",
            };
            return consolidateState(state, result);
        }
        case types.CHANGE_REGISTRY_UPDATE:{
            if(state.items) {
                const recordIds = {};
                for(let i = 0; i < action.changedIds.length;i++){
                    recordIds[action.changedIds[i]] = 1;
                }
                //projdeme všechny zobrazené osoby a pokud je zobreno aktualizovane heslo,musíme přenačíst seznam
                for(let i = 0; i < state.items.length;i++){
                    if(recordIds[state.items[i].record.recordId] === 1) {
                        return {
                            ...state,
                            dirtySearch: true
                        };
                    }
                }
            }
            return state;
        }
        default:
            return state
    }
}
