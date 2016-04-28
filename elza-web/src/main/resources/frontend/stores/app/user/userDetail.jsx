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
                    case perms.FUND_RD:
                    case perms.FUND_ARR:
                    case perms.FUND_OUTPUT_WR:
                    case perms.FUND_VER_WR:
                    case perms.FUND_EXPORT:
                    case perms.FUND_BA:
                    case perms.FUND_CL_VER_WR:
                        if (perm.fundIdsMap[right.fundId]) {
                            return true
                        }
                        break
                    case perms.REG_SCOPE_RD:
                    case perms.REG_SCOPE_WR:
                        if (perm.scopeIdsMap[right.scopeId]) {
                            return true
                        }
                        break
                    default:
                        return true
                }
            }
        }
        break
    }

    return false
}

function hasOne(...rights) {
    if (this.permissionsMap[perms.ADMIN]) {
        return true
    }

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
    if (this.permissionsMap[perms.ADMIN]) {
        return true
    }

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

            action.userDetail.userPermissions = [
                {permission: 'FUND_ARR_ALL', fundIds: [], scopeIds: [1]},
                {permission: 'REG_SCOPE_RD', fundIds: [], scopeIds: [1]},
                {permission: 'REG_SCOPE_WR_ALL1', fundIds: [], scopeIds: []},
                {permission: 'REG_SCOPE_WR', fundIds: [], scopeIds: [2]},
            ]

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

