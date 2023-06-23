/**
 * Komponenta list rejstříků
 */
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import {
    AbstractReactComponent,
    Autocomplete,
    i18n,
    Icon,
    ListBox,
    SearchWithGoto,
    StoreHorizontalLoader,
} from 'components/shared';
import { refRecordTypesFetchIfNeeded } from 'actions/refTables/recordTypes';
import { indexById } from 'stores/app/utils';
import {
    DEFAULT_REGISTRY_LIST_MAX_SIZE,
    registryListFetchIfNeeded,
    registryListFilter,
    registryListInvalidate,
} from 'actions/registry/registry';
import { canSetFocus, focusWasSet, isFocusFor } from 'actions/global/focus';
import { getTreeItemById } from './../../components/registry/registryUtils';
import { StateApproval, StateApprovalCaption } from '../../api/StateApproval';
import { RevStateApproval, RevStateApprovalCaption } from '../../api/RevStateApproval';
import './RegistryList.scss';
import RegistryListItem from './RegistryListItem';
import ListPager from '../shared/listPager/ListPager';
import * as perms from '../../actions/user/Permission';
import { FOCUS_KEYS } from '../../constants.tsx';
import { requestScopesIfNeeded } from '../../actions/refTables/scopesData';
import { Col, Row } from 'react-bootstrap';
import { modalDialogHide, modalDialogShow } from '../../actions/global/modalDialog';
import { Area } from '../../api/Area';
import ExtFilterModal from './modal/ExtFilterModal';
import { Button } from '../ui';
import { withRouter } from "react-router";
import { Link } from "react-router-dom";
import { goToAe, getArchiveEntityUrl } from "../../actions/registry/registry";
import { refRuleSetFetchIfNeeded } from 'actions/refTables/ruleSet';

class RegistryList extends AbstractReactComponent {
    static propTypes = {
        maxSize: PropTypes.number,
        fund: PropTypes.object,
    };

    static defaultProps = {
        maxSize: DEFAULT_REGISTRY_LIST_MAX_SIZE,
    };

    componentDidMount() {
        this.fetchIfNeeded();
        this.trySetFocus();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        this.trySetFocus(nextProps);
        if (nextProps.maxSize !== this.props.maxSize) {
            this.props.dispatch(registryListInvalidate());
        }
    }

    fetchIfNeeded = (props = this.props) => {
        const { maxSize } = props;
        this.props.dispatch(refRecordTypesFetchIfNeeded());
        this.props.dispatch(registryListFetchIfNeeded(0, maxSize));
        this.props.dispatch(requestScopesIfNeeded());
        this.props.dispatch(refRuleSetFetchIfNeeded());
    };

    trySetFocus = (props = this.props) => {
        const { focus } = props;

        if (canSetFocus() && focus) {
            if (isFocusFor(focus, null, 1)) {
                // focus po ztrátě
                if (this.refs.registryList) {
                    // ještě nemusí existovat
                    this.setState({}, () => {
                        this.refs.registryList.focus();
                        focusWasSet();
                    });
                }
            } else if (isFocusFor(focus, FOCUS_KEYS.REGISTRY, 1) || isFocusFor(focus, FOCUS_KEYS.REGISTRY, 1, 'list')) {
                this.setState({}, () => {
                    this.refs.registryList.focus();
                    focusWasSet();
                });
            }
        }
    };

    handleFilterType = e => {
        const val = e.target.value;
        this.props.dispatch(
            registryListFilter({
                ...this.props.registryList.filter,
                from: 0,
                type: val === -1 ? null : val,
            }),
        );
    };

    handleFilterText = filterText => {
        const { dispatch, registryList } = this.props;
        let searchFilter = registryList.filter.searchFilter;
        const text = filterText && filterText.length === 0 ? null : filterText;
        if (searchFilter) {
            searchFilter.search = text;
        }
        dispatch(
            registryListFilter({
                ...registryList.filter,
                from: 0,
                text: text,
                searchFilter: searchFilter,
            }),
        );
        dispatch(registryListInvalidate());
    };

