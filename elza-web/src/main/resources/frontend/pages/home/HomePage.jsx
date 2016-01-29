/**
 * Home stránka
 */

require ('./HomePage.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Icon, i18n} from 'components';
import {AddFaForm, Ribbon, RibbonGroup, ToggleContent, FindindAidFileTree, AbstractReactComponent} from 'components';
import {ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button, Panel} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {modalDialogShow} from 'actions/global/modalDialog'
import {createFa} from 'actions/arr/fa'
import {storeLoadData, storeSave, storeLoad} from 'actions/store/store'
import {Combobox} from 'react-input-enhancements'
import {WebApi} from 'actions'

var Autocomplete  = require('./Autocomplete')
let styles = {
  item: {
    padding: '2px 6px',
    cursor: 'default'
  },

  highlightedItem: {
    color: 'white',
    background: 'hsl(200, 50%, 50%)',
    padding: '2px 6px',
    cursor: 'default'
  },

  menu: {
    border: 'solid 1px #ccc'
  }
}
function matchStateToTerm (state, value) {
  return (
    state.name.toLowerCase().indexOf(value.toLowerCase()) !== -1 ||
    state.abbr.toLowerCase().indexOf(value.toLowerCase()) !== -1
  )
}

function getStates() {
var _id = 0;
  return [
    { id: _id++, abbr: "AL", name: "Alabama"},
    { id: _id++, abbr: "AK", name: "Alaska"},
    { id: _id++, abbr: "AZ", name: "Arizona"},
    { id: _id++, abbr: "AR", name: "Arkansas"},
    { id: _id++, abbr: "CA", name: "California"},
    { id: _id++, abbr: "CO", name: "Colorado"},
    { id: _id++, abbr: "CT", name: "Connecticut"},
    { id: _id++, abbr: "DE", name: "Delaware"},
    { id: _id++, abbr: "FL", name: "Florida"},
    { id: _id++, abbr: "GA", name: "Georgia"},
    { id: _id++, abbr: "HI", name: "Hawaii"},
    { id: _id++, abbr: "ID", name: "Idaho"},
    { id: _id++, abbr: "IL", name: "Illinois"},
    { id: _id++, abbr: "IN", name: "Indiana"},
    { id: _id++, abbr: "IA", name: "Iowa"},
    { id: _id++, abbr: "KS", name: "Kansas"},
    { id: _id++, abbr: "KY", name: "Kentucky"},
    { id: _id++, abbr: "LA", name: "Louisiana"},
    { id: _id++, abbr: "ME", name: "Maine"},
    { id: _id++, abbr: "MD", name: "Maryland"},
    { id: _id++, abbr: "MA", name: "Massachusetts"},
    { id: _id++, abbr: "MI", name: "Michigan"},
    { id: _id++, abbr: "MN", name: "Minnesota"},
    { id: _id++, abbr: "MS", name: "Mississippi"},
    { id: _id++, abbr: "MO", name: "Missouri"},
    { id: _id++, abbr: "MT", name: "Montana"},
    { id: _id++, abbr: "NE", name: "Nebraska"},
    { id: _id++, abbr: "NV", name: "Nevada"},
    { id: _id++, abbr: "NH", name: "New Hampshire"},
    { id: _id++, abbr: "NJ", name: "New Jersey"},
    { id: _id++, abbr: "NM", name: "New Mexico"},
    { id: _id++, abbr: "NY", name: "New York"},
    { id: _id++, abbr: "NC", name: "North Carolina"},
    { id: _id++, abbr: "ND", name: "North Dakota"},
    { id: _id++, abbr: "OH", name: "Ohio"},
    { id: _id++, abbr: "OK", name: "Oklahoma"},
    { id: _id++, abbr: "OR", name: "Oregon"},
    { id: _id++, abbr: "PA", name: "Pennsylvania"},
    { id: _id++, abbr: "RI", name: "Rhode Island"},
    { id: _id++, abbr: "SC", name: "South Carolina"},
    { id: _id++, abbr: "SD", name: "South Dakota"},
    { id: _id++, abbr: "TN", name: "Tennessee"},
    { id: _id++, abbr: "TX", name: "Texas"},
    { id: _id++, abbr: "UT", name: "Utah"},
    { id: _id++, abbr: "VT", name: "Vermont"},
    { id: _id++, abbr: "VA", name: "Virginia"},
    { id: _id++, abbr: "WA", name: "Washington"},
    { id: _id++, abbr: "WV", name: "West Virginia"},
    { id: _id++, abbr: "WI", name: "Wisconsin"},
    { id: _id++, abbr: "WY", name: "Wyoming"}
  ]
}

var HomePage = class HomePage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleAddFa', 'handleCallAddFa', 'renderHistory',
            'renderHistoryItem', 'getFaDesc', 'handleFindParty', 'renderRecord');

        this.buildRibbon = this.buildRibbon.bind(this);

var options = [
{value: null, text: 'nevybrano', static: true},
null,
{value: '1', text: 'aaa1'},
{value: '2', text: 'bbb2'}
]
var options2 = [...options, {value: '3', text: 'ccc3'}]

        this.state = {options: options, registryList: []}

