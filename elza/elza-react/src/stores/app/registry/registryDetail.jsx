import DetailReducer from 'shared/detail/DetailReducer';
import {RESPONSE} from 'shared/detail/DetailActions';
import {i18n} from 'components/shared';

const intialState = {
    variantRecordInternalId: 0,
    coordinatesInternalId: 0,
};

function validateCoordinate(coordinate) {
    const newCord = {...coordinate, error: {value: null}, hasError: false};
    if (newCord.value) {
        if (newCord.value.indexOf('POINT') === 0) {
            let left = newCord.value.indexOf('(') + 1;
            let right = newCord.value.indexOf(')');
            if (right - left === 0) {
                newCord.error.value = i18n('subNodeForm.validate.value.notEmpty');
            }
            let data = newCord.value.substr(left, newCord.value.indexOf(')') - left).split(' ');
            if (
                newCord.value === '' ||
                newCord.value === ' ' ||
                data.length !== 2 ||
                data[0] == null ||
                data[0] === '' ||
                data[1] == null ||
                data[1] === ''
            ) {
                newCord.error.value = i18n('subNodeForm.errorPointCoordinates');
            } else {
                newCord.error.value = null;
            }
        }
    } else {
        newCord.error.value = i18n('subNodeForm.validate.value.notEmpty');
    }
    if (newCord.error.value) {
        newCord.hasError = true;
    }
    return newCord;
}

/**
 * Přetížený detail reducer
 */
export default function reducer(state = undefined, action = {}, config = undefined) {
    switch (action.type) {
        case RESPONSE: {
            const newState = DetailReducer(state, action, config ? {...config, reducer} : {reducer});
            if (state.data) {
                if (state.data.names) {
                    state.data.names.forEach(variant => {
                        if (!variant.id) {
                            newState.data.names.push(variant);
                        }
                    });
                }
                if (state.data.coordinates) {
                    state.data.coordinates.forEach(cord => {
                        if (!cord.id) {
                            newState.data.coordinates.push(cord);
                        }
                    });
                }
            }
            return newState;
        }
        default:
            if (state === undefined) {
                return {
                    ...intialState,
                    ...DetailReducer(state, action, config ? {...config, reducer} : {reducer}),
                };
            }
            return DetailReducer(state, action, config ? {...config, reducer} : {reducer});
    }
}
