/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */

import objectById from "../../shared/utils/objectById";

require('./NodeSubNodeForm.less');

import React from 'react';
import SubNodeForm from "./SubNodeForm";
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton} from 'components/shared';
import {connect} from 'react-redux'
import {lockDescItemType, unlockDescItemType, unlockAllDescItemType,
    copyDescItemType, nocopyDescItemType} from 'actions/arr/nodeSetting.jsx'
import {addNode,deleteNode} from '../../actions/arr/node.jsx'
import {isFundRootId} from './ArrUtils.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {nodeFormActions} from 'actions/arr/subNodeForm.jsx'
import {getOneSettings} from 'components/arr/ArrUtils.jsx';
import ArrHistoryForm from 'components/arr/ArrHistoryForm.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {WebApi} from 'actions/index.jsx';
import {getMapFromList} from 'stores/app/utils.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {fundSelectSubNode} from 'actions/arr/node.jsx';
import {addToastrSuccess, addToastr} from 'components/shared/toastr/ToastrActions.jsx';

class NodeSubNodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
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
     * Zobrazení formuláře historie JP.
     */
    handleShowHistory = () => {
        const {versionId, fund: {nodes}} = this.props;
        const node = nodes.nodes[nodes.activeIndex];
        const nodeObj = getMapFromList(node.childNodes)[node.selectedSubNodeId];
        const form = <ArrHistoryForm versionId={versionId} node={nodeObj} onDeleteChanges={this.handleDeleteChanges} />
        this.dispatch(modalDialogShow(this, i18n('arr.history.title'), form, "dialog-lg"));
    }

    handleDeleteChanges = (nodeId, fromChangeId, toChangeId) => {
        WebApi.revertChanges(this.props.versionId, nodeId, fromChangeId, toChangeId).then(() => {
            this.dispatch(modalDialogHide());
        });
    }
    /**
     * Vybere sousední nebo nadřazenou JP po smazání
     * @param {number} versionId
     * @param {object} prevNode
     * @param {func} callback - function(selectedParent) - funkci je předáván {bool} určující, zda byl vybrán rodič
     */
    selectSubnodeAfterDelete = (versionId, prevNode, callback) => {
        let outdatedParent = {...this.props.parentNode}; //Potřeba kvůli seznamu potomků
        let prevIndex = indexById(outdatedParent.childNodes, prevNode.id);
        let childNodeCount = outdatedParent.childNodes.length;
        let newNodeId = outdatedParent.childNodes[0];
        let selectedParent = false;
        if (childNodeCount <= 1){ //Pokud je posledním potomkem, přejde se na rodiče
            newNodeId = outdatedParent.id;
            outdatedParent = outdatedParent.parentNodes[0];
            selectedParent = true;
        } else if (prevIndex === 0){ //Pokud je smazaná JP první, bude jako další vybrána následující JP
            newNodeId = outdatedParent.childNodes[prevIndex + 1].id;
        } else { //Bude vybrána předcházející JP
            newNodeId = outdatedParent.childNodes[prevIndex - 1].id;
        }
        this.dispatch(fundSelectSubNode(versionId, newNodeId, outdatedParent));
        callback(selectedParent);
    }
    /**
     * Funkce zavolána po skončení smazání JP
     * @param {number} versionId
     * @param {object} prevNode
     * @param {object} parentNode
     */
    afterDeleteCallback = (versionId, prevNode, parentNode) => {
        this.selectSubnodeAfterDelete(versionId, prevNode, (selectedParent) => {
            this.dispatch(addToastrSuccess(i18n('arr.fund.deleteNode.deleted')));
            if(selectedParent){
                this.dispatch(addToastr(i18n('arr.fund.deleteNode.noSibling'), null,"info","lg",5000));
            }
        });

    }
    handleDeleteNode() {
        if (window.confirm(i18n('arr.fund.deleteNode.confirm'))) {
            this.dispatch(deleteNode(this.props.selectedSubNode, this.props.parentNode, this.props.versionId, this.afterDeleteCallback));
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
     * Renderování globálních akcí pro formulář.
     * @return {Object} view
     */
    renderFormActions() {
        const notRoot = !isFundRootId(this.props.nodeId);

        const {fundId, userDetail} = this.props;
        const historyAllowed = userDetail.hasOne(perms.FUND_ADMIN, {type: perms.FUND_VER_WR, fundId},
                                                 perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId});

        return (
            <div ref="nodeToolbar" className='node-form-actions-container'>
                <div className='node-form-actions'>
                    <div className='section'>
                        <NoFocusButton onClick={this.props.onAddDescItemType}><Icon glyph="fa-plus-circle"/>{i18n('subNodeForm.section.item')}</NoFocusButton>
                    </div>
                    <div className='section'>
                        <NoFocusButton onClick={this.handleDescItemTypeUnlockAll}>
                            <Icon glyph="fa-unlock"/>
                        </NoFocusButton>
                        <NoFocusButton onClick={this.props.onVisiblePolicy}>
                            <Icon glyph="fa-cogs"/>
                        </NoFocusButton>
                        {historyAllowed &&
                        <NoFocusButton onClick={this.handleShowHistory}>
                            <Icon glyph="fa-history"/>
                        </NoFocusButton>}
                        {notRoot &&
                        <NoFocusButton onClick={this.handleDeleteNode}>
                            <Icon glyph="fa-trash"/>
                        </NoFocusButton>}
                    </div>
                    <div className='section'>
                        <NoFocusButton onClick={this.props.onDigitizationRequest}>
                            <Icon glyph="fa-camera"/>
                            {i18n("subNodeForm.digitizationRequest")}
                        </NoFocusButton>
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
        const {versionId, focus, closed, fundId, routingKey, rulDataTypes, calendarTypes, descItemTypes, structureTypes,
            subNodeForm, conformityInfo, descItemCopyFromPrevEnabled, singleDescItemTypeId, readMode} = this.props;

        let formActions
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
                    typePrefix="desc"
                    structureTypes={structureTypes}
                    versionId={versionId}
                    fundId={fundId}
                    routingKey={routingKey}
                    nodeSetting={nodeSetting}
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                    descItemTypes={descItemTypes}
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
    const {arrRegion, focus, userDetail, refTables} = state;
    let fund = null;
    let structureTypes = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
        structureTypes = objectById(refTables.structureTypes.data, fund.versionId, "versionId");
    }

    return {
        nodeSettings: arrRegion.nodeSettings,
        structureTypes,
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
    structureTypes: React.PropTypes.object.isRequired,
    subNodeForm: React.PropTypes.object.isRequired,
    closed: React.PropTypes.bool.isRequired,
    conformityInfo: React.PropTypes.object.isRequired,
    descItemCopyFromPrevEnabled: React.PropTypes.bool.isRequired,
    focus: React.PropTypes.object.isRequired,
    userDetail: React.PropTypes.object.isRequired,
    onAddDescItemType: React.PropTypes.func.isRequired,
    onVisiblePolicy: React.PropTypes.func.isRequired,
    onDigitizationRequest: React.PropTypes.func.isRequired,
    singleDescItemTypeId: React.PropTypes.number,
    singleDescItemTypeEdit: React.PropTypes.bool,
    readMode: React.PropTypes.bool,
}

export default connect(mapStateToProps, null, null, { withRef: true })(NodeSubNodeForm);
