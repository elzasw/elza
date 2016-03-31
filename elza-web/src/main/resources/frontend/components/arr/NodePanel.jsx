/**
 * Komponenta panelu formuláře jedné JP.
 */

// Konstance kolik se má maximálně zobrazit v seznamu parents a children záznamů
const PARENT_CHILD_MAX_LENGTH = 250

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Icon, ListBox, AbstractReactComponent, i18n, Loading, SubNodeForm, Accordion, SubNodeRegister, AddNodeDropdown,
        Search, GoToPositionForm} from 'components';
import {Button, Tooltip, OverlayTrigger, Input} from 'react-bootstrap';
import {fundSubNodeFormFetchIfNeeded} from 'actions/arr/subNodeForm'
import {fundSubNodeRegisterFetchIfNeeded} from 'actions/arr/subNodeRegister'
import {fundSubNodeInfoFetchIfNeeded} from 'actions/arr/subNodeInfo'
import {fundNodeInfoFetchIfNeeded} from 'actions/arr/nodeInfo'
import {fundSelectSubNode} from 'actions/arr/nodes'
import {fundNodeSubNodeFulltextSearch, fundSubNodesNext, fundSubNodesPrev, fundSubNodesNextPage, fundSubNodesPrevPage} from 'actions/arr/node'
import {addNode} from 'actions/arr/node'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes'
import {indexById} from 'stores/app/utils.jsx'
import {createFundRoot, isFundRootId} from './ArrUtils.jsx'
import {propsEquals} from 'components/Utils'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
import {createReferenceMarkString, getGlyph} from 'components/arr/ArrUtils'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {Utils} from 'components'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
const scrollIntoView = require('dom-scroll-into-view')
var classNames = require('classnames');
import {setFocus, canSetFocus, focusWasSet, isFocusFor, isFocusExactFor} from 'actions/global/focus'
require ('./NodePanel.less');
import AddDescItemTypeForm from './nodeForm/AddDescItemTypeForm'
import {fundSubNodeFormDescItemTypeAdd} from 'actions/arr/subNodeForm'

var keyModifier = Utils.getKeyModifier()

var keymap = {
    Accordion: {
        prevItem: keyModifier + 'up',
        nextItem: keyModifier + 'down',
        closeItem: 'shift+enter',
    },
    NodePanel: {
        searchItem: keyModifier + 'f',
        addDescItemType: keyModifier + 'p',
        addNodeAfter: keyModifier + '+',
        addNodeBefore: keyModifier + '-',
        addNodeChild: keyModifier + '*',
        addNodeEnd: keyModifier + '/',
    },
}
var shortcutManager = new ShortcutsManager(keymap)

var accordionKeyDownHandlers = {
    Home: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {node} = this.props
        if (node.selectedSubNodeId === null) {
            const index = node.viewStartIndex
            this.setState({focusItemIndex: index}, () => {this.ensureItemVisibleNoForm(index)})
        }
    },
    End: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {node} = this.props
        if (node.selectedSubNodeId === null) {
            const index = Math.min(node.viewStartIndex + node.pageSize - 1, node.childNodes.length - 1)
            this.setState({focusItemIndex: index}, () => {this.ensureItemVisibleNoForm(index)})
        }
    },
    ArrowUp: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {node} = this.props
        if (node.selectedSubNodeId === null) {
            const {focusItemIndex} = this.state
            if (focusItemIndex > node.viewStartIndex) {
                this.setState({focusItemIndex: focusItemIndex - 1}, () => {this.ensureItemVisibleNoForm(focusItemIndex - 1)})
            }
        }
    },
    ArrowDown: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {node} = this.props
        if (node.selectedSubNodeId === null) {
            const {focusItemIndex} = this.state
            const max = Math.min(node.viewStartIndex + node.pageSize, node.childNodes.length)
            if (focusItemIndex + 1 < max) {
                this.setState({focusItemIndex: focusItemIndex + 1}, () => {this.ensureItemVisibleNoForm(focusItemIndex + 1)})
            }
        }
    },
    Enter: function(e) {
        e.preventDefault()
        e.stopPropagation()

        if (!e.shiftKey) {
            const {node} = this.props
            if (node.selectedSubNodeId === null) {
                const {focusItemIndex} = this.state
                this.handleOpenItem(node.childNodes[focusItemIndex])
                this.dispatch(setFocus('arr', 2, 'accordion'))
            } else {
                const {focusItemIndex} = this.state
                this.handleCloseItem(node.childNodes[focusItemIndex])
                this.dispatch(setFocus('arr', 2, 'accordion'))
            }
        }
    },
}

