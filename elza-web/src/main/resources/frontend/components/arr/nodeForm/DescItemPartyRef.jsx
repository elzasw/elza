require('./DescItemPartyRef.less')

import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent, Autocomplete} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import {MenuItem, DropdownButton} from 'react-bootstrap';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import * as perms from 'actions/user/Permission.jsx';
import DescItemLabel from './DescItemLabel.jsx'

const DescItemPartyRef = class DescItemPartyRef extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleChange', 'renderParty', 'handleSearchChange', 'renderFooter', 'handleDetail', 'focus');

        this.state = {partyList: []};
    }

    focus() {
        this.refs.autocomplete.focus()
    }

    componentDidMount() {
        this.dispatch(refPartyTypesFetchIfNeeded());
    }

    handleChange(id, valueObj) {
        this.props.onChange(valueObj);
    }

    handleSearchChange(text) {
        text = text == '' ? null : text;

        WebApi.findParty(text, this.props.versionId).then(json => {
            this.setState({
                partyList: json
            })
        })
    }

    handleCreateParty(partyTypeId) {
        this.refs.autocomplete.closeMenu();
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

        return (
                <div className={cls} key={item.partyId} >
                    <div className="name" title={item.record.record}>{item.record.record}</div>
                    <div className="type">{item.partyType.name}</div>
                    <div className="characteristics" title={item.record.characteristics}>{item.record.characteristics}</div>
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
        const {userDetail, descItem, locked, singleDescItemTypeEdit, readMode} = this.props;
        const value = descItem.party ? descItem.party : null;

        if (readMode) {
            return (
                <DescItemLabel onClick={this.handleDetail.bind(this, descItem.party.partyId)} value={value.record.record} />
            )
        }

        let footer
        if (!singleDescItemTypeEdit) {
            if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, perms.REG_SCOPE_WR)) {
                footer = this.renderFooter()
            }
        }

        const actions = new Array;
        if (descItem.party) {
            if (userDetail.hasOne(perms.REG_SCOPE_RD_ALL, {type: perms.REG_SCOPE_RD, scopeId: descItem.party.record.scopeId})) {
                actions.push(<div onClick={this.handleDetail.bind(this, descItem.party.partyId)}
                                  className={'btn btn-default detail'}><Icon glyph={'fa-user'}/></div>);
            }
        }

        return (
            <div className='desc-item-value desc-item-value-parts'>
                <Autocomplete
                        {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked, ['autocomplete-party'])}
                        ref='autocomplete'
                        customFilter
                        footer={footer}
                        value={value}
                        items={this.state.partyList}
                        getItemId={(item) => item ? item.partyId : null}
                        getItemName={(item) => item && item.record ? item.record.record : ''}
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
    const {refTables, userDetail} = state
    return {
        refTables,
        userDetail,
    }
}
module.exports = connect(mapStateToProps, null, null, { withRef: true })(DescItemPartyRef);
