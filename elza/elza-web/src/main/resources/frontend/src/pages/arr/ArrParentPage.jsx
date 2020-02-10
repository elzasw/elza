import './ArrPage.less';
import './ArrParentPage.less';

/**
 * Stránka předku archivních pomůcek, např. pro pořádání, přesuny atp. Společným znakem je vybraný aktivní archivní soubor.
 */

import PropTypes from 'prop-types';

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Tabs, Icon, Ribbon, i18n, Utils} from 'components/shared';
import {ArrFundPanel, FundSettingsForm} from 'components/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';

import {
    RibbonGroup,
    AbstractReactComponent,
    NodeTabs,
    ListBox2,
    LazyListBox,
    FundPackets,
    FundFiles,
    FundTreeMain
} from 'components/index.jsx';
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {WebApi} from 'actions/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {showRegisterJp, fundsFetchIfNeeded} from 'actions/arr/fund.jsx'
import {versionValidate, versionValidationErrorNext, versionValidationErrorPrevious} from 'actions/arr/versionValidation.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {developerNodeScenariosRequest} from 'actions/global/developer.jsx'
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
import {Shortcuts} from 'react-shortcuts';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'
import PageLayout from "../shared/layout/PageLayout";
import {fundChangeReadMode} from 'actions/arr/fund.jsx'
import defaultKeymap from './ArrParentPageKeymap.jsx';
import {FOCUS_KEYS} from "../../constants.tsx";
import * as groups from "../../actions/refTables/groups";

export default class ArrParentPage extends AbstractReactComponent {

    static defaultKeymap = defaultKeymap;


    static propTypes = {
        splitter: PropTypes.object.isRequired,
        arrRegion: PropTypes.object.isRequired,
        developer: PropTypes.object.isRequired,
        rulDataTypes: PropTypes.object.isRequired,
        calendarTypes: PropTypes.object.isRequired,
        descItemTypes: PropTypes.object.isRequired,
        focus: PropTypes.object.isRequired,
        userDetail: PropTypes.object.isRequired,
        ruleSet: PropTypes.object.isRequired,
    };

    constructor(props, layoutClassName) {
        super(props);

        this.bindMethods('buildRibbon', 'handleShortcuts');

        this.layoutClassName = layoutClassName;

        this.state = {};
    }

    handleShortcuts(action,e) {
        console.log("#handleShortcuts ArrParentPage", '[' + action + ']', this);
        e.preventDefault();
        switch (action) {
            case 'back':
                this.props.dispatch(routerNavigate("/~arr"));
                break;
            case 'arr':
                this.props.dispatch(routerNavigate('/arr'));
                this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 1));
                break;
            case 'movements':
                this.props.dispatch(routerNavigate('/arr/movements'));
                this.props.dispatch(setFocus(FOCUS_KEYS.NONE, 1));
                break;
            case 'dataGrid':
                this.props.dispatch(routerNavigate('/arr/dataGrid'));
                this.props.dispatch(setFocus(FOCUS_KEYS.NONE, 1));
                break;
            case 'output':
                this.props.dispatch(routerNavigate('/arr/output'));
                this.props.dispatch(setFocus(FOCUS_KEYS.FUND_OUTPUT, 1));
                break;
            case 'actions':
                this.props.dispatch(routerNavigate('/arr/actions'));
                this.props.dispatch(setFocus(FOCUS_KEYS.FUND_ACTION, 1));
                break;
            case "TOGGLE_READ_MODE":
                this.toggleReadMode();
                break;
        }
    }

    componentDidMount() {
        this.dispatch(descItemTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
        this.dispatch(fundsFetchIfNeeded());
        var activeFund = this.getActiveFund(this.props);
        if (activeFund !== null) {
            this.requestFundTreeData(activeFund);
            this.props.dispatch(groups.fetchIfNeeded(activeFund.versionId));
        }
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(descItemTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
        this.dispatch(fundsFetchIfNeeded());
        var activeFund = this.getActiveFund(nextProps);
        if (activeFund !== null) {
            this.requestFundTreeData(activeFund);
            this.props.dispatch(groups.fetchIfNeeded(activeFund.versionId));
        }
    }

    requestFundTreeData(activeFund) {
        this.dispatch(fundTreeFetchIfNeeded(types.FUND_TREE_AREA_MAIN, activeFund.versionId, activeFund.fundTree.expandedIds));
    }

    toggleReadMode() {
        const {userDetail} = this.props;
        var settings = userDetail.settings;
        var activeFund = this.getActiveFund(this.props);
        var item = {...getOneSettings(settings, 'FUND_READ_MODE', 'FUND', activeFund.id)};
        item.value = item.value === null || item.value === "true" ? false : true;
        settings = setSettings(settings, item.id, item);
        this.dispatch(fundChangeReadMode(activeFund.versionId, item.value));
        this.dispatch(userDetailsSaveSettings(settings));
    }
    getActiveFund(props) {
        const arrRegion = props.arrRegion;
        return arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
    }

    /**
     * Sestavení Ribbonu. Pro překrytí.
     * @return {Object} view
     */
    buildRibbon(readMode, closed) {
    }

    renderLeftPanel(readMode, closed) {
        return null;
    }

    renderCenterPanel(readMode, closed) {
        return null;
    }

    renderRightPanel(readMode, closed) {
        return null;
    }

    // Nutne prekryt, activeFund muze but null, pokud se vrati true, stranka bude zobrazena, jinak se zobrazi, ze na ni nema pravo
    hasPageShowRights(userDetail, activeFund) {
        console.error("Method hasPageShowRights must be overriden!");
    }

    render() {
        const {splitter, arrRegion, userDetail, ruleSet, rulDataTypes, calendarTypes, descItemTypes} = this.props;

        var activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;

        var statusHeader;
        var leftPanel;
        var rightPanel;
        let readMode = false;
        let closed = false;

        if (this.hasPageShowRights(userDetail, activeFund)) {   // má právo na tuto stránku
            var centerPanel;
            if (activeFund) {

                var settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', activeFund.id);
                var settingsValues = settings.value != 'false';
                readMode = settingsValues;
                closed = activeFund.lockDate != null;

                statusHeader = <ArrFundPanel />

                centerPanel = this.renderCenterPanel(readMode, closed);
                leftPanel = this.renderLeftPanel(readMode, closed);
                rightPanel = this.renderRightPanel(readMode, closed);
            } else {
                centerPanel = (
                    <div className="fund-noselect">{i18n('arr.fund.noselect')}</div>
                )
            }
        } else {
            centerPanel = <div>{i18n('global.insufficient.right')}</div>
        }

        return (
            <Shortcuts name='ArrParent' handler={this.handleShortcuts} global className="main-shortcuts2" stopPropagation={false} alwaysFireHandler>
                <PageLayout
                    splitter={splitter}
                    _className='fa-page'
                    className={this.layoutClassName ? ("arr-abstract-page " + this.layoutClassName) : "arr-abstract-page"}
                    ribbon={this.buildRibbon(readMode, closed)}
                    centerPanel={centerPanel}
                    leftPanel={leftPanel}
                    rightPanel={rightPanel}
                    status={statusHeader}
                />
            </Shortcuts>
        )
    }
}
