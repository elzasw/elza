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

/**
 * PARTY DETAIL CREATORS
 * *********************************************
 * formulář autora osoby
 */
var PartyDetailCreators = class PartyDetailCreators extends AbstractReactComponent {
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

    componentDidMount(){
        this.dispatch(refPartyListFetchIfNeeded());         // načtení osob pro autory osoby
    }

    componentWillReceiveProps(nextProps){
        this.dispatch(refPartyListFetchIfNeeded());         // načtení osob pro autory osoby
    }

   /**
     * HANDLE DELETE CREATOR
     * *********************************************
     * Kliknutí na tlačítko smazání autora
     * @param creatorId - identifikátor autora osoby
     */     
    handleDeleteCreator(creatorId){
        if(confirm(i18n('party.detail.creator.delete'))){              // pokud uživatel potvrdí smazání
            this.deleteCreator(creatorId);                             // autor se smaže
        }
    }

   /**
     * DELETE CREATOR
     * *********************************************
     * Smazání autora
     * @param creatorId - identifikátor autora osoby
     */ 
    deleteCreator(creatorId){
        var party = this.props.partyRegion.selectedPartyData;                   // aktuální osoba
        var creators = []                                                       // nový seznam autorů
        for(var i = 0; i<party.creators.length; i++){                           // projdeme původní autory
            if(party.creators[i].partyId != creatorId){                      // a pokud se nejedna o mazaného autora
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
     * @param obj data - data identifikátorů z formuláře
     */ 
    addCreator(data) {
        var party = this.props.partyRegion.selectedPartyData;                   // aktuálně upravovaná osoba
        party.creators[party.creators.length] = {                               // nového autora vložíme na konec seznamu autorů
            partyId: data.creatorId,
            record:{
                record: data.creatorName
            },
            '@type': '.ParPartyVO'
        }
        this.dispatch(updateParty(party));                                      // aurtor se uloží a osoba znovu načte     
    }

   /**
     * HANDLE ADD CREATOR
     * *********************************************
     * Kliknutí na vložení nového autora
     */ 
    handleAddCreator(){
        var party = this.props.partyRegion.selectedPartyData;                       // načtení aktualní osoby ze store
        var data = {                                                                // výchozí data formuláře
            partyId: null,                                                           // identifikátor typu osoby, jejíž jménu upravujeme
        };
        this.dispatch(modalDialogShow(this, i18n('party.detail.creator.new') ,
                <PartyCreatorForm initData={data}
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
    handleUpdateCreator(creatorId){
        var party = this.props.partyRegion.selectedPartyData;                       // načtení aktualní osoby ze store
        var creator = {};                                                           // pripravený objekt pro autora
        for(var i = 0; i<party.creators.length; i++){                               // prohledáme všechny autory
            if(party.creators[i].creatorId == creatorId){                           // a ten které hledáme
                creator = party.creators[i];                                        // si uložíme
            }
        };
       
        var data = {                                                                // data, která udou poslána formuláři
            creatorId: creator.creatorId,                                           // id upravovaného identifikátoru
            partyTypeId: party.partyType.partyTypeId                                // id typu osoby, jejíž identifikátor upravujeme
        }
        this.dispatch(modalDialogShow(this, "aa" , <PartyCreatorForm initData={data} onSave={this.updateCreator} />));
    }

   /**
     * UPDATE CREATOR
     * *********************************************
     * Uložení změn autora
     * @param obj data - data vyplněná v formuláři 
     */ 
    updateCreator(data){
        var party = this.props.partyRegion.selectedPartyData;                       // id osoby, které patří měněný identifikátor
        var creators = party.partyGroupCreators;                                    // původní autoři osoby
        for(var i = 0; i<creators.length; i++){                                     // je potřeba měněného autora najít mezi ostatními autory
            if(creators[i].partyGroupCreatorId == data.partyGroupCreatorId){        // to je on!            
                party.creators[i].creatorId = data.creatorId;                       // identifikátor autora
            }
        }
        this.dispatch(updateParty(party));                                          // uložení změn a znovu načtení dat osoby
    }

    handleSearchChange(partyId, text) {

        text = text == "" ? null : text;

        WebApi.findPartyForParty(partyId, text)
                .then(json => {
                    this.setState({
                        partyList: json.map(party => {
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
        var cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        var interval;
        if (item.from || item.to) {
            interval = item.from == null ? "" : "TODO" + "-" + item.from == null ? "" : "TODO"
        }

        return (
                <div className={cls} key={item.id} >
                    <div className="name" title={item.name}>{item.name}</div>
                    <div className="type">{item.type}</div>
                    <div className="interval">{interval}</div>
                    <div  className="characteristics" title={item.characteristics}>{item.characteristics}</div>
                </div>
        )
    }


    creatorChange(oldId, valueObj){
        var party = this.props.partyRegion.selectedPartyData;
        var creators = []
        for(var i = 0; i<party.creators.length; i++){
            if(party.creators[i].partyId == oldId){
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
       const {canEdit} = this.props

        var party = this.props.partyRegion.selectedPartyData;

        return  <div className="party-creators">
                    <table>
                        <tbody>
                            {party.creators.map(i=> {
                                var selectName = i.record ? i.record.record : "";
                                var value =  {id: i.partyId, name: selectName};


                                return <tr className="creator">
                                <th className="creator column">{i.name}</th> 
                                <td>
                                    <Autocomplete
                                            customFilter
                                            className='autocomplete-party'
                                            value={value}
                                            items={this.state.partyList}
                                            getItemId={(item) => item ? item.id : null}
                                            getItemName={(item) => item ? item.name : ''}
                                            onSearchChange={text => {this.handleSearchChange(party.partyId,text) }}
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

module.exports = connect()(PartyDetailCreators);
