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
import {getRecordIfNeeded} from 'actions/record/recordList'
import {recordChangeDetail} from 'actions/record/recordData'


var RecordPanel = class RecordPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        if (props.selectedId === null){
            this.dispatch(getRecordIfNeeded(props.selectedId));
        }
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.selectedId !== null){
            this.dispatch(getRecordIfNeeded(nextProps.selectedId));
        }
    }
    
    render() {

        if (!this.props.recordData.isFetching && this.props.recordData.fetched) {
            var detailRecord = (
                    <div>
                        <h2>
                            {this.props.recordData.item.record}
                        </h2>
                        <h3>
                            Charakteristika:
                        </h3>
                        <p>
                            {this.props.recordData.item.characteristics}
                        </p>
                        <h3>
                            Typ rejstříku - hiearchie:
                        </h3>
                        <p>
                            {this.props.recordData.item.registrType}
                        </p>
                        <h3>
                            Variantní jména:
                        </h3>
                        {(this.props.recordData.item) && this.props.recordData.item.variantRecordList.map(item => { 
                                return (
                                        <p key={item.variantRecordId}>{item.variantRecordId}: {item.record}</p>
                                    ) 
                            })
                        }
                    </div>
            )
        }

        return (
            <div>
                {(this.props.selectedId) && detailRecord || <Loading/>}
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {recordData} = state
    return {
        recordData
    }
}

module.exports = connect(mapStateToProps)(RecordPanel);

