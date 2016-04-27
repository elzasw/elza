import * as types from 'actions/constants/ActionTypes';
import {indexById, getSetFromIdsList} from 'stores/app/utils.jsx'
import {consolidateState} from 'components/Utils'
import * as perms from 'actions/user/Permission';

function hasRight(right) {
    switch (typeof right) {
        case 'string':
            if (this.permissionsMap[right]) {
                return true
            }
            break
        case 'object': {
            let perm = this.permissionsMap[right.type]
            if (perm) {
                switch (right.type) {
                    case perms.ADMINISTRATOR:
                        if (perm.fundIdsMap[right.fundId]) {
                            return true
                        }
                        break
                }

                return true
            }
        }
        break
    }

    return false
}

function hasOne(...rights) {
    for (let a=0; a<rights.length; a++) {
        const right = rights[a]
        const has = hasRight.bind(this)(right)
        if (has) {
            return true
        }
    }

    return false
}


function hasAll(...rights) {
    for (let a=0; a<rights.length; a++) {
        const right = rights[a]
        const has = hasRight.bind(this)(right)
        if (!has) {
            return false
        }
    }

    return true
}

const initialState = {
    id: null,
    username: '',
    userPermissions: {},
    permissionsMap: {},
}

export default function userDetail(state = initialState, action = {}) {
    const result = userDetailInt(state, action)

    result.hasOne = hasOne.bind(result)
    result.hasAll = hasAll.bind(result)

    return result
}

function userDetailInt(state, action) {
    switch (action.type) {
        case types.USER_DETAIL_CLEAR:
            return {...initialState}
        case types.USER_DETAIL_CHANGE: {
            let permissionsMap = {}
            action.userDetail.userPermissions.forEach(perm => {
                permissionsMap[perm.permission] = perm

                perm.fundIdsMap = getSetFromIdsList(perm.fundIds)
                perm.scopeIdsMap = getSetFromIdsList(perm.scopeIds)
            })

            return {
                ...state,
                ...action.userDetail,
                permissionsMap,
            }
        }
        default:
            return state
    }
}

