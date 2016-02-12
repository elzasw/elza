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
import {Splitter, Autocomplete, FaForm, Ribbon, RibbonGroup, ToggleContent, FindindAidFileTree, AbstractReactComponent} from 'components';
import {ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button, Panel} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {modalDialogShow} from 'actions/global/modalDialog'
import {createFa} from 'actions/arr/fa'
import {storeLoadData, storeSave, storeLoad} from 'actions/store/store'
import {Combobox} from 'react-input-enhancements'
import {WebApi} from 'actions'
import {dateToString} from 'components/Utils'

var HomePage = class HomePage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleAddFa', 'handleCallAddFa', 'renderHistory',
            'renderHistoryItem', 'getFaDesc');

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    handleAddFa() {
        this.dispatch(modalDialogShow(this, i18n('arr.fa.title.add'), <FaForm create
                                                                              onSubmitForm={this.handleCallAddFa}/>));
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

    getFaDesc(fa) {
        var descs = fa.nodes.nodes.map(nodeobj => nodeobj.name);
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
            var descs = stateRegion.arrRegion.fas.map(faobj => faobj.name);
            var desc = this.arrToString(descs)
            items.push(this.renderHistoryItem(i18n('home.action.arr'), desc, 'ARR_REGION', stateRegion.arrRegion));
        }
        stateRegion.arrRegionFront.forEach(x => {
            var name = x.name + (x.lockDate ? ' ' + dateToString(new Date(x.lockDate)) : '');
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
        const {splitter} = this.props;

        var centerPanel = (
            <div className='splitter-home'>
                {false && <div>
                    <Button onClick={() => this.dispatch(storeSave())}>STORE</Button>
                    <Button onClick={() => this.dispatch(storeLoad())}>LOAD</Button></div>}
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
    const {splitter, arrRegion, refTables, stateRegion} = state
    return {
        splitter,
        arrRegion,
        refTables,
        stateRegion
    }
}

module.exports = connect(mapStateToProps)(HomePage);

