import * as types from 'actions/constants/ActionTypes.js';
import {i18n} from 'components/index.jsx';
import {indexById} from 'stores/app/utils.jsx'

const getLoc = (state, index)  => ({link: {...state.formData.nodeRegisters[index]}})

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    //data: null,
    formData: {
        node: null,
        nodeRegisters: null
    },
    getLoc
};

export default function subNodeRegister(state = initialState, action = {}) {
    switch (action.type) {
        case types.FUND_SUB_NODE_REGISTER_REQUEST:
            return {
                ...state,
                isFetching: true,
            };
        case types.FUND_SUB_NODE_REGISTER_RECEIVE:
            return {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                //data: action.data,
                formData: {
                    ...state.formData,
                    node: action.data.node,
                    nodeRegisters: action.data.nodeRegisters.map(item => ({
                        ...item,
                        prevValue: item.value,
                        hasFocus: false,
                        touched: false,
                        visited: false,
                        error: {hasError: false}
                    })),
                }
            };
        case types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE:{

            const loc = getLoc(state, action.index);

            switch (action.operationType) {
                case 'DELETE':
                    break;
                case 'UPDATE':
                    loc.link.prevValue = action.data.value;
                    break;
                case 'CREATE':
                    loc.link.id = action.data.id;
                    loc.link.prevValue = action.data.value;
                    loc.link.record = action.data.record;
                    break;
            }

            return {
                ...state,
                formData: {
                    node: action.data.node,
                    nodeRegisters: state.formData.nodeRegisters.map(i => {i.node = action.data.node; return i})
                }
            }
        }
        case types.FUND_SUB_NODE_REGISTER_VALUE_DELETE:
            return {
                ...state,
                formData: {
                    nodeRegisters: [
                        ...state.formData.nodeRegisters.slice(0, action.index),
                        ...state.formData.nodeRegisters.slice(action.index + 1)
                    ]
                }
            };
        case types.FUND_SUB_NODE_REGISTER_VALUE_CHANGE:
            return {
                ...state,
                formData: {
                    nodeRegisters: [
                        ...state.formData.nodeRegisters.slice(0, action.index),
                        {
                            ...state.formData.nodeRegisters[action.index],
                            value: action.record.recordId,
                            record: action.record,
                            touched: true
                        },
                        ...state.formData.nodeRegisters.slice(action.index+1)
                    ]
                }
            };
        case types.FUND_SUB_NODE_FORM_VALUE_BLUR:
            return {
                ...state,
                formData: {
                    nodeRegisters: [
                        ...state.formData.nodeRegisters.slice(0, action.index),
                        {
                            ...state.formData.nodeRegisters[action.index],
                            visited: false,
                            hasFocus: false
                        },
                        ...state.formData.nodeRegisters.slice(action.index+1)
                    ]
                }
            };
        case types.FUND_SUB_NODE_FORM_VALUE_FOCUS:
            return {
                ...state,
                formData: {
                    nodeRegisters: [
                        ...state.formData.nodeRegisters.slice(0, action.index),
                        {
                            ...state.formData.nodeRegisters[action.index],
                            visited: true,
                            hasFocus: true
                        },
                        ...state.formData.nodeRegisters.slice(action.index+1)
                    ]
                }
            };
        case types.FUND_SUB_NODE_REGISTER_VALUE_ADD:{
            const formItem = {
                node: {...state.formData.node},
                nodeId: state.formData.node.id,
                value: null,
                prevValue: null,
                hasFocus: false,
                touched: false,
                visited: false,
                error: {hasError:false}
            };

            return {
                ...state,
                formData: {
                    ...state.formData,
                    nodeRegisters: [
                        ...state.formData.nodeRegisters,
                        formItem
                    ]
                }
            }
        }
        case types.CHANGE_NODES:
            return {...state, dirty: true};
        default:
            return state
    }
}

