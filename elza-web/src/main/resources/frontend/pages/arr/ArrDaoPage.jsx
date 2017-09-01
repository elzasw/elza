/**
 * Stránka archivních pomůcek.
 */

import './ArrPage.less';
import './ArrDaoPage.less';

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';

import ArrDaoPackages from '../../components/arr/ArrDaoPackages'
import Ribbon from '../../components/page/Ribbon'
import FundTreeDaos from '../../components/arr/FundTreeDaos'
import ArrDaos from '../../components/arr/ArrDaos'

import {
    i18n,
    Tabs, Icon, Search,
    RibbonGroup,
    AbstractReactComponent,
    ListBox2,
    LazyListBox,
    Loading,
    Utils
} from 'components/shared';
import * as types from 'actions/constants/ActionTypes.js';
import {createFundRoot, getParentNode} from 'components/arr/ArrUtils.jsx'
import {moveNodesUnder, moveNodesBefore, moveNodesAfter} from 'actions/arr/nodes.jsx'
import {addNodeForm} from "actions/arr/addNodeForm.jsx"
import ArrParentPage from "./ArrParentPage.jsx";
import {fundTreeSelectNode} from 'actions/arr/fundTree.jsx'
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import PageLayout from "../shared/layout/PageLayout";
import {WebApi} from 'actions/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {showRegisterJp, fundsFetchIfNeeded} from 'actions/arr/fund.jsx'
import {versionValidate, versionValidationErrorNext, versionValidationErrorPrevious} from 'actions/arr/versionValidation.jsx'
import {packetsFetchIfNeeded} from 'actions/arr/packets.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes.jsx'
import {developerNodeScenariosRequest} from 'actions/global/developer.jsx'
import {isFundRootId, getSettings, setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx';
import {setFocus} from 'actions/global/focus.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {fundNodesPolicyFetchIfNeeded} from 'actions/arr/fundNodesPolicy.jsx'
import {fundActionFormChange, fundActionFormShow} from 'actions/arr/fundAction.jsx'
import {fundSelectSubNode} from 'actions/arr/nodes.jsx'
import {setVisiblePolicyRequest} from 'actions/arr/visiblePolicy.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {fundTreeFetchIfNeeded} from 'actions/arr/fundTree.jsx'
import {Shortcuts} from 'react-shortcuts';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {selectTab} from 'actions/global/tab.jsx'
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'

class ArrDaoPage extends ArrParentPage {
    constructor(props) {
        super(props, "dao-page");

    }

    state = {
        selectedTab: 0,
        selectedUnassignedPackage: null,
        selectedPackage: null,
        selectedDaoLeft: null,  // vybrané dao v levé části
    };

    static PropTypes = {
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
    };

    componentDidMount() {
    }

    componentWillReceiveProps(nextProps) {
    }

    getDestNode() {
        const fund = this.getActiveFund(this.props);
        return fund.fundTreeDaosRight.nodes[indexById(fund.fundTreeDaosRight.nodes, fund.fundTreeDaosRight.selectedId)];
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts ArrDaoPage", '[' + action + ']', this);
        super.handleShortcuts(action);
    }

    handleCreateUnderAndLink = () => {
        const fund = this.getActiveFund(this.props);
        const node = this.getDestNode();
        const {selectedDaoLeft} = this.state;

        let parentNode = getParentNode(node, fund.fundTreeDaosRight.nodes);
        if (parentNode == null) {   // root
            parentNode = createFundRoot(fund);
        }

        const afterCreateCallback = (versionId, node, parentNode) => {
            // Připojení - link
            WebApi.createDaoLink(fund.versionId, selectedDaoLeft.id, node.id).then(()=>{
                this.setState({selectedDaoLeft: null});
            });

            // Výběr node ve stromu
            this.props.dispatch(fundTreeSelectNode(types.FUND_TREE_AREA_DAOS_RIGHT, fund.versionId, node.id, false, false, null, true));
        };

        this.props.dispatch(addNodeForm(
            "CHILD",
            node,
            parentNode,
            fund.versionId,
            afterCreateCallback,
            ["CHILD"]
        ));
        console.log("handleCreateUnder");
    };

    handleLink = () => {
        const fund = this.getActiveFund(this.props);
        const {selectedDaoLeft} = this.state;

        WebApi.createDaoLink(fund.versionId, selectedDaoLeft.id, fund.fundTreeDaosRight.selectedId).then(()=>{
            this.setState({selectedDaoLeft: null});
        });
    };

    handleTabSelect = (item) => {
        this.setState({
            selectedTab: item.id,
            selectedDaoLeft: null,
        });
    };

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon = (readMode, closed) => {
        const activeFund = this.getActiveFund(this.props);

        let altActions = [];

        let itemActions = [];

        let altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt" className="small">{altActions}</RibbonGroup>
        }

        let itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item" className="small">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon arr subMenu fundId={activeFund ? activeFund.id : null} altSection={altSection} itemSection={itemSection}/>
        )
    };

    hasPageShowRights = (userDetail, activeFund) => {
        return userDetail.hasArrPage(activeFund ? activeFund.id : null);
    };

    handleSelectPackage = (pkg, unassigned, selectedIndex) => {
        const fund = this.getActiveFund(this.props);

        if (unassigned) {
            this.setState({selectedUnassignedPackage: pkg, selectedIndex});
        } else {
            this.setState({selectedPackage: pkg, selectedIndex});
        }
    };

    _renderPackages = (unassigned, selectedPackage, readMode) => {
        const {selectedIndex} = this.state;
        const fund = this.getActiveFund(this.props);

        return <div className="packages-container" key={"daoPackages-" + unassigned}>
            <ArrDaoPackages
                activeIndex={selectedIndex}
                unassigned={unassigned}
                onSelect={(item, index) => this.handleSelectPackage(item, unassigned, index)}
            />
            {/*selectedPackage && */<ArrDaos
                type="PACKAGE"
                unassigned={unassigned}
                fund={fund}
                readMode={readMode}
                selectedDaoId={this.state.selectedDaoLeft ? this.state.selectedDaoLeft.id : null}
                daoPackageId={selectedPackage ? selectedPackage.id : null}
                onSelect={item => { this.setState({selectedDaoLeft: item}) }}
            />}
        </div>
    };

    renderUnassignedPackages = (readMode) => {
        const {selectedUnassignedPackage} = this.state;

        return this._renderPackages(true, selectedUnassignedPackage, readMode);
    };

    renderPackages = (readMode) => {
        const {selectedPackage} = this.state;

        return this._renderPackages(false, selectedPackage, readMode);
    };

    renderLeftTree = (readMode) => {
        const fund = this.getActiveFund(this.props);

        return <div className="tree-left-container">
            <FundTreeDaos
                fund={fund}
                versionId={fund.versionId}
                area={types.FUND_TREE_AREA_DAOS_LEFT}
                {...fund.fundTreeDaosLeft}
            />
            {/*fund.fundTreeDaosLeft.selectedId !== null &&*/ <ArrDaos
                type="NODE"
                unassigned={false}
                selectedDaoId={this.state.selectedDaoLeft ? this.state.selectedDaoLeft.id : null}
                fund={fund}
                readMode={readMode}
                nodeId={fund.fundTreeDaosLeft.selectedId ? fund.fundTreeDaosLeft.selectedId : null}
                onSelect={item => { this.setState({selectedDaoLeft: item}) }}
            />}
        </div>
    };

    renderCenterPanel = (readMode, closed) => {
        const {selectedDaoLeft, selectedTab} = this.state;
        const fund = this.getActiveFund(this.props);

        let rightHasSelection = fund.fundTreeDaosRight.selectedId != null;
        let active = rightHasSelection && !readMode && !fund.closed;

        let tabs = [];
        tabs.push({
            id: 0,
            key: 0,
            title: 'Nepřiřazené entity',
            /*desc: 'zbývá 300'*/
        });

        tabs.push({
            id: 1,
            key: 1,
            title: 'Balíčky'
        });

        tabs.push({
            id: 2,
            key: 2,
            title: 'Archivní strom'
        });

        let content;
        switch (selectedTab) {
            case 0:
                content = this.renderUnassignedPackages(readMode);
                break;
            case 1:
                content = this.renderPackages(readMode);
                break;
            case 2:
                content = this.renderLeftTree(readMode);
                break;
            default:
                break;
        }

        let canLink;
        if (selectedDaoLeft && fund.fundTreeDaosRight.selectedId !== null && !readMode) {
            canLink = true;
        }

        return (
            <div className="daos-content-container">
                <div key={1} className='left-container'>
                    <Tabs.Container ref='tabs' className='daos-tabs-container'>
                        <Tabs.Tabs items={tabs} activeItem={{id: selectedTab}} onSelect={this.handleTabSelect} />
                        <Tabs.Content>
                            {content}
                        </Tabs.Content>
                    </Tabs.Container>
                </div>
                <div key={2} className='actions-container'>
                    <Button onClick={this.handleLink} disabled={!canLink}><Icon glyph="fa-thumb-tack"/><div>{i18n('arr.daos.link')}</div></Button>
                    <Button onClick={this.handleCreateUnderAndLink} disabled={!canLink}><Icon glyph="ez-move-under"/><div>{i18n('arr.daos.createUnderAndLink')}</div></Button>
                </div>
                <div key={3} className={"right-container"}>
                    <div className="tree-right-container">
                        <FundTreeDaos
                            fund={fund}
                            versionId={fund.versionId}
                            area={types.FUND_TREE_AREA_DAOS_RIGHT}
                            {...fund.fundTreeDaosRight}
                        />
                        {/*fund.fundTreeDaosRight.selectedId !== null &&*/ <ArrDaos
                            type="NODE_ASSIGN"
                            unassigned={false}
                            fund={fund}
                            readMode={readMode}
                            nodeId={fund.fundTreeDaosRight.selectedId ? fund.fundTreeDaosRight.selectedId : null}
                        />}
                    </div>
                </div>
            </div>
        )
    };
};

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, form, focus, developer, userDetail, tab} = state
    return {
        splitter,
        arrRegion,
        focus,
        developer,
        userDetail,
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
        packetTypes: refTables.packetTypes,
        ruleSet: refTables.ruleSet,
        tab,
    }
}

export default connect(mapStateToProps)(ArrDaoPage);
