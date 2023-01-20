import './ArrPage.scss';
import './ArrDaoPage.scss';
import PropTypes from 'prop-types';

import React from 'react';
import {indexById} from 'stores/app/utils';
import {connect} from 'react-redux';

import ArrDaoPackages from '../../components/arr/ArrDaoPackages';
import Ribbon from '../../components/page/Ribbon';
import FundTreeDaos from '../../components/arr/FundTreeDaos';
import ArrDaos from '../../components/arr/ArrDaos';

import {i18n, Icon, RibbonGroup, Tabs} from 'components/shared';
import * as types from 'actions/constants/ActionTypes';
import {createFundRoot, getParentNode} from 'components/arr/ArrUtils';
import {addNodeForm} from 'actions/arr/addNodeForm';
import ArrParentPage from './ArrParentPage';
import {fundTreeSelectNode} from 'actions/arr/fundTree';
import {Button} from '../../components/ui';
import {WebApi} from 'actions/index';
import {urlFundDaos, getFundVersion} from "../../constants";

/**
 * Stránka archivních pomůcek.
 */

class ArrDaoPage extends ArrParentPage {
    constructor(props) {
        super(props, 'dao-page');
    }

    state = {
        selectedTab: '0',
        selectedUnassignedPackage: null,
        selectedPackage: null,
        selectedDaoLeft: null, // vybrané dao v levé části
        selectedDaoLeftFileId: null, // vybrané dao v levé části
        selectedDaoRight: null, // vybrané dao v pravé části
        selectedDaoRightFileId: null, // vybrané dao v pravé části
    };

    static propTypes = {
        splitter: PropTypes.object.isRequired,
        arrRegion: PropTypes.object.isRequired,
        developer: PropTypes.object.isRequired,
        rulDataTypes: PropTypes.object.isRequired,
        descItemTypes: PropTypes.object.isRequired,
        focus: PropTypes.object.isRequired,
        userDetail: PropTypes.object.isRequired,
        ruleSet: PropTypes.object.isRequired,
    };

    componentDidMount() {
        this.resolveUrls();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {}

    getDestNode() {
        const fund = this.getActiveFund(this.props);
        return fund.fundTreeDaosRight.nodes[indexById(fund.fundTreeDaosRight.nodes, fund.fundTreeDaosRight.selectedId)];
    }

    handleShortcuts(action) {
        console.log('#handleShortcuts ArrDaoPage', '[' + action + ']', this);
        super.handleShortcuts(action);
    }

    getPageUrl(fund) {
        return urlFundDaos(fund.id, getFundVersion(fund));
    }

    handleCreateUnderAndLink = () => {
        const fund = this.getActiveFund(this.props);
        const node = this.getDestNode();
        const {selectedDaoLeft} = this.state;

        let parentNode = getParentNode(node, fund.fundTreeDaosRight.nodes);
        if (parentNode == null) {
            // root
            parentNode = createFundRoot(fund);
        }

        const afterCreateCallback = (versionId, node, parentNode) => {
            // Připojení - link
            WebApi.createDaoLink(fund.versionId, selectedDaoLeft.id, node.id).then(() => {
                this.setState({selectedDaoLeft: null});
            });

            // Výběr node ve stromu
            this.props.dispatch(
                fundTreeSelectNode(types.FUND_TREE_AREA_DAOS_RIGHT, fund.versionId, node.id, false, false, null, true),
            );
        };

        this.props.dispatch(addNodeForm('CHILD', node, parentNode, fund.versionId, afterCreateCallback, ['CHILD']));
        console.log('handleCreateUnder');
    };

    handleLink = () => {
        const fund = this.getActiveFund(this.props);
        const {selectedDaoLeft} = this.state;

        WebApi.createDaoLink(fund.versionId, selectedDaoLeft.id, fund.fundTreeDaosRight.selectedId).then(() => {
            this.setState({selectedDaoLeft: null});
        });
    };

    handleTabSelect = (item, ...other) => {
        console.log(item, other);
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
            altSection = (
                <RibbonGroup key="alt" className="small">
                    {altActions}
                </RibbonGroup>
            );
        }

        let itemSection;
        if (itemActions.length > 0) {
            itemSection = (
                <RibbonGroup key="item" className="small">
                    {itemActions}
                </RibbonGroup>
            );
        }

        return (
            <Ribbon
                arr
                subMenu
                fundId={activeFund ? activeFund.id : null}
                versionId={getFundVersion(activeFund)}
                altSection={altSection}
                itemSection={itemSection}
            />
        );
    };

    hasPageShowRights = (userDetail, activeFund) => {
        return userDetail.hasRdPage(activeFund ? activeFund.id : null);
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

        return (
            <div className="packages-container" key={'daoPackages-' + unassigned}>
                <ArrDaoPackages
                    activeIndex={selectedIndex}
                    unassigned={unassigned}
                    onSelect={(item, index) => this.handleSelectPackage(item, unassigned, index)}
                />
                {
                    /*selectedPackage && */ <ArrDaos
                        type="PACKAGE"
                        unassigned={unassigned}
                        fund={fund}
                        readMode={readMode}
                        selectedDaoId={this.state.selectedDaoLeft ? this.state.selectedDaoLeft.id : null}
                        selectedDaoFileId={this.state.selectedDaoLeftFileId ? this.state.selectedDaoLeftFileId : null}
                        daoPackageId={selectedPackage ? selectedPackage.id : null}
                        onSelect={(item, daoFileId) => {
                            this.setState({selectedDaoLeft: item, selectedDaoLeftFileId: daoFileId});
                        }}
                    />
                }
            </div>
        );
    };

