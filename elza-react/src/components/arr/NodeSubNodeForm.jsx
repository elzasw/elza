import {notEmpty} from '../../shared/utils';
import {objectByProperty} from 'stores/app/utils';
import * as factory from '../../shared/factory';
import PropTypes from 'prop-types';

import React from 'react';
import SubNodeForm from './SubNodeForm';
import {AbstractReactComponent, i18n, Icon, NoFocusButton} from 'components/shared';

import {connect} from 'react-redux';
import {
    copyDescItemType,
    lockDescItemType,
    nocopyDescItemType,
    toggleCopyAllDescItemType,
    unlockAllDescItemType,
    unlockDescItemType,
} from 'actions/arr/nodeSetting';
import {deleteNode} from '../../actions/arr/node';
import {createFundRoot, isFundRootId} from './ArrUtils';
import * as perms from 'actions/user/Permission';
import {nodeFormActions} from 'actions/arr/subNodeForm';
import {getOneSettings, setSettings} from 'components/arr/ArrUtils';
import ArrHistoryForm from 'components/arr/ArrHistoryForm';
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog';
import {WebApi} from 'actions/index';
import {getMapFromList, indexById} from 'stores/app/utils';
import {fundSelectSubNode} from 'actions/arr/node';
import {addToastr, addToastrSuccess} from 'components/shared/toastr/ToastrActions';
import {Dropdown, DropdownButton} from 'react-bootstrap';
import TemplateForm, {EXISTS_TEMPLATE, NEW_TEMPLATE} from './TemplateForm';
import TemplateUseForm from './TemplateUseForm';
import {userDetailsSaveSettings} from 'actions/user/userDetail';
import DescItemFactory from 'components/arr/nodeForm/DescItemFactory';
import {CLS, CLS_ITEM_ENUM} from '../../shared/factory/factoryConsts';
import storeFromArea from '../../shared/utils/storeFromArea';
import * as issuesActions from '../../actions/arr/issues';
import IssueForm from '../form/IssueForm';
import {objectEqualsDiff} from 'components/Utils';
import {NODE_SUB_NODE_FORM_CMP} from '../../stores/app/arr/subNodeForm';

import './NodeSubNodeForm.scss';
import {JAVA_ATTR_CLASS, JAVA_CLASS_ARR_DIGITIZATION_FRONTDESK_SIMPLE_VO, ItemClass} from '../../constants';

import {refExternalSystemsFetchIfNeeded} from 'actions/refTables/externalSystems';

import {TextFragmentsWindow} from "../../components/arr/text-fragments";

/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */

class NodeSubNodeForm extends AbstractReactComponent {
    static propTypes = {
        versionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        parentNode: PropTypes.object.isRequired,
        selectedSubNode: PropTypes.object.isRequired,
        selectedSubNodeId: PropTypes.number.isRequired,
        routingKey: PropTypes.string.isRequired,
        nodeId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
        nodeSettings: PropTypes.object.isRequired,
        rulDataTypes: PropTypes.object.isRequired,
        calendarTypes: PropTypes.object.isRequired,
        descItemTypes: PropTypes.object.isRequired,
        structureTypes: PropTypes.object.isRequired,
        subNodeForm: PropTypes.object.isRequired,
        closed: PropTypes.bool.isRequired,
        conformityInfo: PropTypes.object.isRequired,
        descItemCopyFromPrevEnabled: PropTypes.bool.isRequired,
        focus: PropTypes.object.isRequired,
        userDetail: PropTypes.object.isRequired,
        onAddDescItemType: PropTypes.func.isRequired,
        onVisiblePolicy: PropTypes.func.isRequired,
        onDigitizationRequest: PropTypes.func.isRequired,
        onDigitizationSync: PropTypes.func.isRequired,
        singleDescItemTypeId: PropTypes.number,
        singleDescItemTypeEdit: PropTypes.bool,
        readMode: PropTypes.bool,
        arrPerm: PropTypes.bool,
    };

    refSubNodeForm = null;

