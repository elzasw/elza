import * as types from 'actions/constants/ActionTypes.js';
import fundTree from './fundTree';
const initialState = {
  fundTreeCopy: fundTree(undefined, { type: '' }),
  fund: null,
  versionId: null
};

export default function globalFundTree(state = initialState, action = {}) {
  switch (action.type) {
    case types.SELECT_FUND_GLOBAL: {
      return {
        fund: action.fund,
        versionId: action.versionId,
        fundTreeCopy: initialState.fundTreeCopy
      };
    }
    case types.FUND_FUND_TREE_INVALIDATE: {
      return initialState;
    }
  }

  return {
    ...state,
    fundTreeCopy: {
      ...fundTree(state.fundTreeCopy, action),
      multipleSelection: true
    }
  };
}
