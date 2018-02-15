import React from 'react';
import {connect} from 'react-redux'
import {ListBox, AbstractReactComponent, SearchWithGoto, i18n, ArrPanel, StoreHorizontalLoader, Icon, FormInput} from 'components/shared';
import {AppActions} from 'stores/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {partyListFetchIfNeeded, partyListFilter, partyListInvalidate, partyDetailFetchIfNeeded, partyArrReset, PARTY_TYPE_CODES, RELATION_CLASS_CODES, DEFAULT_PARTY_LIST_MAX_SIZE} from 'actions/party/party.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {WebApi} from 'actions/index.jsx';

import './PartyList.less';
import PartyListItem from "./PartyListItem";
import Autocomplete from "../shared/autocomplete/Autocomplete";
import ListPager from "../shared/listPager/ListPager";
import * as perms from "../../actions/user/Permission";
import {FOCUS_KEYS} from "../../constants";

/**
 * Komponenta list osob
 */
class PartyList extends AbstractReactComponent {

    static PropTypes = {
        maxSize: React.PropTypes.number
    };

    static defaultProps = {
        maxSize: DEFAULT_PARTY_LIST_MAX_SIZE
    };

    componentDidMount() {
        this.fetchIfNeeded();
        this.trySetFocus()
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        this.trySetFocus(nextProps);
        if (nextProps.maxSize !== this.props.maxSize) {
            this.dispatch(partyListInvalidate());
        }
    }

    fetchIfNeeded = () => {
        this.dispatch(partyListFetchIfNeeded(null));
    };

    trySetFocus = (props = this.props) => {
        const {focus} = props;

        if (canSetFocus() && focus) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.partyList) {   // ještě nemusí existovat
                    this.setState({}, () => {
                        this.refs.partyList.focus();
                        focusWasSet()
                    })
                }
            } else if (isFocusFor(focus, FOCUS_KEYS.PARTY, 1) || isFocusFor(focus, FOCUS_KEYS.PARTY, 1, 'list')) {
                this.setState({}, () => {
                    this.refs.partyList.focus();
                    focusWasSet()
                })
            }
        }
    };

    handleFilterType = (e) => {
        const val = e.target.value;
        this.dispatch(partyListFilter({...this.props.partyList.filter, type: val == -1 ? null : val}));
    };

    handleFilterText = (filterText) => {
        this.dispatch(partyListFilter({...this.props.partyList.filter, text: !filterText || filterText.length === 0 ? null : filterText}));
    };

    handleFilterTextClear = () => {
        this.dispatch(partyListFilter({...this.props.partyList.filter, text: null}));
    };

    handlePartyDetail = (item) => {
        this.dispatch(partyDetailFetchIfNeeded(item.id));
    };

    handleFilterPartyScope = (item) => {
        this.dispatch(partyListFilter({...this.props.partyList.filter, scopeId: item ? item.id : null}));
    };

    handleFilterPrev = () => {
        let from = this.props.partyList.filter.from;
        if (this.props.partyList.filter.from >= DEFAULT_PARTY_LIST_MAX_SIZE) {
            from = this.props.partyList.filter.from - DEFAULT_PARTY_LIST_MAX_SIZE;
            this.dispatch(partyListFilter({...this.props.partyList.filter, from}));
        }
    };

    handleFilterNext = () => {
       let from = this.props.partyList.filter.from;
       if (this.props.partyList.filter.from < this.props.partyList.count - DEFAULT_PARTY_LIST_MAX_SIZE) {
           from = this.props.partyList.filter.from + DEFAULT_PARTY_LIST_MAX_SIZE;
           this.dispatch(partyListFilter({...this.props.partyList.filter, from}));
       }
    };

    filterScopes(scopes) {
        const { userDetail } = this.props;
        console.log(scopes);
        return scopes.filter((scope) => userDetail.hasOne(perms.AP_SCOPE_WR_ALL, {type: perms.AP_SCOPE_WR,scopeId: scope.id}));
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

    renderListItem = (item) => <PartyListItem {...item} onClick={this.handlePartyDetail.bind(this, item)} relationTypesForClass={this.props.relationTypesForClass} />;

    render() {
        const {partyDetail, partyList, partyTypes, maxSize, scopes} = this.props;

        let activeIndex = null;
        if (partyList.fetched && partyDetail.id !== null) {
            activeIndex = indexById(partyList.filteredRows, partyDetail.id);
        }

        let list;

        const isFetched = partyList.fetched;

        if (isFetched) {
            if (partyList.rows.length > 0) {
                list = <ListBox
                    ref='partyList'
                    items={partyList.filteredRows}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handlePartyDetail}
                    onSelect={this.handlePartyDetail}
                />;
            } else {
                list = <ul><li className="noResult">{i18n('search.action.noResult')}</li></ul>;
            }
        }

        const partyTypesFetched = partyTypes ? true : false;

        return <div className="party-list">
            <div className="filter">
                <Autocomplete
                    inputProps={ {placeholder: this.getScopeById(partyList.filter.scopeId, scopes) || i18n("party.recordScope")} }
                    items={this.getScopesWithAll(scopes)}
                    onChange={this.handleFilterPartyScope}
                    value={scopes && partyList.filter.scopeId ? this.getScopeById(partyList.filter.scopeId, scopes) : 'registry.all'}
                />
                <FormInput componentClass="select" className="type" onChange={this.handleFilterType} value={partyList.filter.type} disabled={!partyTypesFetched}>
                    <option value={-1}>{i18n('global.all')}</option>
                    {partyTypes && partyTypes.map(type => <option value={type.id} key={type.id}>{type.name}</option>)}
                </FormInput>
                <SearchWithGoto
                    onFulltextSearch={this.handleFilterText}
                    onClear={this.handleFilterTextClear}
                    placeholder={i18n('search.input.search')}
                    filterText={partyList.filter.text}
                    showFilterResult={true}
                    type="INFO"
                    itemsCount={partyList.rows ? partyList.rows.length : 0}
                    allItemsCount={partyList.count}
                />
            </div>
            <StoreHorizontalLoader store={partyList}/>
            {list}
            {isFetched && partyList.rows.length > maxSize && <span className="items-count">{i18n('party.list.itemsVisibleCountFrom', partyList.filteredRows.length, partyList.count)}</span>}
            {partyList.count > maxSize && <ListPager
                prev={this.handleFilterPrev}
                next={this.handleFilterNext}
                from={this.props.partyList.filter.from}
                maxSize={this.props.partyList.count}
            />}
        </div>
    }
}

export default connect((state) => {
    const {app:{partyList, partyDetail}, userDetail, focus, refTables:{partyTypes, scopesData}} = state;
    return {
        focus,
        partyList,
        partyDetail,
        partyTypes: partyTypes.fetched ? partyTypes.items : false,
        relationTypesForClass: partyTypes.fetched ? partyTypes.relationTypesForClass : false,
        scopes: scopesData.scopes,
        userDetail
    }
})(PartyList);
