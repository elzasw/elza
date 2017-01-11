import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions/index.jsx';
import {Icon, i18n, AbstractReactComponent, Autocomplete, ExtImportForm} from 'components/index.jsx';
import {Button} from 'react-bootstrap'
import {connect} from 'react-redux'
import * as perms from 'actions/user/Permission.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'

import {DEFAULT_LIST_SIZE} from 'constants'

import './RegistryField.less'

const AUTOCOMPLETE_REGISTRY_LIST_SIZE = DEFAULT_LIST_SIZE;

class RegistryField extends AbstractReactComponent {

    static defaultProps = {
        detail: false,
        footer: false,
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

    state = {registryList: [], count: null};


    focus = () => {
        this.refs.autocomplete.focus()
    };

    handleSearchChange = (text) => {
        text = text == "" ? null : text;
        const {roleTypeId, partyId, registryParent, registerTypeId, versionId, itemSpecId} = this.props;
        let promise = null;
        if (roleTypeId || partyId) {
            promise = WebApi.findRecordForRelation(text, roleTypeId, partyId, 0, AUTOCOMPLETE_REGISTRY_LIST_SIZE);
        } else {
            promise = WebApi.findRegistry(text, registryParent, registerTypeId, versionId, itemSpecId, 0, AUTOCOMPLETE_REGISTRY_LIST_SIZE);
        }
        promise.then(json => {
            this.setState({
                registryList: json.recordList,
                count: json.count
            })
        })
    };

    handleDetail = (recordId) => {
        this.props.onDetail(recordId);
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

    renderFooter = () => {
        return <div>
            <div className="create-record">
                <Button onClick={this.handleCreateRecord} type="button"><Icon glyph='fa-plus'/>{i18n('registry.addNewRegistry')}</Button>
                <Button onClick={this.handleImport} type="button"><Icon glyph='fa-plus' /> {i18n("ribbon.action.registry.importExt")}</Button>
            </div>
            {this.state.count !== null && this.state.count > AUTOCOMPLETE_REGISTRY_LIST_SIZE && <div className="items-count">
                {i18n('registryField.visibleCount', this.state.registryList.length, this.state.count)}
            </div>}
        </div>
    };

    renderRecord = (item, isHighlighted, isSelected) => {
        let cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        return <div className={cls} key={item.id} >
            <div className="name" title={item.record}>{item.record}</div>
            <div className="characteristics" title={item.characteristics}>{item.characteristics}</div>
        </div>;
    };

    normalizeValue = (call) => (obj,id) => {
        // změna typu aby se objekt dal použít jako návazný
        const newobj = {
            ...obj,
            '@class': 'cz.tacr.elza.controller.vo.RegRecordVO',
        };
        call(newobj, id);
    };

    render() {
        const {onChange, onBlur, footer, detail, userDetail, value, ...otherProps} = this.props;

        let footerRender = null;
        if (footer) {
            if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, perms.REG_SCOPE_WR)) {
                footerRender = this.renderFooter();
            }
        }

        let actions = [];
        if (detail) {
            if (value && userDetail.hasOne(perms.REG_SCOPE_RD_ALL, {type: perms.REG_SCOPE_RD, scopeId: value.scopeId})) {
                actions.push(<div onClick={this.handleDetail.bind(this, value.id)}
                                  className={'btn btn-default detail'}><Icon glyph={'fa-user'}/></div>);
            }
        }

        return <Autocomplete
            ref='autocomplete'
            customFilter
            className="autocomplete-record"
            footer={footerRender}
            items={this.state.registryList}
            getItemId={(item) => item ? item.id : null}
            getItemName={(item) => item && item.record ? item.record : ''}
            onSearchChange={this.handleSearchChange}
            renderItem={this.renderRecord}
            actions={[actions]}
            onChange={this.normalizeValue(onChange)}
            onBlur={this.normalizeValue(onBlur)}
            value={value}
            {...otherProps}
        />;
    }
}

export default connect((state) => {
    const {userDetail} = state;
    return {
        userDetail,
    }
}, null, null, { withRef: true })(RegistryField);
