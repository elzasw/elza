import React from 'react';
import {connect} from 'react-redux'
import {ListBox, AbstractReactComponent, SearchWithGoto, i18n, ArrPanel, Loading, Icon} from 'components/index.jsx';
import FormInput from 'components/form/FormInput.jsx';
import {AppActions} from 'stores/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {partyListFetchIfNeeded, partyListFilter, partyListInvalidate, partyDetailFetchIfNeeded, partyArrReset, PARTY_TYPE_CODES, RELATION_CLASS_CODES, DEFAULT_PARTY_LIST_MAX_SIZE} from 'actions/party/party.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {WebApi} from 'actions/index.jsx';

import './PartyList.less';

const LIST_MAX_COUNT = 200;

/**
 * Komponenta list osob
 */
class PartyList extends AbstractReactComponent {

    state = {
        relationTypesForClass: {
            [RELATION_CLASS_CODES.BIRTH]: [],
            [RELATION_CLASS_CODES.EXTINCTION]: []
        },
        initialized: false
    };

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
        const {partyList: {filter}, maxSize} = props;
        this.dispatch(partyListFetchIfNeeded(filter, null, 0, maxSize));
        if(!this.state.initialized && props.partyTypes) {
            const a = [].concat.apply(...props.partyTypes.map(i => i.relationTypes));
            this.setState({
                relationTypesForClass: {
                    [RELATION_CLASS_CODES.BIRTH]: a.filter(i => i.relationClassType && i.relationClassType.code == RELATION_CLASS_CODES.BIRTH).map(i => i.id).filter((i, index, self) => index == self.indexOf(i)),
                    [RELATION_CLASS_CODES.EXTINCTION]: a.filter(i => i.relationClassType && i.relationClassType.code == RELATION_CLASS_CODES.EXTINCTION).map(i => i.id).filter((i, index, self) => index == self.indexOf(i))
                },
                initialized: true
            });
        }
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

    getDatationRelationString = (array, firstChar) => {
        let datation = "";
        let first = true;
        for (let birth of array) {
            if (first) {
                datation += firstChar;
                first = false;
            } else {
                datation += ',';
            }

            if (birth.from && birth.to) {
                datation += birth.from.value + "..." + birth.to.value;
            } else {
                if (birth.from) {
                    datation += birth.from.value
                } else if (birth.to) {
                    datation += birth.to.value;
                }
            }
        }
        return datation
    };

    renderListItem = (item) => {
        const {relationTypesForClass} = this.state;

        let icon = PartyList.partyIconByPartyTypeCode(item.partyType.code);
        const birth = item.relations == null ? "" : this.getDatationRelationString(item.relations.filter(i => (relationTypesForClass[RELATION_CLASS_CODES.BIRTH].indexOf(i.relationTypeId) !== -1) && ((i.from && i.from.value) || (i.to && i.to.value))),'*');
        const extinction = item.relations == null ? "" : this.getDatationRelationString(item.relations.filter(i => (relationTypesForClass[RELATION_CLASS_CODES.EXTINCTION].indexOf(i.relationTypeId) !== -1) && ((i.from && i.from.value) || (i.to && i.to.value))),'†');
        let datation = null;
        if (birth != "" && extinction != "") {
            datation = birth + ", " + extinction
        } else if (birth != "") {
            datation = birth;
        } else if (extinction != "") {
            datation = extinction;
        }

        return <div className='search-result-row' onClick={this.handlePartyDetail.bind(this, item)}>
            <div>
                <Icon glyph={icon} />
                <span className="name">{item.record.record}</span>
            </div>
            <div>
                <span className="date">{datation}</span>
                {item.record.externalId && item.record.externalSystem && item.record.externalSystem.name && <span className="description">{item.record.externalSystem.name + ':' + item.record.externalId}</span>}
                {item.record.externalId && (!item.record.externalSystem || !item.record.externalSystem.name) && <span className="description">{'UNKNOWN:' + item.record.externalId}</span>}
                {!item.record.externalId && <span className="description">{item.id}</span>}
            </div>
        </div>
    };

    render() {
        const {partyDetail, partyList, partyTypes} = this.props;

        let activeIndex = null;
        if (partyList.fetched && partyDetail.id !== null) {
            activeIndex = indexById(partyList.filteredRows, partyDetail.id);
        }

        let list;

        const isFetched = !partyList.isFetching && partyList.fetched;

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
        // Wrap
        list = <div className="list">{list}</div>;


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
                    itemsCount={partyList.filteredRows ? partyList.filteredRows.length : 0}
                    allItemsCount={partyList.count}
                />
            </div>
            {list}
            {isFetched && partyList.filteredRows.length > LIST_MAX_COUNT && <span className="items-count">{i18n('party.list.itemsVisibleCountFrom', partyList.filteredRows.length, partyList.count)}</span>}
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
