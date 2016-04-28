/**
 * 
 * Stránka detailu / editace rejstříku.
 * @param selectedId int vstupní parametr, pomocí kterého načte detail / editaci konkrétního záznamu z rejstříku
 * 
**/
require ('./RegistryPanel.less');
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Input, Button} from 'react-bootstrap';
import {Icon, NoFocusButton, AbstractReactComponent, RegistryLabel, Loading, EditRegistryForm, RegistryCoordinates, i18n} from 'components/index.jsx';
import {WebApi} from 'actions/index.jsx';
import {getRegistryIfNeeded, fetchRegistryIfNeeded, fetchRegistry} from 'actions/registry/registryRegionList.jsx'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {partySelect} from 'actions/party/party.jsx'
import {
    registryChangeDetail,
    registryRecordUpdate,
    registryVariantAddRow,
    registryVariantCreate,
    registryVariantDelete,
    registryVariantUpdate,
    registryVariantInternalDelete,
    registryRecordNoteUpdate,
    registryRecordCoordinatesAddRow,
    registryRecordCoordinatesCreate,
    registryRecordCoordinatesChange,
    registryRecordCoordinatesDelete,
    registryRecordCoordinatesInternalDelete,
    registryRecordCoordinatesUpdate,
    registryRecordCoordinatesUpload
} from 'actions/registry/registryRegionData.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {Utils} from 'components/index.jsx';
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {setFocus} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';

var keyModifier = Utils.getKeyModifier();

var keymap = {
    RegistryPanel: {
        editRecord: keyModifier + 'e',
        goToPartyPerson: keyModifier + 'b',
        addRegistryVariant: keyModifier + 'i'
    },
    VariantRecord: {
        deleteRegistryVariant: keyModifier + 'd'
    }
};
var shortcutManager = new ShortcutsManager(keymap);

