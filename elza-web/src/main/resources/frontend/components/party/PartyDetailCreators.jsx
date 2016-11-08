/**
 * Autoři zadané osoby 
 */

import React from 'react';
import {WebApi} from 'actions/index.jsx';
import {connect} from 'react-redux'
import {Input, Button} from 'react-bootstrap';
import {PartyCreatorForm, AbstractReactComponent, i18n, Icon, Autocomplete} from 'components/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {AppActions} from 'stores/index.jsx';
import {deleteCreator, updateParty} from 'actions/party/party.jsx'
import {refPartyListFetchIfNeeded} from 'actions/refTables/partyList.jsx'
import {indexById} from 'stores/app/utils.jsx'

/**
 * PARTY DETAIL CREATORS
 * *********************************************
 * formulář autora osoby
 */
const PartyDetailCreators = class PartyDetailCreators extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'handleAddCreator',
            'handleDeleteCreator', 
            'addCreator',
            'deleteCreator',
            'handleSearchChange',
            'creatorChange'
        );
        this.state = {partyList: []};
    }

    componentDidMount() {
        this.dispatch(refPartyListFetchIfNeeded());         // načtení osob pro autory osoby
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(refPartyListFetchIfNeeded());         // načtení osob pro autory osoby
    }

    /**
     * HANDLE DELETE CREATOR
     * *********************************************
     * Kliknutí na tlačítko smazání autora
     * @param creatorId - identifikátor autora osoby
     */     
    handleDeleteCreator(creatorId) {
        if(confirm(i18n('party.detail.creator.delete'))) {              // pokud uživatel potvrdí smazání
            this.deleteCreator(creatorId);                             // autor se smaže
        }
    }

    /**
     * DELETE CREATOR
     * *********************************************
     * Smazání autora
     * @param creatorId - identifikátor autora osoby
     */ 
    deleteCreator(creatorId) {
        const party = this.props.partyRegion.selectedPartyData;                   // aktuální osoba
        let creators = []                                                       // nový seznam autorů
        for(let i = 0; i<party.creators.length; i++) {                           // projdeme původní autory
            if(party.creators[i].partyId != creatorId) {                      // a pokud se nejedna o mazaného autora
                creators[creators.length] = party.creators[i];                  // přidáme ho do seznamu autorů, co budou zachovány
            }
        }
        party.creators = creators;                                              // vyměníme starý seznam identifikátorů za nový
        this.dispatch(updateParty(party));                                      // změny uložíme
    }

    /**
     * ADD CREATOR
     * *********************************************
     * Vložení nového autora
     * @param data object - data identifikátorů z formuláře
     */ 
    addCreator(data) {
        const party = this.props.partyRegion.selectedPartyData;                   // aktuálně upravovaná osoba

        // Přidáme jen pokud již přidaný není
        if (indexById(party.creators, data.creatorId, "partyId") === null) {
            party.creators[party.creators.length] = {                               // nového autora vložíme na konec seznamu autorů
                partyId: data.creatorId,
                record:{
                    record: data.creatorName,
                    ["@class"]: ".RegRecordVO"
                },
                "@type": data["@type"]
            };
            this.dispatch(updateParty(party));                                      // aurtor se uloží a osoba znovu načte
        } else {
            this.dispatch(modalDialogHide());
        }
    }

    /**
     * HANDLE ADD CREATOR
     * *********************************************
     * Kliknutí na vložení nového autora
     */ 
    handleAddCreator() {
        const party = this.props.partyRegion.selectedPartyData;                       // načtení aktualní osoby ze store
        const data = {                                                                // výchozí data formuláře
            partyId: null,                                                           // identifikátor typu osoby, jejíž jménu upravujeme
        };
        this.dispatch(modalDialogShow(this, i18n('party.detail.creator.new') , <PartyCreatorForm initData={data}
              partyId={party.partyId}
              onSave={this.addCreator}
              renderParty={this.renderParty}
        />));    // otevře se formulář nového autora
    }

    /**
     * HANDLE UPDATE CREATOR
     * *********************************************
     * Kliknutí na ikonu editace autora
     * @param creatorId - identifikátor autora osoby
     */ 
    handleUpdateCreator(creatorId) {
        const party = this.props.partyRegion.selectedPartyData;                       // načtení aktualní osoby ze store
        let creator = {};                                                           // pripravený objekt pro autora
        for(let i = 0; i<party.creators.length; i++) {                               // prohledáme všechny autory
            if(party.creators[i].creatorId == creatorId) {                           // a ten které hledáme
                creator = party.creators[i];                                        // si uložíme
            }
        }

        const data = {                                                                // data, která udou poslána formuláři
            creatorId: creator.creatorId,                                           // id upravovaného identifikátoru
            partyTypeId: party.partyType.partyTypeId                                // id typu osoby, jejíž identifikátor upravujeme
        };
        this.dispatch(modalDialogShow(this, "aa" , <PartyCreatorForm initData={data} onSave={this.updateCreator} />));
    }

    /**
     * UPDATE CREATOR
     * *********************************************
     * Uložení změn autora
     * @param data object - data vyplněná v formuláři 
     */ 
    updateCreator(data) {
        const party = this.props.partyRegion.selectedPartyData;                       // id osoby, které patří měněný identifikátor
        const creators = party.partyGroupCreators;                                    // původní autoři osoby
        for(let i = 0; i<creators.length; i++) {                                     // je potřeba měněného autora najít mezi ostatními autory
            if(creators[i].partyGroupCreatorId == data.partyGroupCreatorId) {        // to je on!            
                party.creators[i].creatorId = data.creatorId;                       // identifikátor autora
            }
        }
        this.dispatch(updateParty(party));                                          // uložení změn a znovu načtení dat osoby
    }

    handleSearchChange(partyId, text) {
        text = text == "" ? null : text;

        WebApi.findPartyForParty(partyId, text).then(res => {
            this.setState({
                partyList: res.rows.map(party => {
                    return {
                        id: party.partyId,
                        name: party.record.record,
                        type: party.partyType.name,
                        from: party.from,
                        to: party.to,
                        characteristics: party.record.characteristics
                    }
                })
            })
        })
    }

    renderParty(item, isHighlighted, isSelected) {
        let cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        let interval;
        if (item.from || item.to) {
            interval = item.from == null ? "" : "TODO" + "-" + item.from == null ? "" : "TODO"
        }

        return (
            <div className={cls} key={item.id} >
                <div className="name" title={item.name}>{item.name}</div>
                <div className="type">{item.type}</div>
                <div className="interval">{interval}</div>
                <div className="characteristics" title={item.characteristics}>{item.characteristics}</div>
            </div>
        )
    }


    creatorChange(oldId, valueObj) {
        const party = this.props.partyRegion.selectedPartyData;
        for(let i = 0; i<party.creators.length; i++) {
            if(party.creators[i].partyId == oldId) {
                party.creators[i].partyId = valueObj.id;
            }
        }
        this.dispatch(updateParty(party));
    }


    /**
     * RENDER
     * *********************************************
     * Vykreslení bloku identifikátorů
     */ 
    render() {
        const {canEdit, partyRegion: {selectedPartyData}} = this.props;
        const {partyList} = this.state;

        return <div className="party-creators">
            <table>
                <tbody>
                    {selectedPartyData.creators.map(i=> {
                        const selectName = i.record ? i.record.record : "";
                        const value = {id: i.partyId, name: selectName};
                        return <tr className="creator" key={'party-creator-' + i.partyId}>
                        <th className="creator column">{i.name}</th>
                        <td>
                            <Autocomplete
                                customFilter
                                className='autocomplete-party'
                                value={value}
                                items={partyList}
                                getItemId={(item) => item ? item.id : null}
                                getItemName={(item) => item ? item.name : ''}
                                onSearchChange={text => {this.handleSearchChange(selectedPartyData.partyId, text) }}
                                onChange={(id,valObj) =>{this.creatorChange(i.partyId, valObj)}}
                                renderItem={this.renderParty}
                            />
                        </td>
                        <td className="buttons">
                            {canEdit && <Button classCreator="column" onClick={this.handleDeleteCreator.bind(this, i.partyId)}><Icon glyph="fa-trash"/></Button>}
                        </td>
                    </tr>})}
                </tbody>
            </table>
            {canEdit && <Button className="column" onClick={this.handleAddCreator}><Icon glyph="fa-plus"/> { i18n('party.detail.creator.new')}</Button>}
        </div>
    }
}

export default connect()(PartyDetailCreators);
