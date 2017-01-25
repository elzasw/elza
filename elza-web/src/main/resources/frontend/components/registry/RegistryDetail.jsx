import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {
    AbstractReactComponent,
    Search,
    i18n,
    FormInput,
    Icon,
    CollapsablePanel,
    NoFocusButton,
    RegistryLabel,
    Loading,
    RegistryDetailVariantRecords,
    RegistryDetailCoordinates
} from 'components/index.jsx';
import {Form, Button} from 'react-bootstrap';
import {AppActions} from 'stores/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {partyUpdate} from 'actions/party/party.jsx'
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'
import {registryDetailFetchIfNeeded, registryUpdate} from 'actions/registry/registry.jsx'
import {Utils, EditRegistryForm} from 'components/index.jsx';
import {objectById, indexById} from 'stores/app/utils.jsx';
import {setInputFocus, dateTimeToString} from 'components/Utils.jsx'
import ShortcutsManager from 'react-shortcuts';
import Shortcuts from 'react-shortcuts/component';
import {setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {initForm} from "actions/form/inlineForm.jsx"
import {getMapFromList} from 'stores/app/utils.jsx'
import {setFocus} from 'actions/global/focus.jsx'


import {routerNavigate} from 'actions/router.jsx'
import {partyDetailFetchIfNeeded} from 'actions/party/party.jsx'


const keyModifier = Utils.getKeyModifier();

const keymap = {
    RegistryDetail: {
        editRecord: keyModifier + 'e',
        goToPartyPerson: keyModifier + 'b',
        addRegistryVariant: keyModifier + 'i'
    },

    VariantRecord: {
        deleteRegistryVariant: keyModifier + 'd'
    }
};

const shortcutManager = new ShortcutsManager(keymap);

import './RegistryDetail.less';


/**
 * Komponenta detailu rejstříku
 */
class RegistryDetail extends AbstractReactComponent {

    state = {note:null}

    static childContextTypes = {
        shortcuts: React.PropTypes.object.isRequired
    };

    componentDidMount() {
        this.trySetFocus();
        this.fetchIfNeeded();
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        this.trySetFocus(nextProps);
        const {registryDetail:{id, fetched, data}} = nextProps;
        if ((id !== this.props.registryDetail.id && fetched) || (!this.props.registryDetail.fetched && fetched)) {
            if (this.props.registryDetail.data) {
                this.setState({note: data.note});
            }
        }
    }

    fetchIfNeeded = (props = this.props) => {
        const {registryDetail: {id}} = props;
        this.dispatch(refPartyTypesFetchIfNeeded());    // nacteni typu osob (osoba, rod, událost, ...)
        this.dispatch(calendarTypesFetchIfNeeded());    // načtení typů kalendářů (gregoriánský, juliánský, ...)
        if (id) {
            this.dispatch(registryDetailFetchIfNeeded(id));
        }
    };

    trySetFocus = (props = this.props) => {
        const {focus} = props;
        if (canSetFocus()) {
            if (isFocusFor(focus, 'registry', 2)) {
                this.setState({}, () => {
                    if (this.refs.registryTitle) {
                        this.refs.registryTitle.focus();
                        focusWasSet()
                    }
                })
            }
        }
    };

    getChildContext() {
        return { shortcuts: shortcutManager };
    }


    handleGoToParty = () => {
        this.dispatch(partyDetailFetchIfNeeded(this.props.registryDetail.data.partyId));
        this.dispatch(routerNavigate('party'));
    };

    handleShortcuts = (action)  => {
        switch (action) {
            case 'editRecord':
                if (this.canEdit()) {
                    this.handleRecordUpdate()
                }
                break;
            case 'goToPartyPerson':
                if (this.props.registryDetail.data.partyId) {
                    this.handleGoToParty()
                }
                break;
        }
    };

    handleRecordUpdate = () => {
        const {registryDetail:{data}} = this.props;
        this.dispatch(
            modalDialogShow(
                this,
                i18n('registry.update.title'),
                <EditRegistryForm
                    key='editRegistryForm'
                    initData={data}
                    parentRecordId={data.parentRecordId}
                    parentRegisterTypeId={data.registerTypeId}
                    onSubmitForm={this.handleRecordUpdateCall}
                />
            )
        );
    };



    handleRecordUpdateCall = (value) => {
        const {registryDetail:{data}} = this.props;

        this.dispatch(registryUpdate({
            ...data,
            ...value
        }, () => {
            this.dispatch(modalDialogHide());

            // Nastavení focus
            this.dispatch(setFocus('registry', 2))
        }));

    };

    canEdit() {
        const {userDetail, registryDetail: { data, fetched }} = this.props;

        // Pokud je načteno && není osoba
        if (!fetched || data.partyId) {
            return false
        }

        // Pokud nemá oprávnění, zakážeme editaci
        return userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {
            type: perms.REG_SCOPE_WR,
            scopeId: data ? data.scopeId : null
        });
    }

    handleNoteBlur = (event, element) => {
        if (event.target.value !== this.props.registryDetail.data.note) {
            this.dispatch(registryUpdate({
                ...this.props.registryDetail.data,
                note: event.target.value
            }));
        }
    };

    handleNoteChange = (e) => {
        this.setState({
            note: e.target.value
        });
    };

    render() {
        const {registryDetail} = this.props;
        const {data, fetched, isFetching, id} = registryDetail;

        if (!fetched || (id && !data)) {
            return <Loading />
        }

        if (!id) {
            return <div className="unselected-msg">
                <div className="title">{i18n('registry.noSelection.title')}</div>
                <div className="msg-text">{i18n('registry.noSelection.message')}</div>
            </div>;
        }

        const disableEdit = !this.canEdit();

        const hiearchie = [];

        data.typesToRoot.slice().reverse().map((val) => {
            hiearchie.push(val.name);
        });


        data.parents.slice().reverse().map((val) => {
            hiearchie.push(val.name);
        });

        return <div className='registry'>
            <Shortcuts name='RegistryDetail' handler={this.handleShortcuts}>
                <div ref='registryTitle' className="registry-title" tabIndex={0}>
                    <div className='registry-content'>
                        <h1 className='registry'>
                            {data.record}
                            <NoFocusButton disabled={disableEdit} className="registry-record-edit" onClick={this.handleRecordUpdate}>
                                <Icon glyph='fa-pencil'/>
                            </NoFocusButton>
                            {data.partyId && <NoFocusButton className="registry-record-party" onClick={this.handleGoToParty}>
                                <Icon glyph='fa-user'/>
                            </NoFocusButton>}
                        </h1>
                        <div className='line charakteristik'>
                            <label>{i18n('registry.detail.characteristics')}</label>
                            <div>{data.characteristics}</div>
                        </div>
                        <div className='line hiearch'>
                            <label>{i18n('registry.detail.type')}</label>
                            <div>{hiearchie.join(' > ')}</div>
                        </div>

                        <div className='line variant-name'>
                            <label>{i18n('registry.detail.variantRegistry')}</label>
                            <RegistryDetailVariantRecords value={data.variantRecords ? data.variantRecords : []} regRecordId={data.id} disabled={disableEdit} />
                        </div>
                        <div className='line'>
                            <div className='reg-coordinates-labels'>
                                <label>{i18n('registry.detail.coordinates')}</label>
                                <label>{i18n('registry.coordinates.description')}</label>
                            </div>
                            <RegistryDetailCoordinates value={data.coordinates ? data.coordinates : []} regRecordId={data.id} disabled={disableEdit} />
                        </div>
                        <div className='line note'>
                            <label>{i18n('registry.detail.note')}</label>
                            <FormInput disabled={disableEdit} onChange={this.handleNoteChange} onBlur={this.handleNoteBlur} componentClass='textarea' value={this.state.note} />
                        </div>
                    </div>
                </div>
            </Shortcuts>
        </div>
    }
}

export default connect((state) => {
    const {app: {registryDetail}, userDetail, focus} = state;
    return {
        focus,
        registryDetail,
        userDetail
    }
})(RegistryDetail);
