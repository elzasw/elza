import './ArrPage.scss';
import './ArrDaoPage.scss';
import PropTypes from 'prop-types';

import React from 'react';
import { indexById } from 'stores/app/utils';
import { connect } from 'react-redux';

import ArrDaoPackages from '../../components/arr/ArrDaoPackages';
import Ribbon from '../../components/page/Ribbon';
import FundTreeDaos from '../../components/arr/FundTreeDaos';
import { ArrDaos } from '../../components/arr/ArrDaos';

import { Icon, RibbonGroup, Tabs } from 'components/shared';
import { arrPageMessages } from './messages';
import * as types from 'actions/constants/ActionTypes';
import { createFundRoot, getParentNode } from 'components/arr/ArrUtils';
import { addNodeForm } from 'actions/arr/addNodeForm';
import ArrParentPage from './ArrParentPage';
import { fundTreeSelectNode } from 'actions/arr/fundTree';
import { Button } from '../../components/ui';
import { WebApi } from 'actions/index';
import { urlFundDaos, getFundVersion } from "../../constants";
import {
    DEFAULT_DAO_PAGE_URL_STATE,
    FILE_SYSTEM_DAO_TAB,
    buildDaoPageUrl,
    parseDaoPageUrl,
} from './daoPageUrl';
import { loadDaoPageState, saveDaoPageState } from './daoPageStorage';
import { FileSystemBrowser, extractRepoIdFromFullPath } from 'components/arr/daos';
import { Api } from "api";
import { defineMessages, injectIntl, FormattedMessage } from 'react-intl';

const messages = defineMessages({
    multipleLinksNotAllowed: {
        id: 'arrDaoPage.link.multipleLinksNotAllowed',
        defaultMessage: 'Položka je již připojena k jednotce popisu a repozitář neumožňuje více vazeb',
    },
});

/**
 * Stránka archivních pomůcek.
 */

const AREA = "DAO";

class ArrDaoPage extends ArrParentPage {
    area = AREA;

    constructor(props) {
        super(props, 'dao-page');
    }

    state = {
        selectedUnassignedPackage: null,
        selectedPackage: null,
        selectedDaoLeft: null, // vybrané dao v levé části
        selectedDaoLeftFileId: null, // vybrané dao v levé části
        selectedDaoRight: null, // vybrané dao v pravé části
        selectedDaoRightFileId: null, // vybrané dao v pravé části
        selectedFilePath: null, // vybraná položka souborového repozitáře
        selectedFileItem: null, // data vybrané položky souborového repozitáře
        selectedFileRepo: null, // souborový repozitář vybrané položky
        fsRefreshCounter: 0,
    };

    static propTypes = {
        arrRegion: PropTypes.object.isRequired,
        developer: PropTypes.object.isRequired,
        rulDataTypes: PropTypes.object.isRequired,
        descItemTypes: PropTypes.object.isRequired,
        focus: PropTypes.object.isRequired,
        userDetail: PropTypes.object.isRequired,
        ruleSet: PropTypes.object.isRequired,
    };

    componentDidMount() {
        super.componentDidMount()
        this.resolveUrls();
        this.restoreStoredUrlState();
    }

    UNSAFE_componentWillReceiveProps(nextProps) { }

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

    /** Page state that lives in the url: the open tab and, under it, the file system browser. */
    getUrlState() {
        const { match, location } = this.props;
        return parseDaoPageUrl(match.params, location.search);
    }

    navigateToUrlState = (nextState) => {
        const { history, location, match } = this.props;
        const { id, versionId } = match.params;
        saveDaoPageState(Number(id), nextState);
        const url = buildDaoPageUrl(Number(id), versionId == undefined ? undefined : Number(versionId), nextState);
        if (url !== location.pathname + location.search) {
            history.replace(url);
        }
    };

    /**
     * Open the tab and directory this fund was last left on. An address that says something
     * on its own — a link to a directory, a bookmark — wins over the stored state.
     */
    restoreStoredUrlState() {
        const { match, location } = this.props;
        const isUrlExplicit = match.params.tab != undefined || location.search !== '';
        if (isUrlExplicit) {
            return;
        }
        const stored = loadDaoPageState(Number(match.params.id));
        if (stored) {
            this.navigateToUrlState(stored);
        }
    }

    handleUrlStateChange = (changes) => {
        this.navigateToUrlState({ ...this.getUrlState(), ...changes });
    };

