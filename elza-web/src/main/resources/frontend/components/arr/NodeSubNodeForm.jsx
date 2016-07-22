/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */

require('./NodeSubNodeForm.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, AddPacketForm, AddPartyForm, AddRegistryForm,
    AddPartyEventForm, AddPartyGroupForm, AddPartyDynastyForm, AddPartyOtherForm, AddNodeDropdown} from 'components';
import {connect} from 'react-redux'
import {indexById} from 'stores/app/utils.jsx'
import {lockDescItemType, unlockDescItemType, unlockAllDescItemType,
    copyDescItemType, nocopyDescItemType} from 'actions/arr/nodeSetting.jsx'
import {addNode,deleteNode} from '../../actions/arr/node.jsx'
import {isFundRootId} from './ArrUtils.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {SubNodeForm} from "components/index.jsx";
import {nodeFormActions} from 'actions/arr/subNodeForm.jsx'
import {getOneSettings} from 'components/arr/ArrUtils.jsx';

const NodeSubNodeForm = class NodeSubNodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        
        this.bindMethods(
            "handleAddNodeBefore",
            "handleAddNodeAfter",
            "handleAddChildNode",
            "renderFormActions",
            "handleDeleteNode",
            "handleDescItemTypeCopyFromPrev",
            "handleDescItemTypeLock",
            "handleDescItemTypeCopy",
            "handleDescItemTypeUnlockAll",
            "getNodeSetting",
            "initFocus",
        );
    }

    getNodeSetting() {
        const {nodeSettings, nodeId} = this.props;

        let nodeSetting
        if (nodeSettings) {
            nodeSetting = nodeSettings.nodes[nodeSettings.nodes.map(function (node) {
                return node.id;
            }).indexOf(nodeId)];
        }

        return nodeSetting
    }    
    
    /**
     * Odebrání všech zámků pro všechny atributy
     */
    handleDescItemTypeUnlockAll() {
        this.dispatch(unlockAllDescItemType(this.props.nodeId));
    }    
    
    /**
     * Přidání/odebrání opakovaného pro atribut.
     * @param descItemTypeId {String} id atributu
     * @param copy {Boolean} true, pokud se má opakované kopírování povolit
     */
    handleDescItemTypeCopy(descItemTypeId, copy) {
        if (copy) {
            this.dispatch(copyDescItemType(this.props.nodeId, descItemTypeId));
        } else {
            this.dispatch(nocopyDescItemType(this.props.nodeId, descItemTypeId));
        }
    }    
    
    /**
     * Přidání/odebrání zámku pro atribut.
     * @param descItemTypeId {String} id atributu
     * @param locked {Boolean} true, pokud se má zámek povolit
     */
    handleDescItemTypeLock(descItemTypeId, locked) {
        if (locked) {
            this.dispatch(lockDescItemType(this.props.nodeId, descItemTypeId));
        } else {
            this.dispatch(unlockDescItemType(this.props.nodeId, descItemTypeId));
        }
    }    

    /**
     * @param event Event selectu
     * @param scenario id vybraného scénáře
     *
     * Přidání node před aktuální node a následovné vybrání
     * Využito v dropdown buttonu pro přidání node
     */
    handleAddNodeBefore(scenario) {
        this.dispatch(addNode(this.props.selectedSubNode, this.props.parentNode, this.props.versionId, "BEFORE", this.getDescItemTypeCopyIds(), scenario));
    }

    /**
     * @param event Event selectu
     * @param scenario name vybraného scénáře
     *
     * Přidání node za aktuální node a následovné vybrání
     * Využito v dropdown buttonu pro přidání node
     */
    handleAddNodeAfter(scenario) {
        this.dispatch(addNode(this.props.selectedSubNode, this.props.parentNode, this.props.versionId, "AFTER", this.getDescItemTypeCopyIds(), scenario))
    }

    /**
     * @param event Event selectu
     * @param scenario id vybraného scénáře
     *
     * Přidání podřízeného záznamu
     */
    handleAddChildNode(scenario) {
        this.dispatch(addNode(this.props.selectedSubNode, this.props.selectedSubNode, this.props.versionId, "CHILD", this.getDescItemTypeCopyIds(), scenario));
    }

    handleDeleteNode() {
        if (window.confirm('Opravdu chcete smazat tento JP?')) {
            this.dispatch(deleteNode(this.props.selectedSubNode, this.props.parentNode, this.props.versionId));
        }
    }

    /**
     * Akce okamžitého kopírování hodnot atributu z předcházející JP.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemTypeId {Integer} id desc item type
     */
    handleDescItemTypeCopyFromPrev(descItemGroupIndex, descItemTypeIndex, descItemTypeId) {
        const {routingKey} = this.props

        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
        }
        this.dispatch(nodeFormActions.fundSubNodeFormValuesCopyFromPrev(this.props.versionId, this.props.selectedSubNode.id, this.props.selectedSubNode.version, descItemTypeId, routingKey, valueLocation));
    }    
    
    /**
     * Vrátí pole ke zkopírování
     */
    getDescItemTypeCopyIds() {
        let itemsToCopy = null;
        if (this.props.nodeSettings != "undefined") {
            const nodeIndex = indexById(this.props.nodeSettings.nodes, this.props.nodeId);
            if (nodeIndex != null) {
                itemsToCopy = this.props.nodeSettings.nodes[nodeIndex].descItemTypeCopyIds;
            }
        }
        return itemsToCopy;
    }

    /**
     * Renderování globálních akcí pro formulář.
     * @return {Object} view
     */
    renderFormActions() {
        const notRoot = !isFundRootId(this.props.nodeId);
        return (
            <div className='node-form-actions-container'>
                <div className='node-form-actions'>
                    <div className='section'>
                        <span>{i18n('subNodeForm.section.jp')}</span>
                        {notRoot &&
                            [<AddNodeDropdown key="before"
                                             ref='addNodeBefore'
                                             action={this.handleAddNodeBefore}
                                             title={<span className="fa-stack fa-stack-right-bottom"><Icon glyph='fa-plus'/><Icon glyph='fa-arrow-up'/></span>}
                                             node={this.props.selectedSubNode}
                                             version={this.props.versionId}
                                             direction="BEFORE"
                            />,
                            <AddNodeDropdown key="after"
                                             ref='addNodeAfter'
                                             title={<span className="fa-stack fa-stack-right-bottom"><Icon glyph='fa-plus'/><Icon glyph='fa-arrow-down'/></span>}
                                             action={this.handleAddNodeAfter}
                                             node={this.props.selectedSubNode}
                                             version={this.props.versionId}
                                             direction="AFTER"
                            />]
                        }
                        <AddNodeDropdown key="child"
                                     ref='addNodeChild'
                                     title={<span className="fa-stack fa-stack-right-bottom"><Icon glyph='fa-plus'/><Icon glyph='fa-arrow-right'/></span>}
                                     action={this.handleAddChildNode}
                                     node={this.props.selectedSubNode}
                                     version={this.props.versionId}
                                     direction="CHILD"
                        />
                        {notRoot && <NoFocusButton onClick={this.handleDeleteNode}><Icon glyph="fa-trash"/></NoFocusButton>}
                    </div>
                    <div className='section'>
                        <span>{i18n('subNodeForm.section.item')}</span>
                        <NoFocusButton onClick={this.props.onAddDescItemType}><Icon glyph="fa-plus"/></NoFocusButton>
                        <NoFocusButton onClick={this.handleDescItemTypeUnlockAll}><Icon glyph="fa-unlock"/></NoFocusButton>
                    </div>
                    <div className='section'>
                        <NoFocusButton onClick={this.props.onVisiblePolicy}><Icon glyph="fa-eye"/>{i18n('subNodeForm.visiblePolicy')}</NoFocusButton>
                    </div>
                </div>
            </div>
        )
    }

    initFocus() {
        this.refs.subNodeForm.getWrappedInstance().initFocus();
    }

    render() {
        const {singleDescItemTypeEdit, userDetail} = this.props;
        const {versionId, focus, closed, fundId, routingKey, rulDataTypes, calendarTypes, descItemTypes, packetTypes, packets,
            subNodeForm, conformityInfo, descItemCopyFromPrevEnabled, singleDescItemTypeId} = this.props;
        
        let formActions

        var settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', fundId);
        var settingsValues = settings.value != 'false';
        const readMode = closed || settingsValues;

        if (userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId})) {
            if (!readMode && !singleDescItemTypeEdit) {
                formActions = this.renderFormActions();
            }
        }

        const nodeSetting = this.getNodeSetting();

        return (
            <div className="node-item-form-container">
                {formActions}
                <SubNodeForm
                    ref="subNodeForm"
                    versionId={versionId}
                    fundId={fundId}
                    routingKey={routingKey}
                    nodeSetting={nodeSetting}
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                    descItemTypes={descItemTypes}
                    packetTypes={packetTypes}
                    packets={packets}
                    subNodeForm={subNodeForm}
                    closed={closed}
                    conformityInfo={conformityInfo}
                    descItemCopyFromPrevEnabled={descItemCopyFromPrevEnabled}
                    focus={focus}
                    singleDescItemTypeId={singleDescItemTypeId}
                    singleDescItemTypeEdit={singleDescItemTypeEdit}
                    onDescItemTypeCopyFromPrev={this.handleDescItemTypeCopyFromPrev}
                    onDescItemTypeLock={this.handleDescItemTypeLock}
                    onDescItemTypeCopy={this.handleDescItemTypeCopy}
                    formActions={nodeFormActions}
                    readMode={readMode}
                    showNodeAddons={true}
                    />
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion, focus, userDetail} = state
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }

    return {
        nodeSettings: arrRegion.nodeSettings,
        fund,
        focus,
        userDetail,
    }
}

NodeSubNodeForm.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    fundId: React.PropTypes.number.isRequired,
    parentNode: React.PropTypes.object.isRequired,
    selectedSubNode: React.PropTypes.object.isRequired,
    selectedSubNodeId: React.PropTypes.number.isRequired,
    routingKey: React.PropTypes.string.isRequired,
    nodeId: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.string]),
    nodeSettings: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    subNodeForm: React.PropTypes.object.isRequired,
    closed: React.PropTypes.bool.isRequired,
    conformityInfo: React.PropTypes.object.isRequired,
    descItemCopyFromPrevEnabled: React.PropTypes.bool.isRequired,
    focus: React.PropTypes.object.isRequired,
    userDetail: React.PropTypes.object.isRequired,
    onAddDescItemType: React.PropTypes.func.isRequired,
    onVisiblePolicy: React.PropTypes.func.isRequired,
    singleDescItemTypeId: React.PropTypes.number,
    singleDescItemTypeEdit: React.PropTypes.bool,
}

module.exports = connect(mapStateToProps, null, null, { withRef: true })(NodeSubNodeForm);