    handleFilterRegistryType = item => {
        this.props.dispatch(
            registryListFilter({
                ...this.props.registryList.filter,
                from: 0,
                registryTypeId: item ? item.id : null,
            }),
        );
    };

    handleFilterRegistryScope = id => {
        this.props.dispatch(
            registryListFilter({
                ...this.props.registryList.filter,
                from: 0,
                scopeId: id,
                versionId: this.props.fund?.versionId,
            }),
        );
    };

    handleFilterRegistryState = state => {
        this.props.dispatch(
            registryListFilter({
                ...this.props.registryList.filter,
                from: 0,
                state: state,
            }),
        );
    };

    handleFilterRegistryRevState = revState => {
        this.props.dispatch(
            registryListFilter({
                ...this.props.registryList.filter,
                from: 0,
                revState: revState,
            }),
        );
    };

    handleFilterPrev = () => {
        let from = this.props.registryList.filter.from;
        if (this.props.registryList.filter.from >= DEFAULT_REGISTRY_LIST_MAX_SIZE) {
            from = this.props.registryList.filter.from - DEFAULT_REGISTRY_LIST_MAX_SIZE;
            this.props.dispatch(registryListFilter({ ...this.props.registryList.filter, from }));
        }
    };

    handleFilterNext = () => {
        let from = this.props.registryList.filter.from;
        if (this.props.registryList.filter.from < this.props.registryList.count - DEFAULT_REGISTRY_LIST_MAX_SIZE) {
            from = this.props.registryList.filter.from + DEFAULT_REGISTRY_LIST_MAX_SIZE;
            this.props.dispatch(registryListFilter({ ...this.props.registryList.filter, from }));
        }
    };

    filterRegistryTypeClear = () => {
        this.handleFilterRegistryType({ name: this.registryTypeDefaultValue });
    };

    handleFilterTextClear = () => {
        const { dispatch, registryList } = this.props;
        let searchFilter = registryList.filter.searchFilter;
        if (searchFilter) {
            searchFilter.search = null;
        }
        dispatch(
            registryListFilter({
                ...registryList.filter,
                from: 0,
                text: null,
                searchFilter: searchFilter,
            }),
        );
        dispatch(registryListInvalidate());
    };

    handleRegistryDetail = item => {
        const { dispatch, history, select } = this.props;
        dispatch(goToAe(history, item.id, false, !select));
    };

    renderListItem = props => {
        const { item } = props;
        const { eidTypes, apTypeIdMap, select } = this.props;

        return (
            <Link
                style={{ textDecoration: "none" }}
                to={getArchiveEntityUrl(item.id)}
                onClick={(e) => {
                    // Zabraneni zmeny adresy v adresnim radku, pokud
                    // je RegistryPage v rezimu modalu ( vyber entity
                    // pomoci tlacitka v RegistryField )
                    if (select) {
                        e.preventDefault();
                        e.stopPropagation();
                        this.handleRegistryDetail(item);
                    }
                }}
            >
                <RegistryListItem
                    {...item}
                    eidTypes={eidTypes}
                    apTypeIdMap={apTypeIdMap}
                />
            </Link>
        );
    };

    /**
     * Vrátí pole akcí pro registry type filtr
     * @return {array} pole akcí
     */
    getRegistryTypeActions = () => {
        const {
            registryList: { filter },
        } = this.props;
        const actions = [];
        if (filter.registryTypeId !== null && typeof filter.registryTypeId !== 'undefined') {
            actions.push(
                <div onClick={() => this.filterRegistryTypeClear()} className={'btn btn-default detail'}>
                    <Icon glyph={'fa-times'} />
                </div>,
            );
        }
        return actions;
    };

    filterScopes(scopes) {
        const { fund, userDetail } = this.props;
        return scopes.filter(scope => {
            const hasFundRestriction = fund !== undefined;
            const isAllowedInFund = hasFundRestriction && fund.apScopes?.find((fundScope) => scope.id === fundScope.id);
            return (!hasFundRestriction || isAllowedInFund) && userDetail.hasOne(perms.AP_SCOPE_RD_ALL, {
                type: perms.AP_SCOPE_RD,
                scopeId: scope.id,
            })
        }
        );
    }