    handleFileSystemSelect = (item, fullPath, repo) => {
        this.setState({ selectedFilePath: fullPath, selectedFileItem: item, selectedFileRepo: repo });
    };

    handleCreateUnderAndLink = () => {
        const fund = this.getActiveFund(this.props);
        const node = this.getDestNode();
        const { selectedDaoLeft } = this.state;

        let parentNode = getParentNode(node, fund.fundTreeDaosRight.nodes);
        if (parentNode == null) {
            // root
            parentNode = createFundRoot(fund);
        }

        const afterCreateCallback = (versionId, node, parentNode) => {
            // Připojení - link
            const linkPromise = selectedDaoLeft.id < 0
                ? Api.funds.fundFsMoveDAOLink(fund.id, selectedDaoLeft.daoLink.id, node.id)
                : WebApi.createDaoLink(fund.versionId, selectedDaoLeft.id, node.id);

            linkPromise.then(() => {
                this.setState({ selectedDaoLeft: null });
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
        const { selectedDaoLeft } = this.state;
        const nodeId = fund.fundTreeDaosRight.selectedId;

        const linkPromise = selectedDaoLeft.id < 0
            ? Api.node.nodeFsRelink(nodeId, selectedDaoLeft.daoLink.id)
            : WebApi.createDaoLink(fund.versionId, selectedDaoLeft.id, nodeId);

        linkPromise.then(() => {
            this.setState({ selectedDaoLeft: null });
        });
    };

    handleTabSelect = (item) => {
        this.navigateToUrlState({ ...DEFAULT_DAO_PAGE_URL_STATE, tab: item.id });
        this.setState(({ fsRefreshCounter }) => ({
            selectedDaoLeft: null,
            fsRefreshCounter: item.id === FILE_SYSTEM_DAO_TAB
                ? fsRefreshCounter + 1
                : fsRefreshCounter,
        }));
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
            this.setState({ selectedUnassignedPackage: pkg, selectedIndex });
        } else {
            this.setState({ selectedPackage: pkg, selectedIndex });
        }
    };

    _renderPackages = (unassigned, selectedPackage, readMode) => {
        const { selectedIndex } = this.state;
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
                            this.setState({ selectedDaoLeft: item, selectedDaoLeftFileId: daoFileId });
                        }}
                    />
                }
            </div>
        );
    };

    renderUnassignedPackages = readMode => {
        const { selectedUnassignedPackage } = this.state;

        return this._renderPackages(true, selectedUnassignedPackage, readMode);
    };

    renderPackages = readMode => {
        const { selectedPackage } = this.state;

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
                        this.setState({ selectedDaoLeft: item, selectedDaoLeftFileId: daoFileId });
                    }}
                />
            </div>
        );
    };

    renderFileSystemTree = (readMode) => {
        const fund = this.getActiveFund(this.props);

        return (
            <div className="tree-left-container">
                <FileSystemBrowser
                    fundId={fund.id}
                    onSelect={this.handleFileSystemSelect}
                    refreshCounter={this.state.fsRefreshCounter}
                    state={this.getUrlState()}
                    onStateChange={this.handleUrlStateChange}
                />
            </div>
        )
    }

    isDaoType = (type) => {
        const { selectedDaoLeft } = this.state;
        return selectedDaoLeft && selectedDaoLeft.daoType === type;
    }

    handleFileLink = async () => {
        const { selectedFilePath } = this.state;
        const fund = this.getActiveFund(this.props);

        const [repoId, path] = extractRepoIdFromFullPath(selectedFilePath);
        await Api.funds.fundFsCreateDAOLink(fund.id, repoId, fund.fundTreeDaosRight.selectedId, path);
        this.setState(({ fsRefreshCounter }) => ({ fsRefreshCounter: fsRefreshCounter + 1 }));
    }

    renderCenterButtons = (readMode) => {
        const { selectedDaoLeft, selectedFilePath, selectedFileItem, selectedFileRepo } = this.state;
        const fund = this.getActiveFund(this.props);

        if (this.getUrlState().tab === FILE_SYSTEM_DAO_TAB) {
            // Repozitář bez povolených více vazeb odmítne druhé napojení téže položky,
            // proto se akce nabízí jen pro položku, která ještě není nikam připojena.
            const alreadyLinked = selectedFileItem != null
                && selectedFileItem.links != null
                && selectedFileItem.links.length > 0;
            const multipleLinksBlocked = alreadyLinked
                && !(selectedFileRepo && selectedFileRepo.multipleLinks);
            const canLinkFile = selectedFilePath
                && fund.fundTreeDaosRight.selectedId !== null
                && !readMode
                && !multipleLinksBlocked;
            return (
                <span
                    title={
                        multipleLinksBlocked
                            ? this.props.intl.formatMessage(messages.multipleLinksNotAllowed)
                            : undefined
                    }
                >
                    <Button
                        key="0"
                        onClick={this.handleFileLink}
                        disabled={!canLinkFile}
                    >
                        <Icon
                            glyph="fa-thumb-tack"
                        />
                        <div>
                            {<FormattedMessage {...arrPageMessages.daosLink} />}
                        </div>
                    </Button>
                </span>
            )
        }

        let canLink = selectedDaoLeft
            && fund.fundTreeDaosRight.selectedId !== null
            && !readMode;
        if (this.isDaoType("LEVEL")) {
            return (
                <Button
                    onClick={this.handleLink}
                    disabled={!canLink}
                >
                    <Icon
                        glyph="ez-move-under"
                    />
                    <div>
                        {<FormattedMessage {...arrPageMessages.daosCreateUnderAndLink} />}
                    </div>
                </Button>
            );
        }
        else {
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
                        {<FormattedMessage {...arrPageMessages.daosLink} />}
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
                        {<FormattedMessage {...arrPageMessages.daosCreateUnderAndLink} />}
                    </div>
                </Button>
            ]
        }
    }

    renderSelectedTab = (readMode) => {
        switch (this.getUrlState().tab) {
            case 'unassignedPackages':
                return this.renderUnassignedPackages(readMode);
            case 'packages':
                return this.renderPackages(readMode);
            case 'leftTree':
                return this.renderLeftTree(readMode);
            case 'fileSystemTree':
                return this.renderFileSystemTree(readMode);
            default:
                return <React.Fragment />;
        }
    }

    renderCenterPanel = (readMode, closed) => {
        const { tab: selectedTab } = this.getUrlState();
        const fund = this.getActiveFund(this.props);

        let rightHasSelection = fund.fundTreeDaosRight.selectedId != null;
        let active = rightHasSelection && !readMode && !fund.closed;

        let tabs = [{
            id: 'unassignedPackages',
            title: this.props.intl.formatMessage(arrPageMessages.daosTabUnassignedPackages),
        }, {
            id: 'packages',
            title: this.props.intl.formatMessage(arrPageMessages.daosTabPackages),
        }, {
            id: 'leftTree',
            title: this.props.intl.formatMessage(arrPageMessages.daosTabLeftTree),
        }, {
            id: 'fileSystemTree',
            title: this.props.intl.formatMessage(arrPageMessages.daosTabFileSystemTree),
        }];

        return (
            <div className="daos-content-container">
                <div key={1} className="left-container">
                    <Tabs.Container className="daos-tabs-container">
                        <Tabs.Tabs items={tabs} activeItem={{ id: selectedTab }} onSelect={this.handleTabSelect} />
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
                        {fund.fundTreeDaosRight.selectedId !== null && (
                            <ArrDaos
                                type="NODE_ASSIGN"
                                unassigned={false}
                                fund={fund}
                                selectedDaoId={this.state.selectedDaoRight ? this.state.selectedDaoRight.id : null}
                                selectedDaoFileId={
                                    this.state.selectedDaoRightFileId ? this.state.selectedDaoRightFileId : null
                                }
                                readMode={readMode}
                                onSelect={(item, daoFileId) => {
                                    this.setState({ selectedDaoRight: item, selectedDaoRightFileId: daoFileId });
                                }}
                                onLinkChange={() => this.setState(({ fsRefreshCounter }) => ({ fsRefreshCounter: fsRefreshCounter + 1 }))}
                                nodeId={fund.fundTreeDaosRight.selectedId ? fund.fundTreeDaosRight.selectedId : null}
                            />
                        )}
                    </div>
                </div>
            </div>
        );
    };
}

function mapStateToProps(state) {
    const { arrRegion, refTables, form, focus, developer, userDetail, tab } = state;
    return {
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

export default connect(mapStateToProps)(injectIntl(ArrDaoPage));
