/**
 * Komponenta detailu osoby
 */

require ('./PartyDetail.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button, Input, SplitButton} from 'react-bootstrap';
import {AbstractReactComponent, Search, i18n} from 'components';
import {AppActions} from 'stores';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'

import {findPartyFetchIfNeeded} from 'actions/party/party.jsx'

var PartyDetail = class PartySearch extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(refPartyTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
    }
    
    componentWillReceiveProps(){
        this.dispatch(refPartyTypesFetchIfNeeded());
    }

    render() {
        var data = this.props.selectedPartyData;
        if(!data){
            return <div>Nenalezeno</div>
        }
        console.log(data);
        return  <div className={"partyDetail"}>
                    <h1>{data.record.record}</h1>
                    <label>{i18n('party.detail.characteristics')}</label>
                    <p className={"characteristics"}>{data.record.characteristics}</p>

                    <div className="columns">
                        <Input type="select" className={"aa"} disabled={true} value={data.partyType.partyTypeId} label={i18n('party.detail.type')}>
                            {this.props.refTables.partyTypes.items.map(i=> {return <option value={i.partyTypeId}>{i.name}</option>})}
                        </Input>
                        <Input type="text" label={i18n('party.detail.number')} />
                    </div>
                    <Input type="select" label={i18n('party.calendarType')}>
                        {this.props.refTables.calendarTypes.items.map(i=> {return <option value={i.partyTypeId}>{i.name}</option>})}
                    </Input>
                    <Input type="text" label={i18n('party.detail.validRange')} />

                    <label>{i18n('party.detail.name')}</label>

                    <Input type="textarea" label={i18n('party.detail.note')} />
                    <Input type="textarea" label={i18n('party.detail.history')} value={data.history} />
                    <Input type="textarea" label={i18n('party.detail.sources')} value={data.sourceInformation} />
                </div>
    }
}

module.exports = connect()(PartyDetail);
