import * as types from 'actions/constants/actionTypes';

const initialState = {
    scopes:[],
}

export default function scopesData(state = initialState, action = {}) {
    console.log('tady je moje action', action);
    switch (action.type) {
        case types.REF_SCOPES_TYPES_RECEIVE:
            var exist = false;
            state.scopes.map(scope => {
                if (scope.versionId === action.data.versionId){
                    exist = true;
                }
            })
            if (!exist){
                state.scopes.push({versionId: action.data.versionId, scopes: action.data});
            }
            return Object.assign({}, state);

        default:
            return state
    }
}
