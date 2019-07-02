import * as types from 'actions/constants/ActionTypes.js';
import {indexById, getSetFromIdsList} from 'stores/app/utils.jsx'
import {consolidateState} from 'components/Utils.jsx'
import * as perms from 'actions/user/Permission.jsx';

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
                    case perms.AP_SCOPE_RD:
                    case perms.AP_SCOPE_WR:
                    case perms.AP_CONFIRM:
                    case perms.AP_EDIT_CONFIRMED:
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

function hasRdPage(fundId = null) {     //Zjistí zda má uživatel oprávnění číst archivní soubor
    return hasOne.bind(this)(
        perms.FUND_ADMIN,
        perms.FUND_RD_ALL, {type: perms.FUND_RD, fundId},
        perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId},
    )
}

function hasArrPage(fundId = null) {    //Zjistí zda má uživatel oprávnění pořádat archivní soubor
    return hasOne.bind(this)(
        perms.FUND_ADMIN,
        perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId},
    )
}

function hasArrOutputPage(fundId = null) {  //Zjistí zda má uživatel oprávnění vytvářet výstupy
    return hasOne.bind(this)(
        perms.FUND_ADMIN,
        perms.FUND_OUTPUT_WR_ALL, {type: perms.FUND_OUTPUT_WR, fundId},
    )
}

function hasFundActionPage(fundId = null) { //Zjistí zda má uživatel oprávnění spouštět funkce
    return hasOne.bind(this)(
        perms.FUND_ADMIN,
        perms.FUND_BA_ALL, {type: perms.FUND_BA, fundId},
    )
}

function isAdmin() {
    if (this.permissionsMap[perms.ADMIN]) {
        return true;
    } else {
        return false;
    }
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
    fetched: false,
    fetching: false
}

export default function userDetail(state = initialState, action = {}) {
    const result = userDetailInt(state, action)

    result.hasOne = hasOne.bind(result);
    result.hasAll = hasAll.bind(result);
    result.hasRdPage = hasRdPage.bind(result);
    result.hasArrPage = hasArrPage.bind(result);
    result.hasArrOutputPage = hasArrOutputPage.bind(result);
    result.hasFundActionPage = hasFundActionPage.bind(result);
    result.isAdmin = isAdmin.bind(result);

    return result
}

function userDetailInt(state, action) {
    switch (action.type) {
        case types.STORE_STATE_DATA_INIT:
            return {
                ...state,
                ...action.storageData.userDetail
            }
        case types.USER_DETAIL_CLEAR:
            return {...initialState}
        case types.USER_DETAIL_REQUEST: {
            return {
                ...state,
                fetching: true,
                fetched: false
            }
        }

        case types.USER_DETAIL_CHANGE: {
            let permissionsMap = {}
            let userDetail = {};

            // action.userDetail.userPermissions = [
            //     {permission: 'FUND_ARR_ALL', fundIds: [], scopeIds: [1]},
            //     {permission: 'AP_SCOPE_RD', fundIds: [], scopeIds: [1]},
            //     {permission: 'AP_SCOPE_WR_ALL', fundIds: [], scopeIds: []},
            //     {permission: 'AP_SCOPE_WR', fundIds: [], scopeIds: [2]},
            // ]

            if(action.userDetail){
                userDetail = action.userDetail;
                userDetail.userPermissions.forEach(perm => {
                    permissionsMap[perm.permission] = perm
                    perm.fundIdsMap = getSetFromIdsList(perm.fundIds)
                    perm.scopeIdsMap = getSetFromIdsList(perm.scopeIds)
                })
            }


            return {
                ...state,
                ...userDetail,
                permissionsMap,
                fetching: false,
                fetched: true
            }
        }
        case types.USER_DETAIL_RESPONSE_SETTINGS: {

            var settings = action.settings;

            return {
                ...state,
                settings
            }
        }
        default:
            return state
    }
}

