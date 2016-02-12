/**
 * Úvodní stránka administrace.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'

require ('./AdminPage.less');

import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {RibbonGroup, i18n, Icon, Ribbon, ModalDialog, NodeTabs, PartySearch, AbstractReactComponent} from 'components';
import {ButtonGroup, Button} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {developerSet} from 'actions/global/developer'

var AdminPage = class AdminPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleDeveloperMode', 'buildRibbon')
    }

    handleDeveloperMode() {
        this.dispatch(developerSet(!this.props.developer.enabled));
    }

    buildRibbon() {
        var altActions = [];

        altActions.push(
            <Button active={this.props.developer.enabled} key="edit-version" onClick={this.handleDeveloperMode}><Icon glyph="fa-cogs"/>
                <div><span className="btnText">{i18n('ribbon.action.admin.developer')}</span></div>
            </Button>,
        )

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt" className="large">{altActions}</RibbonGroup>
        }

        return (
            <Ribbon admin altSection={altSection} {...this.props} />
        )
    }

    render() {
        const {splitter} = this.props;

        var centerPanel = (
                <div>
                    Administrace - HOME
                </div>
        )

        return (
            <PageLayout
                splitter={splitter}
                    className='admin-packages-page'
                    ribbon={this.buildRibbon()}
                    centerPanel={centerPanel}
            />
        )
    }
}

function mapStateToProps(state) {
    const {splitter, developer} = state
    
    return {
        splitter,
        developer,
    }
}

module.exports = connect(mapStateToProps)(AdminPage);

