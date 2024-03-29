import PropTypes from 'prop-types';
import React from 'react';
import {WebApi} from 'actions/index.jsx';
import {AbstractReactComponent, Autocomplete, i18n, Icon, TooltipTrigger} from 'components/shared';

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
import {refApTypesFetchIfNeeded} from '../../actions/refTables/apTypes';
import {JAVA_ATTR_CLASS, JAVA_CLASS_AP_ACCESS_POINT_VO} from '../../constants';
import {goToAe} from "../../actions/registry/registry";
import {withRouter} from "react-router";

const AUTOCOMPLETE_REGISTRY_LIST_SIZE = DEFAULT_LIST_SIZE;

class RegistryField extends AbstractReactComponent {
    refAutocomplete = null;

    static defaultProps = {
        detail: false,
        footer: false,
        footerButtons: true,
        itemSpecId: null,
        undefined: false,
        registryParent: null,
        apTypeId: null,
        versionId: null,
        useIdAsValue: false,
        addEmpty: false,
    };

    static propTypes = {
        detail: PropTypes.bool.isRequired,
        footer: PropTypes.bool.isRequired,
        footerButtons: PropTypes.bool,
        value: PropTypes.object,
        undefined: PropTypes.bool,
        onChange: PropTypes.func.isRequired,
        onDetail: PropTypes.func,
        onCreateRecord: PropTypes.func,
        registryParent: PropTypes.number,
        apTypeId: PropTypes.number,
        itemSpecId: PropTypes.number,
        versionId: PropTypes.number,
        useIdAsValue: PropTypes.bool,
        addEmpty: PropTypes.bool,
        emptyTitle: PropTypes.string,
    };

    state = {registryList: [], count: null, searchText: null};

    componentDidMount() {
        this.props.dispatch(refApTypesFetchIfNeeded());
    }

    focus = () => {
        this.refAutocomplete.focus();
    };

    handleSearchChange = debounce(text => {
        text = text === '' ? null : text;
        this.setState({searchText: text});
        const {registryParent, apTypeId, versionId, itemSpecId, itemTypeId} = this.props;
        WebApi.findAccessPoint(
            text,
            registryParent,
            apTypeId,
            versionId,
            itemTypeId,
            itemSpecId,
            0,
            AUTOCOMPLETE_REGISTRY_LIST_SIZE,
        ).then(json => {
            this.setState({
                registryList: json.rows,
                count: json.count,
            });
        });
    }, 500);

    handleDetail = id => {
        const {searchText} = this.state;
        const {onDetail, onSelectModule, value, history} = this.props;

        this.refAutocomplete.closeMenu();

        if (onSelectModule) {
            onSelectModule({
                onSelect: data => {
                    this.handleChange(data);
                    // TODO kvůli setState v DescItemType nutno zpozdit aby stihl uložit hodnotu a uložil tu co potřebujeme
                    setTimeout(() => this.handleBlur(data), 200);
                },
                filterText: searchText,
                value,
            });
        } else if (onDetail) {
            onDetail(id);
        } else {
            this.props.dispatch(goToAe(history, id));
        }
    };

    handleCreateRecord = () => {
        this.refAutocomplete.closeMenu();
        this.props.onCreateRecord && this.props.onCreateRecord();
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
        const {apTypeIdMap, eidTypes, addEmpty, emptyTitle} = this.props;

        return (
            <TooltipTrigger
                key={item.id}
                content={item.description}
                holdOnHover
                placement="horizontal"
                className="tooltip-container"
                {...otherProps}
            >
                <RegistryListItem
                    {...item}
                    key={'reg-' + item.id}
                    eidTypes={eidTypes}
                    apTypeIdMap={apTypeIdMap}
                    addEmpty={addEmpty}
                    emptyTitle={emptyTitle}
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
        if (this.props.addEmpty && (obj == null || obj === '' || obj.id === -1)) {
            return null;
        }
        const newobj = {
            ...obj,
            [JAVA_ATTR_CLASS]: JAVA_CLASS_AP_ACCESS_POINT_VO,
        };
        return newobj;
    };

    render() {
        const {value, footer, detail, className, addEmpty, ...otherProps} = this.props;

        let footerRender = null;
        if (footer) {
            footerRender = this.renderFooter();
        }

        let actions = [];
        if (detail) {
            actions.push(
                <div
                    key={'detail'}
                    onClick={this.handleDetail.bind(this, value ? value.id : null)}
                    className="btn btn-default detail"
                >
                    <Icon glyph="fa-th-list" />
                </div>,
            );
        }

        let tmpVal = '';
        if (this.props.undefined) {
            tmpVal = i18n('subNodeForm.descItemType.undefinedValue');
        }

        let items = this.state.registryList;
        if (addEmpty) {
            items = [{id: -1, name: 'Prázdný'}, ...items];
        }

        return (
            <Autocomplete
                {...otherProps}
                ref={ref => (this.refAutocomplete = ref)}
                customFilter
                className={classNames('autocomplete-record', className)}
                footer={footerRender}
                items={items}
                getItemId={item => (item ? item.id : null)}
                getItemName={item => (item && item.name ? item.name : tmpVal)}
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

export default withRouter(connect(
    state => {
        const {
            userDetail,
            refTables: {apTypes, eidTypes},
        } = state;
        return {
            apTypeIdMap: apTypes.itemsMap,
            userDetail,
            eidTypes: eidTypes.data,
        };
    },
    null,
    null,
    {forwardRef: true},
)(RegistryField));
