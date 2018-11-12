import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'
import {i18n} from 'components/shared';

const initialState = {
    filterText: null,
    filterResult: false,
    searchedIDs: [],
    count: 0,
}

export default function fundModal(state = initialState, action = {}) {
    switch (action.type) {
        case types.FUND_FUND_MODAL_FULLTEXT_CHANGE:
            return {
                ...state,
                filterText: action.filterText,
                filterResult: false,
                searchedIDs: [],
            }
        case types.FUND_FUND_TREE_FULLTEXT_RESULT:
            if (state.filterText === action.filterText) {
                var searchedIds = [];
                action.searchedData.forEach(data => {
                    searchedIds.push(data.id);
                })

                return {
                    ...state,
                    filterResult: !action.clearFilter,
                    searchedIDs: searchedIDs,
                    count: action.searchData.count,
                }
            } else {
                return state;
            }

        default:
            return state
    }
}
