/**
 * Komponenta panelu formuláře jedné JP.
 */

import scrollIntoView from 'dom-scroll-into-view';
import classNames from 'classnames';
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux';
import {AbstractReactComponent, HorizontalLoader, i18n, Icon, ListBox, TooltipTrigger, Utils} from 'components/shared';
import {SubNodeDao} from './sub-node-dao';
import NodeActionsBar from './NodeActionsBar';
import NodeSubNodeForm from './NodeSubNodeForm';
import {Button} from '../ui';
import {addNodeFormArr} from 'actions/arr/addNodeForm';
import {nodeFormActions} from 'actions/arr/subNodeForm';
import {fundSubNodeDaosFetchIfNeeded} from 'actions/arr/subNodeDaos';
import {fundSelectSubNode, fundSubNodesNext, fundSubNodesPrev} from 'actions/arr/node';
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes';
import {indexById} from 'stores/app/utils';
import {createDigitizationName, createFundRoot, getDescItemsAddTree} from './ArrUtils';
import {createReferenceMarkString, getGlyph, getOneSettings} from 'components/arr/ArrUtils';
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes';
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog';
import ArrRequestForm from './ArrRequestForm';
import {WebApi} from 'actions/index';
import {Shortcuts} from 'react-shortcuts';
import {canSetFocus, focusWasSet, isFocusExactFor, isFocusFor, setFocus} from 'actions/global/focus';
import AddDescItemTypeForm from './nodeForm/AddDescItemTypeForm';
import {visiblePolicyTypesFetchIfNeeded} from 'actions/refTables/visiblePolicyTypes';
import * as perms from 'actions/user/Permission';
import {PropTypes} from 'prop-types';
import defaultKeymap from './NodePanelKeymap';

import './NodePanel.scss';
import { NodeSettingsModal } from './node-settings-form';
import {FOCUS_KEYS} from '../../constants';
import ConfirmForm from '../shared/form/ConfirmForm';
import getMapFromList from 'shared/utils/getMapFromList';
import SyncNodes from './SyncNodes';
import objectById from "../../shared/utils/objectById";
import LinkedNodes from "./LinkedNodes";
// Konstance kolik se má maximálně zobrazit v seznamu parents a children záznamů
const PARENT_CHILD_MAX_LENGTH = 250;

