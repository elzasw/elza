import * as types from 'actions/constants/ActionTypes.js';
import {i18n} from 'components/shared';
import {indexById} from 'stores/app/utils.jsx'


const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    node: null,
    data: null,
    currentDataKey: null,
};

export default function subNodeRegister(state = initialState, action = {}) {
    switch (action.type) {
        // List Operation
        case types.FUND_SUB_NODE_REGISTER_REQUEST:
            return {
                ...state,
                currentDataKey: action.dataKey
            };
        case types.FUND_SUB_NODE_REGISTER_RECEIVE:
            return {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                node: action.data.node,
                data: action.data.nodeRegisters.map(item => ({
                    ...item,
                    prevValue: item.value,
                    hasFocus: false,
                    saving: false,
                    touched: false,
                    visited: false,
                    error: {hasError: false}
                }))
            };
        // ------ Values ------
        case types.FUND_SUB_NODE_REGISTER_VALUE_ADD:
            return {
                ...state,
                data: [
                    ...state.data,
                    {
                        //node: {...state.formData.node},
                        //nodeId: state.formData.node.id,
                        id: null,
                        value: null,
                        prevValue: null,
                        hasFocus: false,
                        touched: false,
                        visited: false,
                        saving: false,
                        error: {hasError:false}
                    }
                ]
            };

        case types.FUND_SUB_NODE_FORM_VALUE_FOCUS:{
            const register = state.data[action.index];
            if (register) {
                return {
                    ...state,
                    data: [
                        ...state.data.slice(0, action.index),
                        {
                            ...register,
                            visited: true,
                            hasFocus: true
                        },
                        ...state.data.slice(action.index + 1)
                    ]
                };
            }

            if (!register) {
                console.warn('Sub Node Register - On action "VALUE_BLUR" index ' + action.index + ' not found!');
            }

            return state;
        }
        case types.FUND_SUB_NODE_REGISTER_VALUE_CHANGE:{
            const register = state.data[action.index];
            if (register) {
                return {
                    ...state,
                    data: [
                        ...state.data.slice(0, action.index),
                        {
                            ...register,
                            value: action.record.id,
                            record: action.record,
                            touched: true
                        },
                        ...state.data.slice(action.index + 1)
                    ]
                };
            }

            if (!register) {
                console.warn('Sub Node Register - On action "VALUE_UPDATE" index ' + action.index + ' not found!');
            }

            return state;
        }
        case types.FUND_SUB_NODE_FORM_VALUE_BLUR:{
            const register = state.data[action.index];
            if (register) {
                return {
                    ...state,
                    data: [
                        ...state.data.slice(0, action.index),
                        {
                            ...register,
                            visited: false,
                            hasFocus: false
                        },
                        ...state.data.slice(action.index + 1)
                    ]
                };
            }

            if (!register) {
                console.warn('Sub Node Register - On action "VALUE_BLUR" index ' + action.index + ' not found!');
            }

            return state;
        }
        case types.FUND_SUB_NODE_REGISTER_VALUE_SAVING:{
            const register = state.data[action.index];
            if (register) {
                return {
                    ...state,
                    data: [
                        ...state.data.slice(0, action.index),
                        {
                            ...register,
                            saving: true
                        },
                        ...state.data.slice(action.index + 1)
                    ]
                };
            }

            if (!register) {
                console.warn('Sub Node Register - On action "VALUE_SAVING" index ' + action.index + ' not found!');
            }

            return state;
        }
        case types.FUND_SUB_NODE_REGISTER_VALUE_DELETE: {
            const register = state.data[action.index];

            if (register && !register.id && !register.value) {
                return {
                    ...state,
                    data: [
                        ...state.data.slice(0, action.index),
                        ...state.data.slice(action.index + 1),

                    ]
                }
            }

            if (!register) {
                console.warn('Sub Node Register - On action "VALUE_DELETE" index ' + action.index + ' not found!')
            } else if (register.id) {
                console.warn('Sub Node Register - Invalid action "VALUE_DELETE" on index ' + action.index + ' already has ID - you have to delete it on server first!');
            } else if (register.value) {
                console.warn('Sub Node Register - Invalid action "VALUE_DELETE" on index ' + action.index + ' invalid state - register have a value - expect save (only blank can be deleted)!');
            }
            return state;
        }

        // ----- Server operation ------
        case types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE_CREATE:{
            const register = state.data[action.index];
            if (register && !register.id && register.saving) {
                return {
                    ...state,
                    node: action.data.node,
                    data: [
                        ...state.data.slice(0, action.index),
                        {
                            ...register,
                            ...action.data,
                            prevValue: register.value,
                            saving: false
                        },
                        ...state.data.slice(action.index + 1),

                    ]
                }
            }

            if (!register) {
                console.warn('Sub Node Register - On action "CREATE" index ' + action.index + ' not found!')
            } else if (register.id) {
                console.warn('Sub Node Register - Invalid action "CREATE" on index ' + action.index + ' already has ID!');
            } else if (register.saving) {
                console.warn('Sub Node Register - Invalid action "CREATE" on index ' + action.index + ' it is not in saving state!');
            }

            return state;
        }
        case types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE_UPDATE:{
            const register = state.data[action.index];
            if (register && register.id && register.saving) {
                return {
                    ...state,
                    node: action.data.node,
                    data: [
                        ...state.data.slice(0, action.index),
                        {
                            ...register,
                            ...action.data,
                            prevValue: register.value,
                            saving:false
                        },
                        ...state.data.slice(action.index + 1),

                    ]
                }
            }

            if (!register) {
                console.warn('Sub Node Register - On action "UPDATE" index ' + action.index + ' not found!')
            } else if (register.id) {
                console.warn('Sub Node Register - Invalid action "UPDATE" on index ' + action.index + ' it does not have an ID!');
            } else if (register.saving) {
                console.warn('Sub Node Register - Invalid action "UPDATE" on index ' + action.index + ' it is not in saving state!');
            }

            return state;
        }
        case types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE_DELETE:{
            const register = state.data[action.index];
            if (register && register.saving && register.id) {
                return {
                    ...state,
                    node: action.data.node,
                    data: [
                        ...state.data.slice(0, action.index),
                        ...state.data.slice(action.index + 1),
                    ]
                }
            }

            if (!register) {
                console.warn('Sub Node Register - On action "DELETE" index ' + action.index + ' not found!')
            } else if (register.saving) {
                console.warn('Sub Node Register - Invalid action "DELETE" on index ' + action.index + ' it is not in saving state!');
            } else if (!register.id && register.value) {
                console.warn('Sub Node Register - Invalid action "DELETE" on index ' + action.index + ' doesnt have an ID but have a value!');
            }

            return state;
        }

        // ------ WebSocket -----
        case types.CHANGE_NODES:
            return {
                ...state,currentDataKey: false
            };
        default:
            return state
    }
}
