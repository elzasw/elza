/**
 * Home stránka
 */

require('./HomePage.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Icon, i18n} from 'components/index.jsx';
import {Splitter, Autocomplete, FundForm, Ribbon, RibbonGroup, ToggleContent, FindindAidFileTree, AbstractReactComponent} from 'components/index.jsx';
import {NodeTabs} from 'components/index.jsx';
import {ButtonGroup, Button, Panel} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {createFund} from 'actions/arr/fund.jsx'
import {storeLoadData, storeSave, storeLoad} from 'actions/store/store.jsx'
import {Combobox} from 'react-input-enhancements'
import {setInputFocus, dateToString} from 'components/Utils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'

const rows = []
const selectedIds = [0, 1, 2, 3, 4]
let focus

for (let a=0; a<40; a++) {
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

const cols = []

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


const HomePage = class HomePage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'handleAddFund',
            'renderHistory',
            'renderHistoryItem',
            'getFundDesc',
            'trySetFocus',
            'buildRibbon'
        );
    }

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps)
    }

    componentDidMount() {
        this.trySetFocus(this.props)
    }

    trySetFocus(props) {
        const {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.list) {   // ještě nemusí existovat
                    this.setState({}, () => {
                        const listEl = ReactDOM.findDOMNode(this.refs.list)
                        setInputFocus(listEl, false)
                        focusWasSet()
                    })
                }
            } else if (isFocusFor(focus, 'home', 1) || isFocusFor(focus, 'home', 1, 'list')) {
                this.setState({}, () => {
                    const listEl = ReactDOM.findDOMNode(this.refs.list)
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
        const altActions = [];
        altActions.push(
            <Button key="add-fa" onClick={this.handleAddFund}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.arr.fund.add')}</span></div></Button>
        );

        let altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup className="small" key="ribbon-group-home">{altActions}</RibbonGroup>
        }

        return (
            <Ribbon ref='ribbon' home altSection={altSection} {...this.props} />
        )
    }

    renderHistoryItem(name, desc, type, data, keyIndex) {
        let glyph;
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

        const hasDesc = desc && desc.length > 0
        let descComp;
        if (hasDesc) {
            descComp = <small>{desc}</small>
        } else {
            descComp = <small>&nbsp;</small>
        }
        return (
            <Button className='history-list-item' onClick={() => this.dispatch(storeLoadData(type, data))} key={"button-" + keyIndex}>
                <Icon glyph={glyph}/>
                <div className='history-name'>{name}</div>
                {false && descComp}
            </Button>
        )
        return (
            <Panel className='history-list-item' header={name} onClick={() => this.dispatch(storeLoadData(type, data))} key={"panel-" + keyIndex}>
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
        const descs = fund.nodes.nodes.map(nodeobj => nodeobj.name);
        return this.arrToString(descs);
    }

    renderHistory() {
        const {stateRegion} = this.props;

        const partyItems = stateRegion.partyRegionFront.map((x, index) => {
            if (x._info) {
                const name = x._info.name;
                const desc = x._info.desc
                return this.renderHistoryItem(name, desc, 'PARTY_REGION', x, index)
            }
        })
        const registryItems = stateRegion.registryRegionFront.map((x, index) => {
            if (x._info) {
                const name = x._info.name;
                const desc = x._info.desc
                return this.renderHistoryItem(name, desc, 'REGISTRY_REGION', x, index)
            }
        })
        const arrItems = stateRegion.arrRegionFront.map((x, index) => {
            const name = x.name + (x.lockDate ? ' ' + dateToString(new Date(x.lockDate)) : '');
            const desc = this.getFundDesc(x)
            return this.renderHistoryItem(name, desc, 'ARR_REGION_FUND', x, index)
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

        let centerPanel = (
            <div className='splitter-home'>
                {false && <div>
                    <Button onClick={() => this.dispatch(storeSave())}>STORE</Button>
                    <Button onClick={() => this.dispatch(storeLoad())}>LOAD</Button></div>}
                {this.renderHistory()}
            </div>
        )

        centerPanel = <div>{centerPanel}</div>;

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

