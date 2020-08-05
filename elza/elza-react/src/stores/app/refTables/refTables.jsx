import ruleSet from './ruleSet.jsx';
import institutions from './institutions.jsx';
import recordTypes from './recordTypes.jsx';
import rulDataTypes from './rulDataTypes.jsx';
import calendarTypes from './calendarTypes.jsx';
import scopesData from './scopesData.jsx';
import descItemTypes from './descItemTypes.jsx';
import visiblePolicyTypes from './visiblePolicyTypes.jsx';
import outputTypes from './outputTypes.jsx';
import templates from './templates.jsx';
import externalSystems from './externalSystems.jsx';
import structureTypes from './structureTypes';
import DetailReducer from '../../../shared/detail/DetailReducer';
import processAreaStores from '../../../shared/utils/processAreaStores';
import apTypes from './apTypes';
import partTypes from './partTypes';
import * as types from '../../../actions/constants/ActionTypes';

const initialState = {
    ruleSet: ruleSet(),
    institutions: institutions(),
    partTypes: partTypes(),
    apTypes: apTypes(),
    recordTypes: recordTypes(),
    rulDataTypes: rulDataTypes(),
    calendarTypes: calendarTypes(),
    scopesData: scopesData(),
    structureTypes: structureTypes(),
    descItemTypes: descItemTypes(),
    visiblePolicyTypes: visiblePolicyTypes(),
    outputTypes: outputTypes(),
    templates: templates(),
    externalSystems: externalSystems(),
    groups: DetailReducer(),
    eidTypes: DetailReducer(),
    issueStates: DetailReducer(),
    issueTypes: DetailReducer(),
};

export default function refTables(state = initialState, action = {}) {
    if (action.area && typeof action.area === 'string' && action.area.indexOf('refTables.') === 0) {
        let useArea = action.area.substring('refTables.'.length);
        const actionNew = {...action, area: useArea};
        return processAreaStores(state, actionNew);
    }
    switch (action.type) {
        case types.REF_TEMPLATES_REQUEST:
        case types.REF_TEMPLATES_RECEIVE: {
            return {
                ...state,
                templates: templates(state.templates, action),
            };
        }
        case types.REF_AP_TYPES_REQUEST:
        case types.REF_AP_TYPES_RECEIVE: {
            return {
                ...state,
                apTypes: apTypes(state.templates, action),
            };
        }
        case types.REF_RULE_SET_REQUEST:
        case types.REF_RULE_SET_RECEIVE: {
            return {
                ...state,
                ruleSet: ruleSet(state.ruleSet, action),
            };
        }
        case types.REF_INSTITUTIONS_REQUEST:
        case types.REF_INSTITUTIONS_RECEIVE:
        case types.CHANGE_INSTITUTION: {
            return {
                ...state,
                institutions: institutions(state.institutions, action),
            };
        }
        case types.REF_PART_TYPES_REQUEST:
        case types.REF_PART_TYPES_RECEIVE: {
            return {
                ...state,
                partTypes: partTypes(state.partTypes, action),
            };
        }
        case types.REF_RECORD_TYPES_REQUEST:
        case types.REF_RECORD_TYPES_RECEIVE: {
            return {
                ...state,
                recordTypes: recordTypes(state.recordTypes, action),
            };
        }
        case types.REF_RUL_DATA_TYPES_REQUEST:
        case types.REF_RUL_DATA_TYPES_RECEIVE: {
            return {
                ...state,
                rulDataTypes: rulDataTypes(state.rulDataTypes, action),
            };
        }
        case types.REF_CALENDAR_TYPES_REQUEST:
        case types.REF_CALENDAR_TYPES_RECEIVE: {
            return {
                ...state,
                calendarTypes: calendarTypes(state.calendarTypes, action),
            };
        }
        case types.REF_SCOPES_TYPES_DIRTY:
        case types.REF_SCOPES_TYPES_FETCHING:
        case types.REF_SCOPES_TYPES_REQUEST:
        case types.REF_SCOPES_TYPES_RECEIVE: {
            return {
                ...state,
                scopesData: scopesData(state.scopesData, action),
            };
        }
        case types.REF_STRUCTURE_TYPES_DIRTY:
        case types.REF_STRUCTURE_TYPES_FETCHING:
        case types.REF_STRUCTURE_TYPES_RECEIVE: {
            return {
                ...state,
                structureTypes: structureTypes(state.structureTypes, action),
            };
        }
        case types.REF_DESC_ITEM_TYPES_REQUEST:
        case types.REF_DESC_ITEM_TYPES_RECEIVE: {
            return {
                ...state,
                descItemTypes: descItemTypes(state.descItemTypes, action),
            };
        }
        case types.REF_VISIBLE_POLICY_TYPES_REQUEST:
        case types.REF_VISIBLE_POLICY_TYPES_RECEIVE: {
            return {
                ...state,
                visiblePolicyTypes: visiblePolicyTypes(state.visiblePolicyTypes, action),
            };
        }
        case types.REF_OUTPUT_TYPES_REQUEST:
        case types.REF_OUTPUT_TYPES_RECEIVE: {
            return {
                ...state,
                outputTypes: outputTypes(state.outputTypes, action),
            };
        }

        case types.REF_EXTERNAL_SYSTEMS_INVALID:
        case types.REF_EXTERNAL_SYSTEMS_REQUEST:
        case types.REF_EXTERNAL_SYSTEMS_RECEIVE: {
            return {
                ...state,
                externalSystems: externalSystems(state.externalSystems, action),
            };
        }

        case types.CHANGE_PACKAGE: {
            return {
                ...state,
                ruleSet: ruleSet(state.ruleSet, action),
                descItemTypes: descItemTypes(state.descItemTypes, action),
                outputTypes: outputTypes(state.outputTypes, action),
                templates: templates(state.templates, action),
            };
        }
        default:
            return state;
    }
}
