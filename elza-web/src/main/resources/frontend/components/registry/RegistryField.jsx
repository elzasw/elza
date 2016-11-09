import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, Autocomplete} from 'components/index.jsx';
import {connect} from 'react-redux'
//import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import {MenuItem, DropdownButton, Button} from 'react-bootstrap';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {partySelect, partyAdd} from 'actions/party/party.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {modalDialogHide} from 'actions/global/modalDialog.jsx'
import {indexById} from 'stores/app/utils.jsx'

import './RegistryField.less'

class RegistryField extends AbstractReactComponent {

    static defaultProps = {
        detail: false,
        registryParent: null,
        registerTypeId: null,
        versionId: null,
    };

    static PropTypes = {
        detail: React.PropTypes.bool.isRequired,
        value: React.PropTypes.object,
        onChange: React.PropTypes.func.isRequired,
        onDetail: React.PropTypes.func,
        onCreate: React.PropTypes.func.isRequired,
        registryParent: React.PropTypes.number,
        registerTypeId: React.PropTypes.number,
        versionId: React.PropTypes.number
    };

    state = {partyList: []};

    componentDidMount() {
        this.dispatch(refPartyTypesFetchIfNeeded());
    }

    focus = () => {
        this.refs.autocomplete.focus()
    };

    /**
     * Zajistíme vrácení onChange pouze objekt nebo null
     * @param id
     * @param valueObj
     */
    handleChange = (id, valueObj) => {
        this.props.onChange(valueObj.partyId ? valueObj : null);
    };

    /**
     * Zajistíme vrácení onBlur pouze objekt nebo null
     * @param id
     * @param valueObj
     */
    handleBlur = (id, valueObj) => {
        this.props.onBlur(valueObj.partyId ? valueObj : null);
    };

    handleSearchChange = (text) => {

        text = text == "" ? null : text;

        WebApi.findRegistry(text, this.props.registryParent, this.props.registerTypeId, this.props.versionId).then(json => {
            this.setState({
                partyList: json.rows
            })
        })
    };

    handleCreateParty = (partyTypeId) => {
        this.refs.autocomplete.closeMenu();
        this.props.onCreate(partyTypeId);
    };

    renderRecord = (item, isHighlighted, isSelected) => {
        let cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        return <div className={cls} key={item.recordId} >
            <div className="name" title={item.record}>{item.record}</div>
            <div className="characteristics" title={item.characteristics}>{item.characteristics}</div>
        </div>;
    };

    renderFooter = () => {
        const {refTables} = this.props;
        return (
            <div className="create-party">
                <DropdownButton id="party-field" noCaret title={<div><Icon glyph='fa-download' /><span className="create-party-label">{i18n('party.addParty')}</span></div>}>
                    {refTables.partyTypes.items.map(i=> {return <MenuItem key={'party' + i.partyTypeId} onClick={() => this.handleCreateParty(i.partyTypeId)} eventKey={i.partyTypeId}>{i.name}</MenuItem>})}
                </DropdownButton>
            </div>
        )
    };

    handleDetail = (partyId) => {
        if (this.props.onDetail) {
            this.props.onDetail(partyId);
        } else {
            this.dispatch(modalDialogHide());
            this.dispatch(partySelect(partyId, null));
            this.dispatch(routerNavigate('party'));
        }
    };

    render() {
        /** onChange a onBlur nutno excludnout z other props - jinak by vlezno na autocomplete a přestal by fugnovat event on Change na komponentě **/
        const {userDetail, locked, value, onChange, onBlur, detail, ...otherProps} = this.props;

        let footer;
       /* if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, perms.REG_SCOPE_WR)) {
            footer = this.renderFooter()
        }

        const actions = [];
        if (value && value.partyId && detail) {
            if (userDetail.hasOne(perms.REG_SCOPE_RD_ALL, {type: perms.REG_SCOPE_RD, scopeId: value.record.scopeId})) {
                actions.push(<div onClick={this.handleDetail.bind(this, value.partyId)}
                                  className={'btn btn-default detail'}><Icon glyph={'fa-user'}/></div>);
            }
        }*/
        return <div className='desc-item-value desc-item-value-parts'>
            <Autocomplete
                ref='autocomplete'
                customFilter
                footer={footer}
                value={value}
                items={this.state.registryList}
                getItemId={(item) => item ? item.partyId : null}
                getItemName={(item) => item && item.record ? item.record : ''}
                onSearchChange={this.handleSearchChange}
                onChange={this.handleChange}
                onBlur={this.handleBlur}
                renderItem={this.renderRecord}
                //actions={[actions]}
                {...otherProps}
            />
        </div>;
    }
}

export default connect((state) => {
    const {refTables, userDetail} = state;
    return {
        refTables,
        userDetail,
    }
}, null, null, { withRef: true })(RegistryField);
