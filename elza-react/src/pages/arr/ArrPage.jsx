/**
 * Stránka archivních pomůcek.
 */

import {setVisiblePolicyReceive} from '../../actions/arr/visiblePolicy';

import './ArrPage.scss';

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx';
import {connect} from 'react-redux';
import {i18n, Icon, LazyListBox, ListBox2, Loading, RibbonGroup, Tabs, Utils} from 'components/shared';

import ArrParentPage from './ArrParentPage.jsx';

import {FundFiles, FundSettingsForm, FundTreeMain, NodeTabs, Ribbon} from '../../components/index.jsx';
import {Dropdown, DropdownButton} from 'react-bootstrap';
import {Button} from '../../components/ui';
import {WebApi} from 'actions/index.jsx';
import {modalDialogHide, modalDialogShow} from '../../actions/global/modalDialog.jsx';
import {fundExtendedView} from 'actions/arr/fundExtended.jsx';
import {versionValidationErrorNext, versionValidationErrorPrevious} from 'actions/arr/versionValidation.jsx';
import {developerNodeScenariosRequest} from 'actions/global/developer.jsx';
import {createFundRoot, getOneSettings, isFundRootId, setSettings} from 'components/arr/ArrUtils.jsx';
import {canSetFocus, focusWasSet, isFocusFor, setFocus} from 'actions/global/focus.jsx';
import {fundNodesPolicyFetchIfNeeded} from 'actions/arr/fundNodesPolicy.jsx';
import {fundActionFormChange, fundActionFormShow} from 'actions/arr/fundAction.jsx';
import {fundSelectSubNode, fundSelectSubNodeByNodeId} from 'actions/arr/node.jsx';
import {fundNodeInfoInvalidate} from 'actions/arr/nodeInfo.jsx';
import ArrHistoryForm from 'components/arr/ArrHistoryForm.jsx';
import {routerNavigate} from 'actions/router.jsx';
import * as perms from '../../actions/user/Permission.jsx';
import {selectTab} from 'actions/global/tab.jsx';
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx';
import {PropTypes} from 'prop-types';
import defaultKeymap from './ArrPageKeymap.jsx';
import NodeSettingsForm from '../../components/arr/NodeSettingsForm';
import {FOCUS_KEYS} from '../../constants.tsx';
import ArrStructurePanel from '../../components/arr/ArrStructurePanel';
import {structureTypesFetchIfNeeded} from '../../actions/refTables/structureTypes';
import objectById from '../../shared/utils/objectById';
import FundTemplateSettingsForm from '../../components/arr/FundTemplateSettingsForm';
import SearchFundsForm from '../../components/arr/SearchFundsForm';
import HorizontalSplitter from '../../components/shared/splitter/HorizontalSplitter';
import LecturingTop from '../../components/arr/LecturingTop';
import LecturingBottom from '../../components/arr/LecturingBottom';
import storeFromArea from '../../shared/utils/storeFromArea';
import * as issuesActions from '../../actions/arr/issues';
import {nodeWithIssueByFundVersion} from '../../actions/arr/issues';
import IssueForm from '../../components/form/IssueForm';
import ConfirmForm from '../../components/shared/form/ConfirmForm';
import ArrPageRibbon from './ArrPageRibbon';
import ArrRefTemplates from '../../components/arr/ArrRefTemplates';
import {MODAL_DIALOG_SIZE} from '../../constants';

class ArrPage extends ArrParentPage {
    static TAB_KEY = 'arr-as';

    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

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

    state = {
        developerExpandedSpecsIds: {},
        fundNodesError: null,
        tabs: null,
    };

    /** Object pro dynamické ref */
    refObjects = {};

    /** ref Fund errors */
    refFundErrors = null;

    /** ref Tree */
    refTree = null;

    constructor(props) {
        super(props, 'fa-page');
        this.bindMethods(
            'getActiveInfo',
            'buildRibbon',
            'handleSelectVisiblePoliciesNode',
            'handleShowVisiblePolicies',
            'handleShortcuts',
            'renderFundErrors',
            'renderFundVisiblePolicies',
            'handleSetVisiblePolicy',
            'renderPanel',
            'renderDeveloperDescItems',
            'handleShowHideSpecs',
            'handleTabSelect',
            'handleSelectErrorNode',
            'trySetFocus',
            'handleChangeFundSettingsSubmit',
            'handleSetExtendedView',
            'renderLecturingPanel',
        );
    }

    componentDidMount() {
        super.componentDidMount();
        this.trySetFocus(this.props);
    }

