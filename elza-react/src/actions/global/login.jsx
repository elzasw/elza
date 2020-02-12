import * as types from 'actions/constants/ActionTypes.js';
import {WebApi, _WebApi} from 'actions/index.jsx';
import {userDetailChange, userDetailClear, userDetailRequest} from 'actions/user/userDetail.jsx'
import {routerNavigate} from "actions/router.jsx"
import {partyListInvalidate, partyDetailInvalidate, partyDetailClear} from 'actions/party/party.jsx'


/**
 * Checks if user is logged on server by requesting userDetail. 
 * If yes, saves the returned detail to store.
 * If not, resets store to default.
 */
export function checkUserLogged(callback=()=>{}) {
    return (dispatch, getState) => {
        const state = getState();
        dispatch(userDetailRequest());
        // calls original _getUserDetail method, which is not postponed until login
        _WebApi.getUserDetail().then(userDetail => {
            if(userDetail && !state.login.logged){
                // fake local login if user is logged on server
                dispatch(loginSuccess(userDetail));
                callback(true);
            }
        }, (error) => {
            dispatch(userDetailChange(null))
            dispatch(loginFail());
            callback(false);
        });
    }
}
/**
 * Changes the state to logged-in and connects the websockets.
 */
function loginSuccess(forcedUserDetail) {
    return (dispatch, getState) => {
        const state = getState();

        window.ws.connect();
        
        let action = {
            type: types.LOGIN_SUCCESS
        };

        if(forcedUserDetail){
            dispatch(saveLoggedUser(forcedUserDetail));
            action.reset = state.userDetail.id != forcedUserDetail.id;
            dispatch(action);
        } else {
            dispatch(userDetailRequest());
            WebApi.getUserDetail().then((userDetail)=>{
                dispatch(saveLoggedUser(userDetail));
                action.reset = state.userDetail.id != userDetail.id;
                dispatch(action);
            })
        }

        WebApi.onLogin();
    }
}

function loginFail() {
    return {
        type: types.LOGIN_FAIL
    }
}
/**
 * Saves the given userDetail as currently logged-in user's detail
 */
function saveLoggedUser(userDetail, reset) {
    return (dispatch, getState) => {
        if (reset) {
            dispatch(routerNavigate('/'));
            dispatch(partyListInvalidate());
            dispatch(partyDetailInvalidate());
            dispatch(partyDetailClear());
        }
        dispatch(userDetailChange(userDetail));
    }
}

export function login(username, password) {
    return(dispatch, getState) => {
        return _WebApi.login(username, password).then((data) => {
            dispatch(loginSuccess());
        })
    }
}
/**
 * Disconnects websockets, logs out the user and changes the state to logged-out.
 */
export function logout() {
    return dispatch => {
        window.ws.disconnect();
        dispatch(routerNavigate('/'));
        dispatch({
            type: types.LOGOUT,
            reset: true
        });

        return WebApi.logout()            
    }
}
