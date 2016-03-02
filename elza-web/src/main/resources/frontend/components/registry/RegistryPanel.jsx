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
import {Icon, NoFocusButton, AbstractReactComponent, RegistryLabel, Loading, EditRegistryForm} from 'components';
import {i18n} from 'components';
import {WebApi} from 'actions'
import {getRegistryIfNeeded, fetchRegistryIfNeeded, fetchRegistry} from 'actions/registry/registryRegionList'
import {registryChangeDetail, registryRegionData, updateRegistryVariantRecord} from 'actions/registry/registryRegionData'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes'
import {routerNavigate} from 'actions/router'
import {partySelect} from 'actions/party/party'
import {registryRecordUpdate, registryVariantAddRecordRow, registryAddVariant, registryVariantDelete, registryVariantInternalDelete, registryRecordNoteUpdate  } from 'actions/registry/registryRegionData'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {Utils} from 'components'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');

var keyModifier = Utils.getKeyModifier()

var keymap = {
    RegistryPanel: {
        editRecord: keyModifier + 'e',
        goToPartyPerson: keyModifier + 'b',
        addRegistryVariant: keyModifier + 'i',
    },
    VariantRecord: {
        deleteRegistryVariant: keyModifier + 'd',
    },
}
var shortcutManager = new ShortcutsManager(keymap)

