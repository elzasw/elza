import PropTypes from 'prop-types';
import React from 'react';

import {WebApi} from 'actions/index.jsx';
import {AbstractReactComponent, Autocomplete, i18n, Icon, TooltipTrigger} from 'components/shared';
import {connect} from 'react-redux';
import {Dropdown, DropdownButton} from 'react-bootstrap';
import {Button} from '../ui';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes.jsx';
import * as perms from 'actions/user/Permission.jsx';
import {partyDetailFetchIfNeeded} from 'actions/party/party.jsx';
import {routerNavigate} from 'actions/router.jsx';
import {DEFAULT_LIST_SIZE} from '../../constants.tsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import {debounce} from 'shared/utils';
import classNames from 'classnames';

import './PartyField.scss';
import PartyListItem from './PartyListItem';
import ExtImportForm from '../form/ExtImportForm';

const AUTOCOMPLETE_PARTY_LIST_SIZE = DEFAULT_LIST_SIZE;

class PartyField extends AbstractReactComponent {

    static defaultProps = {
        detail: false,
        footer: true,
        undefined: false,
        footerButtons: true,
        partyTypeId: null,
    };

    static propTypes = {
        detail: PropTypes.bool.isRequired,
        footer: PropTypes.bool,
        footerButtons: PropTypes.bool,
        undefined: PropTypes.bool,
        value: PropTypes.object,
        onChange: PropTypes.func.isRequired,
        onDetail: PropTypes.func,
        onCreate: PropTypes.func.isRequired,
        partyTypeId: PropTypes.number,
        versionId: PropTypes.number,
    };

    state = { partyList: [], count: null, searchText: null };

    componentDidMount() {
        this.props.dispatch(refPartyTypesFetchIfNeeded());
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(refPartyTypesFetchIfNeeded());
    }

    focus = () => {
        this.refs.autocomplete.focus();
    };

    handleSearchChange = debounce((text) => {
        text = text === '' ? null : text;
        this.setState({ searchText: text });
        WebApi.findParty(text, this.props.versionId, this.props.partyTypeId, null, 0, AUTOCOMPLETE_PARTY_LIST_SIZE).then(json => {
            this.setState({
                partyList: json.rows,
                count: json.count,
            });
        });
    });

    handleCreateParty = (partyTypeId) => {
        this.refs.autocomplete.closeMenu();
        this.props.onCreate(partyTypeId);
    };

    renderParty = (props) => {
        const { refTables: { partyTypes: { relationTypesForClass } } } = this.props;
        const { item, highlighted, selected, ...otherProps } = props;

        return <TooltipTrigger
            content={item.characteristics}
            holdOnHover
            placement="horizontal"
            className="tooltip-container"
            {...otherProps}
        >
            <PartyListItem
                {...item}
                className={classNames('item', { focus: highlighted, active: selected })}
                relationTypesForClass={relationTypesForClass}
            />
        </TooltipTrigger>;
    };


    handleChange = (e) => {
        this.setState({ searchText: null });
        this.props.onChange(e);
    };

    handleBlur = (e) => {
        this.setState({ searchText: null });
        this.props.onBlur(e);
    };

    renderFooter = () => {

        const { refTables, footerButtons, userDetail } = this.props;
        const { count } = this.state;

        const buttons = footerButtons && userDetail.hasOne(perms.AP_SCOPE_WR_ALL, perms.AP_SCOPE_WR);
        const hasCount = count !== null && (count > AUTOCOMPLETE_PARTY_LIST_SIZE || count === 0);

        return hasCount || buttons ? <div>
            {buttons && <div className="create-party">
                <DropdownButton title={<div><Icon glyph='fa-download'/><span
                    className="create-party-label">{i18n('party.addParty')}</span></div>} id="party-field">
                    {refTables.partyTypes.items.map(type => <Dropdown.Item key={'party' + type.id}
                                                                           onClick={() => this.handleCreateParty(type.id)}
                                                                           eventKey={type.id}>{type.name}</Dropdown.Item>)}
                </DropdownButton>
                <Button onClick={this.handleImport} type="button"><Icon
                    glyph='fa-plus'/> {i18n('ribbon.action.party.importExt')}</Button>
            </div>}
            {count > AUTOCOMPLETE_PARTY_LIST_SIZE && <div className="items-count">
                {i18n('partyField.visibleCount', this.state.partyList.length, this.state.count)}
            </div>}
            {count === 0 && <div className="items-count">
                {i18n('partyField.noItemsFound')}
            </div>}
        </div> : null;
    };

    handleImport = () => {
        const { versionId } = this.props;
        this.refs.autocomplete.closeMenu();
        this.props.dispatch(modalDialogShow(this, i18n('extImport.title'), <ExtImportForm isParty={true}
                                                                                          versionId={versionId === null ? -1 : versionId}/>, 'dialog-lg'));
    };

    handleDetail = (id) => {
        const { searchText } = this.state;
        const { onDetail, onSelectModule, value } = this.props;

        this.refs.autocomplete.closeMenu();

        if (onSelectModule) {
            onSelectModule({
                onSelect: (data) => {
                    this.handleChange(data);
                    this.handleBlur(data);
                },
                filterText: searchText,
                value,
            });
        } else if (onDetail) {
            onDetail(id);
        } else {
            this.props.dispatch(partyDetailFetchIfNeeded(id));
            this.props.dispatch(routerNavigate('party'));
        }
    };

    render() {
        const { value, footer, detail, ...otherProps } = this.props;

        let footerRender;
        if (footer) {
            footerRender = this.renderFooter();
        }

        const actions = [];
        if (detail) {
            // if (userDetail.hasOne(perms.AP_SCOPE_RD_ALL, {type: perms.AP_SCOPE_RD, scopeId: value.record.scopeId})) {
            actions.push(
                <div
                    onClick={this.handleDetail.bind(this, value ? value.id : null)}
                    className={'btn btn-default detail'}
                >
                    <Icon glyph={'fa-user'}/>
                </div>,
            );
            // }
        }

        let tmpVal = '';
        if (this.props.undefined) {
            tmpVal = i18n('subNodeForm.descItemType.notIdentified');
        }

        return <Autocomplete
            //ref='autocomplete' TODO React 16 REF
            customFilter
            className="autocomplete-party"
            footer={footerRender}
            items={this.state.partyList}
            getItemId={(item) => item ? item.id : null}
            getItemName={(item) => item && item.accessPoint ? item.accessPoint.record : tmpVal}
            onSearchChange={this.handleSearchChange}
            renderItem={this.renderParty}
            actions={[actions]}
            onChange={this.handleChange}
            onBlur={this.handleBlur}
            value={value}
            {...otherProps}
        />;
    }
}

export default connect(({ refTables, userDetail }) => ({ refTables, userDetail }))(PartyField);