    UNSAFE_componentWillMount() {
        this.registerTabs(this.props);
        let newKeymap = Utils.mergeKeymaps(ArrParentPage.defaultKeymap, defaultKeymap);
        Utils.addShortcutManager(this, newKeymap);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        super.UNSAFE_componentWillReceiveProps(nextProps);
        const {selectedTabKey} = this.props;
        const activeFund = this.getActiveFund(nextProps);
        if (activeFund !== null) {
            this.props.dispatch(structureTypesFetchIfNeeded(activeFund.versionId));
            if (selectedTabKey === 'visiblePolicies') {
                this.props.dispatch(fundNodesPolicyFetchIfNeeded(activeFund.versionId));
            }
            if (nextProps.developer.enabled) {
                let node;
                if (activeFund.nodes && activeFund.nodes.activeIndex !== null) {
                    node = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
                }
                if (!node) {
                    return;
                }
                if (
                    node.selectedSubNodeId !== null &&
                    node.subNodeForm.data !== null &&
                    !node.developerScenarios.isFetching &&
                    node.developerScenarios.isDirty
                ) {
                    this.props.dispatch(
                        developerNodeScenariosRequest(
                            {
                                id: node.selectedSubNodeId,
                                key: node.routingKey,
                                version: node.subNodeForm.data.parent.version,
                            },
                            activeFund.versionId,
                        ),
                    );
                }
            }

            if (this.state.fundNodesError !== activeFund.fundNodesError) {
                this.setState({fundNodesError: activeFund.fundNodesError});
                if (this.refFundErrors) {
                    this.refFundErrors.fetchNow();
                }
            }
            if (activeFund.nodes.activeIndex === null && activeFund.fundTree.nodes[0]) {
                const node = activeFund.fundTree.nodes[0];
                const parentNode = createFundRoot(activeFund);
                this.props.dispatch(fundSelectSubNode(activeFund.versionId, node.id, parentNode, false, null, true));
            }
        } else {
            this.setState({fundNodesError: null});
        }

        if (nextProps.issueDetail.id) {
            nextProps.dispatch(issuesActions.detail.fetchIfNeeded(nextProps.issueDetail.id));
        }
        if (!this.props.issueDetail.fetched && nextProps.issueDetail.fetched) {
            if (nextProps.issueDetail.data.nodeId) {
                this.props.dispatch(
                    fundSelectSubNodeByNodeId(
                        activeFund.versionId,
                        nextProps.issueDetail.data.nodeId,
                        false,
                        null,
                        true,
                    ),
                );
            }
        }

        this.registerTabs(nextProps);
        this.trySetFocus(nextProps);
    }

    wrappedFocus = ref => {
        if (this.refObjects[ref]) {
            this.setState({}, () => {
                if (this.refObjects[ref].getWrappedInstance().focus()) {
                    focusWasSet();
                }
            });
        }
    };
    refFocus = ref => {
        if (this.refObjects[ref]) {
            this.setState({}, () => {
                ReactDOM.findDOMNode(this.refObjects[ref]).focus();
                focusWasSet();
            });
        }
    };

    trySetFocus(props) {
        const {focus, selectedTabKey} = props;
        if (this.state.tabs !== null && canSetFocus()) {
            if (isFocusFor(focus, FOCUS_KEYS.ARR, 3)) {
                let selectedTab = this.state.tabs[selectedTabKey];
                if (!selectedTab.focus && !selectedTab.ref) {
                    //Pokud tab nemá zadánu funkci pro focus ani ref
                    focusWasSet();
                } else if (!selectedTab.focus) {
                    //Pokud tab nemá zadánu funkci pro focus
                    this.refFocus(selectedTab.ref);
                } else {
                    selectedTab.focus();
                }
            }
        }
    }

