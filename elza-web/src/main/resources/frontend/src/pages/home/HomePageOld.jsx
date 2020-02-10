/**
 * Home stránka
 */

require ('./HomePage.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Icon, i18n} from 'components/index.jsx';
import {Splitter, Autocomplete, FundForm, Ribbon, RibbonGroup, ToggleContent, FindindAidFileTree, AbstractReactComponent} from 'components/index.jsx';
import {ModalDialog, NodeTabs} from 'components/index.jsx';
import {ButtonGroup, Button, Panel} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {createFund} from 'actions/arr/fund.jsx'
import {storeLoadData, storeLoad} from 'actions/store/store.jsx'
import {WebApi} from 'actions/index.jsx';
import {dateToString} from 'components/Utils.jsx'

var placeholder = document.createElement("div");
placeholder.className = "placeholder";

var List = React.createClass({
  getInitialState: function() {
    return {data: this.props.data};
  },
  dragStart: function(e) {
    this.dragged = e.currentTarget;
    e.dataTransfer.effectAllowed = 'move';

    // Firefox requires dataTransfer data to be set
    e.dataTransfer.setData("text/html", e.currentTarget);
  },
  dragEnd: function(e) {

    this.dragged.style.display = "block";
    this.dragged.parentNode.removeChild(placeholder);

    // Update data
    var data = this.state.data;
    var from = Number(this.dragged.dataset.id);
    var to = Number(this.over.dataset.id);
console.log(from, to);
    if(from < to) to--;
    if(this.nodePlacement == "after") to++;
    data.splice(to, 0, data.splice(from, 1)[0]);
    this.setState({data: data});
  },
  dragOver: function(e) {
    e.preventDefault();
    this.dragged.style.display = "none";
    if(e.target.className == "placeholder") return;
    this.over = e.target;
    // Inside the dragOver method
    var relY = e.clientY - this.over.offsetTop;
    var height = this.over.offsetHeight / 2;
    var parent = e.target.parentNode;

    if(relY > height) {
      this.nodePlacement = "after";
      parent.insertBefore(placeholder, e.target.nextElementSibling);
    }
    else if(relY < height) {
      this.nodePlacement = "before"
      parent.insertBefore(placeholder, e.target);
    }
  },
  render: function() {
    var listItems = this.state.data.map((function(item, i) {
      return (
        <div data-id={i}
            key={i}
            draggable="true"
            onDragEnd={this.dragEnd}
            onDragStart={this.dragStart}>
          {item}
        </div>
      );
    }).bind(this));

    return <div onDragOver={this.dragOver}>{listItems}</div>
  }
});

var colors = ["Red","Green","Blue","Yellow","Black","White","Orange"];


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
        this.bindMethods('handleAddFund', 'handleCallAddFund', 'renderHistory',
            'renderHistoryItem', 'getFundDesc', 'handleFindParty', 'renderRecord');

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

    handleAddFund() {
        this.dispatch(modalDialogShow(this, i18n('arr.fund.title.add'), <FundForm create
                                                                              onSubmitForm={this.handleCallAddFund}/>));
    }

    handleCallAddFund(data) {
        this.dispatch(createFund(data));
    }

    buildRibbon() {
        var altActions = [];
        altActions.push(
            <Button key="add-fa" onClick={this.handleAddFund}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.arr.fund.add')}</span></div></Button>
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
            case 'ARR_REGION_FUND':
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
            <Button className='history-list-item' onClick={() => this.dispatch(storeLoadData(type, data))}>
                <Icon glyph={glyph}/>
                <div className='history-name'>{name}</div>
                {false && descComp}
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

    getFundDesc(fund) {
        var descs = fund.nodes.nodes.map(nodeobj => nodeobj.name);
        return this.arrToString(descs);
    }

    renderHistory() {
        var items = [];

        const {stateRegion} = this.props;

        stateRegion.partyRegionFront.forEach(x => {
            if (x._info) {
                var name = x._info.name;
                var desc = x._info.desc
                items.push(this.renderHistoryItem(name, desc, 'PARTY_REGION', x));
            }
        })
        stateRegion.registryRegionFront.forEach(x => {
            if (x._info) {
                var name = x._info.name;
                var desc = x._info.desc
                items.push(this.renderHistoryItem(name, desc, 'REGISTRY_REGION', x));
            }
        })
        if (false && stateRegion.arrRegion) {
            var descs = stateRegion.arrRegion.funds.map(fundobj => fundobj.name);
            var desc = this.arrToString(descs)
            items.push(this.renderHistoryItem(i18n('home.action.arr'), desc, 'ARR_REGION', stateRegion.arrRegion));
        }
        stateRegion.arrRegionFront.forEach(x => {
            var name = x.name + (x.lockDate ? ' ' + dateToString(new Date(x.lockDate)) : '');
            var desc = this.getFundDesc(x)
            items.push(this.renderHistoryItem(name, desc, 'ARR_REGION_FUND', x));
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
                        return {id: pp.id, name: pp.record}
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
        const {splitter} = this.props;

    if (false) return <Splitter
        leftSize={50}
        rightSize={25}
        left=<div>left</div>
        right=<div>right</div>
        center=<div>center</div>
    />

var items = getStates();

        var centerPanel = (
            <div className='splitter-home'>
                {false && <div>
                    <Button onClick={() => this.dispatch(storeLoad())}>LOAD</Button></div>}
                {this.renderHistory()}

{false && <div>
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
        items={this.state.registryRegionList}
        getItemId={(item) => item ? item.id : null}
        getItemName={(item) => item ? item.name : ''}
        onSearchChange={this.handleFindParty}
        onChange={(x, y)=>{console.log('ON CHANGE', x, y)}}
        renderItem={this.renderRecord}
    />
    </div>}
{false && <List data={colors} />}


            </div>
        )

        return (
            <PageLayout
                splitter={splitter}
                className='party-page'
                ribbon={this.buildRibbon()}
                centerPanel={centerPanel}
            />
        )
    }
}

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, stateRegion} = state
    return {
        splitter,
        arrRegion,
        refTables,
        stateRegion
    }
}

export default connect(mapStateToProps)(HomePage);

