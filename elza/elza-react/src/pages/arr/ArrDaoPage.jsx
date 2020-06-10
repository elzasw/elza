import './ArrPage.scss';
import './ArrDaoPage.scss';
import PropTypes from 'prop-types';

import React from 'react';
import {indexById} from 'stores/app/utils.jsx';
import {connect} from 'react-redux';

import ArrDaoPackages from '../../components/arr/ArrDaoPackages';
import Ribbon from '../../components/page/Ribbon';
import FundTreeDaos from '../../components/arr/FundTreeDaos';
import ArrDaos from '../../components/arr/ArrDaos';

import {i18n, Icon, RibbonGroup, Tabs} from 'components/shared';
import * as types from 'actions/constants/ActionTypes.js';
import {createFundRoot, getParentNode} from 'components/arr/ArrUtils.jsx';
import {addNodeForm} from 'actions/arr/addNodeForm.jsx';
import ArrParentPage from './ArrParentPage.jsx';
import {fundTreeSelectNode} from 'actions/arr/fundTree.jsx';
import {Button} from '../../components/ui';
import {WebApi} from 'actions/index.jsx';

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
        calendarTypes: PropTypes.object.isRequired,
        descItemTypes: PropTypes.object.isRequired,
        focus: PropTypes.object.isRequired,
        userDetail: PropTypes.object.isRequired,
        ruleSet: PropTypes.object.isRequired,
    };

    componentDidMount() {}

    UNSAFE_componentWillReceiveProps(nextProps) {}

    getDestNode() {
        const fund = this.getActiveFund(this.props);
        return fund.fundTreeDaosRight.nodes[indexById(fund.fundTreeDaosRight.nodes, fund.fundTreeDaosRight.selectedId)];
    }

    handleShortcuts(action) {
        console.log('#handleShortcuts ArrDaoPage', '[' + action + ']', this);
        super.handleShortcuts(action);
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
                altSection={altSection}
                itemSection={itemSection}
            />
        );
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
                    /*fund.fundTreeDaosLeft.selectedId !== null &&*/ <ArrDaos
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
                }
            </div>
        );
    };

    renderCenterPanel = (readMode, closed) => {
        const {selectedDaoLeft, selectedTab} = this.state;
        const fund = this.getActiveFund(this.props);

        let rightHasSelection = fund.fundTreeDaosRight.selectedId != null;
        let active = rightHasSelection && !readMode && !fund.closed;

        let tabs = [];
        tabs.push({
            id: '' + 0,
            title: 'Nepřiřazené entity',
            /*desc: 'zbývá 300'*/
        });

        tabs.push({
            id: '' + 1,
            title: 'Balíčky',
        });

        tabs.push({
            id: '' + 2,
            title: 'Archivní strom',
        });

        let content;
        switch (selectedTab) {
            case '0':
                content = this.renderUnassignedPackages(readMode);
                break;
            case '1':
                content = this.renderPackages(readMode);
                break;
            case '2':
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
                <div key={1} className="left-container">
                    <Tabs.Container className="daos-tabs-container">
                        <Tabs.Tabs items={tabs} activeItem={{id: selectedTab}} onSelect={this.handleTabSelect} />
                        <Tabs.Content>{content}</Tabs.Content>
                    </Tabs.Container>
                </div>
                <div key={2} className="actions-container">
                    <Button onClick={this.handleLink} disabled={!canLink}>
                        <Icon glyph="fa-thumb-tack" />
                        <div>{i18n('arr.daos.link')}</div>
                    </Button>
                    <Button onClick={this.handleCreateUnderAndLink} disabled={!canLink}>
                        <Icon glyph="ez-move-under" />
                        <div>{i18n('arr.daos.createUnderAndLink')}</div>
                    </Button>
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
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
        ruleSet: refTables.ruleSet,
        tab,
    };
}

export default connect(mapStateToProps)(ArrDaoPage);
