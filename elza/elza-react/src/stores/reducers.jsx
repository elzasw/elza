import {combineReducers} from 'redux';
import {reducer as formReducer} from 'redux-form';
/**
 * Sestavení reducerů.
 */
import ap from './app/accesspoint/accessPoint.jsx';
import arrRegion from './app/arr/arrRegion.jsx';
import refTables from './app/refTables/refTables.jsx';
import toastr from '../components/shared/toastr/ToastrStore.jsx';
import fundRegion from './app/fund/fundRegion.jsx';
import contextMenu from './app/global/contextMenu.jsx';
import modalDialog from './app/global/modalDialog.jsx';
import webSocket from './app/global/webSocket.jsx';
import login from './app/global/login.jsx';
import splitter from './app/global/splitter.jsx';
import developer from './app/global/developer.jsx';
import focus from './app/global/focus.jsx';
import tab from './app/global/tab.jsx';
import adminRegion from './app/admin/adminRegion.jsx';
import fundForm from './app/arr/form/fundForm.jsx';
import inlineForm from './app/form/inlineForm.jsx';
import searchForm from './app/arr/form/searchForm.jsx';
import stateRegion from './app/state/stateRegion.jsx';
import userDetail from './app/user/userDetail.jsx';
import router from './app/router.jsx';
import status from './app/status.jsx';
import app from './app/app.jsx';

import editRegistryForm from './app/registry/form/editRegistryForm.jsx';

import addUserForm from './app/admin/addUserForm.jsx';
import structures from './app/structures/structures';

const rootReducer = combineReducers({
    ap,
    app,
    arrRegion,
    refTables,
    toastr,
    developer,
    fundRegion,
    contextMenu,
    modalDialog,
    webSocket,
    login,
    splitter,
    focus,
    adminRegion,
    stateRegion,
    router,
    userDetail,
    structures,
    tab,
    status,
    form: formReducer.plugin({
        fundForm,
        outputEditForm: inlineForm,
        permissionsEditForm: inlineForm,
        editRegistryForm,
        searchForm,
        addUserForm,
    }) /*.normalize({
        templateSettingsForm: {
            'evenPageOffsetX': normalizeInt,
            'evenPageOffsetY': normalizeInt,
            'oddPageOffsetX': normalizeInt,
            'oddPageOffsetY': normalizeInt,
        }
    })*/,
});

export default rootReducer;
