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

var HomePage = class HomePage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleAddFa', 'handleCallAddFa', 'renderHistory',
            'renderHistoryItem');

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
            <Button onClick={this.handleAddFa}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.arr.fa.add')}</span></div></Button>
        );

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup className="large">{altActions}</RibbonGroup>
        }

        return (
            <Ribbon home altSection={altSection} {...this.props} />
        )
    }

    renderHistoryItem(name, type, data) {
        return (
            <Panel className='history-list-item' header={name} onClick={() => this.dispatch(storeLoadData(type, data))}>
                ...[{type}]
            </Panel>
        )
    }

    renderHistory() {
        var items = [];

        const {stateRegion} = this.props;

        if (stateRegion.partyRegion) {
            items.push(this.renderHistoryItem('Osoby', 'PARTY_REGION', stateRegion.partyRegion));
        }
        if (stateRegion.registryRegion) {
            items.push(this.renderHistoryItem('Rejstříky', 'REGISTRY_REGION', stateRegion.registryRegion));
        }
        if (stateRegion.arrRegion) {
            items.push(this.renderHistoryItem('Pořádání', 'ARR_REGION', stateRegion.arrRegion));
        }
        Object.keys(stateRegion.faData).forEach(versionId => {
            var fa = stateRegion.faData[versionId];
            items.push(this.renderHistoryItem('Pořádání [' + fa.name + ']', 'ARR_REGION_FA', fa));
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
                HOME
                <Button onClick={() => this.dispatch(storeSave())}>STORE</Button>
                <Button onClick={() => this.dispatch(storeLoad())}>LOAD</Button>
                {this.renderHistory()}

<Combobox defaultValue={'1'}
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
    </Combobox>

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

