/**
 * Komponenta panelu formuláře jedné JP.
 */

import scrollIntoView from "dom-scroll-into-view";
import classNames from "classnames";
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {
    AbstractReactComponent,
    Accordion,
    HorizontalLoader,
    i18n,
    Icon,
    ListBox,
    Loading,
    TooltipTrigger,
    Utils
} from 'components/shared';
import SubNodeDao from './SubNodeDao'
import SubNodeRegister from './SubNodeRegister'
import NodeActionsBar from './NodeActionsBar'
import NodeSubNodeForm from './NodeSubNodeForm'
import {Button} from 'react-bootstrap';
import {addNodeFormArr} from 'actions/arr/addNodeForm.jsx';
import {nodeFormActions} from 'actions/arr/subNodeForm.jsx'
import {fundSubNodeRegisterFetchIfNeeded} from 'actions/arr/subNodeRegister.jsx'
import {fundSubNodeDaosFetchIfNeeded} from 'actions/arr/subNodeDaos.jsx'
import {fundSubNodeInfoFetchIfNeeded} from 'actions/arr/subNodeInfo.jsx'
import {fundNodeInfoFetchIfNeeded} from 'actions/arr/nodeInfo.jsx'
import {
    fundNodeSubNodeFulltextSearch,
    fundSelectSubNode,
    fundSubNodesNext,
    fundSubNodesNextPage,
    fundSubNodesPrev,
    fundSubNodesPrevPage
} from 'actions/arr/node.jsx';
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {createDigitizationName, createFundRoot, getDescItemsAddTree} from './ArrUtils.jsx'
import {propsEquals} from 'components/Utils.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {createReferenceMarkString, getGlyph, getOneSettings} from 'components/arr/ArrUtils.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog.jsx'
import ArrRequestForm from "./ArrRequestForm";
import {WebApi} from 'actions/index.jsx';
import {Shortcuts} from 'react-shortcuts';
import {canSetFocus, focusWasSet, isFocusExactFor, isFocusFor, setFocus} from 'actions/global/focus.jsx'
import AddDescItemTypeForm from './nodeForm/AddDescItemTypeForm.jsx'
import {setVisiblePolicyReceive, setVisiblePolicyRequest} from 'actions/arr/visiblePolicy.jsx'
import {visiblePolicyTypesFetchIfNeeded} from 'actions/refTables/visiblePolicyTypes.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {PropTypes} from 'prop-types';
import defaultKeymap from './NodePanelKeymap.jsx'

import './NodePanel.less';
import NodeSettingsForm from "./NodeSettingsForm";
import {FOCUS_KEYS} from "../../constants.tsx";
// Konstance kolik se má maximálně zobrazit v seznamu parents a children záznamů
const PARENT_CHILD_MAX_LENGTH = 250

