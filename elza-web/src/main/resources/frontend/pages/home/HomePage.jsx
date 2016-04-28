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
import {NodeTabs, FundTreeTabs} from 'components/index.jsx';
import {ButtonGroup, Button, Panel} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {createFund} from 'actions/arr/fund.jsx'
import {storeLoadData, storeSave, storeLoad} from 'actions/store/store.jsx'
import {Combobox} from 'react-input-enhancements'
import {WebApi} from 'actions/index.jsx';
import {setInputFocus, dateToString} from 'components/Utils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'

var rows = []
var selectedIds = [0, 1, 2, 3, 4]
var focus

for (var a=0; a<40; a++) {
    rows.push({
        id: a,
        firstname: 'jan ' + a,
        surname: 'novak ' + a,
        age: 10+2*a,
        address: 'Nejaka ulice ' + a + ', 330 22, Plzen',
        tel: 2*a%10 + 3*a%10 + 4*a%10 + 5*a%10 + 6*a%10 + 7*a%10 + 8*a%10 + 9*a%10 + 2*a%10
    })
    if (a % 4 == 0) {
        rows[rows.length-1].address = rows[rows.length-1].address + rows[rows.length-1].address + rows[rows.length-1].address
    }
}

var cols = []

cols.push({
    dataName: 'id',
    title: 'Id',
    desc: 'popis id',
    width: 60,
})
cols.push({
    dataName: 'firstname',
    title: 'Jmeno',
    desc: 'popis jmena',
    width: 120,
})
cols.push({
    dataName: 'surname',
    title: 'Prijmeni',
    desc: 'popis prijmeni',
    width: 120,
})
cols.push({
    dataName: 'age',
    title: 'Vek',
    desc: 'popis vek',
    width: 160,
})
cols.push({
    dataName: 'address',
    title: 'Adresa',
    desc: 'popis adresy',
    width: 220,
})
cols.push({
    dataName: 'tel',
    title: 'Telefon',
    desc: 'popis telefonu',
    width: 120,
})


var HomePage = class HomePage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleAddFund', 'renderHistory',
            'renderHistoryItem', 'getFundDesc', 'trySetFocus');

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps)
    }

    componentDidMount() {
        this.trySetFocus(this.props)
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.list) {   // ještě nemusí existovat
                    this.setState({}, () => {
                        var listEl = ReactDOM.findDOMNode(this.refs.list)
                        setInputFocus(listEl, false)
                        focusWasSet()
                    })
                }
            } else if (isFocusFor(focus, 'home', 1) || isFocusFor(focus, 'home', 1, 'list')) {
                this.setState({}, () => {
                    var listEl = ReactDOM.findDOMNode(this.refs.list)
                    setInputFocus(listEl, false)
                    focusWasSet()
                })
            }
        }
    }

    handleAddFund() {
        this.dispatch(modalDialogShow(this, i18n('arr.fund.title.add'),
            <FundForm create onSubmitForm={(data) => {this.dispatch(createFund(data))}}/>));
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
            <Ribbon ref='ribbon' home altSection={altSection} {...this.props} />
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
        const {stateRegion} = this.props;

        var partyItems = stateRegion.partyRegionFront.map(x => {
            if (x._info) {
                var name = x._info.name;
                var desc = x._info.desc
                return this.renderHistoryItem(name, desc, 'PARTY_REGION', x)
            }
        })
        var registryItems = stateRegion.registryRegionFront.map(x => {
            if (x._info) {
                var name = x._info.name;
                var desc = x._info.desc
                return this.renderHistoryItem(name, desc, 'REGISTRY_REGION', x)
            }
        })
        var arrItems = stateRegion.arrRegionFront.map(x => {
            var name = x.name + (x.lockDate ? ' ' + dateToString(new Date(x.lockDate)) : '');
            var desc = this.getFundDesc(x)
            return this.renderHistoryItem(name, desc, 'ARR_REGION_FUND', x)
        })

        return (
            <div ref='list' className='history-list-container'>
                <div>{arrItems}</div>
                <div>{registryItems}</div>
                <div>{partyItems}</div>
            </div>
        )
    }

    render() {
        const {splitter} = this.props;

        var centerPanel = (
            <div className='splitter-home'>
                {false && <div>
                    <Button onClick={() => this.dispatch(storeSave())}>STORE</Button>
                    <Button onClick={() => this.dispatch(storeLoad())}>LOAD</Button></div>}
                {this.renderHistory()}
            </div>
        )

centerPanel =
    <div>
        {centerPanel}
    </div>

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
    const {splitter, arrRegion, refTables, stateRegion, focus, userDetail} = state
    return {
        splitter,
        arrRegion,
        refTables,
        stateRegion,
        focus,
        userDetail,
    }
}

module.exports = connect(mapStateToProps)(HomePage);

