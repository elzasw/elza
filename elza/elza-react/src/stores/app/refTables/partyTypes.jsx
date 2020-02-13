import * as types from 'actions/constants/ActionTypes.js';
import {RELATION_CLASS_CODES} from '../../../constants.tsx'

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    items: []
};

export default function partyTypes(state = initialState, action = {}) {
    switch (action.type) {
        case types.REF_PARTY_TYPES_REQUEST:{
            return {
                ...state,
                isFetching: true
            }
        }
        case types.REF_PARTY_TYPES_RECEIVE:{
            const relationType = [...action.items.map(i => i.relationTypes)];

            return {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                items: action.items,
                lastUpdated: action.receivedAt,
                relationTypesForClass: {
                    [RELATION_CLASS_CODES.BIRTH]: relationType.filter(i => i && i.relationClassType && i.relationClassType.code === RELATION_CLASS_CODES.BIRTH).map(i => i.id).filter((i, index, self) => index === self.indexOf(i)),
                    [RELATION_CLASS_CODES.EXTINCTION]: relationType.filter(i => i && i.relationClassType && i.relationClassType.code === RELATION_CLASS_CODES.EXTINCTION).map(i => i.id).filter((i, index, self) => index === self.indexOf(i))
                },
            }
        }
        case types.CHANGE_PACKAGE:
            return initialState;
        default:
            return state
    }
}