var RegistryPanel = class RegistryPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleEditRecord', 'handleDeleteVariant', 'handleCallAddRegistryVariant',
            'handleBlurVariant', 'handleClickAddVariant', 'handleOnEnterAdd', 'handleBlurVariant',
            'handlePoznamkaBlur', 'handleChangeNote', 'handleShortcuts', 'canEdit', 'handleGoToPartyPerson',
            'handleVariantRecordShortcuts');
        this.state = {}
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.selectedId !== null) {
            this.dispatch(getRegistryIfNeeded(nextProps.selectedId));
        }
        var notes = '';
        if (nextProps.registryRegionData.item){
            notes = nextProps.registryRegionData.item.note;
        }

        this.dispatch(getRegistryIfNeeded(nextProps.selectedId));
        this.setState({
            note: notes
        });

    }

    componentDidMount(){
        if (this.props.selectedId !== null) {
            this.dispatch(getRegistryIfNeeded(this.props.selectedId));
        }
        this.dispatch(refRecordTypesFetchIfNeeded());
        this.state = {
            note: ''
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleVariantRecordShortcuts(variantRecord, action) {
        console.log("#handleShortcuts", '[' + action + ']', this, variantRecord);
        switch (action) {
            case 'deleteRegistryVariant':
                if (this.canEdit()) {
                    this.handleDeleteVariant(variantRecord)
                }
                break
        }
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts", '[' + action + ']', this);

        switch (action) {
            case 'editRecord':
                if (this.canEdit()) {
                    this.handleEditRecord()
                }
                break
            case 'goToPartyPerson':
                if (this.props.registryRegionData.item.partyId) {
                    this.handleGoToPartyPerson()
                }
                break
            case 'addRegistryVariant':
                if (this.canEdit()) {
                    this.handleClickAddVariant()
                }
                break
        }
    }

    handleChangeNote(e){
        this.setState({
            note: e.target.value                                  // uložení zadaného řezezce ve stavu komponenty
        });
    }

    handleCallEditRegistry(value){
        var data = Object.assign({}, this.props.registryRegionData.item)
        data.record = value.nameMain;
        data.characteristics = value.characteristics;
        data.scopeId = this.props.registryRegionData.item.scopeId;
        data.registerTypeId = value.registerTypeId;

        this.dispatch(registryRecordUpdate(data));
        this.dispatch(modalDialogHide());
    }

    handleDeleteVariant(item){
        if(confirm(i18n('registry.removeRegistryQuestion'))) {
            if (item.variantRecordId) {
                this.dispatch(registryVariantDelete(item.variantRecordId))
            }
            else if (item.variantRecordInternalId){
                this.dispatch(registryVariantInternalDelete(item.variantRecordInternalId))
            }
        }
    }

    handleClickAddVariant(){
        this.dispatch(registryVariantAddRecordRow());
    }

    handleOnEnterAdd(item, e){
        this.handleCallAddRegistryVariant(item, e);
    }

    handleCallAddRegistryVariant(item, element){
        if (!element.target.value){
            return false;
        }
        var data = {record: element.target.value, regRecordId: this.props.registryRegionData.item.recordId};
        this.dispatch(registryAddVariant(data, item.variantRecordInternalId));
    }

    handleEditRecord(){
        const parentId = this.props.registryRegionData.item.parentRecordId

        var registryParentTypesId = this.props.registryRegion.registryTypesId;
        if (this.props.registryRegionData){
            registryParentTypesId = this.props.registryRegionData.item.registerTypeId;
        }

        this.dispatch(
            modalDialogShow(
                this,
                i18n('registry.editRegistry'),
                <EditRegistryForm
                    key='editRegistryForm'
                    initData={{
                            nameMain: this.props.registryRegionData.item.record,
                            characteristics: this.props.registryRegionData.item.characteristics,
                            registerTypeId: this.props.registryRegionData.item.registerTypeId
                        }}
                    parentRecordId = {parentId}
                    parentRegisterTypeId = {registryParentTypesId}
                    create
                    onSubmitForm={this.handleCallEditRegistry.bind(this)}
                />
            )
        );
    }

    handleOnEnterUpdate(item, element) {
        this.handleBlurVariant(item, element);
    }
    handleBlurVariant(item, element){
        if (!element.target.value){
            return false;
        }

        var data= {
            variantRecordId: item.variantRecordId,
            regRecordId: this.props.registryRegionData.item.recordId,
            record: element.target.value,
            version: item.version
        };

        this.dispatch(updateRegistryVariantRecord(data));
    }
    handlePoznamkaBlur(event, element) {
        var data = Object.assign({}, this.props.registryRegionData.item);
        data.note = event.target.value;
        if (event.target.value !== this.props.registryRegionData.item.note) {
            this.dispatch(registryRecordNoteUpdate(data));
        }
    }

    handleGoToPartyPerson(){
        const partyId = this.props.registryRegionData.item.partyId

        this.dispatch(partySelect(partyId));
        this.dispatch(routerNavigate('party'));
    }

    canEdit() {
        if (this.props.registryRegionData.fetched) {
            if (this.props.registryRegionData && this.props.registryRegionData.item.partyId){
                return false
            } else {
                return true
            }
        }
        return false
    }

    render() {


        if (this.props.registryRegionData.fetched) {
            var disableEdit = !this.canEdit()

            var addVariant = <NoFocusButton disabled={disableEdit} className="registry-variant-add" onClick={this.handleClickAddVariant} ><Icon glyph='fa-plus' /></NoFocusButton>

            var typesToRoot = this.props.registryRegionData.item.typesToRoot.slice();
            var parents = this.props.registryRegionData.item.parents.slice();
            var hiearchie =[];
            typesToRoot.reverse().map((val) => {
                hiearchie.push(val.name);
            });


            parents.reverse().map((val) => {
                hiearchie.push(val.name);
            });

            var detailRegistry = (
                <Shortcuts name='RegistryPanel' handler={this.handleShortcuts}>
                    <div className="registry-title" tabIndex={0}>
                        <div className='registry-content'>
                            <h1 className='registry'>
                                {this.props.registryRegionData.item.record}
                                <NoFocusButton disabled={disableEdit} className="registry-record-edit" onClick={this.handleEditRecord}>
                                    <Icon glyph='fa-pencil'/>
                                </NoFocusButton>
                                {this.props.registryRegionData.item.partyId && <NoFocusButton className="registry-record-party" onClick={this.handleGoToPartyPerson}>
                                    <Icon glyph='fa-user'/>
                                </NoFocusButton>}
                            </h1>
                            <div className='line charakteristik'>
                                <label>{i18n('registry.detail.charakteristika')}</label>
                                <div>{this.props.registryRegionData.item.characteristics}</div>
                            </div>
                            <div className='line hiearch'>
                                <label>{i18n('registry.detail.typ.rejstriku')}</label>
                                <div>{hiearchie.join(' > ')}</div>
                            </div>

                            <div className='line variant-name'>
                                <label>{i18n('registry.detail.variant.name')}</label>
                                { (this.props.registryRegionData.item) && this.props.registryRegionData.item.variantRecords && this.props.registryRegionData.item.variantRecords.map(item => {
                                        var variantKey;
                                        var blurField = this.handleBlurVariant.bind(this,item);
                                        var enterKey = this.handleOnEnterUpdate.bind(this,item);
                                        var clickDelete = this.handleDeleteVariant.bind(this, item);


                                        if(item.variantRecordInternalId) {
                                            variantKey = 'internalId'+item.variantRecordInternalId;
                                            blurField = this.handleCallAddRegistryVariant.bind(this, item);
                                            enterKey = this.handleOnEnterAdd.bind(this, item);
                                        }
                                        else if (item.variantRecordId){
                                            variantKey = item.variantRecordId;
                                        }

                                        return (
                                            <Shortcuts name='VariantRecord' handler={this.handleVariantRecordShortcuts.bind(this, item)}>
                                                <RegistryLabel
                                                    key={variantKey}
                                                    type='variant'
                                                    value={item.record}
                                                    item={item}
                                                    disabled={disableEdit}
                                                    onBlur={blurField}
                                                    onEnter={enterKey}
                                                    onClickDelete={clickDelete}
                                                    />
                                            </Shortcuts>

                                        )
                                    })
                                }

                                {addVariant}
                            </div>
                            <div className='line note'>
                                <label>{i18n('registry.detail.poznamka')}</label>
                                <Input disabled={disableEdit} type='textarea' value={this.state.note} onChange={this.handleChangeNote} onBlur={this.handlePoznamkaBlur.bind(this)} />
                            </div>
                        </div>
                    </div>
                </Shortcuts>
            )
        }

        if (this.props.selectedId !== null) {
            return (
                <div className='registry'>
                    {detailRegistry || <Loading/>}
                </div>
            )
        } else {
            return (
                <div className='registry'>
                </div>
            )
        }
    }
}

function mapStateToProps(state) {
    const {registryRegionData, refTables, registryRegion} = state

    return {
        registryRegionData, refTables, registryRegion
    }
}

RegistryPanel.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
}

module.exports = connect(mapStateToProps)(RegistryPanel);

