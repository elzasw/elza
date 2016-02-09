import * as types from 'actions/constants/ActionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    items: []
}

export default function faFileTree(state = initialState, action) {
    switch (action.type) {
        case types.FA_FA_FILE_TREE_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.FA_FA_FILE_TREE_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                items: action.items.map(item => {
                    return {
                        ...item,
                        createDate: new Date(item.createDate),
                        versions: item.versions.map(ver => {
                            return {
                                ...ver,
                                createDate: new Date(ver.createDate),
                                lockDate: ver.lockDate ? new Date(ver.lockDate) : null,
                            }
                        })
                    }
                }),
                lastUpdated: action.receivedAt
            })
        default:
            return state
    }
}
