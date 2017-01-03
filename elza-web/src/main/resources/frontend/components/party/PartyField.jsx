import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent, Autocomplete} from 'components/index.jsx';
import {connect} from 'react-redux'
//import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import {MenuItem, DropdownButton, Button} from 'react-bootstrap';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {partyDetailFetchIfNeeded, partyAdd} from 'actions/party/party.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {modalDialogHide} from 'actions/global/modalDialog.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {DEFAULT_LIST_SIZE} from 'constants'

import './PartyField.less'

const AUTOCOMPLETE_PARTY_LIST_SIZE = DEFAULT_LIST_SIZE;

class PartyField extends AbstractReactComponent {

    static defaultProps = {
        detail: false,
        footer: true,
        partyTypeId: null
    };

    static PropTypes = {
        detail: React.PropTypes.bool.isRequired,
        value: React.PropTypes.object,
        onChange: React.PropTypes.func.isRequired,
        onDetail: React.PropTypes.func,
        onCreate: React.PropTypes.func.isRequired,
        partyTypeId: React.PropTypes.number,
        versionId: React.PropTypes.number
    };

    state = {partyList: [], count: null};

    componentDidMount() {
        this.dispatch(refPartyTypesFetchIfNeeded());
    }

    focus = () => {
        this.refs.autocomplete.focus()
    };

    handleSearchChange = (text) => {

        text = text == "" ? null : text;

        WebApi.findParty(text, this.props.versionId, this.props.partyTypeId, 0, AUTOCOMPLETE_PARTY_LIST_SIZE).then(json => {
            this.setState({
                partyList: json.rows,
                count: json.count
            })
        })
    };

    handleCreateParty = (partyTypeId) => {
        this.refs.autocomplete.closeMenu();
        this.props.onCreate(partyTypeId);
    };

    renderParty = (item, isHighlighted, isSelected) => {
        let cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        return (
                <div className={cls} key={item.id} >
                    <div className="name" title={item.record.record}>{item.record.record}</div>
                    <div className="type">{item.partyType.name}</div>
                    <div className="characteristics" title={item.record.characteristics}>{item.record.characteristics}</div>
                </div>
        )
    };

    renderFooter = () => {
        const {refTables} = this.props;
        return <div>
            <div className="create-party">
                <DropdownButton noCaret title={<div><Icon glyph='fa-download' /><span className="create-party-label">{i18n('party.addParty')}</span></div>} id="party-field" >
                    {refTables.partyTypes.items.map(type => <MenuItem key={'party' + type.id} onClick={() => this.handleCreateParty(type.id)} eventKey={type.id}>{type.name}</MenuItem>)}
                </DropdownButton>
            </div>
            {this.state.count !== null && this.state.count > AUTOCOMPLETE_PARTY_LIST_SIZE && <div className="items-count">
                {i18n('partyField.visibleCount', this.state.partyList.length, this.state.count)}
            </div>}
        </div>
    };

    handleDetail = (partyId) => {
        if (this.props.onDetail) {
            this.props.onDetail(partyId);
        } else {
            this.dispatch(modalDialogHide());
            this.dispatch(partyDetailFetchIfNeeded(partyId, null));
            this.dispatch(routerNavigate('party'));
        }
    };

    render() {
        const {userDetail, value, detail, footer, ...otherProps} = this.props;

        let footerRender;
        if (footer && userDetail.hasOne(perms.REG_SCOPE_WR_ALL, perms.REG_SCOPE_WR)) {
            footerRender = this.renderFooter()
        }

        const actions = [];
        if (value && value.id && detail) {
            if (userDetail.hasOne(perms.REG_SCOPE_RD_ALL, {type: perms.REG_SCOPE_RD, scopeId: value.record.scopeId})) {
                actions.push(<div onClick={this.handleDetail.bind(this, value.id)}
                                  className={'btn btn-default detail'}><Icon glyph={'fa-user'}/></div>);
            }
        }

        return <Autocomplete
            ref='autocomplete'
            className="autocomplete-party"
            customFilter
            footer={footerRender}
            value={value}
            items={this.state.partyList}
            getItemId={(item) => item ? item.id : null}
            getItemName={(item) => item && item.record ? item.record.record : ''}
            onSearchChange={this.handleSearchChange}
            renderItem={this.renderParty}
            actions={[actions]}
            {...otherProps}
        />
    }
}

export default connect((state) => {
    const {refTables, userDetail} = state;
    return {
        refTables,
        userDetail,
    }
}, null, null, { withRef: true })(PartyField);
