import React from 'react';
import {connect} from 'react-redux'
import {ListBox, AbstractReactComponent, SearchWithGoto, i18n, ArrPanel, Loading, Icon} from 'components/index.jsx';
import FormInput from 'components/form/FormInput.jsx';
import {AppActions} from 'stores/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {partyListFetchIfNeeded, partyListFilter, partyDetailFetchIfNeeded, partyArrReset, PARTY_TYPE_CODES} from 'actions/party/party.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {WebApi} from 'actions/index.jsx';

import './PartyList.less';

/**
 * Komponenta list osob
 */
class PartyList extends AbstractReactComponent {

    componentDidMount() {
        this.fetchIfNeeded();
        this.trySetFocus()
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
        this.trySetFocus(nextProps);
    }

    fetchIfNeeded = (props = this.props) => {
        const {partyList: {filter}} = props;
        this.dispatch(partyListFetchIfNeeded(filter))
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

    handlePartyDetail = (item, e) => {
        this.dispatch(partyDetailFetchIfNeeded(item.id));
    };

    static partyIconByPartyTypeCode = (code) => {
        switch(code) {
            case PARTY_TYPE_CODES.PERSON:
                return 'fa-user';
                break;
            case PARTY_TYPE_CODES.GROUP_PARTY:
                return 'fa-building';
                break;
            case PARTY_TYPE_CODES.EVENT:
                return 'fa-hospital-o';
                break;
            case PARTY_TYPE_CODES.DYNASTY:
                return 'fa-shield';
                break;
            default:
                return 'fa-times';
        }
    };

    renderListItem = (item) => {
        let icon = PartyList.partyIconByPartyTypeCode(item.partyType.code);

        return <div className='search-result-row' onClick={this.handlePartyDetail.bind(this, item)}>
            <div>
                <Icon glyph={icon} />
                <span className="name">{item.record.record}</span>
            </div>
            <div>
                <span className="date">{/** TODO Dodat datum **/}</span>
                {item.record.externalId && item.record.externalSource && item.record.externalSource.name && <span className="description">{item.record.externalSource.name + ':' + item.record.externalId}</span>}
                {item.record.externalId && (!item.record.externalSource || item.record.externalSource.name) && <span className="description">{'UNKNOWN:' + item.record.externalId}</span>}
                {!item.record.externalId && <span className="description">{item.partyType.description + ':' + item.id}</span>}
            </div>
        </div>
    };

    render() {
        const {partyDetail, partyList, partyTypes} = this.props;

        if (!partyTypes || !partyList.fetched) {
            return <Loading />;
        }

        let activeIndex = null;
        if (partyDetail.id !== null) {
            activeIndex = indexById(partyList.filteredRows, partyDetail.id);
        }

        return <div className="party-list">
            <div className="filter">
                <FormInput componentClass="select" className="type" onChange={this.handleFilterType} value={partyList.filter.type}>
                    <option value={-1}>{i18n('global.all')}</option>
                    {partyTypes.map(type => <option value={type.id} key={type.id}>{type.name}</option>)}
                </FormInput>
                <SearchWithGoto
                    onFulltextSearch={this.handleFilterText}
                    onClear={this.handleFilterTextClear}
                    placeholder={i18n('search.input.search')}
                    filterText={partyList.filter.text}
                    showFilterResult={true}
                    type="INFO"
                    itemsCount={partyList.filteredRows ? partyList.filteredRows.length : 0}
                    allItemsCount={partyList.count}
                />
            </div>
            <div className="list">
                {partyList.rows.length > 0 ? <ListBox
                    ref='partyList'
                    items={partyList.filteredRows}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handlePartyDetail}
                    onSelect={this.handlePartyDetail}
                /> :<ul><li className="noResult">{i18n('search.action.noResult')}</li></ul>}
            </div>
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
    }
})(PartyList);