    constructor(props) {
        super(props);

        this.bindMethods(
            'renderFormActions',
            'handleDeleteNode',
            'handleDescItemTypeCopyFromPrev',
            'handleDescItemTypeLock',
            'handleDescItemTypeCopy',
            'handleDescItemTypeUnlockAll',
            'handleCopyAll',
            'getNodeSetting',
            'initFocus',
        );
    }

    shouldComponentUpdate(nextProps, nextState) {
        return true;
        if (this.state !== nextState) {
            return true;
        } else {
            const log = false;
            return (
                !objectEqualsDiff(this.props.subNodeForm, nextProps.subNodeForm, NODE_SUB_NODE_FORM_CMP, '', log) ||
                !objectEqualsDiff(
                    this.props.descItemCopyFromPrevEnabled,
                    nextProps.descItemCopyFromPrevEnabled,
                    {},
                    '',
                    log,
                ) ||
                !objectEqualsDiff(this.props.focus, nextProps.focus, {}, '', log) ||
                !objectEqualsDiff(this.props.nodeSettings, nextProps.nodeSettings, {}, '', log) ||
                !objectEqualsDiff(this.props.readMode, nextProps.readMode, {}, '', log)
            );
        }
    }

    componentDidMount(){
        this.props.dispatch(refExternalSystemsFetchIfNeeded());
    }

    UNSAFE_componentWillReceiveProps() {
        this.props.dispatch(refExternalSystemsFetchIfNeeded());
    }

    getNodeSetting() {
        const {nodeSettings, nodeId} = this.props;

        let nodeSetting;
        if (nodeSettings) {
            nodeSetting =
                nodeSettings.nodes[
                    nodeSettings.nodes
                        .map(function(node) {
                            return node.id;
                        })
                        .indexOf(nodeId)
                ];
        }

        return nodeSetting;
    }

    /**
     * Odebrání všech zámků pro všechny atributy
     */
    handleDescItemTypeUnlockAll() {
        this.props.dispatch(unlockAllDescItemType(this.props.nodeId));
    }

    /**
     * Odebrání všech zámků pro všechny atributy
     */
    handleCopyAll() {
        this.props.dispatch(toggleCopyAllDescItemType(this.props.nodeId));
    }

    /**
     * Přidání/odebrání opakovaného pro atribut.
     * @param descItemTypeId {String} id atributu
     * @param copy {Boolean} true, pokud se má opakované kopírování povolit
     */
    handleDescItemTypeCopy(descItemTypeId, copy) {
        if (copy) {
            this.props.dispatch(copyDescItemType(this.props.nodeId, descItemTypeId));
        } else {
            this.props.dispatch(nocopyDescItemType(this.props.nodeId, descItemTypeId));
        }
    }

    /**
     * Přidání/odebrání zámku pro atribut.
     * @param descItemTypeId {String} id atributu
     * @param locked {Boolean} true, pokud se má zámek povolit
     */
    handleDescItemTypeLock(descItemTypeId, locked) {
        if (locked) {
            this.props.dispatch(lockDescItemType(this.props.nodeId, descItemTypeId));
        } else {
            this.props.dispatch(unlockDescItemType(this.props.nodeId, descItemTypeId));
        }
    }

    /**
     * Zobrazení formuláře historie JP.
     */
    handleShowHistory = () => {
        const {
            versionId,
            fund: {nodes},
        } = this.props;
        const node = nodes.nodes[nodes.activeIndex];
        const nodeObj = getMapFromList(node.childNodes)[node.selectedSubNodeId];
        const form = <ArrHistoryForm versionId={versionId} node={nodeObj} onDeleteChanges={this.handleDeleteChanges} />;
        this.props.dispatch(modalDialogShow(this, i18n('arr.history.title'), form, 'dialog-lg'));
    };

