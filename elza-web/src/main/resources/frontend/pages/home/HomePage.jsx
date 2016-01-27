/**
 * Home stránka
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Icon, i18n} from 'components';
import {AddFaForm, Ribbon, RibbonGroup, ToggleContent, FindindAidFileTree, AbstractReactComponent} from 'components';
import {ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {modalDialogShow} from 'actions/global/modalDialog'
import {createFa} from 'actions/arr/fa'

import {Combobox} from 'react-input-enhancements'

var HomePage = class HomePage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleAddFa', 'handleCallAddFa');

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

    render() {
        var centerPanel = (
            <div>
                HOME

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
    const {arrRegion, refTables} = state
    return {
        arrRegion,
        refTables
    }
}

module.exports = connect(mapStateToProps)(HomePage);