    getScopesWithAll(scopes) {
        const defaultValue = { name: i18n('registry.all') };
        if (scopes && scopes.length > 0 && scopes[0] && scopes[0].scopes && scopes[0].scopes.length > 0) {
            return [defaultValue, ...this.filterScopes([...scopes[0].scopes])];
        }
        return [defaultValue];
    }

    getStateWithAll() {
        const defaultValue = { name: i18n('party.allApStates') };
        return [
            defaultValue,
            ...Object.values(StateApproval).map(item => {
                return {
                    id: item,
                    name: StateApprovalCaption(item),
                };
            }),
        ];
    }

    getRevStateWithAll() {
        const defaultValue = { name: i18n('registry.allRevisionStates') };
        return [
            defaultValue,
            ...Object.values(RevStateApproval).map(item => {
                return {
                    id: item,
                    name: RevStateApprovalCaption(item),
                };
            }),
        ];
    }

    getScopeById(scopeId, scopes) {
        return scopeId && scopes && scopes.length > 0 && scopes[0].scopes.find(scope => scope.id === scopeId).name;
    }

    handleExtFilterResult = searchFilter => {
        this.props.dispatch(
            registryListFilter({
                ...this.props.registryList.filter,
                from: 0,
                text: searchFilter ? searchFilter.search : this.props.registryList.filter.text,
                searchFilter: searchFilter,
            }),
        );
    };

    handleExtFilterClear = () => {
        const { dispatch, registryList } = this.props;
        dispatch(
            registryListFilter({
                ...registryList.filter,
                from: 0,
                searchFilter: null,
            }),
        );
    };

    getRuleSetsIds = () => {
        const { scopes, ruleSet } = this.props;
        const rulSetsIds = [];

        scopes.forEach((scopeData) => scopeData.scopes.forEach((scope) => {
            const _ruleSet = ruleSet.items.find((_ruleSet) => _ruleSet.code === scope.ruleSetCode);
            if (_ruleSet.id == undefined || rulSetsIds.includes(_ruleSet.id)) { return; }
            rulSetsIds.push(_ruleSet.id);
        }))

        return rulSetsIds;
    }

    handleExtFilter = () => {
        const { dispatch, registryList } = this.props;

        dispatch(
            modalDialogShow(
                this,
                i18n('ap.ext-filter.title'),
                <ExtFilterModal
                    scopeId={registryList.filter.scopeId}
                    initialValues={{
                        area: Area.ALLNAMES,
                        onlyMainPart: 'false',
                        search: registryList.filter.text,
                        ...registryList.filter.searchFilter,
                    }}
                    rulSetsIds={this.getRuleSetsIds()}
                    onSubmit={data => {
                        this.handleExtFilterResult(data);
                        dispatch(modalDialogHide());
                    }}
                />,
            ),
        );
    };

    /**
     * Výchozí hodnota/placeholder pro registry type filtr
     */
    registryTypeDefaultValue = i18n('registry.all');