    handleShortcuts(action, e) {
        console.log('#handleShortcuts ArrPage', '[' + action + ']', this);
        e.preventDefault();
        switch (action) {
            case 'area1':
                this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 1));
                break;
            case 'area2':
                this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 2));
                break;
            case 'area3':
                this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 3));
                break;
            default:
                super.handleShortcuts(action, e);
        }
    }

    /**
     * Načtení informačního objektu o aktuálním zobrazení sekce archvní pomůcky.
     * @return {Object} informace o aktuálním zobrazení sekce archvní pomůcky
     */
    getActiveInfo(arrRegion = this.props.arrRegion) {
        let activeFund = null;
        let activeNode = null;
        let activeSubNode = null;
        if (arrRegion.activeIndex != null) {
            activeFund = arrRegion.funds[arrRegion.activeIndex];
            if (activeFund.nodes.activeIndex != null) {
                activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
                if (activeNode.selectedSubNodeId != null) {
                    const index = indexById(activeNode.childNodes, activeNode.selectedSubNodeId);
                    if (index !== null) {
                        activeSubNode = activeNode.childNodes[index];
                    }
                }
            }
        }
        return {
            activeFund,
            activeNode,
            activeSubNode,
        };
    }

    handleOpenFundActionForm = () => {
        const activeInfo = this.getActiveInfo();
        const versionId = activeInfo.activeFund.versionId;
        const subNode = activeInfo.activeSubNode;

        this.props.dispatch(fundActionFormChange(versionId, {nodes: [subNode]}));
        this.props.dispatch(fundActionFormShow(versionId));
        this.props.dispatch(routerNavigate('/arr/actions'));
    };

    /**
     *   getTabs
     *   Vrátí objekt obsahující identifikátor vybrané záložky a objekt obsahující objekty záložek.
     *
     *   @param tabs {Object} - Objekt obsahující vlastnosti záložek
     *   @param settingsValues {Object} - objekt s nastavením
     *   @param selectedTab {String} - id/key záložky
     *   @param ignoreSettings {Boolean} - určuje, zda se bere v potaz nastavení. Při použití false vypíše všechny možné hodnoty splňující podmínku záložky.(určeno pro změnu nastavení)
     */
    getTabs = (tabs, settingsValues, selectedTab = null, ignoreSettings = false) => {
        const items = [];
        for (let tab in tabs) {
            const tabCondition = typeof tabs[tab].condition === 'undefined' || tabs[tab].condition; //Pokud podmínka není definována nebo je splněna
            const tabSettings = ignoreSettings || settingsValues === null || settingsValues[tabs[tab].id]; //Pokud se má nastavení ignorovat, neexistuje nebo je záložka zapnuta
            const showTabCondition =
                typeof tabs[tab].showCondition === 'undefined' || tabs[tab].showCondition || ignoreSettings; //Pokud podmínka pro zobrazení není definována, je splněna, nebo se má ignorovat nastavení

            if (tabSettings && tabCondition) {
                if (showTabCondition) {
                    tabs[tab].checked =
                        settingsValues && settingsValues[tab] !== undefined ? settingsValues[tab] : true;
                    items.push(tabs[tab]);
                    selectedTab = this.selectIfNull(selectedTab, tabs[tab].id);
                }
            }
        }
        return {items: items, selectedTab: selectedTab};
    };

    handleChangeFundSettings = () => {
        const {userDetail} = this.props;
        const activeInfo = this.getActiveInfo();

        let fundId = activeInfo.activeFund.id;

        let settings = getOneSettings(userDetail.settings, 'FUND_RIGHT_PANEL', 'FUND', fundId);
        const dataRight = settings.value ? JSON.parse(settings.value) : null;

        settings = getOneSettings(userDetail.settings, 'FUND_CENTER_PANEL', 'FUND', fundId);
        const dataCenter = settings.value ? JSON.parse(settings.value) : null;

        settings = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', fundId);
        const dataStrictMode = settings.value ? settings.value === 'true' : null;

        const tabsArray = this.getTabs(this.state.tabs, dataRight, null, true).items;

        const init = {
            rightPanel: {
                tabs: tabsArray,
            },
            centerPanel: {
                panels: [
                    {
                        name: i18n('arr.fund.settings.panel.center.parents'),
                        key: 'parents',
                        checked: dataCenter && dataCenter.parents,
                    },
                    {
                        name: i18n('arr.fund.settings.panel.center.children'),
                        key: 'children',
                        checked: dataCenter && dataCenter.children,
                    },
                    {
                        name: i18n('arr.fund.settings.panel.rightPanel'),
                        key: 'rightPanel',
                        checked: dataCenter && dataCenter.rightPanel !== undefined ? dataCenter.rightPanel : true,
                    },
                    {
                        name: i18n('arr.fund.settings.panel.treeColorCoding'),
                        key: 'treeColorCoding',
                        checked:
                            dataCenter && dataCenter.treeColorCoding !== undefined ? dataCenter.treeColorCoding : true,
                    },
                    {
                        name: i18n('arr.fund.settings.panel.acordeon'),
                        key: 'acordeon',
                        checked: dataCenter && dataCenter.acordeon !== undefined ? dataCenter.acordeon : false,
                    },
                ],
            },
            strictMode: {
                name: i18n('arr.fund.settings.rules.strictMode'),
                key: 'strictMode',
                value: dataStrictMode,
            },
        };

        const form = <FundSettingsForm initialValues={init} onSubmitForm={this.handleChangeFundSettingsSubmit} />;
        this.props.dispatch(modalDialogShow(this, i18n('arr.fund.settings.title'), form));
    };

    handleChangeFundTemplateSettings = () => {
        const {userDetail} = this.props;
        const activeInfo = this.getActiveInfo();

        let fundId = activeInfo.activeFund.id;
        let settings = userDetail.settings;
        const fundTemplates = getOneSettings(settings, 'FUND_TEMPLATES', 'FUND', fundId);
        const data = fundTemplates.value ? JSON.parse(fundTemplates.value) : [];
        const init = {
            templates: data,
        };
        const form = (
            <FundTemplateSettingsForm
                initialValues={init}
                onSubmitForm={formData => {
                    formData.templates.sort((a, b) => {
                        return a.name.localeCompare(b.name);
                    });
                    fundTemplates.value = JSON.stringify(formData.templates);
                    settings = setSettings(settings, fundTemplates.id, fundTemplates);
                    return this.props.dispatch(userDetailsSaveSettings(settings)).then(() => {
                        this.props.dispatch(modalDialogHide());
                    });
                }}
            />
        );
        this.props.dispatch(modalDialogShow(this, i18n('arr.fund.template.title'), form));
    };

    handleChangeFundSettingsSubmit(data) {
        const {userDetail} = this.props;
        const activeInfo = this.getActiveInfo();
        const fundVersionId = activeInfo.activeFund.versionId;
        const node = activeInfo.activeNode;
        let fundId = activeInfo.activeFund.id;

        let settings = userDetail.settings;

        // right panel settings
        const rightPanelItem = getOneSettings(settings, 'FUND_RIGHT_PANEL', 'FUND', fundId);
        let value = {};
        data.rightPanel.tabs.forEach(item => {
            value[item.key] = item.checked;
        });
        rightPanelItem.value = JSON.stringify(value);
        settings = setSettings(settings, rightPanelItem.id, rightPanelItem);

        // center panel settings
        const centerPanelItem = getOneSettings(settings, 'FUND_CENTER_PANEL', 'FUND', fundId);
        value = {};
        data.centerPanel.panels.forEach(item => {
            value[item.key] = item.checked;
        });
        centerPanelItem.value = JSON.stringify(value);
        settings = setSettings(settings, centerPanelItem.id, centerPanelItem);

        // strict mode settings
        let strictMode = getOneSettings(settings, 'FUND_STRICT_MODE', 'FUND', fundId);
        strictMode.value = data.strictMode.value === '' ? null : data.strictMode.value;
        settings = setSettings(settings, strictMode.id, strictMode);

        return this.props.dispatch(userDetailsSaveSettings(settings)).then(response => {
            // invalidates node info after the settings have been saved,
            // to load new node info data (mainly for reloading node parents).
            this.props.dispatch(fundNodeInfoInvalidate(fundVersionId, node.id, node.routingKey));
        });
    }

    handleChangeSyncTemplateSettings = fundId => {
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.refTemplates.title'),
                <ArrRefTemplates fundId={fundId} />,
                MODAL_DIALOG_SIZE.XL,
            ),
        );
    };

    /**
     * Zobrazení formuláře historie.
     * @param versionId verze AS
     * @param locked
     */
    handleShowFundHistory = (versionId, locked) => {
        const form = (
            <ArrHistoryForm versionId={versionId} locked={locked} onDeleteChanges={this.handleDeleteChanges} />
        );
        this.props.dispatch(modalDialogShow(this, i18n('arr.history.title'), form, 'dialog-lg'));
    };

    handleDeleteChanges = (nodeId, fromChangeId, toChangeId) => {
        const activeFund = this.getActiveFund(this.props);
        const versionId = activeFund.versionId;
        WebApi.revertChanges(versionId, nodeId, fromChangeId, toChangeId).then(() => {
            this.props.dispatch(modalDialogHide());
        });
    };

    /**
     * Vyvolání dialogu s vyhledáním na všemi AS.
     */
    handleFundsSearchForm = () => {
        this.props.dispatch(modalDialogShow(this, i18n('arr.fund.title.search'), <SearchFundsForm />));
    };

    createIssueFund = () => {
        const {issueProtocol, dispatch} = this.props;

        dispatch(
            modalDialogShow(
                this,
                i18n('arr.issues.add.arr.title'),
                <IssueForm
                    onSubmit={data =>
                        WebApi.addIssue({
                            ...data,
                            issueListId: issueProtocol.id,
                        })
                    }
                    onSubmitSuccess={data => {
                        dispatch(issuesActions.list.invalidate(data.issueListId));
                        dispatch(issuesActions.detail.invalidate(data.id));
                        dispatch(modalDialogHide());
                    }}
                />,
            ),
        );
    };

    createIssueNode = () => {
        const {issueProtocol, dispatch} = this.props;
        const activeFund = this.getActiveFund(this.props);

        let node;
        if (activeFund.nodes && activeFund.nodes.activeIndex !== null) {
            node = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
        }

        dispatch(
            modalDialogShow(
                this,
                i18n('arr.issues.add.node.title'),
                <IssueForm
                    onSubmit={data =>
                        WebApi.addIssue({
                            ...data,
                            issueListId: issueProtocol.id,
                            nodeId: node.selectedSubNodeId,
                        })
                    }
                    onSubmitSuccess={data => {
                        dispatch(issuesActions.list.invalidate(data.issueListId));
                        dispatch(issuesActions.detail.invalidate(data.id));
                        dispatch(modalDialogHide());
                    }}
                />,
            ),
        );
    };

    handleIssuePrevious = () => {
        this.handleIssue(-1);
    };

    handleIssueNext = () => {
        this.handleIssue(1);
    };

    handleIssue = direction => {
        const {dispatch, arrRegion} = this.props;
        const indexFund = arrRegion.activeIndex;
        if (indexFund !== null) {
            const activeFund = arrRegion.funds[indexFund];
            const nodeIndex = activeFund.nodes.activeIndex;
            if (nodeIndex !== null) {
                const activeNode = activeFund.nodes.nodes[nodeIndex];
                dispatch(nodeWithIssueByFundVersion(activeFund, activeNode.selectedSubNodeId, direction));
            }
        }
    };

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon(readMode, closed) {
        const {arrRegion, userDetail, issueProtocol} = this.props;
        const activeInfo = this.getActiveInfo();

        return (
            <ArrPageRibbon
                activeFundId={activeInfo.activeFund ? activeInfo.activeFund.id : null}
                activeFundVersionId={activeInfo.activeFund ? activeInfo.activeFund.versionId : null}
                selectedSubNodeId={activeInfo.activeNode ? activeInfo.activeNode.selectedSubNodeId : null}
                userDetail={userDetail}
                handleChangeFundSettings={this.handleChangeFundSettings}
                handleChangeFundTemplateSettings={this.handleChangeFundTemplateSettings}
                handleChangeSyncTemplateSettings={this.handleChangeSyncTemplateSettings}
                handleErrorPrevious={this.handleErrorPrevious}
                handleErrorNext={this.handleErrorNext}
                handleOpenFundActionForm={this.handleOpenFundActionForm}
                issueProtocol={issueProtocol}
                arrRegionActiveIndex={arrRegion.activeIndex}
            />
        );
    }

    handleErrorNext = () => {
        const activeInfo = this.getActiveInfo();
        this.props.dispatch(
            versionValidationErrorNext(activeInfo.activeFund.versionId, activeInfo.activeNode.selectedSubNodeId),
        );
    };

    handleErrorPrevious = () => {
        const activeInfo = this.getActiveInfo();
        this.props.dispatch(
            versionValidationErrorPrevious(activeInfo.activeFund.versionId, activeInfo.activeNode.selectedSubNodeId),
        );
    };

    handleShowHideSpecs(descItemTypeId) {
        if (this.state.developerExpandedSpecsIds[descItemTypeId]) {
            delete this.state.developerExpandedSpecsIds[descItemTypeId];
            this.setState({developerExpandedSpecsIds: this.state.developerExpandedSpecsIds});
        } else {
            this.state.developerExpandedSpecsIds[descItemTypeId] = true;
            this.setState({developerExpandedSpecsIds: this.state.developerExpandedSpecsIds});
        }
    }

    renderFundErrorItem(item) {
        if (item) {
            return <div>{item.name}</div>;
        } else {
            return '...';
        }
    }

    renderFundErrors(activeFund) {
        let activeNode = null;
        if (activeFund.nodes.activeIndex !== null) {
            activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
        }

        return (
            <div className="errors-listbox-container">
                <LazyListBox
                    ref={ref => (this.refFundErrors = ref)}
                    getItems={(fromIndex, toIndex) => {
                        return WebApi.getValidationItems(activeFund.versionId, fromIndex, toIndex);
                    }}
                    renderItemContent={this.renderFundErrorItem}
                    selectedItem={activeNode ? activeNode.selectedSubNodeId : null}
                    itemHeight={25} // nutne dat stejne cislo i do css jako .pokusny-listbox-container .listbox-item { height: 24px; }
                    onSelect={this.handleSelectErrorNode.bind(this, activeFund)}
                />
            </div>
        );
    }

    handleSelectErrorNode(activeFund, node) {
        if (node.parentNode === null) {
            node.parentNode = createFundRoot(activeFund);
        }
        this.props.dispatch(fundSelectSubNode(activeFund.versionId, node.id, node.parentNode));
    }

    handleSelectVisiblePoliciesNode(activeFund, node) {
        if (node.parentNode == null) {
            node.parentNode = createFundRoot(activeFund);
        }
        this.props.dispatch(fundSelectSubNode(activeFund.versionId, node.id, node.parentNode));
    }

    handleShowVisiblePolicies(activeFund) {
        let node;
        if (activeFund.nodes && activeFund.nodes.activeIndex !== null) {
            node = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
        }
        if (!node) {
            return;
        }
        const form = (
            <NodeSettingsForm
                nodeId={node.selectedSubNodeId}
                fundVersionId={activeFund.versionId}
                onSubmitForm={this.handleSetVisiblePolicy}
                onSubmitSuccess={() => this.props.dispatch(modalDialogHide())}
            />
        );
        this.props.dispatch(modalDialogShow(this, i18n('visiblePolicy.form.title'), form));
    }

    handleSetVisiblePolicy(data) {
        const {node, versionId, dispatch} = this.props;
        const mapIds = {};
        const {records, rules, nodeExtensions} = data;
        if (rules !== NodeSettingsForm.VIEW_POLICY_STATE.PARENT) {
            records.forEach((val, index) => {
                mapIds[parseInt(val.id)] = val.checked;
            });
        }

        const nodeExtensionsIds = Object.values(nodeExtensions)
            .filter(i => i.checked)
            .map(i => i.id);

        return WebApi.setVisiblePolicy(node.selectedSubNodeId, versionId, mapIds, false, nodeExtensionsIds).then(() => {
            dispatch(setVisiblePolicyReceive(node.selectedSubNodeId, versionId));
        });
    }

    renderPolicyItem(node) {
        return <div>{node.name}</div>;
    }

    renderFundVisiblePolicies(activeFund) {
        const nodesPolicy = activeFund.fundNodesPolicy;

        if (!nodesPolicy.fetched) {
            return <Loading />;
        }
        let activeNode = null;
        if (activeFund.nodes.activeIndex != null) {
            activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
        }

        if (nodesPolicy.items.length === 0) {
            return <div>{i18n('global.data.noitem')}</div>;
        }

        return (
            <div className="visiblePolicies-container">
                <ListBox2
                    className="visiblePolicies-listbox"
                    ref="fundVisiblePolicies"
                    items={nodesPolicy.items}
                    selectedItem={activeNode !== null ? activeNode.selectedSubNodeId : null}
                    renderItemContent={this.renderPolicyItem}
                    onSelect={this.handleSelectVisiblePoliciesNode.bind(this, activeFund)}
                    /*onDoubleClick={this.handleShowVisiblePolicies.bind(this, activeFund)}*/
                />
            </div>
        );
    }

    renderDeveloperDescItems(activeFund, node) {
        const rows = [];
        if (!node) {
            return <div className="developer-panel">Je potřeba vybrat jednotku popisu.</div>;
        } else if (node.subNodeForm.fetched) {
            node.subNodeForm.infoGroups.forEach(group => {
                const types = [];
                group.types.forEach(type => {
                    const infoType = node.subNodeForm.infoTypesMap[type.id];
                    const refType = node.subNodeForm.refTypesMap[type.id];

                    let specs;
                    if (refType.useSpecification && this.state.developerExpandedSpecsIds[refType.id]) {
                        if (refType.descItemSpecs.length > 0) {
                            specs = refType.descItemSpecs.map(spec => {
                                const infoSpec = infoType.specs[indexById(infoType.specs, spec.id)];

                                return (
                                    <div key={'spec' + spec.id} className={'desc-item-spec ' + infoSpec.type}>
                                        <h3 title={spec.name}>
                                            {spec.shortcut} <small>[{spec.code}]</small>
                                        </h3>
                                        <div key="1" className="desc">
                                            {spec.description}
                                        </div>
                                        <div key="2" className="attrs">
                                            <div key="1">
                                                <label>type:</label>
                                                {infoSpec.type}
                                            </div>
                                            <div key="2">
                                                <label>repeatable:</label>
                                                {spec.repeatable ? i18n('global.title.yes') : i18n('global.title.no')}
                                            </div>
                                            <div key="3">
                                                <label>viewOrder:</label>
                                                {spec.viewOrder}
                                            </div>
                                        </div>
                                    </div>
                                );
                            });
                        } else {
                            specs = i18n('developer.descItems.specs.empty');
                        }
                        specs = <div>{specs}</div>;
                    }

                    types.push(
                        <div key={'type' + refType.id} className={'desc-item-type ' + infoType.type}>
                            <h2 title={refType.name}>
                                {refType.shortcut} <small>[{refType.code}]</small>
                            </h2>
                            <div key="1" className="desc">
                                {refType.description}
                            </div>
                            <div key="2" className="attrs">
                                <div key="1">
                                    <label>type:</label>
                                    {infoType.type}
                                </div>
                                <div key="2">
                                    <label>repeatable:</label>
                                    {infoType.rep === 1 ? i18n('global.title.yes') : i18n('global.title.no')}
                                </div>
                                <div key="3">
                                    <label>dataType:</label>
                                    {refType.dataType.code}
                                </div>
                                <div key="4">
                                    <label>width:</label>
                                    {infoType.width}
                                </div>
                                <div key="5">
                                    <label>viewOrder:</label>
                                    {refType.viewOrder}
                                </div>
                                <div key="6">
                                    <label>isValueUnique:</label>
                                    {refType.isValueUnique ? i18n('global.title.yes') : i18n('global.title.no')}
                                </div>
                                <div key="7">
                                    <label>canBeOrdered:</label>
                                    {refType.canBeOrdered ? i18n('global.title.yes') : i18n('global.title.no')}
                                </div>
                            </div>
                            {refType.useSpecification && (
                                <Button onClick={this.handleShowHideSpecs.bind(this, refType.id)}>
                                    specifikace{' '}
                                    <Icon
                                        glyph={
                                            this.state.developerExpandedSpecsIds[refType.id]
                                                ? 'fa-angle-up'
                                                : 'fa-angle-down'
                                        }
                                    />
                                </Button>
                            )}
                            {specs}
                        </div>,
                    );
                });

                rows.push(
                    <div key={'group' + group.code}>
                        <h1 key="1">{group.code}</h1>
                        <div key="2">{types}</div>
                    </div>,
                );
            });
        }

        return (
            <div className="developer-panel">
                <div className="desc-items-container">{rows}</div>
            </div>
        );
    }

    renderDeveloperScenarios(activeFund, node) {
        if (!node) {
            return <div className="developer-panel">Je potřeba vybrat jednotku popisu.</div>;
        }
        if (node.developerScenarios.isFetching) {
            return <Loading />;
        }

        let isRootNode = isFundRootId(node.id);

        const rows = [];
        for (let key in node.developerScenarios.data) {
            if (
                !node.developerScenarios.data.hasOwnProperty(key) ||
                (isRootNode && (key === 'after' || key === 'before'))
            ) {
                continue;
            }

            let obj = node.developerScenarios.data[key];

            let types = obj.map(data => (
                <div className="desc-item-type">
                    <h2>{data.name}</h2>
                    {data.groups.map(group =>
                        group.types.map(type => {
                            if (node.subNodeForm.infoTypesMap === null || node.subNodeForm.refTypesMap === null) {
                                return null;
                            }
                            let infoTypes = node.subNodeForm.infoTypesMap[type.id];
                            let refTypes = node.subNodeForm.refTypesMap[type.id];
                            return type.descItems.map(item => {
                                let infoType = infoTypes.specs[indexById(infoTypes.specs, item.descItemSpecId)];
                                let refType = refTypes.descItemSpecsMap[item.descItemSpecId];
                                return (
                                    <div>
                                        <h4 title={refTypes.name}>
                                            {refTypes.shortcut}
                                            <small>[{refTypes.code}]</small>
                                        </h4>
                                        <div>
                                            <label>value:</label>
                                            <span title={refType.name}>{refType.shortcut}</span> |
                                            <small>{infoType.rep}</small>|<small>[{refType.code}]</small>
                                        </div>
                                    </div>
                                );
                            });
                        }),
                    )}
                </div>
            ));

            /** key = after, before, child */
            rows.push(
                <div>
                    <h1>{i18n('developer.scenarios.' + key)}</h1>
                    <div>{types}</div>
                </div>,
            );
        }
        return (
            <div className="developer-panel">
                <div className="desc-items-container">{rows}</div>
            </div>
        );
    }

    renderLecturingPanel(activeFund, node) {
        return (
            <div className="issues-panel">
                <HorizontalSplitter
                    top={<LecturingTop fund={activeFund} node={node} />}
                    bottom={<LecturingBottom fund={activeFund} />}
                />
            </div>
        );
    }

    handleTabSelect(item) {
        this.props.dispatch(selectTab(ArrPage.TAB_KEY, item.id));
        if (item.update) {
            //Pokud má záložka definovánu funkci update(), pak se tato funkce zavolá.
            item.update();
        }
    }

    /**
     *   selectIfNull
     *   Pokud není definována vybraná záložka, vrátí druhou předanou, pokud ne, vrátí zpět původní.
     *
     *   @param selectedTab {String} - id/key záložky
     *   @param tabId {String}- záložka k vybrání
     */
    selectIfNull = (selectedTab, tabId) => {
        if (!selectedTab) {
            return tabId;
        } else {
            return selectedTab;
        }
    };

    /**
     *   registerTabs
     *   Funkce pro definici záložek pravého panelu. Záložky se uloží do state.
     *
     *   @param props {Object}
     */
    registerTabs = props => {
        const {developer, arrRegion, userDetail, structureTypes} = props;
        const activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
        let node;
        if (!activeFund) {
            return;
        }
        if (activeFund.nodes && activeFund.nodes.activeIndex !== null) {
            node = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
        }

        const settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', activeFund.id);
        const readMode = settings.value !== 'false';

        const structureTabs = {};
        if (structureTypes && structureTypes.data) {
            const STRUCTURE_TAB_PREFIX_KEY = 'structure-tab-';
            structureTypes.data.forEach(i => {
                if (!i.anonymous) {
                    const tabKey = STRUCTURE_TAB_PREFIX_KEY + i.id + '-' + i.code;
                    structureTabs[tabKey] = {
                        id: tabKey,
                        key: tabKey,
                        name: i.name,
                        ref: tabKey,
                        render: () => (
                            <ArrStructurePanel
                                {...i}
                                key={tabKey}
                                ref={ref => (this.refObjects[tabKey] = ref)}
                                readMode={readMode}
                                fundId={activeFund.id}
                                fundVersionId={activeFund.versionId}
                                />
                            ),
                        focus: () => this.wrappedFocus(tabKey),
                    };
                }
            });
        }
        /**
         *  Vlastnosti objektu záložky
         *
         *  @prop id {String} identifikátor záložky
         *  @prop key {String} identifikátor záložky
         *  @prop name {String} Nadpis záložky
         *  @prop ref {String} odkaz na záložku
         *  @func render() funkce pro vykreslení obsahu záložky
         *  @func focus() funkce pro získání focusu záložky
         *  @func update() funkce určená pro aktualizaci
         *  @prop condition {Bool} podmínka, která určuje jestli se záložka vykreslí
         *
         */
        const tabs = {
            ...structureTabs,
            files: {
                id: 'files',
                key: 'files',
                name: i18n('arr.panel.title.files'),
                ref: 'fundFiles',
                render: () => this.renderFundFiles(activeFund, readMode),
                focus: () => this.wrappedFocus('fundFiles'),
                condition: userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId: activeFund.id}),
            },
            discrepancies: {
                id: 'discrepancies',
                key: 'discrepancies',
                ref: 'fundErrors',
                name: i18n('arr.panel.title.discrepancies'),
                render: () => this.renderFundErrors(activeFund),
            },
            visiblePolicies: {
                id: 'visiblePolicies',
                key: 'visiblePolicies',
                ref: 'fundVisiblePolicies',
                name: i18n('arr.panel.title.visiblePolicies'),
                render: () => this.renderFundVisiblePolicies(activeFund),
                update: () => this.props.dispatch(fundNodesPolicyFetchIfNeeded(activeFund.versionId)),
            },
            descItems: {
                id: 'descItems',
                key: 'descItems',
                name: i18n('developer.title.descItems'),
                render: () => this.renderDeveloperDescItems(activeFund, node),
                condition: developer.enabled,
                showCondition: !!node,
            },
            scenarios: {
                id: 'scenarios',
                key: 'scenarios',
                name: i18n('developer.title.scenarios'),
                render: () => this.renderDeveloperScenarios(activeFund, node),
                condition: developer.enabled,
                showCondition: !!node,
            },
            lecturing: {
                id: 'lecturing',
                key: 'lecturing',
                name: i18n('arr.panel.title.lecturing'),
                render: () => this.renderLecturingPanel(activeFund, node),
            },
        };
        this.setState({tabs});
    };

    renderPanel() {
        const {arrRegion, userDetail, selectedTabKey} = this.props;

        const activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;

        const settings = getOneSettings(userDetail.settings, 'FUND_RIGHT_PANEL', 'FUND', activeFund.id);
        const centerSettings = getOneSettings(userDetail.settings, 'FUND_CENTER_PANEL', 'FUND', activeFund.id);
        const settingsValues = settings.value ? JSON.parse(settings.value) : null;
        const centerSettingsValues = centerSettings.value ? JSON.parse(centerSettings.value) : null;

        let selectedTab = selectedTabKey;
        // Zjišťuje, zda je vybraná záložka povolená. Pokud ano, vrátí jí zpátky, pokud ne, vrátí null.
        if (selectedTab && settingsValues && !settingsValues[selectedTab]) {
            selectedTab = null;
        }
        // Získání tabů a aktivního tabu... --- mělo by být řešeno přes componentDidMount a dispatch do store - ale není
        const tabs = this.getTabs(this.state.tabs, settingsValues, selectedTab);
        // takže získáme nový selected tab key aby nám to fungovalo
        selectedTab = tabs.selectedTab;

        if (!selectedTab || (centerSettingsValues && centerSettingsValues.rightPanel === false)) {
            //pokud neexistuje žádná vybratelná záložka nebo je vypnutý pravý panel
            return false;
        }

        const tabContent = this.state.tabs[selectedTab] && this.state.tabs[selectedTab].render();

        return (
            <Tabs.Container>
                <Tabs.Tabs items={tabs.items} activeItem={{id: selectedTab}} onSelect={this.handleTabSelect} />
                <Tabs.Content>{tabContent}</Tabs.Content>
            </Tabs.Container>
        );
    }

    renderFundFiles(activeFund, readMode) {
        return (
            <FundFiles
                ref={ref => (this.refObjects['fundFiles'] = ref)}
                versionId={activeFund.versionId}
                fundId={activeFund.id}
                fundFiles={activeFund.fundFiles}
                readMode={readMode}
            />
        );
    }

    handleSetExtendedView(showExtendedView) {
        this.props.dispatch(fundExtendedView(showExtendedView));
    }

    renderCenterPanel(readMode, closed) {
        const {focus, arrRegion, rulDataTypes, calendarTypes, descItemTypes, userDetail} = this.props;
        const showRegisterJp = arrRegion.showRegisterJp;
        const activeFund = this.getActiveFund(this.props);

        const centerSettings = getOneSettings(userDetail.settings, 'FUND_CENTER_PANEL', 'FUND', activeFund.id);
        const centerSettingsValues = centerSettings.value ? JSON.parse(centerSettings.value) : null;

        if (arrRegion.extendedView) {
            // extended view - jiné větší zobrazení stromu, renderuje se zde
            const colorCoded = !(centerSettingsValues && centerSettingsValues.treeColorCoding === false);

            return (
                <FundTreeMain
                    focus={focus}
                    className="extended-tree"
                    fund={activeFund}
                    cutLongLabels={false}
                    versionId={activeFund.versionId}
                    {...activeFund.fundTree}
                    actionAddons={
                        <Button
                            onClick={() => {
                                this.handleSetExtendedView(false);
                            }}
                            className="extended-view-toggle"
                        >
                            <Icon glyph="fa-compress" />
                        </Button>
                    }
                    colorCoded={colorCoded}
                />
            );
        } else if (activeFund.nodes.activeIndex === null) {
            return (
                <div className="arr-output-detail-container">
                    <div className="unselected-msg">
                        <div className="title">{i18n('arr.node.noSelection.title')}</div>
                        <div className="msg-text">{i18n('arr.node.noSelection.message')}</div>
                    </div>
                </div>
            );
        } else {
            // standardní zobrazení pořádání - záložky node
            const accordion = centerSettingsValues && centerSettingsValues.acordeon === true;

            return (
                <NodeTabs
                    versionId={activeFund.versionId}
                    fund={activeFund}
                    closed={activeFund.closed}
                    nodes={activeFund.nodes.nodes}
                    activeIndex={activeFund.nodes.activeIndex}
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                    descItemTypes={descItemTypes}
                    fundId={activeFund.id}
                    showRegisterJp={showRegisterJp}
                    displayAccordion={accordion}
                />
            );
        }
    }

    renderLeftPanel(readMode, closed) {
        const {focus, arrRegion, userDetail} = this.props;
        const activeFund = this.getActiveFund(this.props);
        const centerSettings = getOneSettings(userDetail.settings, 'FUND_CENTER_PANEL', 'FUND', activeFund.id);
        const centerSettingsValues = centerSettings.value ? JSON.parse(centerSettings.value) : null;
        let colorCoded = !(centerSettingsValues && centerSettingsValues.treeColorCoding === false);

        if (arrRegion.extendedView) {
            // extended view - jiné větší zobrazení stromu, ale renderuje se v center panelu, tento bude prázdný
            return null;
        } else {
            // standardní zobrazení pořádání, strom AS
            return (
                <FundTreeMain
                    className="fund-tree-container"
                    fund={activeFund}
                    cutLongLabels={true}
                    versionId={activeFund.versionId}
                    {...activeFund.fundTree}
                    ref={ref => (this.refTree = ref)}
                    focus={focus}
                    actionAddons={
                        <Button
                            onClick={() => {
                                this.handleSetExtendedView(true);
                            }}
                            className="extended-view-toggle"
                        >
                            <Icon glyph="fa-arrows-alt" />
                        </Button>
                    }
                    colorCoded={colorCoded}
                    readMode={readMode}
                />
            );
        }
    }

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasRdPage(activeFund ? activeFund.id : null);
    }

    renderRightPanel() {
        return this.renderPanel();
    }
}

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, focus, developer, userDetail, tab} = state;
    let structureTypes = null;
    if (arrRegion.activeIndex !== null) {
        const fund = arrRegion.funds[arrRegion.activeIndex];
        structureTypes = objectById(refTables.structureTypes.data, fund.versionId, 'versionId');
    }

    return {
        structureTypes,
        splitter,
        arrRegion,
        focus,
        developer,
        userDetail,
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
        ruleSet: refTables.ruleSet,
        selectedTabKey: tab.values[ArrPage.TAB_KEY],
        issueProtocol: storeFromArea(state, issuesActions.AREA_PROTOCOL),
        issueDetail: storeFromArea(state, issuesActions.AREA_DETAIL),
        issueList: storeFromArea(state, issuesActions.AREA_LIST),
    };
}

export default connect(mapStateToProps)(ArrPage);
