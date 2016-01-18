import * as types from 'actions/constants/actionTypes';
import {indexById} from 'stores/app/utils.jsx'

function getLoc(state, valueLocation) {
    var descItemGroup = state.formData.descItemGroups[valueLocation.descItemGroupIndex];
    var descItemType = descItemGroup.descItemTypes[valueLocation.descItemTypeIndex];
    var descItem;
    if (typeof valueLocation.descItemIndex !== 'undefined') {
        descItem = descItemType.descItems[valueLocation.descItemIndex];
    }

    return {
        descItemGroup,
        descItemType,
        descItem
    }
}

const initialState = {
    isFetching: false,
    fetched: false,
    versionId: null,
    nodeId: null,
    data: null,
    getLoc: getLoc
}


function updateFormData(state, rulDataTypes) {
    var formData = {
        descItemGroups: state.data.descItemGroups.map(group => {
            var resultGroup = {
                ...group,
                hasFocus: false
            };

            resultGroup.descItemTypes = group.descItemTypes.map(descItemType => {
                var resultDescItemType = {
                    ...descItemType,
                    hasFocus: false,
                    multipleValue: true,
                    descItems: descItemType.descItems.map(descItem => {
                        return Object.assign(
                            {},
                            descItem,
                            {
                                prevDescItemSpecId: descItem.descItemSpecId,
                                prevValue: descItem.value,
                                hasFocus: false,
                                touched: false,
                                visited: false
                            }
                        )
                    })
                }

                return resultDescItemType;
            });

            return resultGroup;
        })
    }

    var descItemTypeInfos = [];
    state.data.descItemTypeGroups.forEach(descItemGroup => {
        descItemGroup.descItemTypes.forEach(descItemType => {
            var rulDataType = rulDataTypes.items[indexById(rulDataTypes.items, descItemType.dataTypeId)];

            var descItemTypeInfo = Object.assign({}, descItemType, { descItemGroup: descItemGroup, rulDataType: rulDataType});
            descItemTypeInfos.push(descItemTypeInfo);
        });
    });

    state.formData = formData;
    state.descItemTypeInfos = descItemTypeInfos;
}

function getDescItemType(descItemTypeInfo) {
    switch (descItemTypeInfo.rulDataType.code) {
        case 'TEXT':
            return '.ArrDescItemTextVO';
        break;
        case 'STRING':
            return '.ArrDescItemStringVO';
        break;
        case 'INT':
            return '.ArrDescItemIntVO';
        break;
        case 'COORDINATES':
            return '.ArrDescItemCoordinatesVO';
        break;
        case 'DECIMAL':
            return '.ArrDescItemDecimalVO';
        break;
        case 'PARTY_REF':
            return '.ArrDescItemPartyRefVO';
        break;
        case 'RECORD_REF':
            return '.ArrDescItemRecordRefVO';
        break;
        case 'PACKET_REF':
            return '.ArrDescItemPacketVO';
        break;
        case 'ENUM':
            return '.ArrDescItemEnumVO';
        break;
        case 'FORMATTED_TEXT':
            return '.ArrDescItemFormattedTextVO';
        break;
        case 'UNITDATE':
            return '.ArrDescItemUnitdateVO';
        break;
        case 'UNITID':
            return '.ArrDescItemUnitidVO';
        break;
        default:
            console.error("Unsupported data type", descItemTypeInfo.rulDataType);
            return null;
    }
}

function validate(descItem, descItemTypeInfo) {
    var errors = [];
    var error;

    // Specifikace
    if (descItemTypeInfo.useSpecification) {
        if (typeof descItem.descItemSpecId == 'undefined' || descItem.descItemSpecId == "") {
            errors.push('Specifikace je povinná');
        }
    }

    // Hodnota
    switch (descItemTypeInfo.rulDataType) {
        case 'TEXT':
        break;
        case 'STRING':
        break;
        case 'INT':
        break;
        case 'COORDINATES':
        break;
        case 'DECIMAL':
        break;
        default:
    }

    if (errors.length > 0) {
        error = errors.map((err, index) => {
            if (index + 1 < errors.length) {
                return err + ", ";
            } else {
                return err;
            }
        });
    }

    return error;
}

export default function subNodeForm(state = initialState, action) {
    switch (action.type) {
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE:
            var loc = getLoc(state, action.valueLocation);
            var descItemTypeInfo = state.descItemTypeInfos[indexById(state.descItemTypeInfos, loc.descItemType.id)];
            
            loc.descItem.value = action.value;
            loc.descItem.touched = true;
            loc.descItem.error = validate(loc.descItem, descItemTypeInfo);

            state.formData = {...state.formData};
            return state;
        case types.FA_SUB_NODE_FORM_VALUE_CHANGE_SPEC:
            var loc = getLoc(state, action.valueLocation);
            var descItemTypeInfo = state.descItemTypeInfos[indexById(state.descItemTypeInfos, loc.descItemType.id)];
            
            loc.descItem.descItemSpecId = action.value;
            loc.descItem.touched = true;
            loc.descItem.error = validate(loc.descItem, descItemTypeInfo);

            state.formData = {...state.formData};
            return state;
        case types.FA_SUB_NODE_FORM_VALUE_BLUR:
            var loc = getLoc(state, action.valueLocation);

            loc.descItem.hasFocus = false;
            loc.descItemType.hasFocus = false;
            loc.descItemGroup.hasFocus = false;
            
            state.formData = {...state.formData};
            return state;
        case types.FA_SUB_NODE_FORM_VALUE_FOCUS:
            var loc = getLoc(state, action.valueLocation);

            loc.descItem.visited = true;
            loc.descItem.hasFocus = true;
            loc.descItemType.hasFocus = true;
            loc.descItemGroup.hasFocus = true;
            
            state.formData = {...state.formData};
            return state;
        case types.FA_SUB_NODE_FORM_VALUE_ADD:
            var loc = getLoc(state, action.valueLocation);

            var descItemTypeInfo = state.descItemTypeInfos[indexById(state.descItemTypeInfos, loc.descItemType.id)];

            var descItem = {
                '@type': getDescItemType(descItemTypeInfo),
                prevValue: null,
                hasFocus: false,
                touched: false,
                visited: false,
                value: ''
            };

            loc.descItemType.descItems = [...loc.descItemType.descItems, descItem];
            
            state.formData = {...state.formData};
            return state;
        case types.FA_SUB_NODE_FORM_VALUE_RESPONSE:
            var loc = getLoc(state, action.valueLocation);

            state.data.node = action.descItemResult.node;

            switch (action.operationType) {
                case 'DELETE':
                    break;
                case 'UPDATE':
                    loc.descItem.descItemObjectId = action.descItemResult.descItem.descItemObjectId;
                    break;
                case 'CREATE':
                    loc.descItem.descItemObjectId = action.descItemResult.descItem.descItemObjectId;
                    break;
            }

            state.formData = {...state.formData};
            return state;
        case types.FA_SUB_NODE_FORM_VALUE_DELETE:
            var loc = getLoc(state, action.valueLocation);

            loc.descItemType.descItems = [
                ...loc.descItemType.descItems.slice(0, action.valueLocation.descItemIndex),
                ...loc.descItemType.descItems.slice(action.valueLocation.descItemIndex + 1)
            ];
            
            state.formData = {...state.formData};
            return state;
        case types.FA_SUB_NODE_FORM_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.FA_SUB_NODE_FORM_RECEIVE:
            var result = Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                versionId: action.versionId,
                nodeId: action.nodeId,
                data: action.data,
            })

            updateFormData(result, action.rulDataTypes);

            return result;
        default:
            return state
    }
}

