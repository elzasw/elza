/**
 * Stránka předku archivních pomůcek, např. pro pořádání, přesuny atp. Společným znakem je vybraný aktivní archivní soubor.
 */

require('./ArrPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {FundSettingsForm, Tabs, Icon, Ribbon, i18n, ArrFundPanel} from 'components/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';

import {
    BulkActionsDialog,
    RibbonGroup,
    AbstractReactComponent,
    NodeTabs,
    ListBox2,
    LazyListBox,
    VisiblePolicyForm,
    Loading,
    FundPackets,
    FundFiles,
    FundTreeMain
} from 'components/index.jsx';
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {WebApi} from 'actions/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {showRegisterJp, fundsFetchIfNeeded} from 'actions/arr/fund.jsx'
import {versionValidate, versionValidationErrorNext, versionValidationErrorPrevious} from 'actions/arr/versionValidation.jsx'
import {packetsFetchIfNeeded} from 'actions/arr/packets.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes.jsx'
import {developerNodeScenariosRequest} from 'actions/global/developer.jsx'
import {Utils} from 'components/index.jsx';
import {isFundRootId, getSettings, setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx';
import {setFocus} from 'actions/global/focus.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {fundNodesPolicyFetchIfNeeded} from 'actions/arr/fundNodesPolicy.jsx'
import {fundActionFormChange, fundActionFormShow} from 'actions/arr/fundAction.jsx'
import {fundSelectSubNode} from 'actions/arr/nodes.jsx'
import {createFundRoot} from 'components/arr/ArrUtils.jsx'
import {setVisiblePolicyRequest} from 'actions/arr/visiblePolicy.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {fundTreeFetchIfNeeded} from 'actions/arr/fundTree.jsx'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {selectTab} from 'actions/global/tab.jsx'
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'

var ArrParentPage = class ArrParentPage extends AbstractReactComponent {
    constructor(props, layoutClassName) {
        super(props);

        this.bindMethods(
            'buildRibbon',
            'handleShortcuts',
        );

        this.layoutClassName = layoutClassName;

        this.state = {
        };
    }

    componentDidMount() {
        this.dispatch(descItemTypesFetchIfNeeded());
        this.dispatch(packetTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
        this.dispatch(fundsFetchIfNeeded());
        var activeFund = this.getActiveFund(this.props);
        if (activeFund !== null) {
            this.dispatch(packetsFetchIfNeeded(activeFund.id));
            this.requestFundTreeData(activeFund);
        }
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(descItemTypesFetchIfNeeded());
        this.dispatch(packetTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
        this.dispatch(fundsFetchIfNeeded());
        var activeFund = this.getActiveFund(nextProps);
        if (activeFund !== null) {
            this.dispatch(packetsFetchIfNeeded(activeFund.id));
            this.requestFundTreeData(activeFund);
        }
    }

    requestFundTreeData(activeFund) {
        this.dispatch(fundTreeFetchIfNeeded(types.FUND_TREE_AREA_MAIN, activeFund.versionId, activeFund.fundTree.expandedIds, activeFund.fundTree.selectedId));
    }

    getActiveFund(props) {
        var arrRegion = props.arrRegion;
        return arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
    }

    // Pokud ma stranka klavesove zkratky, musi se tato metoda prekryt a vratit zkratky pro nazev ArrParent!!!
    getChildContext() {
        return {shortcuts: new ShortcutsManager({ArrParent: {aaa: "123"}})}
    }

    /**
     * Sestavení Ribbonu. Pro překrytí.
     * @return {Object} view
     */
    buildRibbon() {
    }

    renderLeftPanel() {
        return null;
    }

    renderCenterPanel() {
        return null;
    }

    renderRightPanel() {
        return null;
    }

    // Nutne prekryt, activeFund muze but null, pokud se vrati true, stranka bude zobrazena, jinak se zobrazi, ze na ni nema pravo
    hasPageShowRights(userDetail, activeFund) {
        console.error("Method hasPageShowRights must be overriden!");
    }

    render() {
        const {splitter, arrRegion, userDetail, ruleSet, rulDataTypes, calendarTypes, descItemTypes, packetTypes} = this.props;

        var activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;

        var statusHeader;
        var leftPanel;
        var rightPanel;

        if (this.hasPageShowRights(userDetail, activeFund)) {   // má právo na tuto stránku
            var centerPanel;
            if (activeFund) {
                statusHeader = <ArrFundPanel />

                var packets = [];
                var fundId = activeFund.id;
                if (fundId && arrRegion.packets[fundId]) {
                    packets = arrRegion.packets[fundId].items;
                }

                centerPanel = this.renderCenterPanel();
                leftPanel = this.renderLeftPanel();
                rightPanel = this.renderRightPanel();
            } else {
                centerPanel = (
                    <div className="fund-noselect">{i18n('arr.fund.noselect')}</div>
                )
            }
        } else {
            centerPanel = <div>{i18n('global.insufficient.right')}</div>
        }

        return (
            <Shortcuts name='ArrParent' handler={this.handleShortcuts}>
                <PageLayout
                    splitter={splitter}
                    _className='fa-page'
                    className={this.layoutClassName}
                    ribbon={this.buildRibbon()}
                    centerPanel={centerPanel}
                    leftPanel={leftPanel}
                    rightPanel={rightPanel}
                    status={statusHeader}
                />
            </Shortcuts>
        )
    }
}

ArrParentPage.propTypes = {
    splitter: React.PropTypes.object.isRequired,
    arrRegion: React.PropTypes.object.isRequired,
    developer: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    focus: React.PropTypes.object.isRequired,
    userDetail: React.PropTypes.object.isRequired,
    ruleSet: React.PropTypes.object.isRequired,
}

ArrParentPage.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
};

module.exports = ArrParentPage;
