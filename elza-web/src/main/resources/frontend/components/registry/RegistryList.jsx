import React from 'react';
import {connect} from 'react-redux'
import {ListBox, AbstractReactComponent, SearchWithGoto, Autocomplete, i18n, ArrPanel, Loading, Icon, RegistryListItem, registryTypes} from 'components/index.jsx';
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes.jsx'
import {indexById, objectById} from 'stores/app/utils.jsx'
import {registryListFetchIfNeeded, registryListFilter, registryListInvalidate, registryDetailFetchIfNeeded, registryDetailInvalidate, DEFAULT_REGISTRY_LIST_MAX_SIZE, registrySetFolder} from 'actions/registry/registry.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {WebApi} from 'actions/index.jsx';
import {getTreeItemById} from "./../../components/registry/registryUtils";
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'



import './RegistryList.less';

/**
 * Komponenta list rejstříků
 */
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
            } else if (isFocusFor(focus, 'registry', 1) || isFocusFor(focus, 'registry', 1, 'list')) {
                this.setState({}, () => {
                    this.refs.registryList.focus();
                    focusWasSet()
                })
            }
        }
    };

    handleFilterType = (e) => {
        const val = e.target.value;
        this.dispatch(registryListFilter({...this.props.registryList.filter, type: val == -1 ? null : val}));
    };

    handleFilterText = (filterText) => {
        this.dispatch(registryListFilter({...this.props.registryList.filter, text: filterText.length === 0 ? null : filterText}));
    };

    handleFilterRegistryType = (item) => {
        this.dispatch(registryListFilter({...this.props.registryList.filter, registryTypeId: item ? item.id : null}));
    };

    handleFilterTextClear = () => {
        this.dispatch(registryListFilter({...this.props.registryList.filter, text: null}));
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
            registryTypeId: item.registerTypeId
        }));
    };

    renderListItem = (item) => <RegistryListItem {...item}
                                                 onClick={this.handleRegistryDetail.bind(this, item)}
                                                 onDoubleClick={this.handleRegistrySetParent.bind(this,item)} />;

    render() {
        const {registryDetail, registryList, maxSize, registryTypes} = this.props;


        if (!registryList || !registryList.fetched) {
            return <div className="registry-list"><Loading /></div>;
        }
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

                //list = <ul><li className="noResult">{i18n('search.action.noResult')}</li></ul>;
            }
        } else {
            list = <div className="listbox-container"><Loading /></div>;
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

        return <div className="registry-list">
            <div className="filter">
                {registryTypes ? <Autocomplete
                        inputProps={ {placeholder: filter.registryTypeId === null ? i18n('registry.all') : ""} }
                        items={registryTypes}
                        disabled={registryList.filter.parents.length ? true : false}
                        tree
                        alwaysExpanded
                        allowSelectItem={(id, item) => true}
                        value={filter.registryTypeId === null ? null : getTreeItemById(filter.registryTypeId, registryTypes)}
                        onChange={this.handleFilterRegistryType}
                    /> : <Loading />}
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
            {list}
            {isFetched && registryList.filteredRows.length > maxSize && <span className="items-count">{i18n('party.list.itemsVisibleCountFrom', registryList.filteredRows.length, registryList.count)}</span>}
        </div>
    }
}

export default connect((state) => {
    const {app:{registryList, registryDetail}, focus, refTables:{recordTypes}} = state;
    return {
        focus,
        registryDetail,
        registryList,
        registryTypes: recordTypes && recordTypes.items ? recordTypes.items : false,
    }
})(RegistryList);