setTimeout(()=>this.setState({options: options2}), 4000);
    }

    handleAddFa() {
        this.dispatch(modalDialogShow(this, i18n('arr.fa.title.add'), <AddFaForm create onSubmit={this.handleCallAddFa} />));
    }

    handleCallAddFa(data) {
        this.dispatch(createFa(data));
    }

    buildRibbon() {
        var altActions = [];
        altActions.push(
            <Button key="add-fa" onClick={this.handleAddFa}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.arr.fa.add')}</span></div></Button>
        );

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup className="large">{altActions}</RibbonGroup>
        }

        return (
            <Ribbon home altSection={altSection} {...this.props} />
        )
    }

    renderHistoryItem(name, desc, type, data) {
        var glyph;
        switch (type) {
            case 'PARTY_REGION':
                glyph = 'fa-users';
                break;
            case 'REGISTRY_REGION':
                glyph = 'fa-th-list';
                break;
            case 'ARR_REGION':
                glyph = 'fa-file-text';
                break;
            case 'ARR_REGION_FA':
                glyph = 'fa-file-text';
                break;            
        }

        var hasDesc = desc && desc.length > 0
        var descComp;
        if (hasDesc) {
            descComp = <small>{desc}</small>
        } else {
            descComp = <small>&nbsp;</small>
        }
        return (
            <Button onClick={() => this.dispatch(storeLoadData(type, data))}>
                <Icon glyph={glyph}/>
                <div className='history-name'>{name}</div>
                {descComp}
            </Button>
        )
        return (
            <Panel className='history-list-item' header={name} onClick={() => this.dispatch(storeLoadData(type, data))}>
                ...[{type}]
            </Panel>
        )
    }

    arrToString(arr) {
        return arr.map((d, index) => {
            if (index > 0 && index < arr.length) {
                return ',  ' + d;
            } else {
                return d;
            }
        })
    }

    getFaDesc(fa) {
        var descs = fa.nodes.nodes.map(nodeobj => nodeobj.name);
        return this.arrToString(descs);
    }

    renderHistory() {
        var items = [];

        const {stateRegion} = this.props;

        stateRegion.partyRegionFront.forEach(x => {
            if (x.selectedId != null) {
                var name = x.selectedId;
                var desc = ''
                items.push(this.renderHistoryItem(name, desc, 'PARTY_REGION', x));
            } else {
                items.push(this.renderHistoryItem(i18n('home.action.party'), '', 'PARTY_REGION', x));
            }
        })
        stateRegion.registryRegionFront.forEach(x => {
            if (x.selectedId != null) {
                var name = x.selectedId;
                var desc = ''
                items.push(this.renderHistoryItem(name, desc, 'REGISTRY_REGION', x));
            } else {
                items.push(this.renderHistoryItem(i18n('home.action.registry'), '', 'REGISTRY_REGION', x));
            }
        })
        if (stateRegion.arrRegion) {
            var descs = stateRegion.arrRegion.fas.map(faobj => faobj.name);
            var desc = this.arrToString(descs)
            items.push(this.renderHistoryItem(i18n('home.action.arr'), desc, 'ARR_REGION', stateRegion.arrRegion));
        }
        stateRegion.arrRegionFront.forEach(x => {
            var name = x.name;
            var desc = this.getFaDesc(x)
            items.push(this.renderHistoryItem(name, desc, 'ARR_REGION_FA', x));
        })

        return (
            <div className='history-list-container'>
                {items}
            </div>
        )
    }

    handleFindParty(text) {
        WebApi.findRegistry(text)
            .then(json => {
                this.setState({
                    registryList: json.recordList.map(pp => {
                        return {id: pp.recordId, name: pp.record}
                    })
                })
            })
    }

    renderRecord(item, isHighlighted, isSelected) {
        var cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        return (
            <div
                className={cls}
                key={item.id}
            >
                <div className='c1'>{item.id}</div>
                <div className='c2'>{item.name}</div>
            </div>
        )
    }

    render() {
var items = getStates();

        var centerPanel = (
            <div>
                {false && <div>
                    <Button onClick={() => this.dispatch(storeSave())}>STORE</Button>
                    <Button onClick={() => this.dispatch(storeLoad())}>LOAD</Button></div>}
                {this.renderHistory()}

<h1>Autocomplete input - local data and local filter</h1>
<Autocomplete
    value={null}
    items={items}
    getItemId={(item) => item ? item.id : null}
    getItemName={(item) => item ? item.name : ''}
    shouldItemRender={matchStateToTerm}
    onSearchChange={(x, y)=>{console.log('ON SEARCH CHANGE', x, y)}}
    onChange={(x, y)=>{console.log('ON CHANGE', x, y)}}
/>

<h1>Autocomplete input - server data and server filter</h1>
<Autocomplete
    customFilter
    className='autocomplete-registry'
    footer=<div><Button onClick={()=>{alert('klik')}}>xxx</Button></div>
    header=<div><div className='c1'>id</div><div className='c2'>jmeno</div></div>
    value={null}
    items={this.state.registryList}
    getItemId={(item) => item ? item.id : null}
    getItemName={(item) => item ? item.name : ''}
    onSearchChange={this.handleFindParty}
    onChange={(x, y)=>{console.log('ON CHANGE', x, y)}}
    renderItem={this.renderRecord}
/>


{false && <Combobox defaultValue={'1'}
              options={this.state.options}
              dropdownProps={{ style: { width: '100%' } }}
              onChange={e => console.log('onChange', e.target.value)}
              onValueChange={c => console.log('onValueChange', c)}
              autocomplete>
      {inputProps =>
        <input {...inputProps}
               type='text'
               className={`${inputProps.className} form-control`}
               placeholder='No Country'
               addonAfter={<div>ddddddd</div>}
        />
      }
    </Combobox>}

            </div>
        )

        return (
            <PageLayout
                className='party-page'
                ribbon={this.buildRibbon()}
                centerPanel={centerPanel}
            />
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion, refTables, stateRegion} = state
    return {
        arrRegion,
        refTables,
        stateRegion
    }
}

module.exports = connect(mapStateToProps)(HomePage);

