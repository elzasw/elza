/**
 * Home stránka
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components';
import {AddFaForm, Ribbon, RibbonGroup, ToggleContent, FindindAidFileTree, AbstractReactComponent} from 'components';
import {ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {createFa} from 'actions/arr/fa'

var HomePage = class HomePage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleAddFa', 'handleCallAddFa');

        this.buildRibbon = this.buildRibbon.bind(this);
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
            <Button onClick={this.handleAddFa}><Glyphicon glyph="plus" /><div><span className="btnText">{i18n('ribbon.action.arr.fa.add')}</span></div></Button>
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

