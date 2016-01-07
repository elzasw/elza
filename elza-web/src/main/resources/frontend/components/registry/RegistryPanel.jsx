/**
 * 
 * Stránka detailu / editace rejstříku.
 * @param selectedId int vstupní parametr, pomocí kterého načte detail / editaci konkrétního záznamu z rejstříku
 * 
**/
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, Loading} from 'components';
import {i18n} from 'components';
import {getRegistryIfNeeded} from 'actions/registry/registryList'
import {registryChangeDetail} from 'actions/registry/registryData'


var RegistryPanel = class RegistryPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        if (props.selectedId === null){
            this.dispatch(getRegistryIfNeeded(props.selectedId));
        }
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.selectedId !== null){
            this.dispatch(getRegistryIfNeeded(nextProps.selectedId));
        }
    }
    
    render() {

        if (!this.props.registryData.isFetching && this.props.registryData.fetched) {
console.log('detail', this.props.registryData);
            var detailRegistry = (
                    <div>
                        <h2>
                            {this.props.registryData.item.record}
                        </h2>
                        <h3>
                            Charakteristika:
                        </h3>
                        <p>
                            {this.props.registryData.item.characteristics}
                        </p>
                        <h3>
                            Typ rejstříku - hiearchie:
                        </h3>
                        <p>
                            {this.props.registryData.item.registerType.name}
                        </p>
                        <h3>
                            Variantní jména:
                        </h3>
                        {/*(this.props.registryData.item) && this.props.registryData.item.variantRecordList && this.props.registryData.item.variantRecordList.map(item => { 
                                return (
                                        <p key={item.variantRecordId}>{item.variantRecordId}: {item.record}</p>
                                    ) 
                            })
                        */}
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
    const {registryData} = state

    return {
        registryData
    }
}

module.exports = connect(mapStateToProps)(RegistryPanel);

