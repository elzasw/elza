/**
 * Úvodní stránka administrace.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */

require ('./AdminPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {RibbonGroup, RibbonSplit, i18n, Icon, Ribbon, ModalDialog, NodeTabs, AbstractReactComponent} from 'components/index.jsx';
import {ButtonGroup, Button} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {developerSet} from 'actions/global/developer.jsx'
import {resetLocalStorage} from 'actions/store/storeEx.jsx'
import {WebApi} from 'actions/index.jsx';
import * as perms from 'actions/user/Permission.jsx';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'

const AdminPage = class AdminPage extends AbstractReactComponent {
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

    handleResetServerCache = () => {
        if (confirm(i18n('global.title.processAction'))) {
            WebApi.resetServerCache().then(() => {
                this.dispatch(addToastrSuccess(i18n('admin.resetServerCache.success')));
            });
        }
    };

    buildRibbon() {
        const {userDetail} = this.props

        var altActions = [];

        if (userDetail.hasOne(perms.FUND_ARR_ALL, perms.FUND_ARR, perms.FUND_RD_ALL, perms.FUND_RD)) {
            altActions.push(
                <Button active={this.props.developer.enabled} key="developerMode"
                        onClick={this.handleDeveloperMode}><Icon glyph="fa-cogs"/>
                    <div><span className="btnText">{i18n('ribbon.action.admin.developer')}</span></div>
                </Button>
            )
        }
        altActions.push(
            <Button key="resetLocalStorage" onClick={this.handleResetLocalStorage} title={i18n('ribbon.action.admin.resetLocalStorage.title')}><Icon glyph="fa-refresh"/>
                <div><span className="btnText">{i18n('ribbon.action.admin.resetLocalStorage')}</span></div>
            </Button>
        );
        altActions.push(
            <Button key="resetServerCache" onClick={this.handleResetServerCache} title={i18n('ribbon.action.admin.resetServerCache.title')}><Icon glyph="fa-refresh"/>
                <div><span className="btnText">{i18n('ribbon.action.admin.resetServerCache')}</span></div>
            </Button>
        );

        let altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt" className="small">{altActions}</RibbonGroup>
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
    const {splitter, developer, userDetail} = state
    
    return {
        splitter,
        developer,
        userDetail,
    }
}

module.exports = connect(mapStateToProps)(AdminPage);

