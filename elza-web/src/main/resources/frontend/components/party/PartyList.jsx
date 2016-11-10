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

        if (canSetFocus()) {
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

    handleArrReset = () => {
        //this.dispatch(partyArrReset());
    };

    renderListItem = (item) => {
        let icon;
        switch(item.partyType.code) {
            case PARTY_TYPE_CODES.PERSON:
                icon = 'fa-user';
                break;
            case PARTY_TYPE_CODES.GROUP_PARTY:
                icon = 'fa-building';
                break;
            case PARTY_TYPE_CODES.EVENT:
                icon = 'fa-hospital-o';
                break;
            case PARTY_TYPE_CODES.DYNASTY:
                icon = 'fa-shield';
                break;
            default:
                icon = 'fa-times';
        }

        return <div className='search-result-row' onClick={this.handlePartyDetail.bind(this, item)}>
            <div>
                <Icon glyph={icon} />
                <span className="name">{item.record.record}</span>
            </div>
            <div>
                <span className="date">{/** TODO Dodat datum **/}</span>
                {item.record.external_id && item.record.externalSource && <span className="description">{item.partyType.description + ':' + item.id}</span>}
                {item.record.external_id && !item.record.externalSource && <span className="description">{'UNKNOWN:' + item.record.external_id}</span>}
                {!item.record.external_id && <span className="description">{item.partyType.description + ':' + item.id}</span>}
            </div>
        </div>
    };

    render() {
        const {partyList, partyTypes} = this.props;
        /*
        if(partyList && partyList.length>0){
            var description = '';
            for(var i = 0; i<partyList.length; i++){                                                            // projdu vsechny polozky abych jim nastavil popisek
                if(partyList[i].record && partyList[i].record.characteristics){                                 // pokud ma popisek zadany
                    var lineEndPosition = partyList[i].record.characteristics.indexOf("\n")                     // zjistim kde konci první řádek
                    if(lineEndPosition>=0){                                                                     // pokud vubec nekde konci (popisek muze mit jen 1 radek)
                        description = partyList[i].record.characteristics.substring(0, lineEndPosition);        // oriznu ho do konce radku
                    }else{
                        description = partyList[i].record.characteristics;                                      // jinak ho necham cely                
                    }   
                    partyList[i].record.description = description;                                              // ulozim popisek k objektu                
                }else{
                    partyList[i].record.description = '';                                                       // ulozim popisek k objektu                                                                           // popisek nezadan, nastavim prazdny
                }

            }

            var activeIndex = indexById(partyList, this.props.selectedPartyID, 'id')
            partyListRows = (

            )            
        }else{
            var label = ; ;
            partyListRows =
        }*/

        let arrPanel = null;

        if (false) {
            arrPanel = <ArrPanel onReset={this.handleArrReset} name={partyRegion.panel.name} />
        }

        if (!partyTypes || !partyList.fetched) {
            return <Loading />;
        }

        return <div className="party-list">
            <div>
                {arrPanel}
                <FormInput componentClass="select" onChange={this.handleFilterType}>
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
            <div className="party-listbox-container">
                {partyList.rows.length > 0 ? <ListBox
                    className='party-listbox'
                    ref='partyList'
                    items={partyList.filteredRows}
                    activeIndex={null}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handlePartyDetail}
                    onSelect={this.handlePartyDetail}
                /> :<ul><li className="noResult">{i18n('search.action.noResult')}</li></ul>}
            </div>
        </div>
    }
}

export default connect((state) => {
    const {app:{partyList}, refTables:{partyTypes}} = state;
    return {
        partyList,
        partyTypes: partyTypes.fetched ? partyTypes.items : false,
    }
})(PartyList);
