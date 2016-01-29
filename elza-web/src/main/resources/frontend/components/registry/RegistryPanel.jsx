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
import {Input} from 'react-bootstrap';
import {AbstractReactComponent, RegistryLabel, Loading, DropDownTree, EditRegistryForm, AddRegistryVariantForm} from 'components';
import {i18n} from 'components';
import {WebApi} from 'actions'
import {getRegistryIfNeeded, fetchRegistryIfNeeded, fetchRegistry} from 'actions/registry/registryList'
import {registryChangeDetail, registryData} from 'actions/registry/registryData'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes'
import {registryUpdated} from 'actions/registry/registryData'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'


var RegistryPanel = class RegistryPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleChangeTypeRegistry', 'editRecord', 'handleAddVaraintRecord', 'handleDeleteVariant', 'handleCallAddRegistryVariant', 'handleBlurVariant');

        if (props.selectedId === null) {
            this.dispatch(getRegistryIfNeeded(props.selectedId));
        }

        this.dispatch(refRecordTypesFetchIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.selectedId !== null) {
            this.dispatch(getRegistryIfNeeded(nextProps.selectedId));
        }
        this.dispatch(fetchRegistryIfNeeded());

    }

    handleChangeTypeRegistry(value) {

        var data = Object.assign({}, this.props.registryData.item)
        data.registerType = {id: value};
        WebApi.updateRegistry(data).then(json => {
            this.dispatch(registryUpdated());
        });
    }

    handleCallEditRegistry(value){
        var data = Object.assign({}, this.props.registryData.item)
        data.record = value.nameMain;
        data.characteristics = value.characteristics;
        WebApi.updateRegistry(data).then(json => {
            this.dispatch(registryUpdated());
            this.dispatch(fetchRegistry(this.props.registry.filterText));
            this.dispatch(modalDialogHide())
        });
    }

    handleDeleteVariant(item){
        if(confirm(i18n('registry.removeRegistryQuestion'))) {
            WebApi.deleteVariantRecord(item.variantRecordId).then(json => {
                this.dispatch(registryUpdated());
                this.dispatch(fetchRegistry(this.props.registry.filterText));
            });
        }
    }

    handleAddVaraintRecord(){

        this.dispatch(modalDialogShow(this, i18n('registry.addRegistryVariant') , <AddRegistryVariantForm create onSubmit={this.handleCallAddRegistryVariant.bind(this)} />));
    }

    handleCallAddRegistryVariant(values){
        var data = {record: values.nameMain, regRecordId: this.props.registryData.item.recordId};
        WebApi.addRegistryVariant(data).then(json => {
            this.dispatch(registryUpdated());
            this.dispatch(fetchRegistry(this.props.registry.filterText));
            this.dispatch(modalDialogHide())
        });
    }
    editRecord(){
        this.dispatch(modalDialogShow(this, i18n('registry.editRegistry') , <EditRegistryForm initData={{nameMain: this.props.registryData.item.record , characteristics: this.props.registryData.item.characteristics}} create onSubmit={this.handleCallEditRegistry.bind(this)} />));
    }

    handleBlurVariant(item, element){
        var data= {
            variantRecordId: item.variantRecordId,
            regRecordId: this.props.registryData.item.recordId,
            record: element.target.value,
            version: item.version
        }
        WebApi.editRegistryVariant(data).then(json => {
            this.dispatch(registryUpdated());
            this.dispatch(fetchRegistry(this.props.registry.filterText));
        });
    }

    render() {


        if (!this.props.registryData.isFetching && this.props.registryData.fetched) {

            console.log(this.props.refTables.recordTypes);
            var detailRegistry = (
                    <div>
                        <h2>
                            {this.props.registryData.item.record} <span onClick={this.editRecord} className='btn glyphicon glyphicon-pencil'/>
                        </h2>

                        <p>{this.props.registryData.item.characteristics}</p>

                        <RegistryLabel
                            key='searchTypesRegistry'
                            label={i18n('registry.detail.typ.rejstriku')}
                            type='selectWithChild'
                            items={this.props.refTables.recordTypes.items}
                            value = {this.props.registryData.item.registerTypeId}
                            onSelect = {this.handleChangeTypeRegistry}
                        />


                        <h3>
                            Variantní jména:
                        </h3>

                        { (this.props.registryData.item) && this.props.registryData.item.variantRecords && this.props.registryData.item.variantRecords.map(item => {
                                return (

                                            <RegistryLabel
                                                key={item.record}
                                                type='variant'
                                                value={item.record}
                                                item={item}
                                                onBlur={this.handleBlurVariant.bind(this,item)}
                                                onClickDelete={this.handleDeleteVariant.bind(this, item)}
                                                />

                                )
                            })
                        }
                        <span className="btn glyphicon glyphicon-plus-sign" onClick={this.handleAddVaraintRecord} />
                    </div>
            )
        }

        return (
            <div>
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

