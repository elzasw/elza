/**
 * Home stránka
 */

import './HomePage.less'

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Icon, i18n} from 'components/index.jsx';
import {Splitter, Autocomplete, FundForm, Ribbon, RibbonGroup, ToggleContent, FindindAidFileTree, AbstractReactComponent, PartyList} from 'components/index.jsx';
import {NodeTabs} from 'components/index.jsx';
import {ButtonGroup, Button, Panel} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {createFund} from 'actions/arr/fund.jsx'
import {storeLoadData, storeLoad} from 'actions/store/store.jsx'
import {setInputFocus, dateToString} from 'components/Utils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'

import TooltipTrigger from "./TooltipTrigger"

const HomePage = class HomePage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'handleAddFund',
            'renderHistory',
            'renderHistoryItem',
            'renderMessage',
            'renderLink',
            'getFundDesc',
            'trySetFocus',
            'buildRibbon'
        );

        // this.state = {
        //     data: data
        // };
    }

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps)
    }

    componentDidMount() {
        this.trySetFocus(this.props)
    }

    trySetFocus(props) {
        const {focus} = props;

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
                glyph = PartyList.partyIconByPartyTypeCode(data.partyDetail.data.partyType.code);
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
            <Button className='history-list-item history-button' onClick={() => this.dispatch(storeLoadData(type, data))} key={"button-" + keyIndex}>
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
        var partyItems = stateRegion.partyDetailFront.map((x, index) => {
            if (x.data) {
                const name = x.data.name;
                const desc = x.data.partyType.name;
                return this.renderHistoryItem(name, desc, 'PARTY_REGION', {partyDetail:x}, index)
            }
        });
        var registryItems = stateRegion.registryRegionFront.map((x, index) => {
            if (x.registryRegionData._info) {
                const name = x.registryRegionData._info.name;
                const desc = x.registryRegionData._info.desc;
                return this.renderHistoryItem(name, desc, 'REGISTRY_REGION', x, index)
            }
        });
        var arrItems = stateRegion.arrRegionFront.map((x, index) => {
            const name = x.name + (x.lockDate ? ' ' + dateToString(new Date(x.lockDate)) : '');
            const desc = this.getFundDesc(x)
            return this.renderHistoryItem(name, desc, 'ARR_REGION_FUND', x, index)
        });

        if(arrItems.length === 0){
            arrItems.push(this.renderMessage(i18n('home.recent.fund.emptyList.title'), i18n('home.recent.fund.emptyList.message')));
        }
        if(registryItems.length === 1){ //registryItems vzdy obsahuje 1 objekt
            registryItems.push(this.renderMessage(i18n('home.recent.registry.emptyList.title'), i18n('home.recent.registry.emptyList.message')));
        }
        if(partyItems.length === 0){
            partyItems.push(this.renderMessage(i18n('home.recent.party.emptyList.title'), i18n('home.recent.party.emptyList.message')));
        }

        arrItems.push(this.renderLink("/fund",i18n('home.recent.fund.goTo')));
        partyItems.push(this.renderLink("/party",i18n('home.recent.party.goTo')));
        registryItems.push(this.renderLink("/registry",i18n('home.recent.registry.goTo')));

        return (
            <div ref='list' className='history-list-container'>
                <div className="button-container">
                    <h4>{i18n('home.recent.fund.title')}</h4>
                    <div className="section">{arrItems}</div>
                    <h4>{i18n('home.recent.party.title')}</h4>
                    <div className="section">{partyItems}</div>
                    <h4>{i18n('home.recent.registry.title')}</h4>
                    <div className="section">{registryItems}</div>
                </div>
            </div>
        )
    }
    /*
     * Vykreslení informace o prázné historii
     */
    renderMessage(title,message){
        return(<div className="unselected-msg history-list-item no-history">
                    <div className="title">{title}</div>
                    <div className="message">{message}</div>
                </div>);
    }
    /*
     * Vykreslení odkazu do příslušných modulů
     */
    renderLink(to, text, glyph = "fa-arrow-right"){
        return(
            <LinkContainer to={to}>
                <Button className='history-list-item history-button link'>
                    <Icon glyph={glyph}/>
                    <div className='history-name'>{text}</div>
                </Button>
            </LinkContainer>
        );
    }

    handleSearchChange = (x) => {
        console.log(x);
    }
    handleChange = (id, obj) => {
        console.log(id, obj);
    }

    render() {
        const {splitter} = this.props;

        const tt = <div>
            ZACATEK a alsjdaslk dasklj dalsj dlasj asdklj askldj aslkdj aslka alsjdaslk dasklj dalsj dlasj asdklj askldj aslkdj aslka alsjdaslk dasklj dalsj dlasj asdklj askldj aslkdj aslka alsjdaslk dasklj dalsj dlasj asdklj askldj aslkdj aslka alsjdaslk dasklj dalsj dlasj asdklj askldj aslkdj aslka alsjdaslk dasklj dalsj dlasj asdklj askldj aslkdj aslka alsjdaslk dasklj dalsj dlasj asdklj askldj aslkdj aslka alsjdaslk dasklj dalsj dlasj asdklj askldj aslkdj aslka alsjdaslk dasklj dalsj dlasj asdklj askldj aslkdj aslka alsjdaslk dasklj dalsj dlasj asdklj askldj aslkdj aslka alsjdaslk dasklj dalsj dlasj asdklj askldj aslkdj aslk
            <b>Formát datace</b><br />
            Století: 20. st. <i>nebo</i> 20.st. <i>nebo</i> 20st<br />
            Rok: 1968<br />
            Měsíc.rok: 8.1968<br />
            Datum: 21.8.1698<br />
            Datum a čas: 21.8.1968 8:23 <i>nebo</i> 21.8.1968 8:23:31<br />
            <b>Intervaly</b><br />
            Jednotlivá hodnota: 1968<br />
            Interval: 21.8.1968 0:00-27.6.1989<br />
            <b>Odhad</b><br />
            Definuje se uzavřením hodnoty do kulatých nebo hranatých závorek: [16.8.1977]<br />
            Při použití znaku "/" pro oddělení intervalu jsou od i do chápány jako odhad.
            <b>Formát datace</b><br />
            Století: 20. st. <i>nebo</i> 20.st. <i>nebo</i> 20st<br />
            Rok: 1968<br />
            Měsíc.rok: 8.1968<br />
            Datum: 21.8.1698<br />
            Datum a čas: 21.8.1968 8:23 <i>nebo</i> 21.8.1968 8:23:31<br />
            <b>Intervaly</b><br />
            Jednotlivá hodnota: 1968<br />
            Interval: 21.8.1968 0:00-27.6.1989<br />
            <b>Odhad</b><br />
            Definuje se uzavřením hodnoty do kulatých nebo hranatých závorek: [16.8.1977]<br />
            Při použití znaku "/" pro oddělení intervalu jsou od i do chápány jako odhad. KONEC
            </div>

        let centerPanel = (
            <div className='splitter-home'>

<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
                <TooltipTrigger
                    content={tt}
                    holdOnHover={true}
                    position="right,left"
                    >
                    <button>xxxxxxxx</button>
                </TooltipTrigger>

                {this.renderHistory()}
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

export default connect(mapStateToProps)(HomePage);