class NodePanel extends AbstractReactComponent {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };
    componentWillMount(){
        Utils.addShortcutManager(this,defaultKeymap);
    }
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }
    constructor(props) {
        super(props);

        this.bindMethods('renderParents', 'renderRow',
            'renderChildren', 'handleOpenItem', 'handleSetVisiblePolicy',
            'handleCloseItem', 'handleParentNodeClick', 'handleChildNodeClick',
            'getParentNodes', 'getChildNodes', 'getSiblingNodes',
            'renderAccordion', 'renderState', 'transformConformityInfo', 'renderRowItem',
            'handleShortcuts', 'trySetFocus', 'handleAddDescItemType', 'handleVisiblePolicy',
            'ensureItemVisibleNoForm'
            );

        this.state = {
            focusItemIndex: this.getFocusItemIndex(props, 0)
        }
    }
    selectorMoveUp = ()=>{
        this.selectorMoveRelative(-1);
    }
    selectorMoveDown = ()=>{
        this.selectorMoveRelative(1);
    }
    selectorMoveEnd = ()=>{
        const {node} = this.props
        const index = Math.min(node.viewStartIndex + node.pageSize - 1, node.childNodes.length - 1)
        this.selectorMoveToIndex(index);
    }
    selectorMoveTop = ()=>{
        this.selectorMoveToIndex(0);
    }
    selectorMoveRelative = (step) => {
        const {focusItemIndex} = this.state
        this.selectorMoveToIndex(focusItemIndex + step);
    }
    selectorMoveToIndex = (index) => {
        const {node, versionId} = this.props
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
                this.dispatch(fundSubNodesPrev(versionId, node.id, node.routingKey))
            } else if (index > pageMax) {
                this.dispatch(fundSubNodesNext(versionId, node.id, node.routingKey))
            }

            this.setState({focusItemIndex: index}, () => {this.ensureItemVisibleNoForm(index)})
        }
    }

    getFocusItemIndex(props, prevFocusItemIndex) {
        const {node} = props

        var focusItemIndex = prevFocusItemIndex
        if (node.selectedSubNodeId !== null) {
            focusItemIndex = indexById(node.childNodes, node.selectedSubNodeId)
        }
        return focusItemIndex || prevFocusItemIndex;
    }

    componentDidMount() {
        const settings = this.getSettingsFromProps();

        this.requestData(this.props.versionId, this.props.node, this.props.showRegisterJp, settings);
        this.ensureItemVisible();
        this.trySetFocus(this.props);
    }

    componentWillReceiveProps(nextProps) {
        const settings = this.getSettingsFromProps(nextProps);
        this.requestData(nextProps.versionId, nextProps.node, nextProps.showRegisterJp, settings);

        var newState = {
            focusItemIndex: this.getFocusItemIndex(nextProps, this.state.focusItemIndex)
        }

        var scroll = false;
        if (
            (!this.props.node.nodeInfoFetched || !this.props.node.subNodeForm.fetched)
            && (nextProps.node.nodeInfoFetched && nextProps.node.subNodeForm.fetched)) {    // předchozí stav byl, že něco nebylo načteno, nový je, že je vše načteno
            scroll = true;
            scroll = true;
        }
        if (scroll) {
            this.setState(newState, this.ensureItemVisible)
        } else {
            this.setState(newState);
        }

        this.trySetFocus(nextProps)
    }
    /**
     * Returns fund settings object built from userDetail in props
     */
    getSettingsFromProps(props=this.props){
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
            arrPerm
        };
    }

    trySetFocus(props) {
        var {focus, node} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, FOCUS_KEYS.ARR, 2, 'accordion') || (node.selectedSubNodeId === null && isFocusFor(focus, FOCUS_KEYS.ARR, 2))) {
                this.setState({}, () => {
                   ReactDOM.findDOMNode(this.refs.content).focus()
                   focusWasSet()
                })
            }
            // Jen pokud není třeba focus na něco nižšího, např. prvek formuláře atp
            // Voláno jen pokud formulář úspěšně focus nenastavil - např. pokud jsou všechna pole formuláře zamčena
            else if (isFocusExactFor(focus, FOCUS_KEYS.ARR, 2)) {
                this.setState({}, () => {
                    ReactDOM.findDOMNode(this.refs.content).focus()
                    focusWasSet()
                })
            }
        }
    }

    handleShortcuts(action,e) {
        console.log("#handleShortcuts", '[' + action + ']', this);
        e.preventDefault()
        e.stopPropagation()
        const {node, versionId, closed, userDetail, fundId} = this.props
        const {focusItemIndex} = this.state;
        const index = indexById(node.childNodes, node.selectedSubNodeId)

        var settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', fundId);
        var settingsValues = settings.value != 'false';
        const readMode = closed || settingsValues;

        const actionWithBlur = (action) => {
            const el = document.activeElement;
            if (el && el.blur) {
                el.blur();
            }
            setTimeout(() => {
                action();
            }, 220);
        }

        switch (action) {
            case 'searchItem':
                ReactDOM.findDOMNode(this.refs.search.getInput().refs.input).focus()
                break
            case 'addDescItemType':
                if (node.selectedSubNodeId !== null && !readMode) {
                    this.handleAddDescItemType()
                }
                break
            case 'addNodeAfter':
                if (!readMode) {
                    this.dispatch(addNodeFormArr('AFTER', node, focusItemIndex, versionId));
                }
                break
            case 'addNodeBefore':
                if (!readMode) {
                    this.dispatch(addNodeFormArr('BEFORE', node, focusItemIndex, versionId));
                }
                break
            case 'addNodeChild':
                if (!readMode) {
                    this.dispatch(addNodeFormArr('CHILD', node, focusItemIndex, versionId));
                }
                break
            case 'addNodeEnd':
                if (!readMode) {
                    this.dispatch(addNodeFormArr('ATEND', node, focusItemIndex, versionId));
                }
                break
        }
    }

    handleAccordionShortcuts(action,e) {
        const {node} = this.props;
        const {focusItemIndex} = this.state;
        const index = indexById(node.childNodes, node.selectedSubNodeId);
        let preventDefaultActions = ["prevItem","nextItem","toggleItem"];
        if(preventDefaultActions.indexOf(action) >= 0){
            e.preventDefault();
        }
        switch (action) {
            case 'prevItem':
                if (index > 0) {
                    this.handleOpenItem(node.childNodes[index - 1])
                    this.dispatch(setFocus(FOCUS_KEYS.ARR, 2, 'accordion'))
                }
                break
            case 'nextItem':
                if (index + 1 < node.childNodes.length) {
                    this.handleOpenItem(node.childNodes[index + 1])
                    this.dispatch(setFocus(FOCUS_KEYS.ARR, 2, 'accordion'))
                }
                break
            case 'toggleItem':
                if (node.selectedSubNodeId === null) {
                    const {focusItemIndex} = this.state
                    this.handleOpenItem(node.childNodes[focusItemIndex])
                    this.dispatch(setFocus(FOCUS_KEYS.ARR, 2, 'accordion'))
                } else {
                    const {focusItemIndex} = this.state
                    this.handleCloseItem(node.childNodes[focusItemIndex])
                    this.dispatch(setFocus(FOCUS_KEYS.ARR, 2, 'accordion'))
                }
                break
            case "ACCORDION_MOVE_UP":
                this.selectorMoveUp();
                break;
            case "ACCORDION_MOVE_DOWN":
                this.selectorMoveDown();
                break;
            case "ACCORDION_MOVE_TOP":
                this.selectorMoveTop();
                break;
            case "ACCORDION_MOVE_END":
                this.selectorMoveEnd();
                break;
        }
    }
    /**
     * Zobrazení formuláře pro požadavek na digitalizaci.
     */
    handleDigitizationRequest = () => {
        const {node, versionId} = this.props;
        const nodeId = node.selectedSubNodeId;

        const form = <ArrRequestForm
            fundVersionId={versionId}
            type="DIGITIZATION"
            onSubmitForm={(send, data) => {
                WebApi.arrDigitizationRequestAddNodes(versionId, data.requestId, send, data.description, [nodeId], parseInt(data.digitizationFrontdesk))
                    .then(() => {
                        this.dispatch(modalDialogHide());
                    });
            }}
        />;
        this.dispatch(modalDialogShow(this, i18n('arr.request.digitizationRequest.form.title'), form));
    }

    handleVisiblePolicy() {
        const {node, versionId} = this.props;
        const form = <NodeSettingsForm nodeId={node.selectedSubNodeId} fundVersionId={versionId} onSubmit={this.handleSetVisiblePolicy}
                                       onSubmitSuccess={() => this.props.dispatch(modalDialogHide())}
        />;
        this.dispatch(modalDialogShow(this, i18n('visiblePolicy.form.title'), form));
    }

    handleSetVisiblePolicy(data) {
        const {node, versionId, dispatch} = this.props;
        const mapIds = {};
        const {records, rules, nodeExtensions, ...others} = data;
        if (rules !== "PARENT") {
            records.forEach((val, index) => {
                mapIds[parseInt(val.id)] = val.checked;
            });
        }

        const nodeExtensionsIds = Object.values(nodeExtensions).filter(i => i.checked).map(i => i.id);


        return WebApi.setVisiblePolicy(node.selectedSubNodeId, versionId, mapIds, false, nodeExtensionsIds).then(() => {
            dispatch(setVisiblePolicyReceive(node.selectedSubNodeId, versionId));
        });
    }

    /**
     * Zobrazení dialogu pro přidání atributu.
     */
    handleAddDescItemType() {
        const {node: {subNodeForm, selectedSubNodeId, routingKey}, versionId, fund, userDetail} = this.props;
        let strictMode = fund.activeVersion.strictMode;

        let userStrictMode = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', fund.id);
        if (userStrictMode && userStrictMode.value !== null) {
            strictMode = userStrictMode.value === 'true';
        }

        const formData = subNodeForm.formData;
        const descItemTypes = getDescItemsAddTree(formData.descItemGroups, subNodeForm.infoTypesMap, subNodeForm.refTypesMap, subNodeForm.infoGroups, strictMode);

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

        var submit = (data) => {
            return this.dispatch(nodeFormActions.fundSubNodeFormDescItemTypeAdd(versionId, routingKey, data.descItemTypeId.id));
        };

        // Modální dialog
        const form = <AddDescItemTypeForm descItemTypes={descItemTypes} onSubmitForm={submit} onSubmit2={submit}/>;
        this.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), form));
    }

    ensureItemVisible() {
        if (this.props.node.selectedSubNodeId !== null) {
            var itemNode = ReactDOM.findDOMNode(this.refs['accheader-' + this.props.node.selectedSubNodeId])
            if (itemNode !== null) {
                var contentNode = ReactDOM.findDOMNode(this.refs.accordionContent)
                //scrollIntoView(itemNode, contentNode, { onlyScrollIfNeeded: true, alignWithTop:false })
                contentNode.scrollTop = itemNode.offsetTop - contentNode.offsetHeight/2
            }
        }
    }

    ensureItemVisibleNoForm(index) {
        const {node} = this.props

        var itemNode = ReactDOM.findDOMNode(this.refs['accheader-' + node.childNodes[index].id])
        if (itemNode !== null) {
            var containerNode = ReactDOM.findDOMNode(this.refs.accordionContent)
            scrollIntoView(itemNode, containerNode, { onlyScrollIfNeeded: true, alignWithTop:false })
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
return true
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['versionId', 'fund', 'node', 'calendarTypes', 'descItemTypes', 'rulDataTypes', 'fundId', 'showRegisterJp', 'closed']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    /**
     * Načtení dat, pokud je potřeba.
     * @param versionId {String} verze AS
     * @param node {Object} node
     * @param showRegisterJp {bool} zobrazení rejstřílů vázené k jednotce popisu
     */
    requestData(versionId, node, showRegisterJp, settings) {
        if (node.selectedSubNodeId != null) {
            this.dispatch(descItemTypesFetchIfNeeded());
            this.dispatch(nodeFormActions.fundSubNodeFormFetchIfNeeded(versionId, node.routingKey, node.dirty, settings.showChildren, settings.showParents));
            this.dispatch(refRulDataTypesFetchIfNeeded());

            showRegisterJp && this.dispatch(fundSubNodeRegisterFetchIfNeeded(versionId, node.selectedSubNodeId, node.routingKey));
            this.dispatch(fundSubNodeDaosFetchIfNeeded(versionId, node.selectedSubNodeId, node.routingKey));

        }
        this.dispatch(visiblePolicyTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
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

        this.dispatch(fundSelectSubNode(this.props.versionId, subNodeId, subNodeParentNode, false, null, true));
    }

    /**
     * Kliknutí na položku v seznamu podřízených NODE.
     * @param node {Object} node na který se kliklo
     */
    handleChildNodeClick(node) {
        var subNodeId = node.id;
        var subNodeParentNode = this.getSiblingNodes()[indexById(this.getSiblingNodes(), this.props.node.selectedSubNodeId)];
        this.dispatch(fundSelectSubNode(this.props.versionId, subNodeId, subNodeParentNode, false, null, true));
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
            children = <div key='children' className='children'>
                <HorizontalLoader text={i18n('global.data.loading.node.children')} />
            </div>;
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
        var usedItems = [...items]
        if (items.length > PARENT_CHILD_MAX_LENGTH) {
            usedItems = [
                ...usedItems.slice(0, PARENT_CHILD_MAX_LENGTH),
                {
                    name: i18n('global.title.moreRows', items.length - PARENT_CHILD_MAX_LENGTH)
                }
            ]
        }

        return (
            <ListBox key={key} className={myClass}
                items={usedItems}
                renderItemContent={this.renderRowItem.bind(this, onClick)}
                canSelectItem={(item, index) => typeof item.id !== 'undefined'}
                onSelect={(item, index) => onClick(item)}
            />
        )
    }

    /**
     * Renderování jednoho řádku v listboxu.
     * onClick {Object} on click metoda, která se má zavolat po aktivaci řádku
     * item {Object} položka pro renderování
     */
    renderRowItem(onClick, props) {
        const {item} = props;
        var icon = item.icon ? <Icon className="node-icon" glyph={getGlyph(item.icon)} /> : ''
        var refmark = createReferenceMarkString(item);
        var levels = "";
        if(refmark != "")
            levels = <span className="reference-mark">{refmark}</span>
        var name = item.name ? item.name : <i>{i18n('fundTree.node.name.undefined', item.id)}</i>;
        name = <span title={name} className="name"><span>{name}</span></span>
        const click = typeof item.id !== 'undefined' ? onClick.bind(this, item) : null

        return (
            <div key={item.id} className='node' onClick={click}>
                 {levels} {icon} &nbsp;{name}
            </div>
        )
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
        this.dispatch(fundSelectSubNode(this.props.versionId, null, this.props.node, false, null, false));
    }

    /**
     * Vybrání a otevření položky Accordion.
     * @param item {Object} na který node v Accordion se kliklo
     */
    handleOpenItem(item) {
        var subNodeId = item.id;
        this.dispatch(fundSelectSubNode(this.props.versionId, subNodeId, this.props.node, false, null, true));
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
            var _id=0;

            var policyTypes = item.nodeConformity.policyTypeIdsVisible;

            var description = (item.nodeConformity.description) ? "<br />" + item.nodeConformity.description : "";
            var messages = new Array();

            var errors = item.nodeConformity.errorList;
            var missings = item.nodeConformity.missingList;

            var errorsHide = 0;
            if (errors && errors.length > 0) {
                messages.push(<div key="errors" className="error">Chyby</div>);
                errors.forEach(error => {
                    var cls = "message";
                    if (error.policyTypeId != null
                        && policyTypes[error.policyTypeId] != null
                        && policyTypes[error.policyTypeId] == false) {
                        cls += " ignore";
                        errorsHide++;
                    }
                    messages.push(<div key={'err' + _id++} className={cls}>{error.description}</div>)
                });
            }

            var missingsHide = 0;
            if (missings && missings.length > 0) {
                messages.push(<div key="missings" className="missing">Chybějící</div>);
                missings.forEach(missing => {
                    var cls = "message";
                    if (missing.policyTypeId != null
                        && policyTypes[missing.policyTypeId] != null
                        && policyTypes[missing.policyTypeId] == false) {
                        cls += " ignore";
                        missingsHide++;
                    }
                    messages.push(<div key={'mis' + _id++}  className={cls}>{missing.description}</div>)
                });
            }

            if (item.nodeConformity.state === "OK") {
                icon = <Icon glyph="fa-check" />
                tooltip = <div>{i18n('arr.node.status.ok') + description}</div>
            } else {
                if ((missings == null || missingsHide == missings.length) && (errors == null || errorsHide == errors.length)) {
                    icon = <Icon glyph="fa-check-circle" />
                    tooltip = <div>{i18n('arr.node.status.okx')} {description} {messages}</div>
                } else {
                    icon = <Icon glyph="fa-exclamation-circle" />
                    tooltip = <div>{i18n('arr.node.status.err')} {description} {messages}</div>
                }
            }
        } else {
            icon = <Icon glyph="fa-exclamation-triangle" />
            tooltip = <div>{i18n('arr.node.status.undefined')}</div>
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
                    <div>
                        {icon}
                    </div>
                </TooltipTrigger>
        );
    }

    renderIssues(item) {
        if (!item.issues || item.issues.length < 1) {
            return null
        }

        return (
            <TooltipTrigger
                    content={<span>{item.issues.map((i: IssueVO) => <div key={i.id}>#{i.number} - {i.description}</div>)}</span>}
                    holdOnHover
                    placement="auto"
                    className="issue"
                    showDelay={50}
                    hideDelay={0}
                >
                <Icon glyph="fa-commenting"/>
            </TooltipTrigger>
        );
    }

    /**
     * Renderování Accordion.
     * @param form {Object} editační formulář, pokud je k dispozici (k dispozici je, pokud je nějaká položka Accordion vybraná)
     * @param recordInfo rejstříky k JP
     * @param daos digitální entity k JP
     * @return {Object} view
     */
    renderAccordion(form, recordInfo, daos, readMode, arrPerm) {
        const {node, versionId, userDetail, fund, fundId, closed, displayAccordion} = this.props;
        const {focusItemIndex} = this.state;
        var rows = [];

        if (!node.nodeInfoFetched) {
            if (!node.selectedSubNodeId) {
                console.warn("Není vybraná JP!", node);
            } else {
                rows.push(<HorizontalLoader key="loading" text={i18n('global.data.loading.node')}/>);
            }
        } else{
            if (node.viewStartIndex > 0 && displayAccordion) {
                rows.push(
                    <Button key="prev" onClick={()=>this.dispatch(fundSubNodesPrev(versionId, node.id, node.routingKey))}>
                        <Icon glyph="fa-chevron-left" />{i18n('arr.fund.prev')}
                    </Button>
                )
            }
            for (let a = 0; a < node.childNodes.length; a++) {
                const item = node.childNodes[a];

                const state = this.renderState(item);
                const issues = this.renderIssues(item);
                const accordionLeft = item.accordionLeft ? item.accordionLeft : i18n('accordion.title.left.name.undefined', item.id)
                const accordionRight = item.accordionRight ? item.accordionRight : ''
                const referenceMark = <span className="reference-mark">{createReferenceMarkString(item)}</span>
                const focused = a === this.state.focusItemIndex;
                const disabled = !displayAccordion;

                let digitizationInfo;
                if (item.digitizationRequests && item.digitizationRequests.length > 0) {
                    const title = item.digitizationRequests.map(digReq => {
                        return createDigitizationName(digReq, userDetail)
                    });
                    digitizationInfo = <div className="digitizationInfo" title={title}>
                        <Icon glyph="fa-shopping-basket"/>
                    </div>
                }

                if (node.selectedSubNodeId === item.id) {
                    rows.push(
                        <div key={item.id} ref={'accheader-' + item.id} className={'accordion-item opened' + (focused ? ' focused' : '') + (disabled ? ' disabled' : '')}>
                            <div key='header' className='accordion-header-container' onClick={displayAccordion ? this.handleCloseItem.bind(this, item) : () => ''}>
                                <div className={'accordion-header'}>
                                    <div title={accordionLeft} className='accordion-header-left' key='accordion-header-left'>
                                        {referenceMark} <span className="title" title={accordionLeft}>{accordionLeft}</span>
                                    </div>
                                    <div title={accordionRight} className='accordion-header-right' key='accordion-header-right'>
                                        <span className="title" title={accordionRight}>{accordionRight}</span>
                                    </div>
                                    {issues}
                                    {state}
                                    {digitizationInfo}
                                </div>
                            </div>
                            <div key="body" className='accordion-body'>
                                {form}
                                {recordInfo}
                                {daos}
                            </div>
                        </div>
                    )
                } else if (displayAccordion) {
                    rows.push(
                        <div key={item.id} ref={'accheader-' + item.id} className={'accordion-item closed' + (focused ? ' focused' : '')}>
                            <div key='header' className='accordion-header-container' onClick={this.handleOpenItem.bind(this, item)}>
                                <div className='accordion-header'>
                                    <div title={accordionLeft} className='accordion-header-left' key='accordion-header-left'>
                                        {referenceMark} <span className="title" title={accordionLeft}>{accordionLeft}</span>
                                    </div>
                                    <div title={accordionRight} className='accordion-header-right' key='accordion-header-right'>
                                        <span className="title" title={accordionRight}>{accordionRight}</span>
                                    </div>
                                    {issues}
                                    {state}
                                    {digitizationInfo}
                                </div>
                            </div>
                        </div>
                    )
                }
            }

            if (node.nodeCount > node.pageSize && node.viewStartIndex + node.pageSize/2 < node.nodeCount && node.nodeCount - node.viewStartIndex > node.pageSize && displayAccordion) {
                rows.push(
                    <Button key="next" onClick={()=>this.dispatch(fundSubNodesNext(versionId, node.id, node.routingKey))}><Icon glyph="fa-chevron-right" />{i18n('arr.fund.next')}</Button>
                )
            }
        }
        return (
            <Shortcuts name='Accordion' key='content' className='content' ref='content' handler={(action,e) => this.handleAccordionShortcuts(action,e)} tabIndex={0} global stopPropagation={false}>
                <div  className='inner-wrapper' ref="innerAccordionWrapper">
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
                    <div className='content-wrapper' ref='accordionContent'>
                        {rows}
                    </div>
                </div>
            </Shortcuts>
        )
    }


    render() {
        const {calendarTypes, versionId, rulDataTypes, node,
                fundId, userDetail,
                showRegisterJp, fund, closed, descItemTypes} = this.props;

        const settings = this.getSettingsFromProps();
        const readMode = settings.readMode;
        const arrPerm = settings.arrPerm || false;
        var siblings = this.getSiblingNodes().map(s => <span key={s.id}> {s.id}</span>);

        var form;
        if (node.subNodeForm.fetched && calendarTypes.fetched && descItemTypes.fetched) {
            // Zjisštění, zda pro daný node existuje v accordion předchozí záznam (který ale není vyfiltrovaný), ze kterého je možné přebírat hodnoty atirbutu pro akci okamžité kopírování
            var descItemCopyFromPrevEnabled = false
            var i1 = indexById(node.childNodes, node.selectedSubNodeId)
            var i2 = indexById(node.childNodes, node.selectedSubNodeId)
            if (i1 !== null && i2 !== null && i2 > 0 && i1 > 0) {   // před danám nodem existuje nějaký záznam a v případě filtrování existuje před daným nodem také nějaký záznam
                if (node.childNodes[i1 - 1].id == node.childNodes[i2 - 1].id) {  // jedná se o stejné záznamy, můžeme zobrazit akci kopírování
                    descItemCopyFromPrevEnabled = true
                }
            }


            // Formulář editace JP
            var conformityInfo = this.transformConformityInfo(node);
                // descItemTypeInfos={node.subNodeForm.descItemTypeInfos}
            form = <NodeSubNodeForm
                key={'sub-node-form-' + node.selectedSubNodeId}
                ref='subNodeForm'
                singleDescItemTypeEdit={false}
                nodeId={node.id}
                versionId={versionId}
                selectedSubNodeId={node.selectedSubNodeId}
                routingKey={node.routingKey}
                subNodeForm={node.subNodeForm}
                rulDataTypes={rulDataTypes}
                calendarTypes={calendarTypes}
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
                readMode={readMode}
                arrPerm={arrPerm}
            />
        } else {
            form = <HorizontalLoader text={i18n('global.data.loading.form')}/>
        }

        let record;
        if (showRegisterJp) {
            record = <SubNodeRegister
                        nodeId={node.id}
                        versionId={versionId}
                        selectedSubNodeId={node.selectedSubNodeId}
                        routingKey={node.routingKey}
                        register={node.subNodeRegister}
                        closed={closed}
                        readMode={readMode}/>
        }

        const daos = <SubNodeDao
            nodeId={node.id}
            versionId={versionId}
            selectedSubNodeId={node.selectedSubNodeId}
            routingKey={node.routingKey}
            readMode={readMode}
            daos={node.subNodeDaos} />

        var cls = classNames({
            'node-panel-container': true,
        })

        return (
            <Shortcuts name='NodePanel' key={'node-panel'} className={cls} handler={this.handleShortcuts} tabIndex={0} global stopPropagation={false}>
                <div key='main' className='main'>
                    {settings.showParents && this.renderParents()}
                    {this.renderAccordion(form, record, daos, readMode, arrPerm)}
                    {settings.showChildren &&  this.renderChildren()}
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
            missings: {}
        };

        if (nodeState) {
            var policyTypes = nodeState.policyTypeIdsVisible;

            var errors = nodeState.errorList;
            if (errors && errors.length > 0) {
                errors.forEach(error => {
                    if (conformityInfo.errors[error.descItemObjectId] == null) {
                        conformityInfo.errors[error.descItemObjectId] = new Array();
                    }
                    if (error.policyTypeId == null
                        || policyTypes[error.policyTypeId] == null
                        || policyTypes[error.policyTypeId] == true) {
                        conformityInfo.errors[error.descItemObjectId].push(error);
                    }
                });
            }

            var missings = nodeState.missingList;
            if (missings && missings.length > 0) {
                missings.forEach(missing => {
                    if (conformityInfo.missings[missing.descItemTypeId] == null) {
                        conformityInfo.missings[missing.descItemTypeId] = new Array();
                    }
                    if (missing.policyTypeId == null
                        || policyTypes[missing.policyTypeId] == null
                        || policyTypes[missing.policyTypeId] == true) {
                        conformityInfo.missings[missing.descItemTypeId].push(missing);
                    }
                });
            }
        }
        return conformityInfo;
    }
}

function mapStateToProps(state) {
    const {focus, userDetail} = state;
    return {
        focus,
        userDetail
    }
}

NodePanel.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    fund: React.PropTypes.object.isRequired,
    node: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    fundId: React.PropTypes.number,
    showRegisterJp: React.PropTypes.bool.isRequired,
    displayAccordion: React.PropTypes.bool.isRequired,
    closed: React.PropTypes.bool.isRequired,
    userDetail: React.PropTypes.object.isRequired
};

export default connect(mapStateToProps)(NodePanel);
