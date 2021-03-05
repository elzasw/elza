import {indexById} from 'stores/app/utils';
import * as types from "../../../actions/constants/ActionTypes";

const initialState = {
    scopes: [],
};

export default function scopesData(state = initialState, action = {}) {
    switch (action.type) {
        case types.REF_SCOPES_TYPES_DIRTY: {
            const index = indexById(state.scopes, action.versionId, 'versionId');
            if (index !== null) {
                return {
                    ...state,
                    scopes: [
                        ...state.scopes.slice(0, index),
                        {
                            ...state.scopes[index],
                            isFetching: false,
                            isDirty: true,
                        },
                        ...state.scopes.slice(index + 1),
                    ],
                };
            }
            return {
                ...state,
                scopes: [
                    ...state.scopes,
                    {
                        isFetching: false,
                        isDirty: true,
                    },
                ],
            };
        }
        case types.REF_SCOPES_TYPES_FETCHING: {
            const index = indexById(state.scopes, action.versionId, 'versionId');
            if (index !== null) {
                return {
                    ...state,
                    scopes: [
                        ...state.scopes.slice(0, index),
                        {
                            ...state.scopes[index],
                            isFetching: true,
                        },
                        ...state.scopes.slice(index + 1),
                    ],
                };
            }
            return {
                ...state,
                scopes: [
                    ...state.scopes,
                    {
                        versionId: action.versionId,
                        isFetching: true,
                        isDirty: true,
                    },
                ],
            };
        }
        case types.REF_SCOPES_TYPES_RECEIVE: {
            const index = indexById(state.scopes, action.versionId, 'versionId');
            let scopeToAdd = {
                versionId: action.versionId,
                scopes: action.data,
                isDirty: false,
                isFetching: false,
            };
            if (index === null) {
                return {
                    ...state,
                    scopes: [...state.scopes, scopeToAdd],
                };
            }
            return {
                ...state,
                scopes: [...state.scopes.slice(0, index), scopeToAdd, ...state.scopes.slice(index + 1)],
            };
        }
        default:
            return state;
    }
}
