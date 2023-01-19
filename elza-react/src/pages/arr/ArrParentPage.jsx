import './ArrPage.scss';
import './ArrParentPage.scss';
import PropTypes from 'prop-types';

import React from 'react';
import {i18n, Loading} from 'components/shared';
import {AbstractReactComponent, ArrFundPanel} from 'components/index.jsx';
import * as types from 'actions/constants/ActionTypes';
import {fundChangeReadMode, fundsFetchIfNeeded} from 'actions/arr/fund.jsx';
import {getOneSettings, setSettings, getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx';
import {setFocus} from 'actions/global/focus.jsx';
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx';
import {routerNavigate} from 'actions/router.jsx';
import {fundTreeFetchIfNeeded} from 'actions/arr/fundTree.jsx';
import {Shortcuts} from 'react-shortcuts';
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx';
import PageLayout from '../shared/layout/PageLayout';
import defaultKeymap from './ArrParentPageKeymap.jsx';
import {
    FOCUS_KEYS,
    URL_FUND,
    urlFundActions,
    urlFundGrid,
    urlFundMovements,
    urlFundOutputs,
    urlFundTree,
    getFundVersion,
} from '../../constants.tsx';
import * as groups from '../../actions/refTables/groups';
import {WebApi} from "../../actions";
import {selectFundTab} from "../../actions/arr/fund";

/**
 * Stránka předku archivních pomůcek, např. pro pořádání, přesuny atp. Společným znakem je vybraný aktivní archivní soubor.
 */

export default class ArrParentPage extends AbstractReactComponent {
    static defaultKeymap = defaultKeymap;

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

    constructor(props, layoutClassName) {
        super(props);

        this.bindMethods('buildRibbon', 'handleShortcuts');

        this.layoutClassName = layoutClassName;

        this.state = {};
    }

    handleShortcuts(action, e) {
        console.log('#handleShortcuts ArrParentPage', '[' + action + ']', this);
        e.preventDefault();
        let activeFund = this.getActiveFund(this.props);
        switch (action) {
            case 'back':
                this.props.dispatch(routerNavigate(URL_FUND));
                break;
            case 'arr':
                this.props.dispatch(routerNavigate(urlFundTree(activeFund.id, getFundVersion(activeFund))));
                this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 1));
                break;
            case 'movements':
                this.props.dispatch(routerNavigate(urlFundMovements(activeFund.id, getFundVersion(activeFund))));
                this.props.dispatch(setFocus(FOCUS_KEYS.NONE, 1));
                break;
            case 'dataGrid':
                this.props.dispatch(routerNavigate(urlFundGrid(activeFund.id, getFundVersion(activeFund))));
                this.props.dispatch(setFocus(FOCUS_KEYS.NONE, 1));
                break;
            case 'output':
                this.props.dispatch(routerNavigate(urlFundOutputs(activeFund.id, getFundVersion(activeFund))));
                this.props.dispatch(setFocus(FOCUS_KEYS.FUND_OUTPUT, 1));
                break;
            case 'actions':
                this.props.dispatch(routerNavigate(urlFundActions(activeFund.id, getFundVersion(activeFund))));
                this.props.dispatch(setFocus(FOCUS_KEYS.FUND_ACTION, 1));
                break;
            case 'TOGGLE_READ_MODE':
                this.toggleReadMode();
                break;
            default:
                break;
        }
    }

    getPageUrl(fund) {
        const {match} = this.props;
        if(match?.params?.id !== undefined){
            return urlFundTree(match.params.id, match.params.versionId);
        }
        throw "no fundId or versionId"
    }

    async componentDidMount() {
        const {dispatch, match} = this.props;
        const {id, versionId, nodeId} = match.params;
        dispatch(descItemTypesFetchIfNeeded());
        dispatch(fundsFetchIfNeeded());

        const urlFundId = id ? parseInt(id) : null;
        const urlVersionId = versionId ? parseInt(versionId) : null;
        const activeFund = this.getActiveFund(this.props);
        
        // skip loading data, if fund is currently open
        if(activeFund?.id === urlFundId && getFundVersion(activeFund) == urlVersionId){
            return;
        }

        if (urlFundId) {
            try{
                const data = await WebApi.getFundDetail(urlFundId)

                // select the current version, when it is missing in the path
                const version = urlVersionId ? data.versions.find((version) => version.id === urlVersionId) : data.versions[0];
                dispatch(selectFundTab(getFundFromFundAndVersion(data, version)));
                return;
            }
            catch(e) {
                console.error("Nepodařilo se získat detail o AS", e);
            };
        }
        if((!id && !nodeId) && activeFund) {
            dispatch(routerNavigate(urlFundTree(activeFund.id, getFundVersion(activeFund)),"REPLACE"));
        }
        // }
    }

    // Function to determine whether the fundId in the url is the id of the
    // currently opened(active) fund.
    // Expects the url formats '/fund/{id}' or '/fund/{id}/v/{versionId}'
    isCurrentFundActive = () => {
        const {match} = this.props;
        const {id, versionId} = match.params;
        const activeFund = this.getActiveFund(this.props);

        if(!activeFund){
            return false;
        }

        const urlFundId = id ? parseInt(id) : null;
        const urlVersionId = versionId ? parseInt(versionId) : null;

        if(activeFund.id === urlFundId && getFundVersion(activeFund) == urlVersionId){
            return true;
        }
        return false;
    }

    // Function to determine whether the nodeId in the url is the id of the
    // currently opened(active) node.
    // Expects the url format '/node/{id}'
    isCurrentNodeActive = () => {
        const {match} = this.props;
        const {nodeId} = match.params;
        const urlNodeId = nodeId != null ? parseInt(nodeId) : null;
        if(urlNodeId == null){
            return false;
        }

        const activeFund = this.getActiveFund(this.props);
        if(!activeFund?.nodes || activeFund.nodes.activeIndex == null){
            return false;
        }

        const activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
        if(!activeNode || activeNode.selectedSubNodeId == null){
            return false;
        }

        if(activeNode.selectedSubNodeId === urlNodeId){
            return true;
        }
        return false;
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const {dispatch, descItemTypes} = this.props;

        dispatch(descItemTypesFetchIfNeeded())
        dispatch(fundsFetchIfNeeded());
        var activeFund = this.getActiveFund(nextProps);

        if (activeFund !== null) {
            this.requestFundTreeData(activeFund);

            if(descItemTypes.fetched){
                dispatch(groups.fetchIfNeeded(activeFund.versionId));
            }
        }
    }

    requestFundTreeData(activeFund) {
        this.props.dispatch(
            fundTreeFetchIfNeeded(types.FUND_TREE_AREA_MAIN, activeFund.versionId, activeFund.fundTree.expandedIds),
        );
    }

    toggleReadMode() {
        const {userDetail} = this.props;
        var settings = userDetail.settings;
        var activeFund = this.getActiveFund(this.props);
        var item = {...getOneSettings(settings, 'FUND_READ_MODE', 'FUND', activeFund.id)};
        item.value = item.value === null || item.value === 'true' ? false : true;
        settings = setSettings(settings, item.id, item);
        this.props.dispatch(fundChangeReadMode(activeFund.versionId, item.value));
        this.props.dispatch(userDetailsSaveSettings(settings));
    }
    getActiveFund(props) {
        const arrRegion = props.arrRegion;
        return arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
    }

    /**
     * Sestavení Ribbonu. Pro překrytí.
     * @return {Object} view
     */
    buildRibbon(readMode, closed) {}

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
        console.error('Method hasPageShowRights must be overriden!');
    }

    render() {
        const {splitter, arrRegion, userDetail, ruleSet, rulDataTypes, descItemTypes} = this.props;

        var activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;

        var statusHeader;
        var leftPanel;
        var rightPanel;
        let readMode = false;
        let closed = false;

        if (this.hasPageShowRights(userDetail, activeFund)) {
            // má právo na tuto stránku
            var centerPanel;
            if (activeFund) {
                var settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', activeFund.id);
                var settingsValues = settings.value != 'false';
                readMode = settingsValues;
                closed = activeFund.lockDate != null;

                statusHeader = <ArrFundPanel />;

                centerPanel = this.renderCenterPanel(readMode, closed);
                leftPanel = this.renderLeftPanel(readMode, closed);
                rightPanel = this.renderRightPanel(readMode, closed);
            } else {
                centerPanel = <div className="fund-noselect">{i18n('arr.fund.noselect')}</div>;
            }
        } else {
            centerPanel = <div>{i18n('global.insufficient.right')}</div>;
        }

        if(!this.isCurrentFundActive() && !this.isCurrentNodeActive()){
            leftPanel = undefined;
            rightPanel = undefined;
            statusHeader = undefined;
            centerPanel = <Loading/>
        }

        return (
            <Shortcuts
                name="ArrParent"
                handler={this.handleShortcuts}
                global
                className="main-shortcuts2"
                stopPropagation={false}
                alwaysFireHandler
            >
                <PageLayout
                    splitter={splitter}
                    _className="fa-page"
                    className={this.layoutClassName ? 'arr-abstract-page ' + this.layoutClassName : 'arr-abstract-page'}
                    ribbon={this.buildRibbon(readMode, closed)}
                    centerPanel={centerPanel}
                    leftPanel={leftPanel}
                    rightPanel={rightPanel}
                    status={statusHeader}
                />
            </Shortcuts>
        );
    }
}
