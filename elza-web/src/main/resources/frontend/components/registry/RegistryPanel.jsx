/**
 * 
 * Stránka detailu / editace rejstříku.
 * @param selectedId int vstupní parametr, pomocí kterého načte detail / editaci konkrétního záznamu z rejstříku
 * 
**/
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Input} from 'react-bootstrap';
import {AbstractReactComponent, RegistryLabel, Loading, DropDownTree, AddRegistryForm} from 'components';
import {i18n} from 'components';
import {WebApi} from 'actions'
import {getRegistryIfNeeded} from 'actions/registry/registryList'
import {registryChangeDetail, registryData} from 'actions/registry/registryData'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes'
import {registryUpdated} from 'actions/registry/registryData'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'


var RegistryPanel = class RegistryPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleChangeTypeRegistry', 'editRecord');
        if (props.selectedId === null) {
            this.dispatch(getRegistryIfNeeded(props.selectedId));
        }

        this.dispatch(refRecordTypesFetchIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.selectedId !== null) {
            this.dispatch(getRegistryIfNeeded(nextProps.selectedId));
        }
        this.dispatch(refRecordTypesFetchIfNeeded());

    }

    handleChangeTypeRegistry(value) {

        var data = Object.assign({}, this.props.registryData.item)
        data.registerType = {id: value};
        WebApi.updateRegistry(data).then(json => {
            this.dispatch(registryUpdated());
        });
    }

    handleCallEditRegistry(){

    }
    editRecord(){
        console.log(this.props.registryData.item.record);
        this.dispatch(modalDialogShow(this, i18n('registry.editRegistry') , <AddRegistryForm initData={{nameMain: this.props.registryData.item.record , characteristics: this.props.registryData.item.characteristics}} create onSubmit={this.handleCallEditRegistry.bind(this)} />));
    }

    render() {


        if (!this.props.registryData.isFetching && this.props.registryData.fetched) {

            var detailRegistry = (
                    <div>
                        <h2>
                            {this.props.registryData.item.record} <span onClick={this.editRecord} className='btn glyphicon glyphicon-pencil'/>
                        </h2>

                        <p>{this.props.registryData.item.characteristics}</p>

                        <RegistryLabel
                            label={i18n('registry.detail.typ.rejstriku')}
                            type='selectWithChild'
                            items={this.props.refTables.recordTypes.items}
                            value = {this.props.registryData.item.registerType.id}
                            onSelect = {this.handleChangeTypeRegistry}
                        />


                        <h3>
                            Variantní jména:
                        </h3>
                        { (this.props.registryData.item) && this.props.registryData.item.variantRecords && this.props.registryData.item.variantRecords.map(item => { 
                                return (
                                    <Input key={item.variantRecordId} type="text" value={item.variantRecordId +": "+ item.record}/>
                                )
                            })
                        }
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
    const {registryData, refTables} = state

    return {
        registryData, refTables
    }
}

module.exports = connect(mapStateToProps)(RegistryPanel);

