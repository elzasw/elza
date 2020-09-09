import * as types from 'actions/constants/ActionTypes';
import fundTree from './fundTree';

const initialState = {
    fundTreeCopy: fundTree(undefined, {type: ''}),
    fundTreeUsage: fundTree(undefined, {type: ''}),
    fund: null,
    versionId: null,
};

export default function globalFundTree(state = initialState, action = {}) {
    if (action.area === types.FUND_TREE_AREA_USAGE) {
        switch (action.type) {
            case types.FUND_FUND_TREE_INVALIDATE: {
                return initialState.fundTreeUsage;
            }
            default:
                break;
        }
        return {
            fundTreeUsage: {
                ...fundTree(state.fundTreeUsage, action),
                multipleSelection: false,
            },
        };
    }

    if (action.area === types.FUND_TREE_AREA_COPY) {
        switch (action.type) {
            case types.SELECT_FUND_GLOBAL: {
                return {
                    fund: action.fund,
                    versionId: action.versionId,
                    fundTreeCopy: initialState.fundTreeCopy,
                };
            }
            case types.FUND_FUND_TREE_INVALIDATE: {
                return initialState.fundTreeCopy;
            }
            default:
                break;
        }
        return {
            ...state,
            fundTreeCopy: {
                ...fundTree(state.fundTreeCopy, action),
                multipleSelection: true,
            },
        };
    }
    return state;
}
