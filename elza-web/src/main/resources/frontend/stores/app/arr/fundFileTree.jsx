import * as types from 'actions/constants/ActionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    items: []
}

export default function fundFileTree(state = initialState, action) {
    switch (action.type) {
        case types.FUND_FUND_FILE_TREE_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.FUND_FUND_FILE_TREE_RECEIVE:
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
                                createDate: ver.createDate,
                                lockDate: ver.lockDate,
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
