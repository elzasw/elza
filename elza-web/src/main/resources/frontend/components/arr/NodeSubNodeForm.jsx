/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */

import {notEmpty, objectById} from "../../shared/utils";
import * as factory from "../../shared/factory";
import React from 'react';
import SubNodeForm from "./SubNodeForm";
import {AbstractReactComponent, i18n, Icon, NoFocusButton} from 'components/shared';

import {connect} from 'react-redux'
import {
    copyDescItemType,
    lockDescItemType,
    nocopyDescItemType,
    unlockAllDescItemType,
    unlockDescItemType
} from 'actions/arr/nodeSetting.jsx'
import {deleteNode} from '../../actions/arr/node.jsx'
import {createFundRoot, isFundRootId} from './ArrUtils.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {nodeFormActions} from 'actions/arr/subNodeForm.jsx'
import {getOneSettings, setSettings} from 'components/arr/ArrUtils.jsx';
import ArrHistoryForm from 'components/arr/ArrHistoryForm.jsx'
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog.jsx'
import {WebApi} from 'actions/index.jsx';
import {getMapFromList, indexById} from 'stores/app/utils.jsx'
import {fundSelectSubNode} from 'actions/arr/node.jsx';
import {addToastr, addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {DropdownButton, MenuItem} from 'react-bootstrap';
import TemplateForm, {EXISTS_TEMPLATE, NEW_TEMPLATE} from "./TemplateForm";
import TemplateUseForm from "./TemplateUseForm";
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'
import DescItemFactory from "components/arr/nodeForm/DescItemFactory.jsx";

require('./NodeSubNodeForm.less');

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
        // if the old parent had only one child node
        if (childNodeCount <= 1){
            newNodeId = outdatedParent.id;
            let newParent = outdatedParent.parentNodes[0];
            const selectNearestParent = (parents)=>{
                newParent = parents[0];
                // if there are no parents even in the server response
                // set the newParent as the root node virtual parent
                if(!newParent){
                    if(outdatedParent.depth > 1){
                        console.warn("Missing parent on level deeper than root", outdatedParent);
                    }
                    // create virtual parent for fund root node
                    newParent = createFundRoot(this.props.fund);
                }
                this.dispatch(fundSelectSubNode(versionId, newNodeId, newParent));
                callback(true);
            };
            // if parent doesn't exist, get parents from server
            if(!newParent){
                WebApi.getNodeParents(versionId, newNodeId).then(selectNearestParent);
            }
            // if parent exists, use it
            else {
                selectNearestParent([newParent]);
            }
        } else {
            if (prevIndex === 0){ //Pokud je smazaná JP první, bude jako další vybrána následující JP
            newNodeId = outdatedParent.childNodes[prevIndex + 1].id;
        } else { //Bude vybrána předcházející JP
            newNodeId = outdatedParent.childNodes[prevIndex - 1].id;
        }
        this.dispatch(fundSelectSubNode(versionId, newNodeId, outdatedParent));
            callback(false);
    }
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
                    <div className='section'>
                        <DropdownButton bsStyle="default" title={<Icon glyph="fa-ellipsis-h" />} noCaret id="arr-structure-panel-add">
                            <MenuItem eventKey="1" onClick={this.handleCreateTemplate}>{i18n("subNodeForm.section.createTemplate")}</MenuItem>
                            <MenuItem eventKey="2" onClick={this.handleUseTemplate}>{i18n("subNodeForm.section.useTemplate")}</MenuItem>
                        </DropdownButton>
                    </div>
                </div>
            </div>
        )
    }

    handleCreateTemplate = () => {

        const {userDetail, fund} = this.props;

        let settings = userDetail.settings;

        const fundTemplates = getOneSettings(settings, 'FUND_TEMPLATES', 'FUND', fund.id);

        const initialValues = {
            type: NEW_TEMPLATE,
            withValues: true
        };

        const templates = fundTemplates.value ? JSON.parse(fundTemplates.value).map(template => template.name) : [];

        this.props.dispatch(modalDialogShow(this, i18n('arr.fund.addTemplate.create'), <TemplateForm initialValues={initialValues} templates={templates} onSubmitForm={(data) => {

            const template = this.createTemplate(data.name, data.withValues);

            switch (data.type) {
                case NEW_TEMPLATE: {
                    const value = fundTemplates.value ? [...JSON.parse(fundTemplates.value), template] : [template];
                    value.sort((a, b) => {
                        return a.name.localeCompare(b.name);
                    });
                    fundTemplates.value = JSON.stringify(value);
                    settings = setSettings(settings, fundTemplates.id, fundTemplates);
                    this.props.dispatch(userDetailsSaveSettings(settings));
                    return this.dispatch(modalDialogHide());
                }
                case EXISTS_TEMPLATE: {
                    const value = JSON.parse(fundTemplates.value);
                    const index = indexById(value, data.name, 'name');

                    if (index == null) {
                        console.error("Nebyla nalezena šablona s názvem: " + data.name);
                    } else {
                        value[index] = template;
                        fundTemplates.value = JSON.stringify(value);
                        settings = setSettings(settings, fundTemplates.id, fundTemplates);
                        this.props.dispatch(userDetailsSaveSettings(settings));
                    }

                    return this.dispatch(modalDialogHide());
                }
            }
        }} />));
    };

    createTemplate = (name, withValues) => {
        const {subNodeForm} = this.props;

        let formData = {};
        subNodeForm.formData.descItemGroups.forEach(group => {
            group.descItemTypes.forEach(type => {
                if (type.descItems.length > 0) {

                    const items = [];
                    type.descItems.forEach(item => {
                        const itemCls = factory.createClass(item);
                        const newItem = itemCls.copyItem(withValues);
                        newItem.strValue = itemCls.toSimpleString();
                        items.push(newItem);
                    });

                    formData[type.id] = items;
                }
            });

        });
        let template = {
            name: name,
            withValues: withValues,
            formData: formData
        };
        return template;
    };

    handleUseTemplate = () => {
        const {userDetail, fund, routingKey, selectedSubNode, subNodeForm} = this.props;

        let settings = userDetail.settings;

        const fundTemplates = getOneSettings(settings, 'FUND_TEMPLATES', 'FUND', fund.id);

        const initialValues = {
            replaceValues: false,
            name: templates.indexOf(fund.lastUseTemplateName) >= 0 ? fund.lastUseTemplateName : null
        };

        const templates = fundTemplates.value ? JSON.parse(fundTemplates.value).map(template => template.name) : [];

        this.props.dispatch(modalDialogShow(this, i18n('arr.fund.useTemplate.title'), <TemplateUseForm initialValues={initialValues} templates={templates} onSubmitForm={(data) => {
            const value = JSON.parse(fundTemplates.value);
            const index = indexById(value, data.name, 'name');

            if (index == null) {
                console.error("Nebyla nalezena šablona s názvem: " + data.name);
            } else {
                const template = value[index];
                console.debug("Apply template", template);

                const formData = template.formData;
                let createItems = [];
                let updateItems = [];
                let deleteItems = [];
                let deleteItemsAdded = {};

                const actualFormData = this.createFormData(subNodeForm);

                Object.keys(formData).map(itemTypeId => {
                    this.processItemType(formData, itemTypeId, actualFormData, data, deleteItemsAdded, deleteItems, updateItems, createItems);
                });

                Object.keys(deleteItemsAdded).map(itemObjectId => {
                    let updateItemsTemp = [];
                    for (let i = 0; i < updateItems.length; i++) {
                        const updateItem = updateItems[i];
                        if (itemObjectId === updateItem.descItemObjectId) {
                            createItems.push({
                                ...updateItem,
                                descItemObjectId: null
                            });
                        } else {
                            updateItemsTemp.push(updateItem);
                        }
                    }
                    updateItems = updateItemsTemp;
                });

                if (createItems.length > 0 || updateItems.length > 0 || deleteItems.length > 0) {
                    return WebApi.updateDescItems(fund.versionId, selectedSubNode.id, selectedSubNode.version, createItems, updateItems, deleteItems).then(() => {
                        this.dispatch(nodeFormActions.fundSubNodeFormTemplateUse(fund.versionId, routingKey, template, data.replaceValues));
                        return this.dispatch(modalDialogHide());
                    });
                } else {
                    this.dispatch(nodeFormActions.fundSubNodeFormTemplateUse(fund.versionId, routingKey, template, data.replaceValues));
                    return this.dispatch(modalDialogHide());
                }
            }
        }} />));

    };

    processItemType = (formData, itemTypeId, actualFormData, data, deleteItemsAdded, deleteItems, updateItems, createItems) => {
        const items = formData[itemTypeId];
        items.forEach(item => {
            const newItem = {
                ...item,
                itemTypeId: itemTypeId
            };

            if (notEmpty(newItem.value) || (newItem['@class'] === '.ArrItemEnumVO' && notEmpty(item.descItemSpecId))) {
                if (actualFormData[itemTypeId]) { // pokud existuje
                    this.processExistsItemType(itemTypeId, actualFormData, data, deleteItemsAdded, deleteItems, item, updateItems, createItems, newItem);
                } else { // pokud neexistuje, zakládáme nový
                    createItems.push(newItem);
                }
            }
        });
    };

    processExistsItemType = (itemTypeId, actualFormData, data, deleteItemsAdded, deleteItems, item, updateItems, createItems, newItem) => {
        const itemType = this.findItemType(itemTypeId);
        if (itemType.rep) { // je opakovatelný
            const existsItems = actualFormData[itemTypeId];
            if (data.replaceValues) {
                this.processItemsToDelete(existsItems, deleteItemsAdded, deleteItems);
            } else {
                const addAsNew = this.processRepetitiveItemsToUpdate(existsItems, item, itemTypeId, updateItems);
                if (addAsNew) {
                    createItems.push(newItem);
                }
            }
        } else { // není opakovatelný, pouze aktualizujeme hodnotu
            if (data.replaceValues) { // pouze pokud chceme existující hodnoty nahradit
                const itemOrig = actualFormData[itemTypeId][0];
                if (itemOrig.value !== item.value || itemOrig.descItemSpecId !== item.descItemSpecId) { // pouze pokud jsou odlišené
                    const updateItem = {
                        ...item,
                        descItemObjectId: itemOrig.descItemObjectId,
                        itemTypeId: itemTypeId
                    };
                    updateItems.push(updateItem);
                }
            }
        }
    };

    processItemsToDelete = (existsItems, deleteItemsAdded, deleteItems) => {
        for (let i = 0; i < existsItems.length; i++) {
            const existsItem = existsItems[i];
            const itemObjectId = existsItem.descItemObjectId;
            if (itemObjectId && !deleteItemsAdded[itemObjectId]) {
                deleteItemsAdded[itemObjectId] = true;
                deleteItems.push(existsItem);
            }
        }
    };

    processRepetitiveItemsToUpdate = (existsItems, item, itemTypeId, updateItems) => {
        let addAsNew = true;
        for (let i = 0; i < existsItems.length; i++) {
            const existsItem = existsItems[i];
            const changeValue = existsItem.value !== item.value || existsItem.undefined !== item.undefined; // pokud se liší hodnota (nebo nedefinovanost)

            if (existsItem.descItemSpecId === item.descItemSpecId) { // pokud je stejná specifikace
                if (!changeValue) {
                    addAsNew = false;
                }
            } else {
                const updateItem = {
                    ...item,
                    descItemObjectId: existsItem.descItemObjectId,
                    itemTypeId: itemTypeId
                };
                updateItems.push(updateItem);
            }
        }
        return addAsNew;
    };

    /**
     * Sestaví mapu typů atributů s hodnotami - pouze uložené.
     *
     * @param subNodeForm formulář
     */
    createFormData = (subNodeForm) => {
        const actualFormData = {};
        subNodeForm.formData.descItemGroups.forEach(group => {
            group.descItemTypes.forEach(type => {
                if (type.descItems.length > 0) {
                    let adding = false;
                    type.descItems.forEach(item => {
                        if (item.descItemObjectId) {
                            adding = true;
                        }
                    });
                    if (adding) {
                        actualFormData[type.id] = type.descItems;
                    }
                }
            });
        });
        return actualFormData;
    };

    findItemType = (itemTypeId) => {
        const {subNodeForm, groups} = this.props;
        const groupCode = groups.reverse[itemTypeId];
        const group = objectById(subNodeForm.formData.descItemGroups, groupCode, 'code');
        return objectById(group.types, itemTypeId);

    };

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
                    descItemFactory={DescItemFactory}
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
        groups: refTables.groups.data
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
