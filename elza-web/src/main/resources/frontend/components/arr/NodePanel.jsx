/**
 * Komponenta panelu formuláře jedné JP.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Icon, AbstractReactComponent, i18n, Loading, SubNodeForm, Accordion, SubNodeRegister, AddNodeDropdown, Search} from 'components';
import {Button, Tooltip, OverlayTrigger, Input} from 'react-bootstrap';
import {faSubNodeFormFetchIfNeeded} from 'actions/arr/subNodeForm'
import {faSubNodeRegisterFetchIfNeeded} from 'actions/arr/subNodeRegister'
import {faSubNodeInfoFetchIfNeeded} from 'actions/arr/subNodeInfo'
import {faNodeInfoFetchIfNeeded} from 'actions/arr/nodeInfo'
import {faSelectSubNode, faSubNodesNext, faSubNodesPrev, faSubNodesNextPage, faSubNodesPrevPage} from 'actions/arr/nodes'
import {faNodeSubNodeFulltextSearch} from 'actions/arr/node'
import {addNode} from 'actions/arr/node'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes'
import {indexById} from 'stores/app/utils.jsx'
import {createFaRoot, isFaRootId} from './ArrUtils.jsx'
import {propsEquals} from 'components/Utils'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
import {createReferenceMarkString, getGlyph} from 'components/arr/ArrUtils'
const scrollIntoView = require('dom-scroll-into-view')

require ('./NodePanel.less');

var NodePanel = class NodePanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderParents', 'renderRow',
            'renderChildren', 'handleOpenItem',
            'handleCloseItem', 'handleParentNodeClick', 'handleChildNodeClick',
            'getParentNodes', 'getChildNodes', 'getSiblingNodes',
            'renderAccordion', 'renderState', 'transformConformityInfo', 'handleAddNodeAtEnd',
            'handleChangeFilterText'
            );

        this.state = {
            filterText: props.node.filterText
        }
    }

    componentDidMount() {
        this.requestData(this.props.versionId, this.props.node);
        this.dispatch(calendarTypesFetchIfNeeded());
        this.ensureItemVisible();
    }

    componentWillReceiveProps(nextProps) {
        this.requestData(nextProps.versionId, nextProps.node, nextProps.showRegisterJp);
        this.dispatch(calendarTypesFetchIfNeeded());

        var newState = {
            filterText: nextProps.node.filterText
        }

        var scroll = false;
        if (!this.props.node.nodeInfoFetched && nextProps.node.nodeInfoFetched) {
            scroll = true;
        } else if (nextProps.node.selectedSubNodeId !== null && this.props.node.selectedSubNodeId !== nextProps.node.selectedSubNodeId) {
            scroll = true;
        }
        if (scroll) {
            this.setState(newState, this.ensureItemVisible)
        } else {
            this.setState(newState);
        }
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
                var contentNode = ReactDOM.findDOMNode(this.refs.content)
                scrollIntoView(itemNode, contentNode, { onlyScrollIfNeeded: true, alignWithTop:true })
            }
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['versionId', 'fa', 'node', 'calendarTypes',
            'packetTypes', 'packets', 'rulDataTypes', 'findingAidId', 'showRegisterJp', 'closed']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    /**
     * Načtení dat, pokud je potřeba.
     * @param versionId {String} verze AP
     * @param node {Object} node
     * @param showRegisterJp {bool} zobrazení rejstřílů vázené k jednotce popisu
     */
    requestData(versionId, node, showRegisterJp) {
        if (node.selectedSubNodeId != null) {
            this.dispatch(faSubNodeFormFetchIfNeeded(versionId, node.selectedSubNodeId, node.nodeKey));
            this.dispatch(faSubNodeInfoFetchIfNeeded(versionId, node.selectedSubNodeId, node.nodeKey));
            this.dispatch(refRulDataTypesFetchIfNeeded());

            if (showRegisterJp) {
                this.dispatch(faSubNodeRegisterFetchIfNeeded(versionId, node.selectedSubNodeId, node.nodeKey));
            }

        }
        this.dispatch(faNodeInfoFetchIfNeeded(versionId, node.id, node.nodeKey));
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
            subNodeParentNode = createFaRoot(this.props.fa);
        }

        this.dispatch(faSelectSubNode(subNodeId, subNodeParentNode, false, null, true));
    }

    /**
     * Kliknutí na položku v seznamu podřízených NODE.
     * @param node {Object} node na který se kliklo
     */
    handleChildNodeClick(node) {
        var subNodeId = node.id;
        var subNodeParentNode = this.getSiblingNodes()[indexById(this.getSiblingNodes(), this.props.node.selectedSubNodeId)];
        this.dispatch(faSelectSubNode(subNodeId, subNodeParentNode, false, null, true));
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
        var rows = items.map(item => {
            var icon = <Icon className="node-icon" glyph={getGlyph(item.icon)} />
            var levels = <span className="reference-mark">{createReferenceMarkString(item)}</span>
            var name = item.name ? item.name : <i>{i18n('faTree.node.name.undefined', item.id)}</i>;
            name = <span title={name} className="name">{name}</span>

            return (
                    <div key={item.id} className='node' onClick={onClick.bind(this, item)}>
                        {icon} {levels} {name}
                    </div>
            )
        });

        return (
                <div key={key} className={myClass}>
                    {rows}
                </div>
        )
    }

    /**
     * Načtení seznamu nadřízených NODE.
     * @return {Array} seznam NODE
     */
    getParentNodes() {
        const {node} = this.props;
        if (isFaRootId(node.id)) {
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
        this.dispatch(faSelectSubNode(null, this.props.node, false, null, false));
    }

    /**
     * Vybrání a otevření položky Accordion.
     * @param item {Object} na který node v Accordion se kliklo
     */
    handleOpenItem(item) {
        var subNodeId = item.id;
        this.dispatch(faSelectSubNode(subNodeId, this.props.node, false, null, true));
    }

    /**
     * Přidání JP na konec do aktuálního node
     * Využito v dropdown buttonu pro přidání node
     *
     * @param event Event selectu
     * @param scenario name vybraného scénáře
     */
    handleAddNodeAtEnd(event, scenario) {
        this.dispatch(addNode(this.props.node, this.props.node, this.props.fa.versionId, "CHILD", this.getDescItemTypeCopyIds(), scenario));
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

            var description = (item.nodeConformity.description) ? "<br />" + item.nodeConformity.description : "";
            var messages = new Array();

            var errors = item.nodeConformity.errorList;
            var missings = item.nodeConformity.missingList;

            if (errors && errors.length > 0) {
                messages.push(<div key="errors" className="error">Chyby</div>);
                errors.forEach(error => { messages.push(<div key={'err' + _id++} className="message">{error.description}</div>) });
            }

            if (missings && missings.length > 0) {
                messages.push(<div key="missings" className="missing">Chybějící</div>);
                missings.forEach(missing => { messages.push(<div key={'mis' + _id++}  className="message">{missing.description}</div>) });
            }

            if (item.nodeConformity.state === "OK") {
                icon = <Icon glyph="fa-check" />
                tooltip = <Tooltip id="status-ok">{i18n('arr.node.status.ok') + description}</Tooltip>
            } else {
                icon = <Icon glyph="fa-exclamation-circle" />
                tooltip = <Tooltip id="status-err">{i18n('arr.node.status.err')} {description} {messages}</Tooltip>
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
        const {node} = this.props;

        var rows = [];

        if (node.viewStartIndex > 0) {
            rows.push(
                <Button key="prev" onClick={()=>this.dispatch(faSubNodesPrev())}><Icon glyph="fa-chevron-left" />{i18n('arr.fa.prev')}</Button>
            )
        }

        for (var a=node.viewStartIndex; (a<node.viewStartIndex + node.pageSize) && (a < node.childNodes.length); a++) {
            var item = node.childNodes[a];

            var state = this.renderState(item);
            var accordionLeft = item.accordionLeft ? item.accordionLeft : i18n('accordion.title.left.name.undefined', item.id)
            var accordionRight = item.accordionRight ? item.accordionRight : ''
            var referenceMark = <span className="reference-mark">{createReferenceMarkString(item)}</span>

            if (node.selectedSubNodeId == item.id) {
                rows.push(
                    <div key={item.id} ref={'accheader-' + item.id} className='accordion-item opened'>
                        <div className='accordion-header' onClick={this.handleCloseItem.bind(this, item)}>
                            <div title={accordionLeft} className='accordion-header-left' key='accordion-header-left'>
                                {referenceMark} <span className="title" title={accordionLeft}>{accordionLeft}</span>
                            </div>
                            <div title={accordionRight} className='accordion-header-right' key='accordion-header-right'>
                                <span className="title" title={accordionRight}>{accordionRight}</span> {state}
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
                    <div key={item.id} ref={'accheader-' + item.id} className='accordion-item closed'>
                        <div className='accordion-header' onClick={this.handleOpenItem.bind(this, item)}>
                            <div title={accordionLeft} className='accordion-header-left' key='accordion-header-left'>
                                {referenceMark} <span className="title" title={accordionLeft}>{accordionLeft}</span>
                            </div>
                            <div title={accordionRight} className='accordion-header-right' key='accordion-header-right'>
                                <span className="title" title={accordionRight}>{accordionRight}</span> {state}
                            </div>
                        </div>
                    </div>
                )
            }
        }

        if (node.viewStartIndex + node.pageSize/2 < node.childNodes.length) {
            rows.push(
                <Button key="next" onClick={()=>this.dispatch(faSubNodesNext())}><Icon glyph="fa-chevron-right" />{i18n('arr.fa.next')}</Button>
            )
        }

        return rows;
    }

    render() {
        const {calendarTypes, versionId, rulDataTypes, node, packetTypes, packets, findingAidId, showRegisterJp, fa, closed} = this.props;

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
            <div key='actions' className='actions'>
                {
                    node.nodeInfoFetched && !isFaRootId(node.id) && !closed &&
                    <AddNodeDropdown key="end"
                                     title="Přidat JP na konec"
                                     glyph="fa-plus-circle"
                                     action={this.handleAddNodeAtEnd}
                                     node={this.props.node}
                                     version={fa.versionId}
                                     direction="CHILD"
                    />
                }
                <div className='btn btn-default' disabled={node.viewStartIndex == 0} onClick={()=>this.dispatch(faSubNodesPrevPage())}><Icon glyph="fa-backward" />{i18n('arr.fa.subNodes.prevPage')}</div>
                <div className='btn btn-default' disabled={node.viewStartIndex + node.pageSize >= node.childNodes.length} onClick={()=>this.dispatch(faSubNodesNextPage())}><Icon glyph="fa-forward" />{i18n('arr.fa.subNodes.nextPage')}</div>

                <Search
                    className='search-input'
                    placeholder={i18n('search.input.search')}
                    filterText={this.props.filterText}
                    value={this.state.filterText}
                    onChange={(e) => this.handleChangeFilterText(e.target.value)}
                    onClear={() => {this.handleChangeFilterText(''); this.dispatch(faNodeSubNodeFulltextSearch(this.state.filterText))}}
                    onSearch={() => {this.dispatch(faNodeSubNodeFulltextSearch(this.state.filterText))}}
                />
                {false &&
                <div className="search-input">
                    <Input type="text" onChange={this.handleChangeFilterText} value={this.state.filterText}/>
                    <Button onClick={() => {this.dispatch(faNodeSubNodeFulltextSearch(this.state.filterText))}}><Icon glyph='fa-search'/></Button>
                </div>}
            </div>
        )

        var form;
        if (node.subNodeForm.fetched && calendarTypes.fetched) {
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
                nodeId={node.id}
                versionId={versionId}
                selectedSubNodeId={node.selectedSubNodeId}
                nodeKey={node.nodeKey}
                formData={node.subNodeForm.formData}
                descItemTypeInfos={node.subNodeForm.descItemTypeInfos}
                rulDataTypes={rulDataTypes}
                calendarTypes={calendarTypes}
                packetTypes={packetTypes}
                conformityInfo={conformityInfo}
                packets={packets}
                parentNode={node}
                findingAidId={findingAidId}
                selectedSubNode={node.subNodeForm.data.node}
                descItemCopyFromPrevEnabled={descItemCopyFromPrevEnabled}
                closed={closed}
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

        var accordionInfo = <div>
            {node.viewStartIndex}-{node.viewStartIndex + node.pageSize} [{node.childNodes.length}]
        </div>

        return (
            <div className='node-panel-container'>
                {false && accordionInfo}
                {actions}
                {parents}
                <div key='content' className='content' ref='content'>
                    {this.renderAccordion(form, record)}
                </div>
                {children}
            </div>
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
            var errors = nodeState.errorList;
            if (errors && errors.length > 0) {
                errors.forEach(error => {
                    if (conformityInfo.errors[error.descItemObjectId] == null) {
                        conformityInfo.errors[error.descItemObjectId] = new Array();
                    }
                    conformityInfo.errors[error.descItemObjectId].push(error);
                });
            }

            var missings = nodeState.missingList;
            if (missings && missings.length > 0) {
                missings.forEach(missing => {
                    if (conformityInfo.missings[missing.descItemTypeId] == null) {
                        conformityInfo.missings[missing.descItemTypeId] = new Array();
                    }
                    conformityInfo.missings[missing.descItemTypeId].push(missing);
                });
            }
        }
        return conformityInfo;
    }
}

function mapStateToProps(state) {
    const {arrRegion} = state
    return {
        nodeSettings: arrRegion.nodeSettings
    }
}

NodePanel.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    fa: React.PropTypes.object.isRequired,
    node: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    nodeSettings: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    findingAidId: React.PropTypes.number,
    showRegisterJp: React.PropTypes.bool.isRequired,
    closed: React.PropTypes.bool.isRequired,
}

module.exports = connect(mapStateToProps)(NodePanel);
