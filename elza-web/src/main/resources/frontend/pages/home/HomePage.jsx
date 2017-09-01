import React from 'react';
import ReactDOM from 'react-dom';

import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Icon, i18n} from 'components/index.jsx';
import {TooltipTrigger, Splitter, Autocomplete, RibbonGroup, ToggleContent, AbstractReactComponent, Utils} from 'components/shared';
import {FundForm, Ribbon, FindindAidFileTree, PartyListItem} from 'components/index.jsx';
import {NodeTabs} from 'components/index.jsx';
import {Button} from 'react-bootstrap';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {createFund} from 'actions/arr/fund.jsx'
import {storeLoadData, storeLoad} from 'actions/store/store.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import PageLayout from "../shared/layout/PageLayout";

// Testování
// import AutocompleteTest from "./test/AutocompleteTest";

import './HomePage.less'

/**
 * Home stránka
 */
class HomePage extends AbstractReactComponent {

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps)
    }

    componentDidMount() {
        this.trySetFocus(this.props)
    }

    trySetFocus = (props) => {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.list) {   // ještě nemusí existovat
                    this.setState({}, () => {
                        const listEl = ReactDOM.findDOMNode(this.refs.list);
                        Utils.setInputFocus(listEl, false);
                        focusWasSet()
                    })
                }
            } else if (isFocusFor(focus, 'home', 1) || isFocusFor(focus, 'home', 1, 'list')) {
                this.setState({}, () => {
                    const listEl = ReactDOM.findDOMNode(this.refs.list);
                    Utils.setInputFocus(listEl, false);
                    focusWasSet()
                })
            }
        }
    };

    handleAddFund = () => {
        this.dispatch(modalDialogShow(
            this,
            i18n('arr.fund.title.add'),
            <FundForm create onSubmitForm={(data) => {this.dispatch(createFund(data))}}/>
        ));
    };

    buildRibbon = () => {
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
    };

    renderHistoryItem = (name, desc, type, data, keyIndex) => {
        let glyph;
        switch (type) {
            case 'PARTY_REGION':
                glyph = PartyListItem.partyIconByPartyTypeCode(data.partyDetail.data.partyType.code);
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

        const hasDesc = desc && desc.length > 0;

        let descComp;
        if (hasDesc) {
            descComp = <small>{desc}</small>
        } else {
            descComp = <small>&nbsp;</small>
        }

        return <Button className='history-list-item history-button' onClick={() => this.dispatch(storeLoadData(type, data))} key={"button-" + keyIndex}>
            <Icon glyph={glyph}/>
            <div className='history-name'>{name}</div>
            {false && descComp}
        </Button>;
    };

    arrToString = (arr) => {
        return arr.map((d, index) => {
            if (index > 0 && index < arr.length) {
                return ',  ' + d;
            } else {
                return d;
            }
        })
    };

    getFundDesc = (fund) => {
        const descs = fund.nodes.nodes.map(nodeobj => nodeobj.name);
        return this.arrToString(descs);
    };

    renderHistory = () => {
        const {stateRegion} = this.props;
        const partyItems = stateRegion.partyDetailFront.map((x, index) => {
            if (x.data) {
                const name = x.data.name;
                const desc = x.data.partyType.name;
                return this.renderHistoryItem(name, desc, 'PARTY_REGION', {partyDetail:x}, index)
            }
        });
        const registryItems = stateRegion.registryRegionFront.map((x, index) => {
            if (x.data) {
                const name = x.data.record;
                const desc = x.data.characteristics;
                return this.renderHistoryItem(name, desc, 'REGISTRY_REGION', x, index)
            }
        });
        const arrItems = stateRegion.arrRegionFront.map((x, index) => {
            const name = x.name + (x.lockDate ? ' ' + Utils.dateToString(new Date(x.lockDate)) : '');
            const desc = this.getFundDesc(x);
            return this.renderHistoryItem(name, desc, 'ARR_REGION_FUND', x, index)
        });

        if (arrItems.length === 0) {
            arrItems.push(this.renderMessage(i18n('home.recent.fund.emptyList.title'), i18n('home.recent.fund.emptyList.message')));
        }
        if (registryItems.length === 0) {
            registryItems.push(this.renderMessage(i18n('home.recent.registry.emptyList.title'), i18n('home.recent.registry.emptyList.message')));
        }
        if (partyItems.length === 0) {
            partyItems.push(this.renderMessage(i18n('home.recent.party.emptyList.title'), i18n('home.recent.party.emptyList.message')));
        }

        arrItems.push(this.renderLink("/fund", i18n('home.recent.fund.goTo')));
        partyItems.push(this.renderLink("/party", i18n('home.recent.party.goTo')));
        registryItems.push(this.renderLink("/registry", i18n('home.recent.registry.goTo')));

        return <div ref='list' className='history-list-container'>
            <div className="button-container">
                <h4>{i18n('home.recent.fund.title')}</h4>
                <div className="section">{arrItems}</div>
                <h4>{i18n('home.recent.party.title')}</h4>
                <div className="section">{partyItems}</div>
                <h4>{i18n('home.recent.registry.title')}</h4>
                <div className="section">{registryItems}</div>
            </div>
        </div>
    };

    /**
     * Vykreslení informace o prázné historii
     */
    renderMessage = (title, message) => <div key="blank" className="unselected-msg history-list-item no-history">
        <div className="title">{title}</div>
        <div className="message">{message}</div>
    </div>;

    /**
     * Vykreslení odkazu do příslušných modulů
     */
    renderLink = (to, text, glyph = "fa-arrow-right") => <LinkContainer key={to} to={to} className='history-list-item history-button link'>
        <Button>
            <Icon glyph={glyph}/>
            <div className='history-name'>{text}</div>
        </Button>
    </LinkContainer>;

    render() {
        // Test komponent - jen pro vývojové účely
        // return <AutocompleteTest/>

        const {splitter} = this.props;

        let centerPanel = <div className='splitter-home'>
            {this.renderHistory()}
        </div>;

        return <PageLayout
            splitter={splitter}
            className='party-page'
            ribbon={this.buildRibbon()}
            centerPanel={centerPanel}
        />;
    }
}

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, stateRegion, focus, userDetail} = state;
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