    handleDeleteChanges = (nodeId, fromChangeId, toChangeId) => {
        WebApi.revertChanges(this.props.versionId, nodeId, fromChangeId, toChangeId).then(() => {
            this.props.dispatch(modalDialogHide());
        });
    };
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
        if (childNodeCount <= 1) {
            newNodeId = outdatedParent.id;
            let newParent = outdatedParent.parentNodes[0];
            const selectNearestParent = parents => {
                newParent = parents[0];
                // if there are no parents even in the server response
                // set the newParent as the root node virtual parent
                if (!newParent) {
                    if (outdatedParent.depth > 1) {
                        console.warn('Missing parent on level deeper than root', outdatedParent);
                    }
                    // create virtual parent for fund root node
                    newParent = createFundRoot(this.props.fund);
                }
                this.props.dispatch(fundSelectSubNode(versionId, newNodeId, newParent));
                callback(true);
            };
            // if parent doesn't exist, get parents from server
            if (!newParent) {
                WebApi.getNodeParents(versionId, newNodeId).then(selectNearestParent);
            }
            // if parent exists, use it
            else {
                selectNearestParent([newParent]);
            }
        } else {
            if (prevIndex === 0) {
                //Pokud je smazaná JP první, bude jako další vybrána následující JP
                newNodeId = outdatedParent.childNodes[prevIndex + 1].id;
            } else {
                //Bude vybrána předcházející JP
                newNodeId = outdatedParent.childNodes[prevIndex - 1].id;
            }
            this.props.dispatch(fundSelectSubNode(versionId, newNodeId, outdatedParent));
            callback(false);
        }
    };
    /**
     * Funkce zavolána po skončení smazání JP
     * @param {number} versionId
     * @param {object} prevNode
     * @param {object} parentNode
     */
    afterDeleteCallback = (versionId, prevNode, parentNode) => {
        this.selectSubnodeAfterDelete(versionId, prevNode, selectedParent => {
            this.props.dispatch(addToastrSuccess(i18n('arr.fund.deleteNode.deleted')));
            if (selectedParent) {
                this.props.dispatch(addToastr(i18n('arr.fund.deleteNode.noSibling'), null, 'info', 'lg', 5000));
            }
        });
    };

    handleDeleteNode() {
        if (window.confirm(i18n('arr.fund.deleteNode.confirm'))) {
            this.props.dispatch(
                deleteNode(
                    this.props.selectedSubNode,
                    this.props.parentNode,
                    this.props.versionId,
                    this.afterDeleteCallback,
                ),
            );
        }
    }

    /**
     * Akce okamžitého kopírování hodnot atributu z předcházející JP.
     * @param descItemGroupIndex {Integer} index skupiny atributů v seznamu
     * @param descItemTypeIndex {Integer} index atributu v seznamu
     * @param descItemTypeId {Integer} id desc item type
     */
    handleDescItemTypeCopyFromPrev(descItemGroupIndex, descItemTypeIndex, descItemTypeId) {
        const {routingKey} = this.props;

        const valueLocation = {
            descItemGroupIndex,
            descItemTypeIndex,
        };
        this.props.dispatch(
            nodeFormActions.fundSubNodeFormValuesCopyFromPrev(
                this.props.versionId,
                this.props.selectedSubNode.id,
                this.props.selectedSubNode.version,
                descItemTypeId,
                routingKey,
                valueLocation,
            ),
        );
    }

    /**
     * Checks if there is an external system of type 'digitization frontdesk'
     */
    isDigitizationFrontdeskDefined = () => {
        const { externalSystems } = this.props;
        if(externalSystems.fetched){
            return externalSystems.items.some((extSystem)=>(
                extSystem[JAVA_ATTR_CLASS] === JAVA_CLASS_ARR_DIGITIZATION_FRONTDESK_SIMPLE_VO
            ))
        }
        return false;
    }

    /**
     * Checks if currently selected subnode has a desc item of the given class (data type)
     */
    subNodeHasDescItemClass = (itemClass) => {
        const { subNodeForm } = this.props;
        if(subNodeForm && subNodeForm.data && subNodeForm.data.descItems){
            return subNodeForm.data.descItems.some((item)=>( item[JAVA_ATTR_CLASS] === itemClass))
        }
        return false;
    }

    handleToggleSpecialCharacters = () => {
        this.setState({
            showSpecialCharactersWindow: !this.state?.showSpecialCharactersWindow,
        })
    }

    /**
     * Renderování globálních akcí pro formulář.
     * @return {Object} view
     */
    renderFormActions() {
        const notRoot = !isFundRootId(this.props.nodeId);

        const {fundId, userDetail, nodeId, nodeSettings} = this.props;

        const editPermAllowed = userDetail.hasOne(
            perms.FUND_ADMIN,
            {type: perms.FUND_VER_WR, fundId},
            perms.FUND_ARR_ALL,
            {type: perms.FUND_ARR, fundId},
        );

        const nodeSettingsIndex = indexById(nodeSettings.nodes, nodeId);
        const nodeSetting = nodeSettings.nodes[nodeSettingsIndex];
        const isCopyAll = nodeSetting && nodeSetting.copyAll;

        const haveProtocolPermissionToWrite = userDetail.hasOne(perms.FUND_ISSUE_ADMIN_ALL) ||
            userDetail.permissionsMap?.[perms.FUND_ISSUE_LIST_WR]?.issueListIds.length > 0;

        return (
            <div ref="nodeToolbar" className="node-form-actions-container">
                <div className="node-form-actions">
                    <div className="section">
                        <NoFocusButton onClick={this.props.onAddDescItemType}>
                            <Icon glyph="fa-plus-circle" />
                            {i18n('subNodeForm.section.item')}
                        </NoFocusButton>
                    </div>
                    <div className="section">
                        <NoFocusButton
                            onClick={this.handleDescItemTypeUnlockAll}
                            title={i18n('subNodeForm.descItemTypeUnlockAll')}
                        >
                            <Icon glyph="fa-unlock" />
                        </NoFocusButton>
                        <NoFocusButton
                            onClick={this.handleCopyAll}
                            active={isCopyAll}
                            title={i18n('subNodeForm.section.copyAll')}
                        >
                            <Icon glyph="fa-files-o" />
                        </NoFocusButton>
                        <NoFocusButton onClick={this.props.onVisiblePolicy}>
                            <Icon glyph="fa-cogs" />
                        </NoFocusButton>
                        {editPermAllowed && (
                            <NoFocusButton onClick={this.handleShowHistory}>
                                <Icon glyph="fa-history" />
                            </NoFocusButton>
                        )}
                        {notRoot && (
                            <NoFocusButton disabled={this.hasDaos(["LEVEL"])} onClick={this.handleDeleteNode}>
                                <Icon glyph="fa-trash" />
                            </NoFocusButton>
                        )}
                    </div>

                    <div className="section">
                        <NoFocusButton active={this.state?.showSpecialCharactersWindow} onClick={this.handleToggleSpecialCharacters}>
                            {/*<Icon glyph="fa-cog" />*/}
                            &Omega;
                        </NoFocusButton>
                        {this.state?.showSpecialCharactersWindow &&
                            <TextFragmentsWindow onClose={()=>{this.setState({showSpecialCharactersWindow: false})}}/>
                        }
                    </div>
                    <div className="section">
                        { this.isDigitizationFrontdeskDefined() &&
                            <NoFocusButton onClick={this.props.onDigitizationRequest}>
                                <Icon glyph="fa-camera" />
                                {i18n('subNodeForm.digitizationRequest')}
                            </NoFocusButton>
                        }
                        {editPermAllowed && this.hasDaos() && (
                            <NoFocusButton onClick={this.props.onDigitizationSync}>
                                <Icon glyph="fa-camera" />
                                {i18n('subNodeForm.digitizationSync')}
                            </NoFocusButton>
                        )}
                    </div>
                    <div className="section">
                        { this.subNodeHasDescItemClass(ItemClass.URI_REF) &&
                            <NoFocusButton onClick={this.props.onRefSync}>
                                <Icon glyph="fa-refresh" />
                                {i18n('subNodeForm.refSync')}
                            </NoFocusButton>
                        }
                    </div>
                    <div className="section">
                        <NoFocusButton
                            onClick={this.handleCreateIssueNode}
                            disabled={!haveProtocolPermissionToWrite}
                            title={i18n('subNodeForm.issueAdd')}
                        >
                            <Icon glyph="fa-commenting" />
                        </NoFocusButton>
                    </div>
                    <div className="section">
                        <DropdownButton
                            variant="default"
                            title={<Icon glyph="fa-ellipsis-h" />}
                            id="arr-structure-panel-add"
                        >
                            <Dropdown.Item eventKey="1" onClick={this.handleCreateTemplate}>
                                {i18n('subNodeForm.section.createTemplate')}
                            </Dropdown.Item>
                            <Dropdown.Item eventKey="2" onClick={this.handleUseTemplate}>
                                {i18n('subNodeForm.section.useTemplate')}
                            </Dropdown.Item>
                            <Dropdown.Item eventKey="3" onClick={this.handleCopyUuid}>
                                {i18n('subNodeForm.section.copyUuid')}
                            </Dropdown.Item>
                            { !this.subNodeHasDescItemClass(ItemClass.URI_REF) &&
                                <>
                                    <Dropdown.Divider/>
                                    <Dropdown.Item onClick={this.props.onRefSync}>
                                        <Icon glyph="fa-refresh" />
                                        {i18n('subNodeForm.refSync')}
                                    </Dropdown.Item>
                                </>
                            }
                        </DropdownButton>
                    </div>
                </div>
            </div>
        );
    }

    /**
     * Checks if current subNode has daos. 
     * If given an array of types, checks only for daos of those types.
     */
    hasDaos = (daoTypes) => {
        const { subNodeDaos } = this.props.parentNode;
        if(subNodeDaos && subNodeDaos.data){
            const data = subNodeDaos.data;
            for(let i = 0; i < data.length; i++){
                const daoType = data[i].daoType;
                return daoTypes ? daoTypes.indexOf(daoType) >= 0 : true;
            }
        }
        return false;
    }

    handleCreateIssueNode = () => {
        const {issueProtocol, issueTypes, dispatch, fund} = this.props;

        let node;
        if (fund.nodes && fund.nodes.activeIndex !== null) {
            node = fund.nodes.nodes[fund.nodes.activeIndex];
        }

        dispatch(
            modalDialogShow(
                this,
                i18n('arr.issues.add.node.title'),
                <IssueForm
                    onSubmit={data =>
                        WebApi.addIssue({
                            ...data,
                            nodeId: node.selectedSubNodeId,
                        })
                    }
                    initialValues={{
                        issueListId: issueProtocol.id,
                        issueTypeId: issueTypes?.data?.[0].id,
                    }}
                    onSubmitSuccess={data => {
                        dispatch(issuesActions.list.invalidate(data.issueListId));
                        dispatch(issuesActions.detail.invalidate(data.id));
                        dispatch(modalDialogHide());
                    }}
                />,
            ),
        );
    };

    handleCreateTemplate = () => {
        const {userDetail, fund} = this.props;

        let settings = userDetail.settings;

        const fundTemplates = getOneSettings(settings, 'FUND_TEMPLATES', 'FUND', fund.id);

        const initialValues = {
            type: NEW_TEMPLATE,
            withValues: true,
        };

        const templates = fundTemplates.value ? JSON.parse(fundTemplates.value).map(template => template.name) : [];

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.addTemplate.create'),
                <TemplateForm
                    initialValues={initialValues}
                    templates={templates}
                    onSubmitForm={data => {
                        const template = this.createTemplate(data.name, data.withValues);

                        switch (data.type) {
                            case NEW_TEMPLATE: {
                                const value = fundTemplates.value
                                    ? [...JSON.parse(fundTemplates.value), template]
                                    : [template];
                                value.sort((a, b) => {
                                    return a.name.localeCompare(b.name);
                                });
                                fundTemplates.value = JSON.stringify(value);
                                settings = setSettings(settings, fundTemplates.id, fundTemplates);
                                this.props.dispatch(userDetailsSaveSettings(settings));
                                return this.props.dispatch(modalDialogHide());
                            }
                            case EXISTS_TEMPLATE: {
                                const value = JSON.parse(fundTemplates.value);
                                const index = indexById(value, data.name, 'name');

                                if (index == null) {
                                    console.error('Nebyla nalezena šablona s názvem: ' + data.name);
                                } else {
                                    value[index] = template;
                                    fundTemplates.value = JSON.stringify(value);
                                    settings = setSettings(settings, fundTemplates.id, fundTemplates);
                                    this.props.dispatch(userDetailsSaveSettings(settings));
                                }

                                return this.props.dispatch(modalDialogHide());
                            }
                            default:
                                break;
                        }
                    }}
                />,
            ),
        );
    };

    /**
     * Return new template
     *
     * If template is without values only enums are stored
     */
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
                        // enums are always stored
                        if (withValues || item[CLS] === CLS_ITEM_ENUM) {
                            newItem.strValue = itemCls.toSimpleString();
                        }
                        items.push(newItem);
                    });

                    formData[type.id] = items;
                }
            });
        });
        let template = {
            name: name,
            withValues: withValues,
            formData: formData,
        };
        return template;
    };

    handleUseTemplate = () => {
        const {userDetail, fund, routingKey, selectedSubNode, subNodeForm} = this.props;

        let settings = userDetail.settings;

        const fundTemplates = getOneSettings(settings, 'FUND_TEMPLATES', 'FUND', fund.id);

        const templates = fundTemplates.value ? JSON.parse(fundTemplates.value).map(template => template.name) : [];

        const initialValues = {
            replaceValues: false,
            name: templates.indexOf(fund.lastUseTemplateName) >= 0 ? fund.lastUseTemplateName : null,
        };

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.useTemplate.title'),
                <TemplateUseForm
                    initialValues={initialValues}
                    templates={templates}
                    onSubmitForm={data => {
                        const value = JSON.parse(fundTemplates.value);
                        const index = indexById(value, data.name, 'name');

                        if (index == null) {
                            console.error('Nebyla nalezena šablona s názvem: ' + data.name);
                        } else {
                            const template = value[index];
                            console.debug('Apply template', template);

                            const formData = template.formData;
                            let createItems = [];
                            let updateItems = [];
                            let deleteItems = [];
                            let deleteItemsAdded = {};

                            const actualFormData = this.createFormData(subNodeForm);

                            Object.keys(formData).forEach(itemTypeId => {
                                this.processItemType(
                                    formData,
                                    itemTypeId,
                                    actualFormData,
                                    data,
                                    deleteItemsAdded,
                                    deleteItems,
                                    updateItems,
                                    createItems,
                                );
                            });

                            Object.keys(deleteItemsAdded).forEach(itemObjectId => {
                                let updateItemsTemp = [];
                                for (let i = 0; i < updateItems.length; i++) {
                                    const updateItem = updateItems[i];
                                    if (itemObjectId === updateItem.descItemObjectId) {
                                        createItems.push({
                                            ...updateItem,
                                            descItemObjectId: null,
                                        });
                                    } else {
                                        updateItemsTemp.push(updateItem);
                                    }
                                }
                                updateItems = updateItemsTemp;
                            });

                            if (createItems.length > 0 || updateItems.length > 0 || deleteItems.length > 0) {
                                return WebApi.updateDescItems(
                                    fund.versionId,
                                    selectedSubNode.id,
                                    selectedSubNode.version,
                                    createItems,
                                    updateItems,
                                    deleteItems,
                                ).then(() => {
                                    this.props.dispatch(
                                        nodeFormActions.fundSubNodeFormTemplateUse(
                                            fund.versionId,
                                            routingKey,
                                            template,
                                            data.replaceValues,
                                            true,
                                        ),
                                    );
                                    return this.props.dispatch(modalDialogHide());
                                });
                            } else {
                                this.props.dispatch(
                                    nodeFormActions.fundSubNodeFormTemplateUse(
                                        fund.versionId,
                                        routingKey,
                                        template,
                                        data.replaceValues,
                                        false,
                                    ),
                                );
                                return this.props.dispatch(modalDialogHide());
                            }
                        }
                    }}
                />,
            ),
        );
    };

    handleCopyUuid = () => {
        const {selectedSubNodeId} = this.props;
        const el = document.createElement('textarea');
        el.value = selectedSubNodeId;
        document.body.appendChild(el);
        el.select();
        document.execCommand('copy');
        document.body.removeChild(el);
    };

    processItemType = (
        formData,
        itemTypeId,
        actualFormData,
        data,
        deleteItemsAdded,
        deleteItems,
        updateItems,
        createItems,
    ) => {
        const items = formData[itemTypeId];
        items.forEach(item => {
            const newItem = {
                ...item,
                itemTypeId: itemTypeId,
            };

            if (
                notEmpty(newItem.value) ||
                (newItem[JAVA_ATTR_CLASS] === ItemClass.ENUM && notEmpty(item.descItemSpecId))
            ) {
                if (actualFormData[itemTypeId]) {
                    // pokud existuje
                    this.processExistsItemType(
                        itemTypeId,
                        actualFormData,
                        data,
                        deleteItemsAdded,
                        deleteItems,
                        item,
                        updateItems,
                        createItems,
                        newItem,
                    );
                } else {
                    // pokud neexistuje, zakládáme nový
                    createItems.push(newItem);
                }
            }
        });
    };

    processExistsItemType = (
        itemTypeId,
        actualFormData,
        data,
        deleteItemsAdded,
        deleteItems,
        item,
        updateItems,
        createItems,
        newItem,
    ) => {
        const itemType = this.findItemType(itemTypeId);
        if (itemType && itemType.rep) {
            // je opakovatelný
            const existsItems = actualFormData[itemTypeId];
            if (data.replaceValues) {
                this.processItemsToDelete(existsItems, deleteItemsAdded, deleteItems);
            } else {
                const addAsNew = this.processRepetitiveItemsToUpdate(existsItems, item, itemTypeId, updateItems);
                if (addAsNew) {
                    createItems.push(newItem);
                }
            }
        } else {
            // není opakovatelný, pouze aktualizujeme hodnotu
            if (data.replaceValues) {
                // pouze pokud chceme existující hodnoty nahradit
                const itemOrig = actualFormData[itemTypeId][0];
                if (itemOrig.value !== item.value || itemOrig.descItemSpecId !== item.descItemSpecId) {
                    // pouze pokud jsou odlišené
                    const updateItem = {
                        ...item,
                        descItemObjectId: itemOrig.descItemObjectId,
                        itemTypeId: itemTypeId,
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

            if (existsItem.descItemSpecId === item.descItemSpecId) {
                // pokud je stejná specifikace
                if (!changeValue) {
                    addAsNew = false;
                }
            } else {
                const updateItem = {
                    ...item,
                    descItemObjectId: existsItem.descItemObjectId,
                    itemTypeId: itemTypeId,
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
    createFormData = subNodeForm => {
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

    findItemType = itemTypeId => {
        const {subNodeForm, groups} = this.props;
        const groupCode = groups.reverse[itemTypeId];
        const group = objectByProperty(subNodeForm.formData.descItemGroups, groupCode, 'code');
        return objectByProperty(group.types, parseInt(itemTypeId), 'id');
    };

    initFocus() {
        this.refSubNodeForm.initFocus();
    }

    render() {
        const {singleDescItemTypeEdit, userDetail} = this.props;
        const {
            versionId,
            focus,
            closed,
            fundId,
            routingKey,
            rulDataTypes,
            calendarTypes,
            descItemTypes,
            structureTypes,
            subNodeForm,
            conformityInfo,
            descItemCopyFromPrevEnabled,
            singleDescItemTypeId,
            readMode,
            arrPerm,
        } = this.props;

        // console.info('{NodeSubNodeForm}');

        let formActions;
        if (userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId}) || arrPerm) {
            if (!readMode && !singleDescItemTypeEdit && arrPerm) {
                formActions = this.renderFormActions();
            }
        }
        const nodeSetting = this.getNodeSetting();
        return (
            <div className="node-item-form-container">
                {formActions}
                <SubNodeForm
                    ref={ref => (this.refSubNodeForm = ref)}
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
                    arrPerm={arrPerm}
                />
            </div>
        );
    }
}

function mapStateToProps(state) {
    const {arrRegion, focus, userDetail, refTables} = state;
    let fund = null;
    let structureTypes = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
        structureTypes = objectByProperty(refTables.structureTypes.data, fund.versionId, 'versionId');
    }

    return {
        externalSystems: refTables.externalSystems,
        nodeSettings: arrRegion.nodeSettings,
        structureTypes,
        fund,
        focus,
        userDetail,
        groups: refTables.groups.data,
        issueProtocol: storeFromArea(state, issuesActions.AREA_PROTOCOL),
        issueTypes: refTables.issueTypes,
    };
}

export default connect(mapStateToProps, null, null, {forwardRef: true})(NodeSubNodeForm);
