import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, TooltipTrigger, AbstractReactComponent, Autocomplete, ExtImportForm, RegistryListItem} from 'components/index.jsx';
import {registryListFilter} from 'actions/registry/registry.jsx'

import {Button} from 'react-bootstrap'
import {connect} from 'react-redux'
import * as perms from 'actions/user/Permission.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {registryDetailFetchIfNeeded} from 'actions/registry/registry.jsx'
import classNames from 'classnames';
import {routerNavigate} from 'actions/router.jsx'
import {debounce} from 'shared/utils'

import {DEFAULT_LIST_SIZE, MODAL_DIALOG_VARIANT} from 'constants'

import './RegistryField.less'

const AUTOCOMPLETE_REGISTRY_LIST_SIZE = DEFAULT_LIST_SIZE;

class RegistryField extends AbstractReactComponent {

    static defaultProps = {
        detail: false,
        footer: false,
        footerButtons: true,
        itemSpecId: null,
        registryParent: null,
        registerTypeId: null,
        roleTypeId: null,
        partyId: null,
        versionId: null,
    };

    static PropTypes = {
        detail: React.PropTypes.bool.isRequired,
        footer: React.PropTypes.bool.isRequired,
        footerButtons: React.PropTypes.bool,
        value: React.PropTypes.object,
        onChange: React.PropTypes.func.isRequired,
        onDetail: React.PropTypes.func,
        onCreate: React.PropTypes.func.isRequired,
        registryParent: React.PropTypes.number,
        registerTypeId: React.PropTypes.number,
        itemSpecId: React.PropTypes.number,
        roleTypeId: React.PropTypes.number,
        partyId: React.PropTypes.number,
        versionId: React.PropTypes.number
    };

    state = {registryList: [], count: null, searchText: null};


    focus = () => {
        this.refs.autocomplete.focus()
    };

    handleSearchChange = debounce((text) => {
        text = text == "" ? null : text;
        this.setState({searchText: text});
        const {roleTypeId, partyId, registryParent, registerTypeId, versionId, itemSpecId} = this.props;
        let promise = null;
        if (roleTypeId || partyId) {
            promise = WebApi.findRecordForRelation(text, roleTypeId, partyId, 0, AUTOCOMPLETE_REGISTRY_LIST_SIZE);
        } else {
            promise = WebApi.findRegistry(text, registryParent, registerTypeId, versionId, itemSpecId, 0, AUTOCOMPLETE_REGISTRY_LIST_SIZE);
        }
        promise.then(json => {
            this.setState({
                registryList: json.rows,
                count: json.count
            })
        })
    }, 500);

    handleDetail = (id) => {
        const {searchText} = this.state;
        const {onChange, onBlur, onDetail, onSelectModule, value} = this.props;
        if (onSelectModule) {
            onSelectModule({
                onSelect: (data) => {
                    this.handleChange(data);
                    this.handleBlur(data);
                },
                filterText: searchText,
                value
            })
        } else if (onDetail) {
            onDetail(id);
        } else {
            this.dispatch(registryDetailFetchIfNeeded(id));
            this.dispatch(routerNavigate('registry'));
        }
    };


    handleImport = () => {
        const {versionId} = this.props;
        this.refs.autocomplete.closeMenu();
        this.dispatch(modalDialogShow(this, i18n('extImport.title'), <ExtImportForm isParty={false} versionId={versionId}/>, "dialog-lg"));
    };

    handleCreateRecord = () => {
        this.refs.autocomplete.closeMenu();
        this.props.onCreateRecord();
    };

    handleChange = (e) => {
        this.setState({searchText: null});
        this.normalizeValue(this.props.onChange)(e)
    };

    handleBlur = (e) => {
        this.setState({searchText: null});
        this.normalizeValue(this.props.onBlur)(e)
    };

    renderFooter = () => {
        const {footerButtons, userDetail} = this.props;
        return <div>
            {footerButtons && userDetail.hasOne(perms.REG_SCOPE_WR_ALL, perms.REG_SCOPE_WR) && <div className="create-record">
                <Button onClick={this.handleCreateRecord} type="button"><Icon glyph='fa-plus'/>{i18n('registry.addNewRegistry')}</Button>
                <Button onClick={this.handleImport} type="button"><Icon glyph='fa-plus' /> {i18n("ribbon.action.registry.importExt")}</Button>
            </div>}
            {this.state.count !== null && this.state.count > AUTOCOMPLETE_REGISTRY_LIST_SIZE && <div className="items-count">
                {i18n('registryField.visibleCount', this.state.registryList.length, this.state.count)}
            </div>}
        </div>
    };

    renderRecord = (item, focus, active) => <TooltipTrigger
            content={item.characteristics}
            holdOnHover
            placement="horizontal"
            className="tooltip-container"
        >
            <RegistryListItem {...item} className={classNames('item', {focus, active})} />
        </TooltipTrigger>;


    normalizeValue = (call) => (obj,id) => {
        // změna typu aby se objekt dal použít jako návazný
        const newobj = {
            ...obj,
            '@class': 'cz.tacr.elza.controller.vo.RegRecordVO',
        };
        call(newobj, id);
    };

    render() {
        const {onChange, onBlur, footer, detail, value, className, ...otherProps} = this.props;

        let footerRender = null;
        if (footer) {
            footerRender = this.renderFooter();
        }

        let actions = [];
        if (detail) {
            // if (value && userDetail.hasOne(perms.REG_SCOPE_RD_ALL, {type: perms.REG_SCOPE_RD, scopeId: value.scopeId})) {
                actions.push(<div onClick={this.handleDetail.bind(this, value ? value.id : null)} className={'btn btn-default detail'}><Icon glyph={'fa-th-list'}/></div>);
            // }
        }

        return <Autocomplete
            ref='autocomplete'
            customFilter
            className={classNames("autocomplete-record", className)}
            footer={footerRender}
            items={this.state.registryList}
            getItemId={(item) => item ? item.id : null}
            getItemName={(item) => item && item.record ? item.record : ''}
            onSearchChange={this.handleSearchChange}
            renderItem={this.renderRecord}
            actions={[actions]}
            onChange={this.handleChange}
            onBlur={this.handleBlur}
            value={value}
            {...otherProps}
        />;
    }
}

export default connect(({userDetail}) => ({userDetail}), null, null, { withRef: true })(RegistryField);
