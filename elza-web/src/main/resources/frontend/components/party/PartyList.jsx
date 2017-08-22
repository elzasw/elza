import React from 'react';
import {connect} from 'react-redux'
import {ListBox, AbstractReactComponent, SearchWithGoto, i18n, ArrPanel, Loading, Icon, FormInput} from 'components/shared';
import {AppActions} from 'stores/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {partyListFetchIfNeeded, partyListFilter, partyListInvalidate, partyDetailFetchIfNeeded, partyArrReset, PARTY_TYPE_CODES, RELATION_CLASS_CODES, DEFAULT_PARTY_LIST_MAX_SIZE} from 'actions/party/party.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {WebApi} from 'actions/index.jsx';

import './PartyList.less';
import PartyListItem from "./PartyListItem";

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

    fetchIfNeeded = (props = this.props) => {
        const {maxSize} = props;
        this.dispatch(partyListFetchIfNeeded(null, 0, maxSize));
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
            } else if (isFocusFor(focus, 'party', 1) || isFocusFor(focus, 'party', 1, 'list')) {
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
        this.dispatch(partyListFilter({...this.props.partyList.filter, text: filterText.length === 0 ? null : filterText}));
    };

    handleFilterTextClear = () => {
        this.dispatch(partyListFilter({...this.props.partyList.filter, text: null}));
    };

    handlePartyDetail = (item) => {
        this.dispatch(partyDetailFetchIfNeeded(item.id));
    };

    renderListItem = (item) => <PartyListItem {...item} onClick={this.handlePartyDetail.bind(this, item)} relationTypesForClass={this.props.relationTypesForClass} />;

    render() {
        const {partyDetail, partyList, partyTypes, maxSize} = this.props;

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
        } else {
            list = <div className="listbox-container"><Loading /></div>;
        }

        return <div className="party-list">
            <div className="filter">
                {partyTypes ? <FormInput componentClass="select" className="type" onChange={this.handleFilterType} value={partyList.filter.type}>
                    <option value={-1}>{i18n('global.all')}</option>
                    {partyTypes.map(type => <option value={type.id} key={type.id}>{type.name}</option>)}
                </FormInput> : <Loading />}
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
            {list}
            {isFetched && partyList.rows.length > maxSize && <span className="items-count">{i18n('party.list.itemsVisibleCountFrom', partyList.filteredRows.length, partyList.count)}</span>}
        </div>
    }
}

export default connect((state) => {
    const {app:{partyList, partyDetail}, focus, refTables:{partyTypes}} = state;
    return {
        focus,
        partyList,
        partyDetail,
        partyTypes: partyTypes.fetched ? partyTypes.items : false,
        relationTypesForClass: partyTypes.fetched ? partyTypes.relationTypesForClass : false,
    }
})(PartyList);
