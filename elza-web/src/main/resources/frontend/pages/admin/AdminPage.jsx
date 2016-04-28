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
import {RibbonGroup, RibbonSplit, i18n, Icon, Ribbon, ModalDialog, NodeTabs, PartySearch, AbstractReactComponent} from 'components/index.jsx';
import {ButtonGroup, Button} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {developerSet} from 'actions/global/developer.jsx'
import {resetLocalStorage} from 'actions/store/store.jsx'

var AdminPage = class AdminPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleDeveloperMode', 'buildRibbon', 'handleResetLocalStorage')
    }

    handleDeveloperMode() {
        this.dispatch(developerSet(!this.props.developer.enabled));
    }

    handleResetLocalStorage() {
        if (confirm(i18n('global.title.processAction'))) {
            resetLocalStorage();
        }
    }

    buildRibbon() {
        var altActions = [];

        altActions.push(
            <Button active={this.props.developer.enabled} key="developerMode" onClick={this.handleDeveloperMode}><Icon glyph="fa-cogs"/>
                <div><span className="btnText">{i18n('ribbon.action.admin.developer')}</span></div>
            </Button>,
            <Button key="resetLocalStorage" onClick={this.handleResetLocalStorage} title={i18n('ribbon.action.admin.resetLocalStorage.title')}><Icon glyph="fa-refresh"/>
                <div><span className="btnText">{i18n('ribbon.action.admin.resetLocalStorage')}</span></div>
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

