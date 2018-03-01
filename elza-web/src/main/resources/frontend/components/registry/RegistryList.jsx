/**
 * Komponenta list rejstříků
 */
import React from 'react';
import {connect} from 'react-redux'
import {ListBox, AbstractReactComponent, SearchWithGoto, Autocomplete, i18n, ArrPanel, StoreHorizontalLoader, Icon} from 'components/shared';
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes.jsx'
import {indexById, objectById} from 'stores/app/utils.jsx'
import {registryListFetchIfNeeded, registryListFilter, registryListInvalidate, registryDetailFetchIfNeeded, registryDetailInvalidate, DEFAULT_REGISTRY_LIST_MAX_SIZE, registrySetFolder} from 'actions/registry/registry.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {WebApi} from 'actions/index.jsx';
import {getTreeItemById} from "./../../components/registry/registryUtils";
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'



import './RegistryList.less';
import RegistryListItem from "./RegistryListItem";
import ListPager from "../shared/listPager/ListPager";
import * as perms from "../../actions/user/Permission";
import {FOCUS_KEYS} from "../../constants";
import {requestScopesIfNeeded} from "../../actions/refTables/scopesData";

class RegistryList extends AbstractReactComponent {

    static PropTypes = {
        maxSize: React.PropTypes.number
    };

    static defaultProps = {
        maxSize: DEFAULT_REGISTRY_LIST_MAX_SIZE
    };

    componentDidMount() {
        this.fetchIfNeeded();
        this.trySetFocus()
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        this.trySetFocus(nextProps);
        if (nextProps.maxSize !== this.props.maxSize) {
            this.dispatch(registryListInvalidate());
        }
    }

    fetchIfNeeded = (props = this.props) => {
        const {maxSize} = props;
        this.dispatch(refRecordTypesFetchIfNeeded());
        this.dispatch(registryListFetchIfNeeded(0, maxSize));
        this.dispatch(requestScopesIfNeeded());
    };