var NodePanel = class NodePanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderParents', 'renderRow',
            'renderChildren', 'handleOpenItem',
            'handleCloseItem', 'handleParentNodeClick', 'handleChildNodeClick',
            'getParentNodes', 'getChildNodes', 'getSiblingNodes',
            'renderAccordion', 'renderState', 'transformConformityInfo', 'handleAddNodeAtEnd',
            'handleChangeFilterText', 'renderRowItem', 'handleFindPosition', 'handleFindPositionSubmit',
            'handleShortcuts', 'trySetFocus', 'handleAddDescItemType', 'handleAccordionKeyDown',
            'ensureItemVisibleNoForm'
            );

        this.state = {
            filterText: props.node.filterText,
            focusItemIndex: this.getFocusItemIndex(props, 0)
        }
    }

    handleAccordionKeyDown(event) {
        if (document.activeElement === ReactDOM.findDOMNode(this.refs.accordionContent)) { // focus má accordion
            if (accordionKeyDownHandlers[event.key]) {
                accordionKeyDownHandlers[event.key].call(this, event)
            }
        }
    }

    getFocusItemIndex(props, prevFocusItemIndex) {
        const {node} = props

        var focusItemIndex = prevFocusItemIndex
        if (node.selectedSubNodeId !== null) {
            focusItemIndex = indexById(node.childNodes, node.selectedSubNodeId)
        }
        return focusItemIndex
    }

    componentDidMount() {
        this.requestData(this.props.versionId, this.props.node);
        this.dispatch(calendarTypesFetchIfNeeded());
        this.ensureItemVisible();
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        this.requestData(nextProps.versionId, nextProps.node, nextProps.showRegisterJp);
        this.dispatch(calendarTypesFetchIfNeeded());

        var newState = {
            filterText: nextProps.node.filterText,
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

    trySetFocus(props) {
        var {focus, node} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, 'arr', 2, 'accordion') || (node.selectedSubNodeId === null && isFocusFor(focus, 'arr', 2))) {
                this.setState({}, () => {
                   ReactDOM.findDOMNode(this.refs.accordionContent).focus()
                   focusWasSet()
                })
            } else if (isFocusExactFor(focus, 'arr', 2)) {   // jen pokud není třeba focus na něco nižšího, např. prvek formuláře atp
                // Voláne jen pokud formulář úspěšně focus nenastavil - např. pokud jsou všechna pole formuláře zamčena
                this.setState({}, () => {
                    ReactDOM.findDOMNode(this.refs.accordionContent).focus()
                    focusWasSet()
                })
            }
        }
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts", '[' + action + ']', this);

        const {node, focus} = this.props
        const index = indexById(node.childNodes, node.selectedSubNodeId)

        switch (action) {
            case 'prevItem':
                if (index > 0) {
                    this.handleOpenItem(node.childNodes[index - 1])
                    this.dispatch(setFocus('arr', 2, 'accordion'))
                }
                break
            case 'nextItem':
                if (index + 1 < node.childNodes.length) {
                    this.handleOpenItem(node.childNodes[index + 1])
                    this.dispatch(setFocus('arr', 2, 'accordion'))
                }
                break
            case 'searchItem':
                this.refs.search.getInput().getInputDOMNode().focus()
                break
            case 'addDescItemType':
                const {node} = this.props
                if (node.selectedSubNodeId !== null) {
                    this.handleAddDescItemType()
                }
                break
            case 'closeItem':
                if (node.selectedSubNodeId !== null) {
                    this.handleCloseItem(node.childNodes[index])
                    this.dispatch(setFocus('arr', 2, 'accordion'))
                }
                break
            case 'addNodeAfter':
                this.refs.subNodeForm.getWrappedInstance().addNodeAfterClick()
                break
            case 'addNodeBefore':
                this.refs.subNodeForm.getWrappedInstance().addNodeBeforeClick()
                break
            case 'addNodeChild':
                this.refs.subNodeForm.getWrappedInstance().addNodeChildClick()
                break
            case 'addNodeEnd':
                this.refs.addNodeChild.handleToggle(true, false)
                break
        }
    }

    /**
     * Zobrazení dialogu pro přidání atributu.
     */
    handleAddDescItemType() {
        const {node: {subNodeForm, selectedSubNodeId, nodeKey}, versionId} = this.props;

        const formData = subNodeForm.formData

        // Pro přidání chceme jen ty, které zatím ještě nemáme
        var infoTypesMap = {...subNodeForm.infoTypesMap};
        formData.descItemGroups.forEach(group => {
            group.descItemTypes.forEach(descItemType => {
                delete infoTypesMap[descItemType.id];
            })
        })
        var descItemTypes = [];
        Object.keys(infoTypesMap).forEach(function (key) {
            descItemTypes.push({
                ...subNodeForm.refTypesMap[key],
                ...infoTypesMap[key],
            });
        });

        function typeId(type) {
            switch (type) {
                case "REQUIRED":
                    return 0;
                case "RECOMMENDED":
                    return 1;
                case "POSSIBLE":
                    return 2;
                case "IMPOSSIBLE":
                    return 99;
                default:
                    return 3;
            }
        }

        // Seřazení podle position
        descItemTypes.sort((a, b) => typeId(a.type) - typeId(b.type));
        var submit = (data) => {
            this.dispatch(modalDialogHide());
            this.dispatch(fundSubNodeFormDescItemTypeAdd(versionId, selectedSubNodeId, nodeKey, data.descItemTypeId));
        };
        // Modální dialog
        var form = <AddDescItemTypeForm descItemTypes={descItemTypes} onSubmitForm={submit} onSubmit2={submit}/>;
        this.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), form));
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleChangeFilterText(value) {
        this.setState({
            filterText: value
        })
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
        var eqProps = ['versionId', 'fund', 'node', 'calendarTypes', 'descItemTypes',
            'packetTypes', 'packets', 'rulDataTypes', 'fundId', 'showRegisterJp', 'closed']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    /**
     * Načtení dat, pokud je potřeba.
     * @param versionId {String} verze AS
     * @param node {Object} node
     * @param showRegisterJp {bool} zobrazení rejstřílů vázené k jednotce popisu
     */
    requestData(versionId, node, showRegisterJp) {
        if (node.selectedSubNodeId != null) {
            this.dispatch(descItemTypesFetchIfNeeded());
            this.dispatch(fundSubNodeFormFetchIfNeeded(versionId, node.selectedSubNodeId, node.nodeKey));
            this.dispatch(fundSubNodeInfoFetchIfNeeded(versionId, node.selectedSubNodeId, node.nodeKey));
            this.dispatch(refRulDataTypesFetchIfNeeded());

            if (showRegisterJp) {
                this.dispatch(fundSubNodeRegisterFetchIfNeeded(versionId, node.selectedSubNodeId, node.nodeKey));
            }

        }
        this.dispatch(fundNodeInfoFetchIfNeeded(versionId, node.id, node.nodeKey));
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
    renderParents(parents) {
        return this.renderRow(parents, 'parents', 'parents', this.handleParentNodeClick);
    }

    /**
     * Renderování seznamu podřízených NODE.
     * @param children {Array} seznam node pro vyrenderování
     * @return {Object} view
     */
    renderChildren(children) {
        return this.renderRow(children, 'children', 'children', this.handleChildNodeClick);
    }

    /**
     * Renderování seznamu NODE.
     *
     * @param items {Array} seznam node pro vyrenderování
     * @param key {String} klíč objektu
     * @param myClass {String} třída objektu
     * @param onClick {Functions} callback po kliku na položku
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
    renderRowItem(onClick, item) {
        var icon = item.icon ? <Icon className="node-icon" glyph={getGlyph(item.icon)} /> : ''
        var levels = <span className="reference-mark">{createReferenceMarkString(item)}</span>
        var name = item.name ? item.name : <i>{i18n('fundTree.node.name.undefined', item.id)}</i>;
        name = <span title={name} className="name">{name}</span>

        const click = typeof item.id !== 'undefined' ? onClick.bind(this, item) : null

        return (
            <div key={item.id} className='node' onClick={click}>
                {icon} {levels} {name}
            </div>
        )
    }

    /**
     * Načtení seznamu nadřízených NODE.
     * @return {Array} seznam NODE
     */
    getParentNodes() {
        const {node} = this.props;
        if (isFundRootId(node.id)) {
            return [...node.parentNodes];
        } else {
            return [node, ...node.parentNodes];
        }
    }

    /**
     * Načtení seznamu podřízených NODE.
     * @return {Array} seznam NODE
     */
    getChildNodes() {
        return [...this.props.node.subNodeInfo.childNodes];
    }

    /**
     * Načtení seznamu souroyeneckých NODE.
     * @return {Array} seznam NODE
     */
    getSiblingNodes() {
        return [...this.props.node.childNodes];
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
     * Přidání JP na konec do aktuálního node
     * Využito v dropdown buttonu pro přidání node
     *
     * @param event Event selectu
     * @param scenario name vybraného scénáře
     */
    handleAddNodeAtEnd(event, scenario) {
        this.dispatch(addNode(this.props.node, this.props.node, this.props.fund.versionId, "CHILD", this.getDescItemTypeCopyIds(), scenario));
    }

    /**
     * Vrátí pole ke zkopírování
     */
    getDescItemTypeCopyIds() {
        var itemsToCopy = null;
        if (this.props.nodeSettings != "undefined") {
            var nodeIndex = indexById(this.props.nodeSettings.nodes, this.props.node.id);
            if (nodeIndex != null) {
                itemsToCopy = this.props.nodeSettings.nodes[nodeIndex].descItemTypeCopyIds;
            }
        }
        return itemsToCopy;
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
                tooltip = <Tooltip id="status-ok">{i18n('arr.node.status.ok') + description}</Tooltip>
            } else {
                if ((missings == null || missingsHide == missings.length) && (errors == null || errorsHide == errors.length)) {
                    icon = <Icon glyph="fa-check-circle" />
                    tooltip = <Tooltip id="status-err">{i18n('arr.node.status.okx')} {description} {messages}</Tooltip>
                } else {
                    icon = <Icon glyph="fa-exclamation-circle" />
                    tooltip = <Tooltip id="status-err">{i18n('arr.node.status.err')} {description} {messages}</Tooltip>
                }
            }
        } else {
            icon = <Icon glyph="fa-exclamation-triangle" />
            tooltip = <Tooltip id="status-undefined">{i18n('arr.node.status.undefined')}</Tooltip>
        }

        return (
                <OverlayTrigger placement="left" overlay={tooltip}>
                    <div className="status">
                        {icon}
                    </div>
                </OverlayTrigger>
        );
    }

    /**
     * Renderování Accordion.
     * @param form {Object} editační formulář, pokud je k dispozici (k dispozici je, pokud je nějaká položka Accordion vybraná)
     * @return {Object} view
     */
    renderAccordion(form, recordInfo) {
        const {node, versionId } = this.props;

        var rows = [];

        if (node.viewStartIndex > 0) {
            rows.push(
                <Button key="prev" onClick={()=>this.dispatch(fundSubNodesPrev(versionId, node.id, node.nodeKey))}><Icon glyph="fa-chevron-left" />{i18n('arr.fund.prev')}</Button>
            )
        }

        for (var a=node.viewStartIndex; (a<node.viewStartIndex + node.pageSize) && (a < node.childNodes.length); a++) {
            var item = node.childNodes[a];

            var state = this.renderState(item);
            var accordionLeft = item.accordionLeft ? item.accordionLeft : i18n('accordion.title.left.name.undefined', item.id)
            var accordionRight = item.accordionRight ? item.accordionRight : ''
            var referenceMark = <span className="reference-mark">{createReferenceMarkString(item)}</span>

            var focused = a === this.state.focusItemIndex

            if (node.selectedSubNodeId == item.id) {
                rows.push(
                    <div key={item.id} ref={'accheader-' + item.id} className={'accordion-item opened' + (focused ? ' focused' : '')}>
                        <div key='header' className='accordion-header-container' onClick={this.handleCloseItem.bind(this, item)}>
                            <div className='accordion-header'>
                                <div title={accordionLeft} className='accordion-header-left' key='accordion-header-left'>
                                    {referenceMark} <span className="title" title={accordionLeft}>{accordionLeft}</span>
                                </div>
                                <div title={accordionRight} className='accordion-header-right' key='accordion-header-right'>
                                    <span className="title" title={accordionRight}>{accordionRight}</span>
                                </div>
                                {state}
                            </div>
                        </div>
                        <div key="body" className='accordion-body'>
                            {form}
                            {recordInfo}
                        </div>
                    </div>
                )
            } else {
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
                                {state}
                            </div>
                        </div>
                    </div>
                )
            }
        }

        if (node.viewStartIndex + node.pageSize/2 < node.childNodes.length) {
            rows.push(
                <Button key="next" onClick={()=>this.dispatch(fundSubNodesNext(versionId, node.id, node.nodeKey))}><Icon glyph="fa-chevron-right" />{i18n('arr.fund.next')}</Button>
            )
        }

        return (
            <Shortcuts name='Accordion' key='content' className='content' ref='content' handler={this.handleShortcuts}>
                <div tabIndex={0} className='content-wrapper' ref='accordionContent' onKeyDown={this.handleAccordionKeyDown}>
                    {rows}
                </div>
            </Shortcuts>
        )
    }

    /**
     * Akce po úspěšném vybrání pozice JP z formuláře.
     *
     * @param form data z formuláře
     */
    handleFindPositionSubmit(form) {
        const {node} = this.props;

        var index = form.position - 1;
        var subNodeId = node.allChildNodes[index].id;

        this.dispatch(fundSelectSubNode(this.props.versionId, subNodeId, node));
        this.dispatch(modalDialogHide());
    }

    /**
     * Akce pro vybrání JO podle pozice.
     */
    handleFindPosition() {
        const {node} = this.props;

        var count = 0;
        if (node.allChildNodes) {
            count = node.allChildNodes.length;
        }

        this.dispatch(modalDialogShow(this, i18n('arr.fund.subNodes.findPosition'),
                        <GoToPositionForm onSubmitForm={this.handleFindPositionSubmit} maxPosition={count} />
                )
        )
    }

    render() {
        const {developer, calendarTypes, versionId, rulDataTypes, node,
                packetTypes, packets, fundId,
                showRegisterJp, fund, closed, descItemTypes} = this.props;

        if (!node.nodeInfoFetched) {
            return <Loading value={i18n('global.data.loading.node')}/>
        }

        var parents = this.renderParents(this.getParentNodes().reverse());
        var children;
        if (node.subNodeInfo.fetched || node.selectedSubNodeId == null) {
            children = this.renderChildren(this.getChildNodes());
        } else {
            children = <div key='children' className='children'><Loading value={i18n('global.data.loading.node.children')} /></div>
        }
        var siblings = this.getSiblingNodes().map(s => <span key={s.id}> {s.id}</span>);
        var actions = (
            <div key='actions' className='actions-container'>
                <div key='actions' className='actions'>
                    {
                        node.nodeInfoFetched && !isFundRootId(node.id) && !closed &&
                        <AddNodeDropdown key="end"
                                        ref='addNodeChild'
                                         title={i18n('nodePanel.addSubNode')}
                                         glyph="fa-plus-circle"
                                         action={this.handleAddNodeAtEnd}
                                         node={this.props.node}
                                         version={fund.versionId}
                                         direction="CHILD"
                        />
                    }
                    <div className='btn btn-default' disabled={node.viewStartIndex == 0} onClick={()=>this.dispatch(fundSubNodesPrevPage(versionId, node.id, node.nodeKey))}><Icon glyph="fa-backward" />{i18n('arr.fund.subNodes.prevPage')}</div>
                    <div className='btn btn-default' disabled={node.viewStartIndex + node.pageSize >= node.childNodes.length} onClick={()=>this.dispatch(fundSubNodesNextPage(versionId, node.id, node.nodeKey))}><Icon glyph="fa-forward" />{i18n('arr.fund.subNodes.nextPage')}</div>

                    <div className='btn btn-default' onClick={this.handleFindPosition} title={i18n('arr.fund.subNodes.findPosition')} ><Icon glyph="fa-hand-o-down" /></div>

                    <Search
                        ref='search'
                        className='search-input'
                        placeholder={i18n('search.input.search')}
                        filterText={this.props.filterText}
                        value={this.state.filterText}
                        onChange={(e) => this.handleChangeFilterText(e.target.value)}
                        onClear={() => {this.handleChangeFilterText(''); this.dispatch(fundNodeSubNodeFulltextSearch(this.state.filterText))}}
                        onSearch={() => {this.dispatch(fundNodeSubNodeFulltextSearch(this.state.filterText))}}
                    />
                </div>
            </div>
        )

        var form;
        if (node.subNodeForm.fetched && calendarTypes.fetched && descItemTypes.fetched) {
            // Zjisštění, zda pro daný node existuje v accordion předchozí záznam (který ale není vyfiltrovaný), ze kterého je možné přebírat hodnoty atirbutu pro akci okamžité kopírování
            var descItemCopyFromPrevEnabled = false
            var i1 = indexById(node.childNodes, node.selectedSubNodeId)
            var i2 = indexById(node.allChildNodes, node.selectedSubNodeId)
            if (i1 !== null && i2 !== null && i2 > 0 && i1 > 0) {   // před danám nodem existuje nějaký záznam a v případě filtrování existuje před daným nodem také nějaký záznam
                if (node.childNodes[i1 - 1].id == node.allChildNodes[i2 - 1].id) {  // jedná se o stejné záznamy, můžeme zobrazit akci kopírování
                    descItemCopyFromPrevEnabled = true
                }
            }


            // Formulář editace JP
            var conformityInfo = this.transformConformityInfo(node);
            form = <SubNodeForm
                key={'sub-node-form-' + node.selectedSubNodeId}
                ref='subNodeForm'
                nodeId={node.id}
                versionId={versionId}
                selectedSubNodeId={node.selectedSubNodeId}
                nodeKey={node.nodeKey}
                subNodeForm={node.subNodeForm}
                descItemTypeInfos={node.subNodeForm.descItemTypeInfos}
                rulDataTypes={rulDataTypes}
                calendarTypes={calendarTypes}
                packetTypes={packetTypes}
                descItemTypes={descItemTypes}
                conformityInfo={conformityInfo}
                packets={packets}
                parentNode={node}
                fundId={fundId}
                selectedSubNode={node.subNodeForm.data.node}
                descItemCopyFromPrevEnabled={descItemCopyFromPrevEnabled}
                closed={closed}
                onAddDescItemType={this.handleAddDescItemType}
            />
        } else {
            form = <Loading value={i18n('global.data.loading.form')}/>
        }

        var record;

        if (showRegisterJp) {
            record = <SubNodeRegister
                        nodeId={node.id}
                        versionId={versionId}
                        selectedSubNodeId={node.selectedSubNodeId}
                        nodeKey={node.nodeKey}
                        register={node.subNodeRegister}
                        closed={closed}/>
        }

        var cls = classNames({
            'node-panel-container': true,
        })

        return (
            <Shortcuts name='NodePanel' key={'node-panel'} className={cls} handler={this.handleShortcuts}>
                <div key='main' className='main'>
                    {actions}
                    {parents}
                    {this.renderAccordion(form, record)}
                    {children}
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
    const {arrRegion, developer, focus} = state
    return {
        nodeSettings: arrRegion.nodeSettings,
        developer,
        focus,
    }
}

NodePanel.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    fund: React.PropTypes.object.isRequired,
    node: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    nodeSettings: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    fundId: React.PropTypes.number,
    showRegisterJp: React.PropTypes.bool.isRequired,
    closed: React.PropTypes.bool.isRequired,
}

NodePanel.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
}

module.exports = connect(mapStateToProps)(NodePanel);
