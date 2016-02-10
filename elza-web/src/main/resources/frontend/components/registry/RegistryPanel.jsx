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
import {Icon, AbstractReactComponent, RegistryLabel, Loading, DropDownTree, EditRegistryForm, AddRegistryVariantForm} from 'components';
import {i18n} from 'components';
import {WebApi} from 'actions'
import {getRegistryIfNeeded, fetchRegistryIfNeeded, fetchRegistry} from 'actions/registry/registryList'
import {registryChangeDetail, registryData, updateRegistryVariantRecord} from 'actions/registry/registryData'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes'
import {routerNavigate} from 'actions/router'
import {partySelect} from 'actions/party/party'
import {registryRecordUpdate, registryVariantAddRecordRow, registryAddVariant, registryVariantDelete, registryVariantInternalDelete, registryRecordNoteUpdate  } from 'actions/registry/registryData'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'


var RegistryPanel = class RegistryPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('editRecord', 'handleDeleteVariant', 'handleCallAddRegistryVariant', 'handleBlurVariant', 'handleClickAddVariant', 'handleOnEnterAdd', 'handleBlurVariant', 'handlePoznamkaBlur', 'handleChangeNote');
        this.state = {}
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.selectedId !== null) {
            this.dispatch(getRegistryIfNeeded(nextProps.selectedId));
        }
        var notes = '';
        if (nextProps.registryData.item){
            notes = nextProps.registryData.item.note;
        }

        this.dispatch(getRegistryIfNeeded(nextProps.selectedId));
        this.setState({
            note: notes
        });

    }

    componentDidMount(){
        if (this.props.selectedId === null) {
            this.dispatch(getRegistryIfNeeded(this.props.selectedId));
        }
        this.dispatch(refRecordTypesFetchIfNeeded());
        this.state = {
            note: ''
        }
    }

    handleChangeNote(e){
        this.setState({
            note: e.target.value                                  // uložení zadaného řezezce ve stavu komponenty
        });
    }


    handleCallEditRegistry(value){
        var data = Object.assign({}, this.props.registryData.item)
        data.record = value.nameMain;
        data.characteristics = value.characteristics;
        data.scopeId = value.scopeId;
        data.registerTypeId = value.registerTypeId;
        console.log(value);
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
        var data = {record: element.target.value, regRecordId: this.props.registryData.item.recordId};
        this.dispatch(registryAddVariant(data, item.variantRecordInternalId));
    }
    editRecord(parentId, event){
        var registryParentTypesId = this.props.registry.registryTypesId;
        if (this.props.registry.registryData){
            registryParentTypesId = this.props.registry.registryData.item.registerTypeId;
        }

        this.dispatch(
            modalDialogShow(
                this,
                i18n('registry.editRegistry'),
                <EditRegistryForm
                    key='editRegistryForm'
                    initData={{
                            nameMain: this.props.registryData.item.record,
                            characteristics: this.props.registryData.item.characteristics,
                            scopeId: this.props.registryData.item.scopeId,
                            registerTypeId: this.props.registryData.item.registerTypeId
                        }}
                    parentRecordId = {parentId}
                    parentRegisterTypeId = {registryParentTypesId}
                    create
                    onSubmit={this.handleCallEditRegistry.bind(this)}
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
            regRecordId: this.props.registryData.item.recordId,
            record: element.target.value,
            version: item.version
        };

        this.dispatch(updateRegistryVariantRecord(data));
    }
    handlePoznamkaBlur(event, element) {
        var data = Object.assign({}, this.props.registryData.item);
        data.note = event.target.value;
        this.dispatch(registryRecordNoteUpdate(data));
    }

    handleGoToPartyPerson(partyId){
        this.dispatch(partySelect(partyId));
        this.dispatch(routerNavigate('party'));
    }

    render() {


        if (!this.props.registryData.isFetching && this.props.registryData.fetched) {
            var disableEdit = false;
            if (this.props.registryData.item.partyId){
                disableEdit = true;
            }

            var addVariant = <Button disabled={disableEdit} className="registry-variant-add" onClick={this.handleClickAddVariant} ><Icon glyph='fa-plus' /></Button>

            var typesToRoot = this.props.registryData.item.typesToRoot.slice();
            var parents = this.props.registryData.item.parents.slice();
            var hiearchie =[];
            typesToRoot.reverse().map((val) => {
                hiearchie.push(val.name);
            });


            parents.reverse().map((val) => {
                hiearchie.push(val.name);
            });


            var detailRegistry = (
                <div className="registry-title">
                    <h1 className='registry'>
                        {this.props.registryData.item.record}
                        <Button disabled={disableEdit} className="registry-record-edit" onClick={this.editRecord.bind(this, this.props.registry.registryParentId)}>
                            <Icon glyph='fa-pencil'/>
                        </Button>
                        {this.props.registryData.item.partyId && <Button className="registry-record-party" onClick={this.handleGoToPartyPerson.bind(this, this.props.registryData.item.partyId)}>
                            <Icon glyph='fa-user'/>
                        </Button>}
                    </h1>
                    <div className='line charakteristik'>
                        <label>{i18n('registry.detail.charakteristika')}</label>
                        <div>{this.props.registryData.item.characteristics}</div>
                    </div>
                    <div className='line hiearch'>
                        <label>{i18n('registry.detail.typ.rejstriku')}</label>
                        <div>{hiearchie.join(' > ')}</div>
                    </div>

                    <div className='line variant-name'>
                        <label>{i18n('registry.detail.variant.name')}</label>
                        { (this.props.registryData.item) && this.props.registryData.item.variantRecords && this.props.registryData.item.variantRecords.map(item => {
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
            )
        }

        return (
            <div className='registry'>
                {(this.props.selectedId) && detailRegistry || <Loading/>}
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {registryData, refTables, registry} = state

    return {
        registryData, refTables, registry
    }
}

module.exports = connect(mapStateToProps)(RegistryPanel);

