import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    items: [],
    typeIdMap: {},
};

export default function recordTypes(state = initialState, action = {}) {
    switch (action.type) {
        case types.REF_RECORD_TYPES_REQUEST: {
            return {
                ...state,
                isFetching: true,
            };
        }
        case types.REF_RECORD_TYPES_RECEIVE: {
            prepareApTypeIdMap(action.items, state.typeIdMap);

            return {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                items: action.items,
                lastUpdated: action.receivedAt,
            };
        }
        default:
            return state;
    }
}

function prepareApTypeIdMap(types, map) {
    if (!types) {
        return;
    }
    types.forEach(type => {
        if (map[type.id]) {
            throw new Error(`Unable to create AP type map, duplicate id=${type.id}`);
        }
        map[type.id] = type;
        // call recursive on childern
        if (type.children) {
            prepareApTypeIdMap(type.children, map);
        }
    });
}