    render() {
        const { registryDetail, registryList, maxSize, registryTypes, scopes, eidTypes } = this.props;

        let activeIndex = null;
        if (registryList.fetched && registryDetail.id !== null) {
            activeIndex = indexById(registryList.filteredRows, registryDetail.id);
        }

        let list;

        const isFetched = registryList.fetched;

        if (isFetched && eidTypes !== null) {
            if (registryList.rows.length > 0) {
                list = (
                    <ListBox
                        ref="registryList"
                        items={registryList.filteredRows}
                        activeIndex={activeIndex}
                        renderItemContent={this.renderListItem}
                        onFocus={this.handleRegistryDetail}
                    />
                );
            } else {
                list = <div className="search-norecord">{i18n('registry.list.noRecord')}</div>;
            }
        }

        const { filter } = registryList;

        let apTypesWithAll = [...registryTypes];
        apTypesWithAll.unshift({ name: this.registryTypeDefaultValue });

        let filterCls = 'mb-1 pt-1 pb-1';
        if (registryList.filter.searchFilter) {
            filterCls = filterCls + ' ext-filter-used';
        }

        return (
            <div className="registry-list">
                <div className="filter">
                    <Autocomplete
                        placeholder={this.getScopeById(filter.scopeId, scopes) || i18n('party.recordScope')}
                        items={this.getScopesWithAll(scopes)}
                        onChange={this.handleFilterRegistryScope}
                        value={filter.scopeId}
                        useIdAsValue
                    />
                    <Autocomplete
                        placeholder={filter.state ? StateApprovalCaption(filter.state) : i18n('party.apState')}
                        items={this.getStateWithAll()}
                        onChange={this.handleFilterRegistryState}
                        value={filter.state}
                        useIdAsValue
                    />
                    <Autocomplete
                        placeholder={filter.revState ? RevStateApprovalCaption(filter.revState) : i18n('registry.revisionState')}
                        items={this.getRevStateWithAll()}
                        onChange={this.handleFilterRegistryRevState}
                        value={filter.revState}
                        useIdAsValue
                    />
                    <Autocomplete
                        placeholder={!filter.registryTypeId ? i18n('registry.type') : ''}
                        items={apTypesWithAll}
                        disabled={
                            !registryTypes
                        }
                        tree
                        alwaysExpanded
                        allowSelectItem={item => true}
                        value={
                            !filter.registryTypeId
                                ? this.registryTypeDefaultValue
                                : getTreeItemById(filter.registryTypeId, registryTypes)
                        }
                        onChange={this.handleFilterRegistryType}
                        actions={this.getRegistryTypeActions()}
                    />
                    <SearchWithGoto
                        onFulltextSearch={this.handleFilterText}
                        onClear={this.handleFilterTextClear}
                        placeholder={i18n('search.input.search')}
                        filterText={registryList.filter.text}
                        showFilterResult={true}
                        type="INFO"
                        itemsCount={registryList.filteredRows ? registryList.filteredRows.length : 0}
                        allItemsCount={registryList.count}
                    />
                    <Row noGutters className={filterCls}>
                        {!registryList.filter.searchFilter && (
                            <Col>
                                <Button variant="link" onClick={this.handleExtFilter}>
                                    {i18n('ap.ext-filter.use')}
                                </Button>
                            </Col>
                        )}
                        {registryList.filter.searchFilter && (
                            <>
                                <Col title={i18n('ap.ext-filter.used')} className="align-self-center used">
                                    {i18n('ap.ext-filter.used')}
                                </Col>
                                <Col xs="auto">
                                    <Button variant="link" onClick={this.handleExtFilter}>
                                        {i18n('global.action.update')}
                                    </Button>
                                    <Button variant="link" onClick={this.handleExtFilterClear}>
                                        {i18n('global.action.cancel')}
                                    </Button>
                                </Col>
                            </>
                        )}
                    </Row>
                </div>
                <StoreHorizontalLoader store={registryList} />
                {list}
                {isFetched && registryList.filteredRows.length > maxSize && (
                    <span className="items-count">
                        {i18n('party.list.itemsVisibleCountFrom', registryList.filteredRows.length, registryList.count)}
                    </span>
                )}
                {(
                    registryList.count > maxSize ||
                    this.props.registryList.filter.from !== 0
                ) && (
                        <ListPager
                            prev={this.handleFilterPrev}
                            next={this.handleFilterNext}
                            from={this.props.registryList.filter.from}
                            pageSize={maxSize}
                            totalCount={this.props.registryList.count}
                        />
                    )}
            </div>
        );
    }
}

export default withRouter(connect(state => {
    const {
        app: { registryList, registryDetail },
        userDetail,
        focus,
        refTables: { recordTypes, scopesData, eidTypes, ruleSet },
    } = state;
    return {
        focus,
        registryDetail,
        registryList,
        registryTypes: recordTypes.items,
        apTypeIdMap: recordTypes.typeIdMap,
        scopes: scopesData.scopes,
        userDetail,
        eidTypes: eidTypes.data,
        ruleSet,
    };
})(RegistryList));
