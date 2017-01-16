import React from 'react';
import ReactDOM from 'react-dom';

import {WebApi} from 'actions/index.jsx';
import {TooltipTrigger, Icon, i18n, AbstractReactComponent, Autocomplete, ExtImportForm, PartyListItem} from 'components/index.jsx';
import {connect} from 'react-redux'
//import {decorateAutocompleteValue} from './DescItemUtils.jsx'
import {MenuItem, DropdownButton, Button} from 'react-bootstrap';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {partyDetailFetchIfNeeded, partyAdd, RELATION_CLASS_CODES, partyListFilter} from 'actions/party/party.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {DEFAULT_LIST_SIZE, MODAL_DIALOG_VARIANT} from 'constants'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import classNames from 'classnames';

import {PartySelectPage} from 'pages'

import './PartyField.less'

const AUTOCOMPLETE_PARTY_LIST_SIZE = DEFAULT_LIST_SIZE;

class PartyField extends AbstractReactComponent {

    static defaultProps = {
        detail: false,
        footer: true,
        footerButtons: true,
        partyTypeId: null
    };

    static PropTypes = {
        detail: React.PropTypes.bool.isRequired,
        footer: React.PropTypes.bool,
        footerButtons: React.PropTypes.bool,
        value: React.PropTypes.object,
        onChange: React.PropTypes.func.isRequired,
        onDetail: React.PropTypes.func,
        onCreate: React.PropTypes.func.isRequired,
        partyTypeId: React.PropTypes.number,
        versionId: React.PropTypes.number
    };

    state = {partyList: [], count: null, searchText: null};

    componentDidMount() {
        this.dispatch(refPartyTypesFetchIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(refPartyTypesFetchIfNeeded());
    }

    focus = () => {
        this.refs.autocomplete.focus()
    };

    handleSearchChange = (text) => {

        text = text == "" ? null : text;
        this.setState({searchText: text});
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
        const {refTables:{partyTypes:{relationTypesForClass}}} = this.props;

        return <PartyListItem {...item} className={classNames('item', {focus: isHighlighted, active: isSelected})} relationTypesForClass={relationTypesForClass} />;
        /*<TooltipTrigger
            content={item.characteristics}
            holdOnHover
            placement="horizontal"
            className="tooltip-container"
        >

        </TooltipTrigger>*/
    };



    handleChange = (e) => {
        this.setState({searchText: null});
        this.props.onChange(e)
    };

    handleBlur = (e) => {
        this.setState({searchText: null});
        this.props.onBlur(e)
    };

    renderFooter = () => {
        const {refTables, footerButtons, userDetail} = this.props;
        return <div>
            {footerButtons && userDetail.hasOne(perms.REG_SCOPE_WR_ALL, perms.REG_SCOPE_WR) && <div className="create-party">
                <DropdownButton noCaret title={<div><Icon glyph='fa-download' /><span className="create-party-label">{i18n('party.addParty')}</span></div>} id="party-field" >
                    {refTables.partyTypes.items.map(type => <MenuItem key={'party' + type.id} onClick={() => this.handleCreateParty(type.id)} eventKey={type.id}>{type.name}</MenuItem>)}
                </DropdownButton>
                <Button onClick={this.handleImport} type="button"><Icon glyph='fa-plus' /> {i18n("ribbon.action.party.importExt")}</Button>
            </div>}
            {this.state.count !== null && this.state.count > AUTOCOMPLETE_PARTY_LIST_SIZE && <div className="items-count">
                {i18n('partyField.visibleCount', this.state.partyList.length, this.state.count)}
            </div>}
        </div>
    };

    handleImport = () => {
        const {versionId} = this.props;
        this.refs.autocomplete.closeMenu();
        this.dispatch(modalDialogShow(this, i18n('extImport.title'), <ExtImportForm isParty={true} versionId={versionId}/>, "dialog-lg"));
    };

    handleDetail = (partyId) => {
        if (this.props.onDetail) {
            this.props.onDetail(partyId);
        } else {
            const {onChange, onBlur, partyList:{filter}} = this.props;
            const {searchText} = this.state;
            this.dispatch(partyListFilter({...filter, text:searchText}))
            this.dispatch(partyDetailFetchIfNeeded(partyId));
            this.dispatch(modalDialogShow(this, null, <PartySelectPage onChange={(data) => {
                onChange(data);
                onBlur(data);
            }} />, classNames(MODAL_DIALOG_VARIANT.FULLSCREEN, MODAL_DIALOG_VARIANT.NO_HEADER)));
        }
    };

    render() {
        const {userDetail, value, detail, footer, ...otherProps} = this.props;

        let footerRender;
        if (footer) {
            // if () {
                footerRender = this.renderFooter();
            // }
        }

        const actions = [];
        if (detail) {
            // if (userDetail.hasOne(perms.REG_SCOPE_RD_ALL, {type: perms.REG_SCOPE_RD, scopeId: value.record.scopeId})) {
                actions.push(<div onClick={this.handleDetail.bind(this, value ? value.id : null)}
                                  className={'btn btn-default detail'}><Icon glyph={'fa-user'}/></div>);
            // }
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
            onBlur={this.handleBlur}
            onChange={this.handleChange}
        />
    }
}

export default connect((state) => {
    const {refTables, userDetail, app:{partyList}} = state;
    return {
        refTables,
        userDetail,
        partyList
    }
}, null, null, { withRef: true })(PartyField);