    trySetFocus = (props = this.props) => {
        const {focus} = props;

        if (canSetFocus() && focus) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.registryList) {   // ještě nemusí existovat
                    this.setState({}, () => {
                        this.refs.registryList.focus();
                        focusWasSet()
                    })
                }
            } else if (isFocusFor(focus, FOCUS_KEYS.REGISTRY, 1) || isFocusFor(focus, FOCUS_KEYS.REGISTRY, 1, 'list')) {
                this.setState({}, () => {
                    this.refs.registryList.focus();
                    focusWasSet()
                })
            }
        }
    };

    handleFilterType = (e) => {
        const val = e.target.value;
        this.dispatch(registryListFilter({...this.props.registryList.filter, from: 0, type: val == -1 ? null : val}));
    };

    handleFilterText = (filterText) => {
        this.dispatch(registryListFilter({...this.props.registryList.filter, from: 0, text: filterText && filterText.length === 0 ? null : filterText}));
    };

    handleFilterRegistryType = (item) => {
        this.dispatch(registryListFilter({...this.props.registryList.filter, itemSpecId:null, from: 0, registryTypeId: item ? item.id : null}));
    };

    handleFilterRegistryScope = (item) => {
        this.dispatch(registryListFilter({...this.props.registryList.filter, from: 0, scopeId: item ? item.id : null}));
    };

    handleFilterPrev = () => {
        let from = this.props.registryList.filter.from;
        if (this.props.registryList.filter.from >= DEFAULT_REGISTRY_LIST_MAX_SIZE) {
            from = this.props.registryList.filter.from - DEFAULT_REGISTRY_LIST_MAX_SIZE;
            this.dispatch(registryListFilter({...this.props.registryList.filter, from}));
        }
    };

    handleFilterNext = () => {
        let from = this.props.registryList.filter.from;
        if (this.props.registryList.filter.from < this.props.registryList.count - DEFAULT_REGISTRY_LIST_MAX_SIZE) {
            from = this.props.registryList.filter.from + DEFAULT_REGISTRY_LIST_MAX_SIZE;
            this.dispatch(registryListFilter({...this.props.registryList.filter, from}));
        }
    };

    filterRegistryTypeClear = () => {
        this.handleFilterRegistryType({name:this.registryTypeDefaultValue})
    }

    handleFilterTextClear = () => {
        this.dispatch(registryListFilter({...this.props.registryList.filter, from: 0, text: null}));
    };

    handleRegistryDetail = (item) => {
        this.dispatch(registryDetailFetchIfNeeded(item.id));
    };

    handleRegistryNavigation = (recordIdForOpen) => {
        this.dispatch(registrySetFolder(recordIdForOpen));
    };

    handleRegistryTypesSelectNavigation = (id) => {
        this.handleFilterParentClear();
        this.handleFilterRegistryType({id});
    };

    handleFilterParentClear = () => {
        this.dispatch(registryListFilter({
            ...this.props.registryList.filter,
            parents: [],
            typesToRoot: null,
            text: null,
            registryParentId: null,
            registryTypeId: null
        }));
    };

    handleRegistrySetParent = (item) => {
        if (!item.hierarchical) {
            return
        }
        const {registryList: {recordForMove}} = this.props;

        if (recordForMove && recordForMove.id === item.id) {
            this.dispatch(addToastrWarning(i18n('registry.disallowedMoveAction.title'), i18n('registry.disallowedMoveAction.text')));
            return false;
        }

        this.dispatch(registryListFilter({
            ...this.props.registryList.filter,
            parents: [
                {id: item.id, name:item.record},
                ...this.props.registryList.filter.parents
            ],
            typesToRoot: item.typesToRoot,
            text: null,
            registryParentId: item.id,
            registryTypeId: item.apTypeId
        }));
    };

    renderListItem = (item) => <RegistryListItem {...item}
                                                 onClick={this.handleRegistryDetail.bind(this, item)}
                                                 onDoubleClick={this.handleRegistrySetParent.bind(this,item)} />

    /**
     * Vrátí pole akcí pro registry type filtr
     * @return {array} pole akcí
     */
    getRegistryTypeActions = () => {
        const {registryList:{filter}} = this.props;
        const actions = [];
        if (filter.registryTypeId !== null && typeof filter.registryTypeId !== "undefined") {
            actions.push(
                <div
                onClick={()=>this.filterRegistryTypeClear()}
                className={'btn btn-default detail'}>
                    <Icon glyph={'fa-times'}/>
                </div>
            );
        }
        return actions;
    }

    filterScopes(scopes) {
        const { userDetail } = this.props;
        return scopes.filter((scope) => userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {type: perms.AP_SCOPE_RD,scopeId: scope.id}));
    }

    getScopesWithAll(scopes) {
        const defaultValue = {name: i18n('registry.all')};
        if (scopes && scopes.length > 0 && scopes[0] && scopes[0].scopes && scopes[0].scopes.length > 0) {
            return this.filterScopes([defaultValue, ...scopes[0].scopes])
        }
        return [defaultValue];
    }

    getScopeById(scopeId, scopes){
        return scopeId && scopes && scopes.length > 0 && scopes[0].scopes.find(scope => (scope.id === scopeId)).name;
    }

    /**
     * Výchozí hodnota/placeholder pro registry type filtr
     */
    registryTypeDefaultValue = i18n('registry.all');

    render() {
        const {registryDetail, registryList, maxSize, registryTypes, scopes} = this.props;


        let activeIndex = null;
        if (registryList.fetched && registryDetail.id !== null) {
            activeIndex = indexById(registryList.filteredRows, registryDetail.id);
        }

        let list;

        const isFetched = registryList.fetched;

        if (isFetched) {
            if (registryList.rows.length > 0) {
                list = <ListBox
                    ref='registryList'
                    items={registryList.filteredRows}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handleRegistryDetail}
                    onSelect={this.handleRegistrySetParent}
                />;
            } else {
                list = <div className='search-norecord'>{i18n('registry.list.noRecord')}</div>;
            }
        }

        const {filter} = registryList;

        let navParents = null;

        if (filter.registryTypeId !== null && filter.parents && filter.parents.length > 0) {
            const tmpParents = filter.parents.slice();
            const nazevRodice = tmpParents[0].name;
            tmpParents.shift();

            const cestaRodice = [];
            tmpParents.map((val, index) => {
                cestaRodice.push(<span className='clickAwaiblePath parentPath' key={index}  title={val.name} onClick={this.handleRegistryNavigation.bind(this,val.id)}>{val.name}</span>);
            });

            if (filter.typesToRoot) {
                filter.typesToRoot.map((val, index) => {
                    cestaRodice.push(<span className='clickAwaiblePath parentPath' key={index} title={val.name} onClick={this.handleRegistryTypesSelectNavigation.bind(this,val.id)} >{val.name}</span>);
                });
            }

            const breadcrumbs = [];
            cestaRodice.map((val, key) => {
                if (key) {
                    breadcrumbs.push(<span className='parentPath' key={key}><span className='parentPath'>&nbsp;|&nbsp;</span>{val}</span>);
                } else {
                    breadcrumbs.push(<span key={key} className='parentPath'>{val}</span>);
                }
            });

            navParents = <div className="record-parent-info">
                <div className='record-selected-name'>
                    <div className="icon"><Icon glyph="fa-folder-open"/></div>
                    <div className="title" title={nazevRodice}>{nazevRodice}</div>
                    <div className="back" onClick={this.handleFilterParentClear}><Icon glyph="fa-close"/></div>
                </div>
                <div className='record-selected-breadcrumbs'>{breadcrumbs}</div>
            </div>

        }

        let apTypesWithAll = [...registryTypes];
        apTypesWithAll.unshift({name:this.registryTypeDefaultValue});

        return <div className="registry-list">
            <div className="filter">
                <Autocomplete
                    inputProps={ {placeholder: this.getScopeById(filter.scopeId, scopes) || i18n("party.recordScope")} }
                    items={this.getScopesWithAll(scopes)}
                    onChange={this.handleFilterRegistryScope}
                    value={this.getScopeById(filter.scopeId, scopes)}
                />
                <Autocomplete
                        inputProps={ {placeholder: !filter.registryTypeId ? this.registryTypeDefaultValue : ""} }
                        items={apTypesWithAll}
                        disabled={!registryTypes || registryList.filter.parents.length || registryList.filter.itemSpecId ? true : false}
                        tree
                        alwaysExpanded
                        allowSelectItem={(id, item) => true}
                        value={!filter.registryTypeId ? this.registryTypeDefaultValue : getTreeItemById(filter.registryTypeId, registryTypes)}
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
            </div>
            <div className='registry-list-breadcrumbs' key='breadcrumbs'>{navParents}</div>
            <StoreHorizontalLoader store={registryList}/>
            {list}
            {isFetched && registryList.filteredRows.length > maxSize && <span className="items-count">{i18n('party.list.itemsVisibleCountFrom', registryList.filteredRows.length, registryList.count)}</span>}
            {registryList.count > maxSize &&
            <ListPager
                prev={this.handleFilterPrev}
                next={this.handleFilterNext}
                from={this.props.registryList.filter.from}
                maxSize={this.props.registryList.count}
            />}
        </div>
    }
}

export default connect((state) => {
    const {app:{registryList, registryDetail}, userDetail,  focus, refTables:{recordTypes, scopesData}} = state;
    return {
        focus,
        registryDetail,
        registryList,
        registryTypes: recordTypes && recordTypes.items ? recordTypes.items : false,
        scopes: scopesData.scopes,
        userDetail
    }
})(RegistryList);
