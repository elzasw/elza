import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent, Autocomplete, ExtImportForm} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import {MenuItem, DropdownButton, Button} from 'react-bootstrap';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import * as perms from 'actions/user/Permission.jsx';
import DescItemLabel from './DescItemLabel.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'


import './DescItemPartyRef.less'

/**
 * Asi by bylo možné spojit s PartyField
 */
class DescItemPartyRef extends AbstractReactComponent {

    static PropTypes = {
        onChange: React.PropTypes.func.isRequired,
        onCreateParty: React.PropTypes.func.isRequired,
        onDetail: React.PropTypes.func.isRequired,
        versionId: React.PropTypes.number
    };

    state = {partyList: []};

    focus = () => {
        this.refs.autocomplete.focus()
    };

    componentDidMount() {
        this.dispatch(refPartyTypesFetchIfNeeded());
    }

    handleSearchChange = (text) => {
        text = text == '' ? null : text;

        WebApi.findParty(text, this.props.versionId).then(response => {
            this.setState({
                partyList: response.rows
            })
        })
    };

    handleCreateParty = (partyTypeId) => {
        this.refs.autocomplete.closeMenu();
        this.props.onCreateParty(partyTypeId);
    };

    handleImport = () => {
        const {versionId} = this.props;
        this.refs.autocomplete.closeMenu();
        this.dispatch(modalDialogShow(this, i18n('extImport.title'), <ExtImportForm isParty={true} versionId={versionId}/>, "dialog-lg"));
    };

    renderParty = (item, isHighlighted, isSelected) => {
        var cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        return <div className={cls} key={item.id} >
            <div className="name" title={item.record.record}>{item.record.record}</div>
            <div className="type">{item.partyType.name}</div>
            <div className="characteristics" title={item.record.characteristics}>{item.record.characteristics}</div>
        </div>;
    };

    renderFooter = () => {
        const {refTables} = this.props;
        return <div className="create-party">
            <DropdownButton noCaret title={<div><Icon glyph='fa-download' /><span className="create-party-label">{i18n('party.addParty')}</span></div>}>
                {refTables.partyTypes.items.map(type=> <MenuItem key={'party' + type.id} onClick={this.handleCreateParty.bind(this, type.id)} eventKey={type.id}>{type.name}</MenuItem>)}
            </DropdownButton>
            <Button onClick={this.handleImport}><Icon glyph='fa-plus' /> {i18n("ribbon.action.party.importExt")}</Button>
        </div>;
    };

    handleDetail = (partyId) => {
        this.props.onDetail(partyId);
    };

    render() {
        const {userDetail, onChange, onBlur, descItem, locked, singleDescItemTypeEdit, readMode, cal} = this.props;
        const value = descItem.party ? descItem.party : null;

        if (readMode) {
            if (value) {
                return <DescItemLabel onClick={this.handleDetail.bind(this, descItem.party.id)} value={value.record.record} />;
            } else {
                return <DescItemLabel value={cal ? i18n("subNodeForm.descItemType.calculable") : ""} cal={cal} />
            }
        }

        let footer
        if (!singleDescItemTypeEdit) {
            if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, perms.REG_SCOPE_WR)) {
                footer = this.renderFooter()
            }
        }

        const actions = [];
        if (descItem.party) {
            if (userDetail.hasOne(perms.REG_SCOPE_RD_ALL, {type: perms.REG_SCOPE_RD, scopeId: descItem.party.record.scopeId})) {
                actions.push(<div onClick={this.handleDetail.bind(this, descItem.party.id)}
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
                        getItemId={(item) => item ? item.id : null}
                        getItemName={(item) => item && item.record ? item.record.record : ''}
                        onSearchChange={this.handleSearchChange}
                        onChange={onChange}
                        onBlur={onBlur}
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

export default connect(mapStateToProps, null, null, { withRef: true })(DescItemPartyRef);
