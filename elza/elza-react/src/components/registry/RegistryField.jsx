import PropTypes from 'prop-types';
import React from 'react';
import {WebApi} from 'actions/index.jsx';
import {AbstractReactComponent, Autocomplete, i18n, Icon, TooltipTrigger} from 'components/shared';
import {registryDetailFetchIfNeeded} from 'actions/registry/registry.jsx';

import {Button} from '../ui';
import {connect} from 'react-redux';
import * as perms from 'actions/user/Permission.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import classNames from 'classnames';
import {routerNavigate} from 'actions/router.jsx';
import {debounce} from 'shared/utils';

import {DEFAULT_LIST_SIZE} from '../../constants.tsx';

import './RegistryField.scss';
import RegistryListItem from './RegistryListItem';
import ExtImportForm from '../form/ExtImportForm';
import {refRecordTypesFetchIfNeeded} from '../../actions/refTables/recordTypes';

const AUTOCOMPLETE_REGISTRY_LIST_SIZE = DEFAULT_LIST_SIZE;

class RegistryField extends AbstractReactComponent {
    static defaultProps = {
        detail: false,
        footer: false,
        footerButtons: true,
        itemSpecId: null,
        undefined: false,
        registryParent: null,
        apTypeId: null,
        roleTypeId: null,
        partyId: null,
        versionId: null,
        useIdAsValue: false
    };

    static propTypes = {
        detail: PropTypes.bool.isRequired,
        footer: PropTypes.bool.isRequired,
        footerButtons: PropTypes.bool,
        value: PropTypes.object,
        undefined: PropTypes.bool,
        onChange: PropTypes.func.isRequired,
        onDetail: PropTypes.func,
        onCreate: PropTypes.func.isRequired,
        registryParent: PropTypes.number,
        apTypeId: PropTypes.number,
        itemSpecId: PropTypes.number,
        roleTypeId: PropTypes.number,
        partyId: PropTypes.number,
        versionId: PropTypes.number,
        useIdAsValue: PropTypes.bool,
    };

    state = {registryList: [], count: null, searchText: null};

    componentDidMount() {
        this.props.dispatch(refRecordTypesFetchIfNeeded());
    }

    focus = () => {
        this.refs.autocomplete.focus();
    };

    handleSearchChange = debounce(text => {
        text = text === '' ? null : text;
        this.setState({searchText: text});
        const {roleTypeId, partyId, registryParent, apTypeId, versionId, itemSpecId, itemTypeId} = this.props;
        let promise = null;
        if (roleTypeId || partyId) {
            promise = WebApi.findRecordForRelation(text, roleTypeId, partyId, 0, AUTOCOMPLETE_REGISTRY_LIST_SIZE);
        } else {
            promise = WebApi.findRegistry(
                text,
                registryParent,
                apTypeId,
                versionId,
                itemTypeId,
                itemSpecId,
                0,
                AUTOCOMPLETE_REGISTRY_LIST_SIZE,
            );
        }
        promise.then(json => {
            this.setState({
                registryList: json.rows,
                count: json.count,
            });
        });
    }, 500);

    handleDetail = id => {
        const {searchText} = this.state;
        const {onDetail, onSelectModule, value} = this.props;

        this.refs.autocomplete.closeMenu();

        if (onSelectModule) {
            onSelectModule({
                onSelect: data => {
                    this.handleChange(data);
                    this.handleBlur(data);
                },
                filterText: searchText,
                value,
            });
        } else if (onDetail) {
            onDetail(id);
        } else {
            this.props.dispatch(registryDetailFetchIfNeeded(id));
            this.props.dispatch(routerNavigate('registry'));
        }
    };

    handleImport = () => {
        const {versionId} = this.props;
        this.refs.autocomplete.closeMenu();
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('extImport.title'),
                <ExtImportForm isParty={false} versionId={versionId} />,
                'dialog-lg',
            ),
        );
    };

    handleCreateRecord = () => {
        this.refs.autocomplete.closeMenu();
        this.props.onCreateRecord();
    };

    handleChange = e => {
        this.setState({searchText: null});
        const value = this.normalizeValue(e);
        this.props.onChange(value);
    };

    handleBlur = e => {
        this.setState({searchText: null});
        const value = this.normalizeValue(e);
        this.props.onBlur(value);
    };

    renderFooter = () => {
        const {footerButtons, userDetail} = this.props;
        const {count, registryList} = this.state;

        const buttons = footerButtons && userDetail.hasOne(perms.AP_SCOPE_WR_ALL, perms.AP_SCOPE_WR);
        const hasCount = count !== null && (count > AUTOCOMPLETE_REGISTRY_LIST_SIZE || count === 0);

        return hasCount || buttons ? (
            <div>
                {buttons && (
                    <div className="create-record">
                        <Button onClick={this.handleCreateRecord} type="button">
                            <Icon glyph="fa-plus" />
                            {i18n('registry.addNewRegistry')}
                        </Button>
                        <Button onClick={this.handleImport} type="button">
                            <Icon glyph="fa-plus" /> {i18n('ribbon.action.registry.importExt')}
                        </Button>
                    </div>
                )}
                {count > AUTOCOMPLETE_REGISTRY_LIST_SIZE && (
                    <div className="items-count">{i18n('registryField.visibleCount', registryList.length, count)}</div>
                )}
                {count === 0 && <div className="items-count">{i18n('registryField.noItemsFound')}</div>}
            </div>
        ) : null;
    };

    renderRecord = props => {
        const {item, highlighted, selected, ...otherProps} = props;
        const {apTypeIdMap, eidTypes} = this.props;

        return (
            <TooltipTrigger
                content={item.characteristics}
                holdOnHover
                placement="horizontal"
                className="tooltip-container"
                {...otherProps}
            >
                <RegistryListItem
                    {...item}
                    eidTypes={eidTypes}
                    apTypeIdMap={apTypeIdMap}
                    className={classNames('item', {focus: highlighted, active: selected})}
                />
            </TooltipTrigger>
        );
    };

    normalizeValue = obj => {
        if (this.props.useIdAsValue) {
            return obj;
        }
        // změna typu aby se objekt dal použít jako návazný
        const newobj = {
            ...obj,
            '@class': 'cz.tacr.elza.controller.vo.ApAccessPointVO',
        };
        return newobj;
    };

    render() {
        const {value, footer, detail, className, ...otherProps} = this.props;

        let footerRender = null;
        if (footer) {
            footerRender = this.renderFooter();
        }

        let actions = [];
        if (detail) {
            actions.push(
                <div onClick={this.handleDetail.bind(this, value ? value.id : null)} className="btn btn-default detail">
                    <Icon glyph="fa-th-list" />
                </div>,
            );
        }

        let tmpVal = '';
        if (this.props.undefined) {
            tmpVal = i18n('subNodeForm.descItemType.notIdentified');
        }

        return (
            <Autocomplete
                {...otherProps}
                ref="autocomplete"
                customFilter
                className={classNames('autocomplete-record', className)}
                footer={footerRender}
                items={this.state.registryList}
                getItemId={item => (item ? item.id : null)}
                getItemName={item => (item && item.record ? item.record : tmpVal)}
                onSearchChange={this.handleSearchChange}
                renderItem={this.renderRecord}
                actions={[actions]}
                onChange={this.handleChange}
                onBlur={this.handleBlur}
                value={value}
            />
        );
    }
}

export default connect(state => {
    const {
        userDetail,
        refTables: {apTypes, eidTypes},
    } = state;
    return {
        apTypeIdMap: apTypes.itemsMap,
        userDetail,
        eidTypes: eidTypes.data,
    };
})(RegistryField);
