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

function sortStates (a, b, value) {
  return (
    a.name.toLowerCase().indexOf(value.toLowerCase()) >
    b.name.toLowerCase().indexOf(value.toLowerCase()) ? 1 : -1
  )
}
function getStates() {
  return [
    { abbr: "AL", name: "Alabama"},
    { abbr: "AK", name: "Alaska"},
    { abbr: "AZ", name: "Arizona"},
    { abbr: "AR", name: "Arkansas"},
    { abbr: "CA", name: "California"},
    { abbr: "CO", name: "Colorado"},
    { abbr: "CT", name: "Connecticut"},
    { abbr: "DE", name: "Delaware"},
    { abbr: "FL", name: "Florida"},
    { abbr: "GA", name: "Georgia"},
    { abbr: "HI", name: "Hawaii"},
    { abbr: "ID", name: "Idaho"},
    { abbr: "IL", name: "Illinois"},
    { abbr: "IN", name: "Indiana"},
    { abbr: "IA", name: "Iowa"},
    { abbr: "KS", name: "Kansas"},
    { abbr: "KY", name: "Kentucky"},
    { abbr: "LA", name: "Louisiana"},
    { abbr: "ME", name: "Maine"},
    { abbr: "MD", name: "Maryland"},
    { abbr: "MA", name: "Massachusetts"},
    { abbr: "MI", name: "Michigan"},
    { abbr: "MN", name: "Minnesota"},
    { abbr: "MS", name: "Mississippi"},
    { abbr: "MO", name: "Missouri"},
    { abbr: "MT", name: "Montana"},
    { abbr: "NE", name: "Nebraska"},
    { abbr: "NV", name: "Nevada"},
    { abbr: "NH", name: "New Hampshire"},
    { abbr: "NJ", name: "New Jersey"},
    { abbr: "NM", name: "New Mexico"},
    { abbr: "NY", name: "New York"},
    { abbr: "NC", name: "North Carolina"},
    { abbr: "ND", name: "North Dakota"},
    { abbr: "OH", name: "Ohio"},
    { abbr: "OK", name: "Oklahoma"},
    { abbr: "OR", name: "Oregon"},
    { abbr: "PA", name: "Pennsylvania"},
    { abbr: "RI", name: "Rhode Island"},
    { abbr: "SC", name: "South Carolina"},
    { abbr: "SD", name: "South Dakota"},
    { abbr: "TN", name: "Tennessee"},
    { abbr: "TX", name: "Texas"},
    { abbr: "UT", name: "Utah"},
    { abbr: "VT", name: "Vermont"},
    { abbr: "VA", name: "Virginia"},
    { abbr: "WA", name: "Washington"},
    { abbr: "WV", name: "West Virginia"},
    { abbr: "WI", name: "Wisconsin"},
    { abbr: "WY", name: "Wyoming"}
  ]
}

var HomePage = class HomePage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleAddFa', 'handleCallAddFa', 'renderHistory',
            'renderHistoryItem', 'getFaDesc');

        this.buildRibbon = this.buildRibbon.bind(this);

var options = [
{value: null, text: 'nevybrano', static: true},
null,
{value: '1', text: 'aaa1'},
{value: '2', text: 'bbb2'}
]
var options2 = [...options, {value: '3', text: 'ccc3'}]

        this.state = {options: options}

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

    render() {
        var centerPanel = (
            <div>
                {false && <div>
                    <Button onClick={() => this.dispatch(storeSave())}>STORE</Button>
                    <Button onClick={() => this.dispatch(storeLoad())}>LOAD</Button></div>}
                {this.renderHistory()}


<Autocomplete
          initialValue="Ma"
          items={getStates()}
          getItemValue={(item) => item.abbr}
          shouldItemRender={matchStateToTerm}
          sortItems={sortStates}
          onBlur={()=>{console.log('ON BLUR')}}
          onFocus={()=>{console.log('ON FOCUS')}}
          onChange={(x, y)=>{console.log('ON CHANGE', x, y)}}
          onSelect={(x)=>{console.log('ON SELECT', x)}}
          renderItem={(item, isHighlighted) => (
            <div
              style={isHighlighted ? styles.highlightedItem : styles.item}
              key={item.abbr}
            >{item.name}</div>)}
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

