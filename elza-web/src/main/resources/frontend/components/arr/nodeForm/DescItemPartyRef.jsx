require ('./DescItemPartyRef.less')

import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions'
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete} from 'components';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils'

import {MenuItem, DropdownButton, Button} from 'react-bootstrap';

import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes'

var DescItemPartyRef = class DescItemPartyRef extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleChange', 'renderParty', 'handleSearchChange', 'renderFooter', 'handleDetail');

        this.state = {partyList: []};
    }

    componentDidMount() {
        this.dispatch(refPartyTypesFetchIfNeeded());
    }

    handleChange(id, valueObj) {
        this.props.onChange(valueObj);
    }

    handleSearchChange(text) {

        text = text == "" ? null : text;

        WebApi.findParty(text, this.props.versionId)
                .then(json => {
                    this.setState({
                        partyList: json.map(party => {
                            return party
                        })
                    })
                })
    }

    handleCreateParty(partyTypeId) {
        this.props.onCreateParty(partyTypeId);
    }

    renderParty(item, isHighlighted, isSelected) {
        var cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        var interval;
        if (item.from || item.to) {
            interval = item.from == null ? "" : "TODO" + "-" + item.from == null ? "" : "TODO"
        }

        return (
                <div className={cls} key={item.partyId} >
                    <div className="name" title={item.record.record}>{item.record.record}</div>
                    <div className="type">{item.partyType.name}</div>
                    <div className="interval">{interval}</div>
                    <div  className="characteristics" title={item.record.characteristics}>{item.record.characteristics}</div>
                </div>
        )
    }

    renderFooter() {
        const {refTables} = this.props;
        return (
                <div className="create-party">
                    <DropdownButton noCaret title={<div><Icon glyph='fa-download' /><span className="create-party-label">{i18n('party.addParty')}</span></div>}>
                        {refTables.partyTypes.items.map(i=> {return <MenuItem key={'party' + i.partyTypeId} onClick={this.handleCreateParty.bind(this, i.partyTypeId)} eventKey={i.partyTypeId}>{i.name}</MenuItem>})}
                    </DropdownButton>
                </div>
        )
    }

    handleDetail(partyId) {
        this.props.onDetail(partyId);
    }

    render() {
        const {descItem, locked} = this.props;
        var footer = this.renderFooter();
        var value = descItem.party ? descItem.party : null;

        var actions = new Array;

        if (descItem.party) {
            actions.push(<div onClick={this.handleDetail.bind(this, descItem.party.partyId)} className={'btn btn-default detail'}><Icon glyph={'fa-user'}/></div>);
        }

        return (
            <div className='desc-item-value desc-item-value-parts'>
                <Autocomplete
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                        customFilter
                        className='autocomplete-party'
                        footer={footer}
                        value={value}
                        items={this.state.partyList}
                        getItemId={(item) => item ? item.partyId : null}
                        getItemName={(item) => item ? item.record.record : ''}
                        onSearchChange={this.handleSearchChange}
                        onChange={this.handleChange}
                        renderItem={this.renderParty}
                        actions={[actions]}
                />
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {refTables} = state
    return {
        refTables
    }
}
module.exports = connect(mapStateToProps)(DescItemPartyRef);
