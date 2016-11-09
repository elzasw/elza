import {combineReducers} from 'redux'
import {reducer as formReducer} from 'redux-form';

import {normalizeInt} from 'components/validate.jsx';




/**
 * Sestavení reducerů.
 */
import arrRegion from './app/arr/arrRegion.jsx';
import refTables from './app/refTables/refTables.jsx';
import registryRegion from './app/registry/registryRegion.jsx';
import registryRegionRecordTypes from './app/registry/registryRegionRecordTypes.jsx';
import toastr from '../components/shared/toastr/ToastrStore.jsx';
import partyRegion from './app/party/partyRegion.jsx';
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
import addPacketForm from './app/arr/form/addPacketForm.jsx';
import stateRegion from './app/state/stateRegion.jsx';
import userDetail from './app/user/userDetail.jsx';
import router from './app/router.jsx';
import status from './app/status.jsx';
import app from './app/app.jsx';

import addPartyForm from './app/party/form/addPartyForm.jsx';
import addPartyNameForm from './app/party/form/addPartyNameForm.jsx';
import partyNameForm from './app/party/form/partyNameForm.jsx';
import partyIdentifierForm from './app/party/form/partyIdentifierForm.jsx';
import partyCreatorForm from './app/party/form/partyCreatorForm.jsx';
import relationForm from './app/party/form/relationForm.jsx';

import addRegistryForm from './app/registry/form/addRegistryForm.jsx';
import editRegistryForm from './app/registry/form/editRegistryForm.jsx';

import addUserForm from './app/admin/addUserForm.jsx';


const normalizePacketSize = (value, previousValue, allValues, previousAllValues) => {
    const vv = normalizeInt(value, previousValue, allValues, previousAllValues);
    if (vv > 32) {
        return previousValue
    }
    return vv
};




const rootReducer = combineReducers({
    app,
    arrRegion,
    refTables,
    registryRegion,
    registryRegionRecordTypes,
    toastr,
    developer,
    partyRegion,
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
    tab,
    status,
    form: formReducer.plugin({
        fundForm: fundForm,
        outputEditForm: inlineForm,
        partyDetail: inlineForm,
        partyForm: inlineForm,
        permissionsEditForm: inlineForm,
        addPacketForm: addPacketForm,
        addPartyForm: addPartyForm,
        partyNameForm: partyNameForm,
        partyCreatorForm: partyCreatorForm,
        relationForm: relationForm,
        addRegistryForm: addRegistryForm,
        editRegistryForm: editRegistryForm,
        addUserForm,
    }).normalize({
        addPacketForm: {
            'start': normalizeInt,
            'size': normalizePacketSize,
            'count': normalizeInt,
        }
    })
});

export default rootReducer