    renderUnassignedPackages = readMode => {
        const {selectedUnassignedPackage} = this.state;

        return this._renderPackages(true, selectedUnassignedPackage, readMode);
    };

    renderPackages = readMode => {
        const {selectedPackage} = this.state;

        return this._renderPackages(false, selectedPackage, readMode);
    };

    renderLeftTree = readMode => {
        const fund = this.getActiveFund(this.props);

        return (
            <div className="tree-left-container">
                <FundTreeDaos
                    fund={fund}
                    versionId={fund.versionId}
                    area={types.FUND_TREE_AREA_DAOS_LEFT}
                    {...fund.fundTreeDaosLeft}
                />
                {
                    /*fund.fundTreeDaosLeft.selectedId !== null &&*/
                }
                <ArrDaos
                        type="NODE"
                        unassigned={false}
                        selectedDaoId={this.state.selectedDaoLeft ? this.state.selectedDaoLeft.id : null}
                        selectedDaoFileId={this.state.selectedDaoLeftFileId ? this.state.selectedDaoLeftFileId : null}
                        fund={fund}
                        readMode={readMode}
                        nodeId={fund.fundTreeDaosLeft.selectedId ? fund.fundTreeDaosLeft.selectedId : null}
                        onSelect={(item, daoFileId) => {
                            this.setState({selectedDaoLeft: item, selectedDaoLeftFileId: daoFileId});
                        }}
                    />
            </div>
        );
    };

    isDaoType = (type) => {
        const {selectedDaoLeft} = this.state;
        return selectedDaoLeft && selectedDaoLeft.daoType === type;
    }

    renderCenterButtons = (readMode) => {
        const {selectedDaoLeft} = this.state;
        const fund = this.getActiveFund(this.props);

        let canLink = selectedDaoLeft
            && fund.fundTreeDaosRight.selectedId !== null
            && !readMode;

        if(this.isDaoType("LEVEL")){
            return (
                <Button
                    onClick={this.handleLink}
                    disabled={!canLink}
                >
                    <Icon
                        glyph="ez-move-under"
                    />
                    <div>
                        {i18n('arr.daos.createUnderAndLink')}
                    </div>
                </Button>
            );
        } else {
            return [
                <Button
                    key="0"
                    onClick={this.handleLink}
                    disabled={!canLink}
                >
                    <Icon
                        glyph="fa-thumb-tack"
                    />
                    <div>
                        {i18n('arr.daos.link')}
                    </div>
                </Button>,
                <Button
                    key="1"
                    onClick={this.handleCreateUnderAndLink}
                    disabled={!canLink}
                >
                    <Icon
                        glyph="ez-move-under"
                    />
                    <div>
                        {i18n('arr.daos.createUnderAndLink')}
                    </div>
                </Button>
            ]
        }
    }

    renderSelectedTab = (readMode) => {
        const {selectedTab} = this.state;
        switch (selectedTab) {
            case '0':
                return this.renderUnassignedPackages(readMode);
            case '1':
                return this.renderPackages(readMode);
            case '2':
                return this.renderLeftTree(readMode);
            default:
                return <React.Fragment/>;
        }
    }

    renderCenterPanel = (readMode, closed) => {
        const {selectedTab} = this.state;
        const fund = this.getActiveFund(this.props);

        let rightHasSelection = fund.fundTreeDaosRight.selectedId != null;
        let active = rightHasSelection && !readMode && !fund.closed;

        let tabs = [{
            id: '0',
            title: 'Nepřiřazené entity',
        },{
            id: '1',
            title: 'Balíčky'
        },{
            id: '2',
            title: 'Archivní strom'
        }];

        return (
            <div className="daos-content-container">
                <div key={1} className="left-container">
                    <Tabs.Container className="daos-tabs-container">
                        <Tabs.Tabs items={tabs} activeItem={{id: selectedTab}} onSelect={this.handleTabSelect} />
                        <Tabs.Content>
                            {this.renderSelectedTab(readMode)}
                        </Tabs.Content>
                    </Tabs.Container>
                </div>
                <div key={2} className='actions-container'>
                    {this.renderCenterButtons(readMode)}
                </div>
                <div key={3} className={'right-container'}>
                    <div className="tree-right-container">
                        <FundTreeDaos
                            fund={fund}
                            versionId={fund.versionId}
                            area={types.FUND_TREE_AREA_DAOS_RIGHT}
                            {...fund.fundTreeDaosRight}
                        />
                        {
                            /*fund.fundTreeDaosRight.selectedId !== null &&*/ <ArrDaos
                                type="NODE_ASSIGN"
                                unassigned={false}
                                fund={fund}
                                selectedDaoId={this.state.selectedDaoRight ? this.state.selectedDaoRight.id : null}
                                selectedDaoFileId={
                                    this.state.selectedDaoRightFileId ? this.state.selectedDaoRightFileId : null
                                }
                                readMode={readMode}
                                onSelect={(item, daoFileId) => {
                                    this.setState({selectedDaoRight: item, selectedDaoRightFileId: daoFileId});
                                }}
                                nodeId={fund.fundTreeDaosRight.selectedId ? fund.fundTreeDaosRight.selectedId : null}
                            />
                        }
                    </div>
                </div>
            </div>
        );
    };
}

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, form, focus, developer, userDetail, tab} = state;
    return {
        splitter,
        arrRegion,
        focus,
        developer,
        userDetail,
        rulDataTypes: refTables.rulDataTypes,
        descItemTypes: refTables.descItemTypes,
        ruleSet: refTables.ruleSet,
        tab,
    };
}

export default connect(mapStateToProps)(ArrDaoPage);
