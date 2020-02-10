import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx';

const initialState = {
    data: []
};

export default function structureTypes(state = initialState, action = {}) {
    switch (action.type) {
        case types.REF_STRUCTURE_TYPES_DIRTY:{
            const index = indexById(state.data, action.versionId, 'versionId');
            if (index !== null) {
                return {
                    ...state,
                    data: [
                        ...state.data.slice(0, index),
                        {
                            ...state.data[index],
                            isFetching: false,
                            isDirty: true
                        },
                        ...state.data.slice(index + 1)
                    ]
                }
            }

            return state;
        }
        case types.REF_STRUCTURE_TYPES_FETCHING:
        {
            const index = indexById(state.data, action.versionId, 'versionId');
            if (index !== null) {
                return {
                    ...state,
                    data: [
                        ...state.data.slice(0, index),
                        {
                            ...state.data[index],
                            isFetching: true
                        },
                        ...state.data.slice(index + 1)
                    ]
                }
            }
            return {
                ...state,
                data: [
                    ...state.data,
                    {
                        versionId: action.versionId,
                        isFetching: true,
                        isDirty: true
                    }
                ]
            }
        }
        case types.REF_STRUCTURE_TYPES_RECEIVE:{
            const index = indexById(state.data, action.versionId, 'versionId');
            let toAdd = {
                versionId: action.versionId,
                data: action.data,
                isDirty: false,
                isFetching: false
            };
            if (index === null) {
                return {
                    ...state,
                    data: [
                        ...state.data,
                        toAdd
                    ]
                };
            }
            return {
                ...state,
                data: [
                    ...state.data.slice(0, index),
                    toAdd,
                    ...state.data.slice(index + 1)
                ]
            }
        }
        default:
            return state;
    }
}
