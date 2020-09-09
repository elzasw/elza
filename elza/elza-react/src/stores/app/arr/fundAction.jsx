import * as types from 'actions/constants/ActionTypes';

const initialState = {
    isFormVisible: false,
    config: {
        isFetching: false,
        fetched: false,
        currentDataKey: null,
        data: null,
    },
    form: {
        nodes: [],
        code: '',
    },
    list: {
        isFetching: false,
        fetched: false,
        currentDataKey: null,
        data: null,
    },
    detail: {
        isFetching: false,
        fetched: false,
        currentDataKey: null,
        data: null,
    },
};

export default function fundAction(state = initialState, action = {}) {
    switch (action.type) {
        case types.CHANGE_FUND_ACTION: {
            const newState = {
                ...state,
                list: {
                    ...state.list,
                    currentDataKey: null,
                },
            };

            if (state.detail.data && state.detail.data.id === action.id) {
                state.detail.data.currentDataKey = null;
            }
            return newState;
        }
        case types.FUND_ACTION_ACTION_SELECT: {
            return {
                ...state,
                isFormVisible: false,
                detail: {
                    ...state.detail,
                    currentDataKey: action.dataKey,
                },
            };
        }
        case types.FUND_ACTION_ACTION_DETAIL_REQUEST: {
            return {
                ...state,
                detail: {
                    ...state.detail,
                    currentDataKey: action.dataKey,
                    isFetching: true,
                },
            };
        }
        case types.FUND_ACTION_ACTION_DETAIL_RECEIVE: {
            if (state.detail.currentDataKey !== action.dataKey) {
                return state;
            }
            return {
                ...state,
                detail: {
                    ...state.detail,
                    isFetching: false,
                    fetched: true,
                    data: action.data,
                },
            };
        }
        case types.FUND_ACTION_LIST_REQUEST: {
            return {
                ...state,
                list: {
                    ...state.list,
                    currentDataKey: action.dataKey,
                    isFetching: true,
                },
            };
        }
        case types.FUND_ACTION_LIST_RECEIVE: {
            if (state.list.currentDataKey !== action.dataKey) {
                return state;
            }
            return {
                ...state,
                list: {
                    ...state.list,
                    isFetching: false,
                    fetched: true,
                    data: action.data,
                },
            };
        }
        case types.FUND_ACTION_CONFIG_REQUEST: {
            return {
                ...state,
                config: {
                    ...state.config,
                    currentDataKey: action.dataKey,
                    isFetching: true,
                },
            };
        }
        case types.FUND_ACTION_CONFIG_RECEIVE: {
            if (state.config.currentDataKey !== action.dataKey) {
                return state;
            }
            return {
                ...state,
                config: {
                    ...state.config,
                    isFetching: false,
                    fetched: true,
                    data: action.data,
                },
            };
        }
        case types.FUND_ACTION_FORM_SHOW: {
            return {
                ...state,
                isFormVisible: true,
                detail: {
                    ...state.detail,
                    currentDataKey: null,
                },
            };
        }
        case types.FUND_ACTION_FORM_RESET: {
            return {
                ...state,
                form: initialState.form,
            };
        }
        case types.FUND_ACTION_FORM_CHANGE: {
            return {
                ...state,
                form: {
                    ...state.form,
                    ...action.data,
                },
            };
        }
        case types.FUND_ACTION_FORM_SUBMIT: {
            return {
                ...state,
                isFormVisible: false,
                form: initialState.form,
            };
        }

        case types.FUND_INVALID: {
            return {
                ...state,
                list: {
                    ...state.list,
                    currentDataKey: null,
                },
                detail: {
                    ...state.detail,
                    currentDataKey: null,
                },
            };
        }

        default:
            return state;
    }
}
