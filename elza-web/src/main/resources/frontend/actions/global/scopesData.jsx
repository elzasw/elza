import * as types from 'actions/constants/actionTypes';

export function scopesDataRecive(versionId, data) {
    data = {data: data, versionId: versionId};
    return {
        type: types.REF_SCOPES_TYPES_RECEIVE,
        data: data
    };
}