var RegistryPanel = class RegistryPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'canEdit',
            'handleCoordinatesAdd',
            'handleCoordinatesBlur',
            'handleCoordinatesChange',
            'handleCoordinatesDownload',
            'handleCoordinatesUploadButtonClick',
            'handleCoordinatesUpload',
            'handleNoteBlur',
            'handleNoteChange',
            'handleRecordUpdate',
            'handleRecordUpdateCall',
            'handleVariantAdd',
            'handleVariantBlur',
            'handleVariantCreateCall',
            'handleVariantDelete',
            'handleGoToPartyPerson',
            'handleShortcuts',
            'handleVariantRecordShortcuts',
            'trySetFocus',
        );
        this.state = {}
    }

    componentWillReceiveProps(nextProps) {
        this.prepareData(nextProps);
    }

    componentDidMount() {
        this.prepareData(this.props);
    }

    prepareData(props) {
        if (props.selectedId !== null) {
            this.dispatch(getRegistryIfNeeded(props.selectedId));
        }

        this.dispatch(getRegistryIfNeeded(props.selectedId));
        this.setState({
            note: props.registryRegionData.item ? props.registryRegionData.item.note : ''
        });

        this.trySetFocus(props)
    }

    trySetFocus(props) {
        var {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, 'registry', 2, 'variantRecords')) {   // focus na konkrétní variantní rejstříkové heslo
                this.setState({}, () => {
                   this.refs['variant-' + focus.item.index].getWrappedInstance().focus();
                   focusWasSet()
                })
            } else if (isFocusFor(focus, 'registry', 2)) {
                this.setState({}, () => {
                   this.refs.registryTitle.focus();
                   focusWasSet()
                })
            }
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleVariantRecordShortcuts(variantRecord, index, action) {
        console.log("#handleShortcuts", '[' + action + ']', this, variantRecord, index);
        switch (action) {
            case 'deleteRegistryVariant':
                if (this.canEdit()) {
                    this.handleVariantDelete(variantRecord, index)
                }
                break
        }
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts", '[' + action + ']', this);

        switch (action) {
            case 'editRecord':
                if (this.canEdit()) {
                    this.handleRecordUpdate()
                }
                break;
            case 'goToPartyPerson':
                if (this.props.registryRegionData.item.partyId) {
                    this.handleGoToPartyPerson()
                }
                break;
            case 'addRegistryVariant':
                if (this.canEdit()) {
                    this.handleVariantAdd()
                }
                break
        }
    }
    
    handleCoordinatesChange(item) {
        this.dispatch(registryRecordCoordinatesChange(item))
    }

    handleNoteChange(e) {
        this.setState({
            note: e.target.value
        });
    }

    handleRecordUpdateCall(value) {
        this.dispatch(registryRecordUpdate({
            ...this.props.registryRegionData.item,
            ...value
        }));
        this.dispatch(modalDialogHide());

        // Nastavení focus
        this.dispatch(setFocus('registry', 2))
    }

    handleVariantDelete(item, index) {
        if(confirm(i18n('registry.deleteRegistryQuestion'))) {
            // Zjištění nového indexu focusu po smazání - zjisšťujeme zde, abychom měli aktuální stav store
            const {registryRegionData} = this.props;
            var setFocusFunc;
            if (index + 1 < registryRegionData.item.variantRecords.length) {    // má položku za, nový index bude aktuální
                setFocusFunc = () => setFocus('registry', 2, 'variantRecords', {index: index})
            } else if (index > 0) { // má položku před
                setFocusFunc = () => setFocus('registry', 2, 'variantRecords', {index: index - 1})
            } else {    // byla smazána poslední položka, focus dostane formulář
                setFocusFunc = () => setFocus('registry', 2)
            }

            // Smazání
            if (item.variantRecordId) {
                this.dispatch(registryVariantDelete(item.variantRecordId))
            } else {
                console.log(item);
                this.dispatch(registryVariantInternalDelete(item.variantRecordInternalId))
            }

            // Nastavení focus
            this.dispatch(setFocusFunc())
        }
    }
    
    handleCoordinatesDownload(objectId) {
        window.open(window.location.origin + '/api/kmlManagerV1/export/regCoordinates/' + objectId);
    }

    handleCoordinatesDelete(item, index) {
        if(confirm(i18n('registry.deleteCoordinatesQuestion'))) {
            // Zjištění nového indexu focusu po smazání - zjisšťujeme zde, abychom měli aktuální stav store
            const {registryRegionData} = this.props;
            var setFocusFunc;
            if (index + 1 < registryRegionData.item.coordinates.length) {    // má položku za, nový index bude aktuální
                setFocusFunc = () => setFocus('registry', 2, 'coordinates', {index: index})
            } else if (index > 0) { // má položku před
                setFocusFunc = () => setFocus('registry', 2, 'coordinates', {index: index - 1})
            } else {    // byla smazána poslední položka, focus dostane formulář
                setFocusFunc = () => setFocus('registry', 2)
            }

            // Smazání
            if (item.coordinatesId) {
                this.dispatch(registryRecordCoordinatesDelete(item.coordinatesId))
            } else {
                this.dispatch(registryRecordCoordinatesInternalDelete(item.coordinatesInternalId))
            }

            // Nastavení focus
            this.dispatch(setFocusFunc())
        }
    }

    handleVariantAdd() {
        // Index Musíme zjistit předem, protože po dispatch přidání záznamu se store změní
        const newIndex = this.props.registryRegionData.item.variantRecords.length;

        // Přidání nového proádného záznamu
        this.dispatch(registryVariantAddRow());

        // Nastavení focus
        this.dispatch(setFocus('registry', 2, 'variantRecords', {index: newIndex}))
    }

    handleCoordinatesAdd() {
        // Index Musíme zjistit předem, protože po dispatch přidání záznamu se store změní
        const newIndex = this.props.registryRegionData.item.coordinates.length;

        // Přidání nového proádného záznamu
        this.dispatch(registryRecordCoordinatesAddRow());

        // Nastavení focus
        this.dispatch(setFocus('registry', 2, 'coordinates', {index: newIndex}))
    }

    handleVariantCreateCall(item, element) {
        if (!element.target.value) {
            return false;
        }
        var data = {record: element.target.value, regRecordId: this.props.registryRegionData.item.recordId};
        this.dispatch(registryVariantCreate(data, item.variantRecordInternalId));
    }

    handleCoordinatesCreate(item) {
        !item.hasError && this.dispatch(registryRecordCoordinatesCreate(item, item.coordinatesInternalId));
    }

    handleRecordUpdate() {
        const {item} = this.props.registryRegionData;
        this.dispatch(
            modalDialogShow(
                this,
                i18n('registry.update.title'),
                <EditRegistryForm
                    key='editRegistryForm'
                    initData={item}
                    parentRecordId={item.parentRecordId}
                    parentRegisterTypeId={item.registerTypeId}
                    onSubmitForm={this.handleRecordUpdateCall}
                />
            )
        );
    }

    handleVariantBlur(item, element) {
        if (!element.target.value) {
            return false;
        }

        this.dispatch(registryVariantUpdate({
            variantRecordId: item.variantRecordId,
            regRecordId: this.props.registryRegionData.item.recordId,
            record: element.target.value,
            version: item.version
        }));
    }

    handleCoordinatesBlur(item) {
        if (item.hasError === undefined) {
            this.handleCoordinatesChange(item);
        }
        if(!item.hasError && (item.oldValue.description !== item.description || item.oldValue.value !== item.value)) {
            this.dispatch(registryRecordCoordinatesUpdate(item));
        }
    }

    handleNoteBlur(event, element) {
        if (event.target.value !== this.props.registryRegionData.item.note) {
            this.dispatch(registryRecordNoteUpdate({
                ...this.props.registryRegionData.item,
                note: event.target.value
            }));
        }
    }

    handleCoordinatesUploadButtonClick() {
        this.refs.uploadInput.getInputDOMNode().click();
    }

    handleCoordinatesUpload() {
        const fileList = this.refs.uploadInput.getInputDOMNode().files;

        if (fileList.length != 1) {
            return;
        }

        this.dispatch(registryRecordCoordinatesUpload(fileList[0], this.props.registryRegionData.item.recordId));

        this.refs.uploadInput.value = null;
    }

    handleGoToPartyPerson() {
        this.dispatch(partySelect(this.props.registryRegionData.item.partyId));
        this.dispatch(routerNavigate('party'));
    }

    canEdit() {
        const {userDetail, registryRegionData: { item, fetched }} = this.props;

        var canEdit = fetched && !item.partyId

        // Pokud nemá oprávnění, zakážeme editaci
        if (fetched && !userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: item ? item.scopeId : null})) {
            canEdit = false
        }

        return canEdit
    }

    render() {
        const {selectedId, registryRegionData: { item, fetched }} = this.props;
        var detailRegistry = false;
        if (fetched) {
            const disableEdit = !this.canEdit();

            const addVariant = <NoFocusButton disabled={disableEdit} className="registry-variant-add" onClick={this.handleVariantAdd} ><Icon glyph='fa-plus' /></NoFocusButton>
            const addCoordinate = <div className="registry-coordinate-add">
                    <NoFocusButton disabled={disableEdit} onClick={this.handleCoordinatesAdd} title={i18n('registry.coordinates.addPoint')}  ><Icon glyph='fa-plus' /></NoFocusButton>
                    <NoFocusButton onClick={this.handleCoordinatesUploadButtonClick} title={i18n('registry.coordinates.upload')} disabled={disableEdit}><Icon glyph="fa-upload" /></NoFocusButton>
                    <Input className="hidden" accept="application/vnd.google-earth.kml+xml" type="file" ref='uploadInput' onChange={this.handleCoordinatesUpload} />
            </div>;

            const hiearchie = [];

            item.typesToRoot.slice().reverse().map((val) => {
                hiearchie.push(val.name);
            });


            item.parents.slice().reverse().map((val) => {
                hiearchie.push(val.name);
            });

            detailRegistry = (
                <Shortcuts name='RegistryPanel' handler={this.handleShortcuts}>
                    <div ref='registryTitle' className="registry-title" tabIndex={0}>
                        <div className='registry-content'>
                            <h1 className='registry'>
                                {item.record}
                                <NoFocusButton disabled={disableEdit} className="registry-record-edit" onClick={this.handleRecordUpdate}>
                                    <Icon glyph='fa-pencil'/>
                                </NoFocusButton>
                                {item.partyId && <NoFocusButton className="registry-record-party" onClick={this.handleGoToPartyPerson}>
                                    <Icon glyph='fa-user'/>
                                </NoFocusButton>}
                            </h1>
                            <div className='line charakteristik'>
                                <label>{i18n('registry.detail.characteristics')}</label>
                                <div>{item.characteristics}</div>
                            </div>
                            <div className='line hiearch'>
                                <label>{i18n('registry.detail.type')}</label>
                                <div>{hiearchie.join(' > ')}</div>
                            </div>

                            <div className='line variant-name'>
                                <label>{i18n('registry.detail.variantRegistry')}</label>
                                {
                                    item.variantRecords && item.variantRecords.map((item, index) => {
                                        var variantKey;
                                        var blurField = this.handleVariantBlur.bind(this, item);
                                        var clickDelete = this.handleVariantDelete.bind(this, item, index);

                                        if (!item.variantRecordId) {
                                            variantKey = 'internalId' + item.variantRecordInternalId;
                                            blurField = this.handleVariantCreateCall.bind(this, item);
                                        } else {
                                            variantKey = item.variantRecordId;
                                        }

                                        return (
                                            <Shortcuts key={variantKey} name='VariantRecord'
                                                       handler={this.handleVariantRecordShortcuts.bind(this, item, index)}>
                                                <RegistryLabel
                                                    ref={'variant-' + index}
                                                    value={item.record}
                                                    item={item}
                                                    disabled={disableEdit}
                                                    onBlur={blurField}
                                                    onEnter={blurField}
                                                    onClickDelete={clickDelete}
                                                />
                                            </Shortcuts>
                                        )
                                    })
                                }
                                {addVariant}
                            </div>
                            <div className='line'>
                                <div className='reg-coordinates-labels'>
                                    <label>{i18n('registry.detail.coordinates')}</label>
                                    <label>{i18n('registry.coordinates.description')}</label>
                                </div>
                                {
                                    item.coordinates && item.coordinates.map((item, index) => {
                                        let variantKey;
                                        let blurField = this.handleCoordinatesBlur.bind(this, item);

                                        if (!item.coordinatesId) {
                                            variantKey = 'internalId' + item.coordinatesInternalId;
                                            blurField = this.handleCoordinatesCreate.bind(this, item);
                                        } else {
                                            variantKey = item.coordinatesId;
                                        }
                                        return <RegistryCoordinates
                                            ref={'coordinates-' + index}
                                            key={variantKey}
                                            item={item}
                                            disabled={disableEdit}
                                            onChange={this.handleCoordinatesChange}
                                            onBlur={blurField}
                                            onEnterKey={blurField}
                                            onDelete={this.handleCoordinatesDelete.bind(this, item, index)}
                                            onDownload={this.handleCoordinatesDownload.bind(this, item.coordinatesId)}
                                        />
                                    })
                                }
                                {addCoordinate}
                            </div>
                            <div className='line note'>
                                <label>{i18n('registry.detail.note')}</label>
                                <Input disabled={disableEdit} type='textarea' value={this.state.note} onChange={this.handleNoteChange} onBlur={this.handleNoteBlur.bind(this)} />
                            </div>
                        </div>
                    </div>
                </Shortcuts>
            )
        }
        return (
            <div className='registry'>
                {selectedId !== null ? (detailRegistry || <Loading/>) : null}
            </div>
        )
    }
};

function mapStateToProps(state) {
    const {registryRegion: {registryRegionData}, focus, userDetail} = state;

    return {
        registryRegionData,
        focus,
        userDetail,
    }
}

RegistryPanel.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired,
    userDetail: React.PropTypes.object.isRequired,
    focus: React.PropTypes.object.isRequired,
};

module.exports = connect(mapStateToProps, null, null, { withRef: true })(RegistryPanel);

