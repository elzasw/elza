/**
 * Stránka požadavků na digitalizaci.
 */

import React from 'react';
import Utils from "components/Utils.jsx";
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {
    ListBox,
    Ribbon,
    Loading,
    RibbonGroup,
    FundNodesSelectForm,
    Icon,
    FundNodesList,
    i18n,
    ArrRequestDetail,
    ArrOutputDetail,
    AddOutputForm,
    AbstractReactComponent,
    Tabs,
    FundOutputFiles,
    FundOutputFunctions,
    RunActionForm,
    FormInput,
    ArrFundPanel
} from 'components/index.jsx';
import {Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {canSetFocus, setFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {
    fundOutputFetchIfNeeded,
    fundOutputRemoveNodes,
    fundOutputSelectOutput,
    fundOutputCreate,
    fundOutputUsageEnd,
    fundOutputDelete,
    fundOutputAddNodes,
    fundOutputGenerate,
    fundOutputRevert,
    fundOutputClone,
    fundOutputFilterByState
} from 'actions/arr/fundOutput.jsx'
import {fundOutputActionRun} from 'actions/arr/fundOutputFunctions.jsx'
import * as perms from 'actions/user/Permission.jsx';
import * as digitizationActions from 'actions/arr/digitizationActions';
import {fundActionFormShow, fundActionFormChange} from 'actions/arr/fundAction.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {packetsFetchIfNeeded} from 'actions/arr/packets.jsx'
import {templatesFetchIfNeeded} from 'actions/refTables/templates.jsx'
import AddDescItemTypeForm from 'components/arr/nodeForm/AddDescItemTypeForm.jsx'
import {outputFormActions} from 'actions/arr/subNodeForm.jsx'
import {outputTypesFetchIfNeeded} from "actions/refTables/outputTypes.jsx";
import {createDigitizationName, getDescItemsAddTree, getOneSettings} from 'components/arr/ArrUtils.jsx';
import ArrParentPage from "./ArrParentPage.jsx";

const classNames = require('classnames');
const ShortcutsManager = require('react-shortcuts');
const Shortcuts = require('react-shortcuts/component');

const keyModifier = Utils.getKeyModifier();

require("./ArrRequestPage.less");

const keymap = ArrParentPage.mergeKeymap({
    ArrParent: {
        area1: keyModifier + '1',
        area2: keyModifier + '2',
    }
});
const shortcutManager = new ShortcutsManager(keymap);

const ArrRequestPage = class extends ArrParentPage {
    constructor(props) {
        super(props, "arr-request-page");
    }

    static PropTypes = {
        splitter: React.PropTypes.object.isRequired,
        arrRegion: React.PropTypes.object.isRequired,
        focus: React.PropTypes.object.isRequired,
        userDetail: React.PropTypes.object.isRequired
    };

    componentDidMount() {
        super.componentDidMount();

        const fund = this.getActiveFund(this.props);
        this.dispatch(digitizationActions.fetchListIfNeeded(fund.versionId));

        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        super.componentWillReceiveProps(nextProps);

        const fund = this.getActiveFund(nextProps);
        this.dispatch(digitizationActions.fetchListIfNeeded(fund.versionId));

        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, 'fund-request', 1)) {
                this.refs.fundOutputList && this.setState({}, () => {
                    ReactDOM.findDOMNode(this.refs.fundOutputList).focus()
                })
                focusWasSet()
            }
        }
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts ArrRequestPage", '[' + action + ']', this);
        switch (action) {
            case 'newOutput':
                this.handleAddOutput();
                break;
            case 'area1':
                this.dispatch(setFocus('fund-request', 1));
                break;
            case 'area2':
                this.dispatch(setFocus('fund-request', 2));
                break;
            default:
                super.handleShortcuts(action);
        }
    }

    getChildContext() {
        return {shortcuts: shortcutManager};
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon(readMode, closed) {
        const {userDetail} = this.props;

        const fund = this.getActiveFund(this.props);
        var itemActions = [];
        var altActions = [];
        if (fund) {
            const requestDetail = fund.requestDetail;
            const detailSelected = requestDetail.id !== null;
            const detailLoaded = requestDetail.fetched && !requestDetail.isFetching;

            if (!readMode && !closed) {
                if (detailSelected && !closed) {
                    itemActions.push(
                        <Button key="send" onClick={() => {this.handleSend(requestDetail.id)}} disabled={!detailLoaded || requestDetail.data.state != "OPEN"}><Icon glyph="fa-youtube-play" />
                            <div><span className="btnText">{i18n('ribbon.action.arr.fund.request.send')}</span></div>
                        </Button>
                    )
                }
            }
        }

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt" className="small">{altActions}</RibbonGroup>
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item" className="small">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon arr subMenu fundId={fund ? fund.id : null} altSection={altSection} itemSection={itemSection}/>
        )
    }

    handleSelect = (item) => {
        const fund = this.getActiveFund(this.props);
        this.dispatch(digitizationActions.selectDetail(fund.versionId, item.id));
    }

    handleSend = (id) => {
        const fund = this.getActiveFund(this.props);
        this.dispatch(digitizationActions.sendRequest(fund.versionId, id));
    }

    isEditable(item) {
        return !item.lockDate && item.outputDefinition && item.outputDefinition.state === OutputState.OPEN
    }

    renderListItem = (item, isActive, index) => {
        const {userDetail} = this.props;
        const fund = this.getActiveFund(this.props);

        var cls = {
        }

        return (
            <div className={classNames(cls)}>
                <div className='name'>{createDigitizationName(item, userDetail)}</div>
                <div className='state'>{i18n("arr.request.title.state")}: {i18n("arr.request.title.state." + item.state)}</div>
            </div>
        )
    }

    renderLeftPanel(readMode, closed) {
        const fund = this.getActiveFund(this.props);
        const {requestList, requestDetail} = fund;

        let activeIndex = null;
        if (requestDetail.id !== null) {
            activeIndex = indexById(requestList.rows, requestDetail.id)
        }

        return (
            <div className="fund-request-list-container">
                <ListBox
                    className='fund-request-listbox'
                    ref='fundDigitizationRequestList'
                    items={requestList.rows}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />
            </div>
        )
    }

    renderCenterPanel(readMode, closed) {
        const fund = this.getActiveFund(this.props);
        const {requestDetail} = fund;

        return (
            <ArrRequestDetail
                versionId={fund.versionId}
                fund={fund}
                requestDetail={requestDetail}
            />
        )
    }

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasArrOutputPage(activeFund ? activeFund.id : null);
    }
};

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, focus, userDetail} = state
    return {
        splitter,
        arrRegion,
        focus,
        userDetail,
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
        packetTypes: refTables.packetTypes,
        ruleSet: refTables.ruleSet,
        templates: refTables.templates,
        outputTypes: refTables.outputTypes.items,
    }
}

export default connect(mapStateToProps)(ArrRequestPage);
