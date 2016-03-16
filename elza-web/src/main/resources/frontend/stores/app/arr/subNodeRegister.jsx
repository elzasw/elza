import * as types from 'actions/constants/ActionTypes';
import {i18n} from 'components'
import {indexById} from 'stores/app/utils.jsx'

function getLoc(state, index) {
    return {link: state.formData.nodeRegisters[index]};
}

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    data: null,
    getLoc: getLoc
}

function updateFormData(state) {

    var formData = {nodeRegisters: [], node: state.data.node};

    state.data.nodeRegisters.forEach(item => {

        var formItem = {
            ...item,
            prevValue: item.value,
            hasFocus: false,
            touched: false,
            visited: false,
            error: {hasError:false}
        }

        formData.nodeRegisters.push(formItem);
    });

    state.formData = formData;
}

export default function subNodeRegister(state = initialState, action) {
    switch (action.type) {

        case types.FUND_SUB_NODE_REGISTER_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })

        case types.FUND_SUB_NODE_REGISTER_RECEIVE:
            var result = Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                dirty: false,
                data: action.data,
            });

            updateFormData(result);

            return result;

        case types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE:

            var loc = getLoc(state, action.index);

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

            state.formData.nodeRegisters.forEach(link => link.node = action.data.node);
            state.formData.node = action.data.node;
            state.formData = {...state.formData};
            return {...state};

        case types.FUND_SUB_NODE_REGISTER_VALUE_DELETE:

            state.formData.nodeRegisters = [
                ...state.formData.nodeRegisters.slice(0, action.index),
                ...state.formData.nodeRegisters.slice(action.index + 1)
            ];

            return {...state};

        case types.FUND_SUB_NODE_REGISTER_VALUE_CHANGE:
            var link = state.formData.nodeRegisters[action.index];
            link.value = action.value;
            link.touched = true;
            state.formData.nodeRegisters[action.index] = link;
            state.formData.nodeRegisters = [...state.formData.nodeRegisters];
            return {...state};

        case types.FUND_SUB_NODE_FORM_VALUE_BLUR:

            var link = state.formData.nodeRegisters[action.index];

            link.visited = false;
            link.hasFocus = false;

            state.formData.nodeRegisters[action.index] = data;

            return {...state}

        case types.FUND_SUB_NODE_FORM_VALUE_FOCUS:

            var link = state.formData.nodeRegisters[action.index];

            link.visited = true;
            link.hasFocus = true;
            link.hasFocus = true;
            link.hasFocus = true;

            state.formData.nodeRegisters[action.index] = data;

            return {...state}

        case types.FUND_SUB_NODE_REGISTER_VALUE_ADD:

            var formItem = {
                node: state.formData.node,
                value: null,
                prevValue: null,
                hasFocus: false,
                touched: false,
                visited: false,
                error: {hasError:false}
            }

            state.formData.nodeRegisters.push(formItem);

            return {...state};
        case types.CHANGE_NODES:
            return {...state, dirty: true}

        default:
            return state
    }
}