class NodePanel extends AbstractReactComponent {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    static propTypes = {
        versionId: PropTypes.number.isRequired,
        fund: PropTypes.object.isRequired,
        node: PropTypes.object.isRequired,
        descItemTypes: PropTypes.object.isRequired,
        rulDataTypes: PropTypes.object.isRequired,
        fundId: PropTypes.number,
        displayAccordion: PropTypes.bool,
        closed: PropTypes.bool.isRequired,
        userDetail: PropTypes.object.isRequired,
    };

    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap);
    }

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    refSubNodeForm = null;
    refContent = null;
    refObjects = {};

    constructor(props) {
        super(props);

        this.bindMethods(
            'renderParents',
            'renderRow',
            'renderChildren',
            'handleOpenItem',
            'handleSetVisiblePolicy',
            'handleCloseItem',
            'handleParentNodeClick',
            'handleChildNodeClick',
            'getParentNodes',
            'getChildNodes',
            'getSiblingNodes',
            'renderAccordion',
            'renderState',
            'transformConformityInfo',
            'renderRowItem',
            'handleShortcuts',
            'trySetFocus',
            'handleAddDescItemType',
            'handleVisiblePolicy',
            'ensureItemVisibleNoForm',
        );

        this.state = {
            focusItemIndex: this.getFocusItemIndex(props, 0),
        };
    }

    selectorMoveUp = () => {
        this.selectorMoveRelative(-1);
    };
    selectorMoveDown = () => {
        this.selectorMoveRelative(1);
    };
    selectorMoveEnd = () => {
        const {node} = this.props;
        const index = Math.min(node.viewStartIndex + node.pageSize - 1, node.childNodes.length - 1);
        this.selectorMoveToIndex(index);
    };
    selectorMoveTop = () => {
        this.selectorMoveToIndex(0);
    };
    selectorMoveRelative = step => {
        const {focusItemIndex} = this.state;
        this.selectorMoveToIndex(focusItemIndex + step);
    };
    selectorMoveToIndex = index => {
        const {node, versionId} = this.props;
        if (node.selectedSubNodeId === null) {
            const pageMax = node.viewStartIndex + node.pageSize - 1;
            const pageMin = node.viewStartIndex;
            const max = node.childNodes.length - 1;
            const min = 0;
            if (index < min) {
                index = min;
            } else if (index > max) {
                index = max;
            } else if (index < pageMin) {
                this.props.dispatch(fundSubNodesPrev(versionId, node.id, node.routingKey));
            } else if (index > pageMax) {
                this.props.dispatch(fundSubNodesNext(versionId, node.id, node.routingKey));
            }

            this.setState({focusItemIndex: index}, () => {
                this.ensureItemVisibleNoForm(index);
            });
        }
    };

    getFocusItemIndex(props, prevFocusItemIndex) {
        const {node} = props;

        let focusItemIndex = prevFocusItemIndex;
        if (node.selectedSubNodeId !== null) {
            focusItemIndex = indexById(node.childNodes, node.selectedSubNodeId);
        }
        if (focusItemIndex == null) {
            return prevFocusItemIndex;
        } else {
            return focusItemIndex;
        }
    }

    componentDidMount() {
        const settings = this.getSettingsFromProps();

        this.requestData(this.props.versionId, this.props.node, settings);
        this.ensureItemVisible();
        this.trySetFocus(this.props);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const settings = this.getSettingsFromProps(nextProps);
        this.requestData(nextProps.versionId, nextProps.node, settings);

        var newState = {
            focusItemIndex: this.getFocusItemIndex(nextProps, this.state.focusItemIndex),
        };

        var scroll = false;
        if (
            (!this.props.node.nodeInfoFetched || !this.props.node.subNodeForm.fetched) &&
            nextProps.node.nodeInfoFetched &&
            nextProps.node.subNodeForm.fetched
        ) {
            // předchozí stav byl, že něco nebylo načteno, nový je, že je vše načteno
            scroll = true;
            scroll = true;
        }
        if (scroll) {
            this.setState(newState, this.ensureItemVisible);
        } else {
            this.setState(newState);
        }

        this.trySetFocus(nextProps);
    }

    /**
     * Returns fund settings object built from userDetail in props
     */
    getSettingsFromProps(props = this.props) {
        const {userDetail, fundId, closed, node} = props;
        // center panel settings
        var settings = getOneSettings(userDetail.settings, 'FUND_CENTER_PANEL', 'FUND', fundId);
        var settingsValues = settings.value ? JSON.parse(settings.value) : null;

        var showParents = settingsValues && settingsValues['parents'];
        var showChildren = settingsValues && settingsValues['children'];

        // read mode settings
        settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', fundId);
        settingsValues = settings.value != 'false';

        let readMode = closed || settingsValues;

        const subNodeForm = node.subNodeForm;
        const arrPerm = subNodeForm.data && subNodeForm.data.arrPerm;

        // sets read mode when user does not have permissions to arrange fund
        if (!userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId}) && !arrPerm) {
            readMode = true;
        }

        return {
            showParents,
            showChildren,
            readMode,
            arrPerm,
        };
    }

    trySetFocus(props) {
        var {focus, node} = props;

        if (canSetFocus()) {
            if (
                isFocusFor(focus, FOCUS_KEYS.ARR, 2, 'accordion') ||
                (node.selectedSubNodeId === null && isFocusFor(focus, FOCUS_KEYS.ARR, 2))
            ) {
                this.setState({}, () => {
                    ReactDOM.findDOMNode(this.refContent).focus();
                    focusWasSet();
                });
            }
            // Jen pokud není třeba focus na něco nižšího, např. prvek formuláře atp
            // Voláno jen pokud formulář úspěšně focus nenastavil - např. pokud jsou všechna pole formuláře zamčena
            else if (isFocusExactFor(focus, FOCUS_KEYS.ARR, 2)) {
                this.setState({}, () => {
                    ReactDOM.findDOMNode(this.refContent).focus();
                    focusWasSet();
                });
            }
        }
    }

    handleShortcuts(action, e) {
        console.log('#handleShortcuts', '[' + action + ']', this);
        e.preventDefault();
        e.stopPropagation();
        const {node, versionId, closed, userDetail, fundId} = this.props;
        const {focusItemIndex} = this.state;
        const index = indexById(node.childNodes, node.selectedSubNodeId);

        var settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', fundId);
        var settingsValues = settings.value != 'false';
        const readMode = closed || settingsValues;

        const actionWithBlur = action => {
            const el = document.activeElement;
            if (el && el.blur) {
                el.blur();
            }
            setTimeout(() => {
                action();
            }, 220);
        };

        switch (action) {
            case 'searchItem':
                ReactDOM.findDOMNode(this.refs.search.getInput().refs.input).focus();
                break;
            case 'addDescItemType':
                if (node.selectedSubNodeId !== null && !readMode) {
                    this.handleAddDescItemType();
                }
                break;
            case 'addNodeAfter':
                if (!readMode) {
                    this.props.dispatch(addNodeFormArr('AFTER', node, focusItemIndex, versionId));
                }
                break;
            case 'addNodeBefore':
                if (!readMode) {
                    this.props.dispatch(addNodeFormArr('BEFORE', node, focusItemIndex, versionId));
                }
                break;
            case 'addNodeChild':
                if (!readMode) {
                    this.props.dispatch(addNodeFormArr('CHILD', node, focusItemIndex, versionId));
                }
                break;
            case 'addNodeEnd':
                if (!readMode) {
                    this.props.dispatch(addNodeFormArr('ATEND', node, focusItemIndex, versionId));
                }
                break;
            default:
                break;
        }
    }

    handleAccordionShortcuts(action, e) {
        const {node} = this.props;
        const index = indexById(node.childNodes, node.selectedSubNodeId);
        let preventDefaultActions = ['prevItem', 'nextItem', 'toggleItem'];
        if (preventDefaultActions.indexOf(action) >= 0) {
            e.preventDefault();
        }
        switch (action) {
            case 'prevItem':
                if (index > 0) {
                    this.handleOpenItem(node.childNodes[index - 1]);
                    this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 2, 'accordion'));
                }
                break;
            case 'nextItem':
                if (index + 1 < node.childNodes.length) {
                    this.handleOpenItem(node.childNodes[index + 1]);
                    this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 2, 'accordion'));
                }
                break;
            case 'toggleItem':
                if (node.selectedSubNodeId === null) {
                    const {focusItemIndex} = this.state;
                    this.handleOpenItem(node.childNodes[focusItemIndex]);
                    this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 2, 'accordion'));
                } else {
                    const {focusItemIndex} = this.state;
                    this.handleCloseItem(node.childNodes[focusItemIndex]);
                    this.props.dispatch(setFocus(FOCUS_KEYS.ARR, 2, 'accordion'));
                }
                break;
            case 'ACCORDION_MOVE_UP':
                this.selectorMoveUp();
                break;
            case 'ACCORDION_MOVE_DOWN':
                this.selectorMoveDown();
                break;
            case 'ACCORDION_MOVE_TOP':
                this.selectorMoveTop();
                break;
            case 'ACCORDION_MOVE_END':
                this.selectorMoveEnd();
                break;
            default:
                break;
        }
    }

    /**
     * Zobrazení formuláře pro požadavek na digitalizaci.
     */
    handleDigitizationRequest = () => {
        const {node, versionId} = this.props;
        const nodeId = node.selectedSubNodeId;

        const form = (
            <ArrRequestForm
                fundVersionId={versionId}
                type="DIGITIZATION"
                onSubmitForm={(send, data) => {
                    return WebApi.arrDigitizationRequestAddNodes(
                        versionId,
                        data.requestId,
                        send,
                        data.description,
                        [nodeId],
                        parseInt(data.digitizationFrontdesk),
                    );
                }}
                onSubmitSuccess={(result, dispatch) => dispatch(modalDialogHide())}
            />
        );
        this.props.dispatch(modalDialogShow(this, i18n('arr.request.digitizationRequest.form.title'), form));
    };

    /**
     * Zobrazení formuláře pro potvrzení synchronizace DAO.
     */
    handleDigitizationSync = () => {
        const {node, versionId} = this.props;
        const nodeId = node.selectedSubNodeId;

        const confirmForm = (
            <ConfirmForm
                confirmMessage={i18n('arr.daos.node.sync.confirm-message')}
                submittingMessage={i18n('arr.daos.node.sync.submitting-message')}
                submitTitle={i18n('global.action.run')}
                onSubmit={() => {
                    return WebApi.syncDaoLink(versionId, nodeId);
                }}
                onSubmitSuccess={() => {
                    this.props.dispatch(modalDialogHide());
                }}
            />
        );
        this.props.dispatch(modalDialogShow(this, i18n('arr.daos.node.sync.title'), confirmForm));
    };

    handleRefSync = () => {
        const {node, fundId} = this.props;
        const nodeId = node.selectedSubNodeId;

        let nodeVersion = node.version;
        if (!nodeVersion) {
            const subNode = objectById(node.childNodes, nodeId);
            if (subNode == null) {
                console.error("Nedohledána verze pro JP", nodeId);
            } else {
                nodeVersion = subNode.version;
            }
        }

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.syncNodes.title'),
                <SyncNodes nodeId={nodeId} nodeVersion={nodeVersion} fundId={fundId} />,
            ),
        );
    };

    handleVisiblePolicy() {
        const { dispatch, node, versionId } = this.props;

        dispatch(modalDialogShow(this, i18n('visiblePolicy.form.title'), (
            <NodeSettingsModal
                nodeId={node.selectedSubNodeId}
                fundVersionId={versionId}
            />
        )));
    }

    /**
     * Zobrazení dialogu pro přidání atributu.
     */
    handleAddDescItemType() {
        const {
            node: {subNodeForm, selectedSubNodeId, routingKey},
            versionId,
            fund,
            userDetail,
        } = this.props;
        let strictMode = fund.activeVersion.strictMode;

        let userStrictMode = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', fund.id);
        if (userStrictMode && userStrictMode.value !== null) {
            strictMode = userStrictMode.value === 'true';
        }

        const formData = subNodeForm.formData;
        const descItemTypes = getDescItemsAddTree(
            formData.descItemGroups,
            subNodeForm.infoTypesMap,
            subNodeForm.refTypesMap,
            subNodeForm.infoGroups,
            strictMode,
        );

        // Zatím zakomentováno, možná se bude ještě nějak řadit - zatím není jasné podle čeho řadit - podle uvedení v yaml nebo jinak?
        // function typeId(type) {
        //     switch (type) {
        //         case "REQUIRED":
        //             return 0;
        //         case "RECOMMENDED":
        //             return 1;
        //         case "POSSIBLE":
        //             return 2;
        //         case "IMPOSSIBLE":
        //             return 99;
        //         default:
        //             return 3;
        //     }
        // }
        //
        // // Seřazení podle position
        // descItemTypes.sort((a, b) => typeId(a.type) - typeId(b.type));

        var submit = data => {
            return this.props.dispatch(
                nodeFormActions.fundSubNodeFormDescItemTypeAdd(versionId, routingKey, data.descItemTypeId.id),
            );
        };

        // Modální dialog
        const form = <AddDescItemTypeForm descItemTypes={descItemTypes} onSubmitForm={submit} onSubmit2={submit} />;
        this.props.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), form, "dialog-md"));
    }

    ensureItemVisible() {
        if (this.props.node.selectedSubNodeId !== null) {
            var itemNode = ReactDOM.findDOMNode(this.refObjects['accheader-' + this.props.node.selectedSubNodeId]);
            if (itemNode !== null) {
                var contentNode = ReactDOM.findDOMNode(this.refAccordionContent);
                //scrollIntoView(itemNode, contentNode, { onlyScrollIfNeeded: true, alignWithTop:false })
                contentNode.scrollTop = itemNode.offsetTop - contentNode.offsetHeight / 2;
            }
        }
    }

    ensureItemVisibleNoForm(index) {
        const {node} = this.props;

        var itemNode = ReactDOM.findDOMNode(this.refObjects['accheader-' + node.childNodes[index].id]);
        if (itemNode !== null) {
            var containerNode = ReactDOM.findDOMNode(this.refAccordionContent);
            scrollIntoView(itemNode, containerNode, {onlyScrollIfNeeded: true, alignWithTop: false});
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        return true;
        // if (this.state !== nextState) {
        //     return true;
        // }
        // var eqProps = ['versionId', 'fund', 'node', 'descItemTypes', 'rulDataTypes', 'fundId', 'closed'];
        // return !propsEquals(this.props, nextProps, eqProps);
    }

    /**
     * Načtení dat, pokud je potřeba.
     * @param versionId {String} verze AS
     * @param node {Object} node
     */
    requestData(versionId, node, settings) {
        if (node.selectedSubNodeId != null) {
            this.props.dispatch(descItemTypesFetchIfNeeded());
            this.props.dispatch(
                nodeFormActions.fundSubNodeFormFetchIfNeeded(
                    versionId,
                    node.routingKey,
                    node.dirty,
                    settings.showChildren,
                    settings.showParents,
                ),
            );
            this.props.dispatch(refRulDataTypesFetchIfNeeded());

            this.props.dispatch(fundSubNodeDaosFetchIfNeeded(versionId, node.selectedSubNodeId, node.routingKey));
        }
        this.props.dispatch(visiblePolicyTypesFetchIfNeeded());
    }

    /**
     * Kliknutí na položku v seznamu nadřízených NODE.
     * @param node {Object} node na který se kliklo
     */
    handleParentNodeClick(node) {
        var parentNodes = this.getParentNodes();
        var index = indexById(parentNodes, node.id);
        var subNodeId = node.id;
        var subNodeParentNode = index + 1 < parentNodes.length ? parentNodes[index + 1] : null;
        if (subNodeParentNode == null) {
            subNodeParentNode = createFundRoot(this.props.fund);
        }

        this.props.dispatch(fundSelectSubNode(this.props.versionId, subNodeId, subNodeParentNode, false, null, true));
    }

    /**
     * Kliknutí na položku v seznamu podřízených NODE.
     * @param node {Object} node na který se kliklo
     */
    handleChildNodeClick(node) {
        var subNodeId = node.id;
        var subNodeParentNode = this.getSiblingNodes()[
            indexById(this.getSiblingNodes(), this.props.node.selectedSubNodeId)
        ];
        this.props.dispatch(fundSelectSubNode(this.props.versionId, subNodeId, subNodeParentNode, false, null, true));
    }

    /**
     * Renderování seznamu nadřízených NODE.
     * @param parents {Array} seznam node pro vyrenderování
     * @return {Object} view
     */
    renderParents() {
        const parents = this.getParentNodes().reverse();
        return this.renderRow(parents, 'parents', 'parents', this.handleParentNodeClick);
    }

    /**
     * Renderování seznamu podřízených NODE.
     * @param children {Array} seznam node pro vyrenderování
     * @return {Object} view
     */
    renderChildren() {
        const {node} = this.props;
        let nodes = this.getChildNodes();
        let children;
        if (node.subNodeInfo.fetched || node.selectedSubNodeId == null) {
            children = this.renderRow(nodes, 'children', 'children', this.handleChildNodeClick);
        } else {
            children = (
                <div key="children" className="children">
                    <HorizontalLoader text={i18n('global.data.loading.node.children')} />
                </div>
            );
        }
        return children;
    }

    /**
     * Renderování seznamu NODE.
     *
     * @param items {Array} seznam node pro vyrenderování
     * @param key {String} klíč objektu
     * @param myClass {String} třída objektu
     * @param onClick {Function} callback po kliku na položku
     * @return {Object} view
     */
    renderRow(items, key, myClass, onClick) {
        var usedItems = [...items];
        if (items.length > PARENT_CHILD_MAX_LENGTH) {
            usedItems = [
                ...usedItems.slice(0, PARENT_CHILD_MAX_LENGTH),
                {
                    name: i18n('global.title.moreRows', items.length - PARENT_CHILD_MAX_LENGTH),
                },
            ];
        }

        return (
            <ListBox
                key={key}
                className={myClass}
                items={usedItems}
                renderItemContent={this.renderRowItem.bind(this, onClick)}
                canSelectItem={(item, index) => typeof item.id !== 'undefined'}
                onSelect={(item, index) => onClick(item)}
            />
        );
    }

    /**
     * Renderování jednoho řádku v listboxu.
     * onClick {Object} on click metoda, která se má zavolat po aktivaci řádku
     * item {Object} položka pro renderování
     */
    renderRowItem(onClick, props) {
        const {item} = props;
        var icon = item.icon ? <Icon className="node-icon" glyph={getGlyph(item.icon)} /> : '';
        var refmark = createReferenceMarkString(item);
        var levels = '';
        if (refmark != '') levels = <span className="reference-mark">{refmark}</span>;
        var name = item.name ? item.name : <i>{i18n('fundTree.node.name.undefined', item.id)}</i>;
        name = (
            <span title={name} className="name">
                <span>{name}</span>
            </span>
        );
        const click = typeof item.id !== 'undefined' ? onClick.bind(this, item) : null;

        return (
            <div key={item.id} className="node" onClick={click}>
                {levels} {icon} &nbsp;{name}
            </div>
        );
    }

    /**
     * Načtení seznamu nadřízených NODE.
     * @return {Array} seznam NODE
     */
    getParentNodes() {
        const {node} = this.props;
        if (node) {
            return [...node.parentNodes];
        } else {
            return [];
        }
    }

    /**
     * Načtení seznamu podřízených NODE.
     * @return {Array} seznam NODE
     */
    getChildNodes() {
        return this.props.node.subNodeInfo.childNodes ? [...this.props.node.subNodeInfo.childNodes] : [];
    }

    /**
     * Načtení seznamu souroyeneckých NODE.
     * @return {Array} seznam NODE
     */
    getSiblingNodes() {
        return this.props.node.childNodes ? [...this.props.node.childNodes] : [];
    }

    /**
     * Zavření položky Accordion.
     * @param item {Object} na který node v Accordion se kliklo
     */
    handleCloseItem(item) {
        this.props.dispatch(fundSelectSubNode(this.props.versionId, null, this.props.node, false, null, false));
    }

    /**
     * Vybrání a otevření položky Accordion.
     * @param item {Object} na který node v Accordion se kliklo
     */
    handleOpenItem(item) {
        var subNodeId = item.id;
        this.props.dispatch(fundSelectSubNode(this.props.versionId, subNodeId, this.props.node, false, null, true));
    }

    /**
     * Renderování stavu.
     * @param item {object} na který node v Accordion se kliklo
     * @return {Object} view
     */
    renderState(item) {
        var icon;
        var tooltip;

        if (item.nodeConformity) {
            var _id = 0;

            var policyTypes = item.nodeConformity.policyTypeIdsVisible;

            var description = item.nodeConformity.description ? '<br />' + item.nodeConformity.description : '';
            var messages = [];

            var errors = item.nodeConformity.errorList;
            var missings = item.nodeConformity.missingList;

            var errorsHide = 0;
            if (errors && errors.length > 0) {
                messages.push(
                    <div key="errors" className="error">
                        Chyby
                    </div>,
                );
                errors.forEach(error => {
                    var cls = 'message';
                    if (
                        error.policyTypeId != null &&
                        policyTypes[error.policyTypeId] != null &&
                        policyTypes[error.policyTypeId] == false
                    ) {
                        cls += ' ignore';
                        errorsHide++;
                    }
                    messages.push(
                        <div key={'err' + _id++} className={cls}>
                            {error.description}
                        </div>,
                    );
                });
            }

            var missingsHide = 0;
            if (missings && missings.length > 0) {
                messages.push(
                    <div key="missings" className="missing">
                        Chybějící
                    </div>,
                );
                missings.forEach(missing => {
                    var cls = 'message';
                    if (
                        missing.policyTypeId != null &&
                        policyTypes[missing.policyTypeId] != null &&
                        policyTypes[missing.policyTypeId] == false
                    ) {
                        cls += ' ignore';
                        missingsHide++;
                    }
                    messages.push(
                        <div key={'mis' + _id++} className={cls}>
                            {missing.description}
                        </div>,
                    );
                });
            }

            if (item.nodeConformity.state === 'OK') {
                icon = <Icon glyph="fa-check" />;
                tooltip = <div>{i18n('arr.node.status.ok') + description}</div>;
            } else {
                if (
                    (missings == null || missingsHide == missings.length) &&
                    (errors == null || errorsHide == errors.length)
                ) {
                    icon = <Icon glyph="fa-check-circle" />;
                    tooltip = (
                        <div>
                            {i18n('arr.node.status.okx')} {description} {messages}
                        </div>
                    );
                } else {
                    icon = <Icon glyph="fa-exclamation-circle" />;
                    tooltip = (
                        <div>
                            {i18n('arr.node.status.err')} {description} {messages}
                        </div>
                    );
                }
            }
        } else {
            icon = <Icon glyph="fa-exclamation-triangle" />;
            tooltip = <div>{i18n('arr.node.status.undefined')}</div>;
        }

        return (
            <TooltipTrigger
                content={tooltip}
                holdOnHover
                placement="auto"
                className="status"
                showDelay={50}
                hideDelay={0}
            >
                <div>{icon}</div>
            </TooltipTrigger>
        );
    }

    renderIssues(item) {
        if (!item.issues || item.issues.length < 1) {
            return null;
        }

        return (
            <TooltipTrigger
                content={
                    <span>
                        {item.issues.map(i => (
                            <div key={i.id}>
                                #{i.number} - {i.description}
                            </div>
                        ))}
                    </span>
                }
                holdOnHover
                placement="auto"
                className="issue"
                showDelay={50}
                hideDelay={0}
            >
                <Icon glyph="fa-commenting" />
            </TooltipTrigger>
        );
    }

    /**
     * Renderování Accordion.
     * @param form {Object} editační formulář, pokud je k dispozici (k dispozici je, pokud je nějaká položka Accordion vybraná)
     * @param daos digitální entity k JP
     * @param linkedNodes seznam JP odkazujícíh na tuto JP
     * @param readMode pouze čtení
     * @param arrPerm oprávnění pořádání
     * @return {Object} view
     */
    renderAccordion(form, daos, linkedNodes, readMode, arrPerm) {
        const {node, versionId, userDetail, fund, fundId, closed, displayAccordion} = this.props;
        const {focusItemIndex} = this.state;
        var rows = [];

        if (!node.nodeInfoFetched) {
            if (!node.selectedSubNodeId) {
                console.warn('Není vybraná JP!', node);
            } else {
                rows.push(<HorizontalLoader key="loading" text={i18n('global.data.loading.node')} />);
            }
        } else {
            if (node.viewStartIndex > 0 && displayAccordion) {
                rows.push(
                    <Button
                        key="prev"
                        onClick={() => this.props.dispatch(fundSubNodesPrev(versionId, node.id, node.routingKey))}
                    >
                        <Icon glyph="fa-chevron-left" />
                        {i18n('arr.fund.prev')}
                    </Button>,
                );
            }
            for (let a = 0; a < node.childNodes.length; a++) {
                const item = node.childNodes[a];

                const state = this.renderState(item);
                const issues = this.renderIssues(item);
                const accordionLeft = item.accordionLeft
                    ? item.accordionLeft
                    : i18n('accordion.title.left.name.undefined', item.id);
                const accordionRight = item.accordionRight ? item.accordionRight : '';
                const referenceMark = <span className="reference-mark">{createReferenceMarkString(item)}</span>;
                const focused = a === this.state.focusItemIndex;
                const disabled = !displayAccordion;

                let digitizationInfo;
                if (item.digitizationRequests && item.digitizationRequests.length > 0) {
                    const title = item.digitizationRequests.map(digReq => {
                        return createDigitizationName(digReq, userDetail);
                    });
                    digitizationInfo = (
                        <div className="digitizationInfo" title={title}>
                            <Icon glyph="fa-shopping-basket" />
                        </div>
                    );
                }

                if (node.selectedSubNodeId === item.id) {
                    rows.push(
                        <div
                            key={item.id}
                            ref={ref => (this.refObjects['accheader-' + item.id] = ref)}
                            className={
                                'accordion-item opened' + (focused ? ' focused' : '') + (disabled ? ' disabled' : '')
                            }
                        >
                            <div
                                key="header"
                                className="accordion-header-container"
                                onClick={displayAccordion ? this.handleCloseItem.bind(this, item) : () => ''}
                            >
                                <div className={'accordion-header'}>
                                    <div
                                        title={accordionLeft}
                                        className="accordion-header-left"
                                        key="accordion-header-left"
                                    >
                                        {referenceMark}{' '}
                                        <span className="title" title={accordionLeft}>
                                            {accordionLeft}
                                        </span>
                                    </div>
                                    <div
                                        title={accordionRight}
                                        className="accordion-header-right"
                                        key="accordion-header-right"
                                    >
                                        <span className="title" title={accordionRight}>
                                            {accordionRight}
                                        </span>
                                    </div>
                                    {issues}
                                    {state}
                                    {digitizationInfo}
                                </div>
                            </div>
                            <div key="body" className="accordion-body">
                                {form}
                                {linkedNodes}
                                {daos}
                            </div>
                        </div>,
                    );
                } else if (displayAccordion) {
                    rows.push(
                        <div
                            key={item.id}
                            ref={'accheader-' + item.id}
                            className={'accordion-item closed' + (focused ? ' focused' : '')}
                        >
                            <div
                                key="header"
                                className="accordion-header-container"
                                onClick={this.handleOpenItem.bind(this, item)}
                            >
                                <div className="accordion-header">
                                    <div
                                        title={accordionLeft}
                                        className="accordion-header-left"
                                        key="accordion-header-left"
                                    >
                                        {referenceMark}{' '}
                                        <span className="title" title={accordionLeft}>
                                            {accordionLeft}
                                        </span>
                                    </div>
                                    <div
                                        title={accordionRight}
                                        className="accordion-header-right"
                                        key="accordion-header-right"
                                    >
                                        <span className="title" title={accordionRight}>
                                            {accordionRight}
                                        </span>
                                    </div>
                                    {issues}
                                    {state}
                                    {digitizationInfo}
                                </div>
                            </div>
                        </div>,
                    );
                }
            }

            if (
                node.nodeCount > node.pageSize &&
                node.viewStartIndex + node.pageSize / 2 < node.nodeCount &&
                node.nodeCount - node.viewStartIndex > node.pageSize &&
                displayAccordion
            ) {
                rows.push(
                    <Button
                        key="next"
                        onClick={() => this.props.dispatch(fundSubNodesNext(versionId, node.id, node.routingKey))}
                    >
                        <Icon glyph="fa-chevron-right" />
                        {i18n('arr.fund.next')}
                    </Button>,
                );
            }
        }
        return (
            <Shortcuts
                name="Accordion"
                key="content"
                className="content"
                ref={ref => (this.refContent = ref)}
                handler={(action, e) => this.handleAccordionShortcuts(action, e)}
                tabIndex={0}
                global
                stopPropagation={false}
            >
                <div className="inner-wrapper">
                    <div className="menu-wrapper">
                        <NodeActionsBar
                            simplified={!displayAccordion}
                            node={node}
                            selectedSubNodeIndex={focusItemIndex}
                            versionId={versionId}
                            userDetail={userDetail}
                            fundId={fundId}
                            closed={closed}
                            arrPerm={arrPerm}
                            onSwitchNode={this.handleAccordionShortcuts.bind(this)}
                        />
                    </div>
                    <div className="content-wrapper" ref={ref => (this.refAccordionContent = ref)}>
                        {rows}
                    </div>
                </div>
            </Shortcuts>
        );
    }

    render() {
        const {
            versionId,
            rulDataTypes,
            node,
            fundId,
            userDetail,
            fund,
            closed,
            descItemTypes,
        } = this.props;

        const settings = this.getSettingsFromProps();
        const readMode = settings.readMode;
        const arrPerm = settings.arrPerm || false;
        var siblings = this.getSiblingNodes().map(s => <span key={s.id}> {s.id}</span>);

        var form;
        if (node.subNodeForm.fetched && descItemTypes.fetched) {
            // Zjisštění, zda pro daný node existuje v accordion předchozí záznam (který ale není vyfiltrovaný), ze kterého je možné přebírat hodnoty atirbutu pro akci okamžité kopírování
            var descItemCopyFromPrevEnabled = false;
            var i1 = indexById(node.childNodes, node.selectedSubNodeId);
            var i2 = indexById(node.childNodes, node.selectedSubNodeId);
            if (i1 !== null && i2 !== null && i2 > 0 && i1 > 0) {
                // před danám nodem existuje nějaký záznam a v případě filtrování existuje před daným nodem také nějaký záznam
                if (node.childNodes[i1 - 1].id == node.childNodes[i2 - 1].id) {
                    // jedná se o stejné záznamy, můžeme zobrazit akci kopírování
                    descItemCopyFromPrevEnabled = true;
                }
            }

            // Formulář editace JP
            var conformityInfo = this.transformConformityInfo(node);
            // descItemTypeInfos={node.subNodeForm.descItemTypeInfos}
            form = (
                <NodeSubNodeForm
                    key={'sub-node-form-' + node.selectedSubNodeId}
                    ref={ref => (this.refSubNodeForm = ref)}
                    singleDescItemTypeEdit={false}
                    nodeId={node.id}
                    versionId={versionId}
                    selectedSubNodeId={node.selectedSubNodeId}
                    routingKey={node.routingKey}
                    subNodeForm={node.subNodeForm}
                    rulDataTypes={rulDataTypes}
                    descItemTypes={descItemTypes}
                    conformityInfo={conformityInfo}
                    parentNode={node}
                    fundId={fundId}
                    selectedSubNode={node.subNodeForm.data.parent}
                    descItemCopyFromPrevEnabled={descItemCopyFromPrevEnabled}
                    closed={closed}
                    onAddDescItemType={this.handleAddDescItemType}
                    onVisiblePolicy={this.handleVisiblePolicy}
                    onDigitizationRequest={this.handleDigitizationRequest}
                    onDigitizationSync={this.handleDigitizationSync}
                    onRefSync={this.handleRefSync}
                    readMode={readMode}
                    arrPerm={arrPerm}
                />
            );
        } else {
            form = <HorizontalLoader text={i18n('global.data.loading.form')} />;
        }

        const daos = (
            <SubNodeDao
                nodeId={node.id}
                versionId={versionId}
                selectedSubNodeId={node.selectedSubNodeId}
                routingKey={node.routingKey}
                readMode={readMode}
                daos={node.subNodeDaos}
            />
        );

        const linkedNodes = <LinkedNodes nodeId={node.selectedSubNodeId} versionId={versionId} />

        var cls = classNames({
            'node-panel-container': true,
        });

        return (
            <Shortcuts
                name="NodePanel"
                key={'node-panel'}
                className={cls}
                handler={this.handleShortcuts}
                tabIndex={0}
                global
                stopPropagation={false}
            >
                <div key="main" className="main">
                    {settings.showParents && this.renderParents()}
                    {this.renderAccordion(form, daos, linkedNodes, readMode, arrPerm)}
                    {settings.showChildren && this.renderChildren()}
                </div>
            </Shortcuts>
        );
    }

    /**
     * Převedení dat do lepších struktur.
     *
     * @param node {object} JP
     * @returns {{errors: {}, missings: {}}}
     */
    transformConformityInfo(node) {
        var nodeId = node.subNodeForm.nodeId;

        var nodeState;

        for (var i = 0; i < node.childNodes.length; i++) {
            if (node.childNodes[i].id == nodeId) {
                nodeState = node.childNodes[i].nodeConformity;
                break;
            }
        }

        var conformityInfo = {
            errors: {},
            missings: {},
        };

        if (nodeState) {
            var policyTypes = nodeState.policyTypeIdsVisible;

            var errors = nodeState.errorList;
            if (errors && errors.length > 0) {
                errors.forEach(error => {
                    if (conformityInfo.errors[error.descItemObjectId] == null) {
                        conformityInfo.errors[error.descItemObjectId] = [];
                    }
                    if (
                        error.policyTypeId == null ||
                        policyTypes[error.policyTypeId] == null ||
                        policyTypes[error.policyTypeId] == true
                    ) {
                        conformityInfo.errors[error.descItemObjectId].push(error);
                    }
                });
            }

            var missings = nodeState.missingList;
            if (missings && missings.length > 0) {
                missings.forEach(missing => {
                    if (conformityInfo.missings[missing.descItemTypeId] == null) {
                        conformityInfo.missings[missing.descItemTypeId] = [];
                    }
                    if (
                        missing.policyTypeId == null ||
                        policyTypes[missing.policyTypeId] == null ||
                        policyTypes[missing.policyTypeId] == true
                    ) {
                        conformityInfo.missings[missing.descItemTypeId].push(missing);
                    }
                });
            }
        }
        return conformityInfo;
    }
}

function mapState(state) {
    return {
        focus: state.focus,
        userDetail: state.userDetail,
    };
}

export default connect(mapState)(NodePanel);
