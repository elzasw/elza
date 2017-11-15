/**
 * Stránka požadavků na digitalizaci.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {
    FundNodesSelectForm,
    Ribbon,
    FundNodesList,
    ArrRequestDetail,
    ArrOutputDetail,
    AddOutputForm,
    FundOutputFiles,
    FundOutputFunctions,
    RunActionForm,
    ArrFundPanel
} from 'components/index.jsx';
import {
    ListBox,
    FormInput,
    Loading,
    RibbonGroup,
    Icon,
    i18n,
    AbstractReactComponent,
    Tabs,
    SearchWithGoto,
    StoreHorizontalLoader,
    Utils
} from 'components/shared';
import {Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import PageLayout from "../shared/layout/PageLayout";
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
import * as arrRequestActions from 'actions/arr/arrRequestActions';
import {fundActionFormShow, fundActionFormChange} from 'actions/arr/fundAction.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {packetsFetchIfNeeded} from 'actions/arr/packets.jsx'
import {templatesFetchIfNeeded} from 'actions/refTables/templates.jsx'
import {outputFormActions} from 'actions/arr/subNodeForm.jsx'
import {outputTypesFetchIfNeeded} from "actions/refTables/outputTypes.jsx";
import {
    DIGITIZATION,
    DAO,
    DAO_LINK,
    createDigitizationName,
    getDescItemsAddTree,
    getOneSettings
} from 'components/arr/ArrUtils.jsx';
import ArrParentPage from "./ArrParentPage.jsx";

const classNames = require('classnames');
import {Shortcuts} from 'react-shortcuts';

import "./ArrRequestPage.less";
import {FOCUS_KEYS} from "../../constants";

class ArrRequestPage extends ArrParentPage {
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
        if (fund) {
            this.dispatch(arrRequestActions.fetchListIfNeeded(fund.versionId));
        }

        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        super.componentWillReceiveProps(nextProps);

        const fund = this.getActiveFund(nextProps);
        if (fund) {
            this.dispatch(arrRequestActions.fetchListIfNeeded(fund.versionId));
        }

        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, FOCUS_KEYS.FUND_REQUEST, 1)) {
                this.refs.fundOutputList && this.setState({}, () => {
                    ReactDOM.findDOMNode(this.refs.fundOutputList).focus()
                })
                focusWasSet()
            }
        }
    }

    handleShortcuts(action,e) {
        console.log("#handleShortcuts ArrRequestPage", '[' + action + ']', this);
        switch (action) {
            case 'newOutput':
                this.handleAddOutput();
                break;
            case 'area1':
                this.dispatch(setFocus(FOCUS_KEYS.FUND_REQUEST, 1));
                break;
            case 'area2':
                this.dispatch(setFocus(FOCUS_KEYS.FUND_REQUEST, 2));
                break;
            default:
                super.handleShortcuts(action);
        }
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
                    );
                    itemActions.push(
                        <Button key="delete" onClick={() => {this.handleDelete(requestDetail.id)}} disabled={!detailLoaded || requestDetail.data.state != "OPEN"}><Icon glyph="fa-trash" />
                            <div><span className="btnText">{i18n('ribbon.action.arr.fund.request.delete')}</span></div>
                        </Button>
                    );
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
        this.dispatch(arrRequestActions.selectDetail(fund.versionId, item.id));
    };

    handleSend = (id) => {
        const fund = this.getActiveFund(this.props);
        this.dispatch(arrRequestActions.sendRequest(fund.versionId, id));
    };

    handleDelete = (id) => {
        const fund = this.getActiveFund(this.props);
        if (confirm(i18n("ribbon.action.arr.fund.request.delete.confirm"))) {
            this.dispatch(arrRequestActions.deleteRequest(fund.versionId, id));
        }
    };

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
    };

    handleFilterType = (e) => {
        const fund = this.getActiveFund(this.props);
        const {requestList: {filter}} = fund;

        const val = e.target.value;
        const newFilter = {
            ...filter,
            type: val
        }

        this.dispatch(arrRequestActions.filterList(fund.versionId, newFilter));
    };

    handleFilterText = (filterText) => {
        const fund = this.getActiveFund(this.props);
        const {requestList: {filter}} = fund;

        const newFilter = {
            ...filter,
            description: filterText
        };

        this.dispatch(arrRequestActions.filterList(fund.versionId, newFilter));
    };

    handleFilterTextClear = () => {
        this.handleFilterText(null);
    };

    renderLeftPanel(readMode, closed) {
        const fund = this.getActiveFund(this.props);
        const {requestList, requestDetail} = fund;

        let activeIndex = null;
        if (requestDetail.id !== null) {
            activeIndex = indexById(requestList.rows, requestDetail.id)
        }

        return (
            <div className="fund-request-list-container">
                <div className="filter">
                    <FormInput componentClass="select" className="type" onChange={this.handleFilterType} value={requestList.filter.type}>
                        <option value={""}>{i18n('global.all')}</option>
                        <option value="DIGITIZATION" key="DIGITIZATION">{i18n("arr.request.title.type." + DIGITIZATION)}</option>
                        <option value="DESTRUCTION" key="DESTRUCTION">{i18n("arr.request.title.type.dao.DESTRUCTION")}</option>
                        <option value="TRANSFER" key="TRANSFER">{i18n("arr.request.title.type.dao.TRANSFER")}</option>
                        {/*<option value="DAO_LINK" key="DAO_LINK">{i18n("arr.request.title.type." + DAO_LINK)}</option>*/}
                    </FormInput>
                    <SearchWithGoto
                        onFulltextSearch={this.handleFilterText}
                        onClear={this.handleFilterTextClear}
                        placeholder={i18n('search.input.search')}
                        filterText={requestList.filter.description}
                        showFilterResult={true}
                        type="INFO"
                        itemsCount={requestList.filteredRows ? requestList.filteredRows.length : 0}
                        allItemsCount={requestList.count}
                    />
                </div>
                <StoreHorizontalLoader store={requestList} />
                {requestList.fetched && <ListBox
                    className='fund-request-listbox'
                    ref='fundDigitizationRequestList'
                    items={requestList.rows}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />}
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
        return userDetail.hasRdPage(activeFund ? activeFund.id : null);
    }
}

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
