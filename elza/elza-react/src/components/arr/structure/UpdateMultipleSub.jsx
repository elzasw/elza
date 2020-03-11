import React from 'react';
import { AbstractReactComponent, i18n, Icon, Loading, NoFocusButton } from 'components/shared';
import { reduxForm } from 'redux-form';
import { Form, FormCheck, Modal } from 'react-bootstrap';
import { Button } from '../../ui';
import '../NodeSubNodeForm.scss';
import { structureFormActions } from '../../../actions/arr/subNodeForm';
import PropTypes from 'prop-types';
import { getDescItemsAddTree, getOneSettings } from '../ArrUtils';
import AddDescItemTypeForm from '../nodeForm/AddDescItemTypeForm';
import { modalDialogShow } from '../../../actions/global/modalDialog';
import { structureNodeFormFetchIfNeeded, structureNodeFormSelectId } from '../../../actions/arr/structureNodeForm';
import DescItemType from '../nodeForm/DescItemType';
import classNames from 'classnames';

import './StructureSubNodeForm.scss';


/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */
class UpdateMultipleSub extends AbstractReactComponent {

    static propTypes = {
        fundVersionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        selectedSubNodeId: PropTypes.number.isRequired,
        // Store
        rulDataTypes: PropTypes.object,
        calendarTypes: PropTypes.object,
        descItemTypes: PropTypes.object,
        subNodeForm: PropTypes.object,
        focus: PropTypes.object,
        descItemFactory: PropTypes.object.isRequired,
    };

    initFocus = () => {
        this.refs.subNodeForm.getWrappedInstance().initFocus();
    };

    UNSAFE_componentWillMount() {
        const { fundVersionId, id } = this.props;
        this.props.dispatch(structureNodeFormSelectId(fundVersionId, id));
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, id));
    }

    componentDidMount() {
        const { fundVersionId } = this.props;
        this.props.dispatch(structureFormActions.fundSubNodeFormFetchIfNeeded(fundVersionId, null));
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const { fundVersionId, id } = nextProps;
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, id));
    }

    /**
     * Zobrazení dialogu pro přidání atributu.
     */
    handleAddDescItemType = () => {
        const { fundVersionId } = this.props;

        const descItemTypes = this.descItemTypeItems();

        // Modální dialog
        this.props.dispatch(modalDialogShow(
            this,
            i18n('subNodeForm.descItemType.title.add'),
            <AddDescItemTypeForm
                descItemTypes={descItemTypes}
                onSubmitForm={(data) => this.props.dispatch(structureFormActions.fundSubNodeFormDescItemTypeAdd(fundVersionId, null, data.descItemTypeId.id))}
            />,
        ));
    };

    handleNotModify = (id) => {
        const { fields: { autoincrementItemTypeIds, deleteItemTypeIds, items } } = this.props;
        const incrementIndex = autoincrementItemTypeIds.value.indexOf(id);
        if (incrementIndex !== -1) {
            autoincrementItemTypeIds.onChange([
                ...autoincrementItemTypeIds.value.slice(0, incrementIndex),
                ...autoincrementItemTypeIds.value.slice(incrementIndex + 1),
            ]);
        }
        const deleteIndex = deleteItemTypeIds.value.indexOf(id);
        if (deleteIndex !== -1) {
            deleteItemTypeIds.onChange([
                ...deleteItemTypeIds.value.slice(0, deleteIndex),
                ...deleteItemTypeIds.value.slice(deleteIndex + 1),
            ]);
        }

        if (items.value.hasOwnProperty(id)) {
            const newItems = { ...items.value };
            delete newItems[id];
            items.onChange(newItems);
        }

    };

    handleModify = (id, descItems) => {
        const { fields: { items } } = this.props;
        if (!items.value.hasOwnProperty(id)) {
            items.onChange({
                ...items.value,
                [id]: descItems.map(i => ({ ...i, id: undefined })),
            });
        }

    };

    handleDeleteToggle = (id) => {
        const { fields: { autoincrementItemTypeIds, deleteItemTypeIds, items } } = this.props;
        const indexInDeleted = deleteItemTypeIds.value.indexOf(id);
        if (indexInDeleted !== -1) {
            deleteItemTypeIds.onChange([
                ...deleteItemTypeIds.value.slice(0, indexInDeleted),
                ...deleteItemTypeIds.value.slice(indexInDeleted + 1),
            ]);
        } else {
            const indexInAutoincrement = autoincrementItemTypeIds.value.indexOf(id);
            if (indexInAutoincrement !== -1) {
                autoincrementItemTypeIds.onChange([
                    ...autoincrementItemTypeIds.value.slice(0, indexInAutoincrement),
                    ...autoincrementItemTypeIds.value.slice(indexInAutoincrement + 1),
                ]);
            }

            if (items.value.hasOwnProperty(id)) {
                const newItems = { ...items.value };
                delete newItems[id];
                items.onChange(newItems);
            }

            deleteItemTypeIds.onChange([
                ...deleteItemTypeIds.value,
                id,
            ]);
        }
    };

    customInRender = (descItems, code, infoType) => {
        const { fields: { autoincrementItemTypeIds, deleteItemTypeIds, items } } = this.props;
        const parts = [];


        let glyph = 'fa-lock';
        let clickFunction = this.handleModify.bind(this, infoType.id, descItems);


        const isInDeleted = deleteItemTypeIds.value.indexOf(infoType.id) !== -1;
        if (
            autoincrementItemTypeIds.value.indexOf(infoType.id) !== -1 ||
            isInDeleted ||
            items.value.hasOwnProperty(infoType.id)
        ) {
            glyph = 'fa-unlock';
            clickFunction = this.handleNotModify.bind(this, infoType.id);
        }

        parts.push(<NoFocusButton className={'lock'} key="lock" onClick={clickFunction}>
            <Icon glyph={glyph}/>
        </NoFocusButton>);

        parts.push(<NoFocusButton className={'delete'} key="delete" active={isInDeleted}
                                  onClick={this.handleDeleteToggle.bind(this, infoType.id)}>
            <Icon glyph={'fa-trash'}/>
        </NoFocusButton>);


        if (code === 'INT' && items.value.hasOwnProperty(infoType.id)) {
            const index = autoincrementItemTypeIds.value.indexOf(infoType.id);
            const checked = index !== -1;


            parts.push(<FormCheck key="increment" checked={checked} onChange={() => {
                if (checked) {
                    autoincrementItemTypeIds.onChange([
                        ...autoincrementItemTypeIds.value.slice(0, index),
                        ...autoincrementItemTypeIds.value.slice(index + 1),
                    ]);
                } else {
                    autoincrementItemTypeIds.onChange([
                        ...autoincrementItemTypeIds.value,
                        infoType.id,
                    ]);
                }

            }}>
                {i18n('arr.structure.modal.increment')}
            </FormCheck>);
        }

        return parts;
    };

    getStrictMode = () => {
        const { fund, userDetail } = this.props;
        let { activeVersion: { strictMode } } = fund;

        const userStrictMode = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', fund.id);
        if (userStrictMode && userStrictMode.value !== null) {
            strictMode = userStrictMode.value === 'true';
        }
        return strictMode;
    };

    descItemTypeItems = () => {
        const { subNodeForm } = this.props;

        const strictMode = this.getStrictMode();
        return getDescItemsAddTree(subNodeForm.formData.descItemGroups, subNodeForm.infoTypesMap, subNodeForm.refTypesMap, subNodeForm.infoGroups, strictMode);
    };


    handleBlank = (...e) => {
        //console.log(e);
    };


    handleChange = (id, index, value) => {
        const { fields: { items } } = this.props;
        if (items.value.hasOwnProperty(id)) {
            items.onChange({
                ...items.value,
                [id]: [
                    ...items.value[id].slice(0, index),
                    { ...items.value[id][index], value },
                    ...items.value[id].slice(index + 1),
                ],
            });
        }
    };

    handleChangeSpec = (id, index, descItemSpecId) => {
        const { fields: { items } } = this.props;
        if (items.value.hasOwnProperty(id)) {
            items.onChange({
                ...items.value,
                [id]: [
                    ...items.value[id].slice(0, index),
                    { ...items.value[id][index], descItemSpecId },
                    ...items.value[id].slice(index + 1),
                ],
            });
        }
    };

    /*
    handleChangePosition = (id, from, to) => {
        const {fields: {items}} = this.props;
        if (items.value.hasOwnProperty(id)) {
            const moved = items.value[id][from];
            const data = {
                ...items.value[id].slice(0, from),
                ...items.value[id].slice(from+1)
            };
            items.onChange({
                ...items.value,
                [id]: [
                    ...data.slice(0, to),
                    moved,
                    ...data.slice(to),
                ]
            })
        }
    };
*/

    handleOnDescItemAdd = (id) => {
        const { fields: { items } } = this.props;
        if (items.value.hasOwnProperty(id)) {

            const lastAdded = items.value[id][items.value[id].length - 1];

            items.onChange({
                ...items.value,
                [id]: [
                    ...items.value[id],
                    {
                        ...lastAdded,
                        descItemObjectId: null,
                        formKey: 'fk_' + parseInt(lastAdded.formKey.split('_')[1]) + 1,
                        value: '',
                    },
                ],
            });
        }
    };

    handleOnDescItemDelete = (id, index) => {
        const { fields: { items } } = this.props;
        if (items.value.hasOwnProperty(id)) {
            items.onChange({
                ...items.value,
                [id]: [
                    ...items.value[id].slice(0, index),
                    ...items.value[id].slice(index + 1),
                ],
            });
        }
    };

    renderDescItemType = (descItemType, descItemTypeIndex, descItemGroupIndex, nodeSetting) => {
        const { fields: { items, deleteItemTypeIds } } = this.props;
        const { fundId, subNodeForm, descItemCopyFromPrevEnabled, singleDescItemTypeEdit, calendarTypes, closed, showNodeAddons, fundVersionId, typePrefix, descItemFactory } = this.props;

        const refType = subNodeForm.refTypesMap[descItemType.id];
        const infoType = subNodeForm.infoTypesMap[descItemType.id];
        const rulDataType = refType.dataType;


        const strictMode = this.getStrictMode();
        const itemModified = items.value.hasOwnProperty(infoType.id);


        const { descItems, ...allowedProps } = descItemType;

        let hackedDescItems = [];

        let overrideInfo = null;

        if (itemModified) {
            hackedDescItems = items.value[infoType.id];
        } else {
            const itemDeleted = deleteItemTypeIds.value.indexOf(infoType.id) !== -1;
            let value = i18n(itemDeleted ? 'arr.structure.modal.updateMultiple.deletedValue' : 'arr.structure.modal.updateMultiple.originalValue');
            if (rulDataType.code === 'ENUM') {
                overrideInfo = {
                    ...infoType,
                    descItemSpecsMap: {
                        ...infoType.descItemSpecsMap,
                        [-1]: {
                            id: -1,
                            rep: 1,
                            type: 'POSSIBLE',
                            name: value,
                        },
                    },
                };

                hackedDescItems.push({
                    ...descItems[0],
                    value,
                    descItemSpecId: -1,
                });
            } else {
                hackedDescItems.push({
                    ...descItems[0],
                    value,
                });
            }
        }

        const hackedDescItemType = {
            ...allowedProps,
            descItems: hackedDescItems,
        };


        return <DescItemType key={descItemType.id}
                             typePrefix={typePrefix}
                             ref={'descItemType' + descItemType.id}
                             descItemType={hackedDescItemType}
                             singleDescItemTypeEdit={singleDescItemTypeEdit}
                             refType={refType}
                             infoType={overrideInfo || infoType}
                             rulDataType={rulDataType}
                             calendarTypes={calendarTypes}
                             onDescItemAdd={this.handleOnDescItemAdd.bind(this, infoType.id)}
                             onDescItemRemove={this.handleOnDescItemDelete.bind(this, infoType.id)}
                             onChange={itemModified ? this.handleChange.bind(this, infoType.id) : this.handleBlank}
                             onChangeSpec={this.handleChangeSpec.bind(this, infoType.id)}
                             onBlur={this.handleBlank}
                             onFocus={this.handleBlank}
                             showNodeAddons={showNodeAddons}
                             locked={!itemModified}
                             closed={closed}
                             conformityInfo={{ missings: [], errors: [] }}
                             descItemCopyFromPrevEnabled={descItemCopyFromPrevEnabled}
                             versionId={fundVersionId}
                             fundId={fundId}
                             draggable={false}
                             readMode={false}
                             strictMode={strictMode}
                             customActions={this.customInRender(descItems, rulDataType.code, infoType)}
                             hideDelete={true}
                             descItemFactory={descItemFactory}
            // onChangePosition={this.handleChangePosition.bind(this, infoType.id)}
            // onBlur={this.handleBlur.bind(this, descItemGroupIndex, descItemTypeIndex)}
            // onFocus={this.handleFocus.bind(this, descItemGroupIndex, descItemTypeIndex)}
            // onDescItemTypeRemove={this.handleDescItemTypeRemove.bind(this, descItemGroupIndex, descItemTypeIndex)}
            // onSwitchCalculating={this.handleSwitchCalculating.bind(this, descItemGroupIndex, descItemTypeIndex)}
            // onDescItemTypeLock={this.handleDescItemTypeLock.bind(this, descItemType.id)}
            // onDescItemTypeCopy={this.handleDescItemTypeCopy.bind(this, descItemType.id)}
            // onDescItemTypeCopyFromPrev={this.handleDescItemTypeCopyFromPrev.bind(this, descItemGroupIndex, descItemTypeIndex, descItemType.id)}
        />;
    };


    /**
     * Renderování skupiny atributů.
     * @param descItemGroup {Object} skupina
     * @param descItemGroupIndex {Integer} index skupiny v seznamu
     * @param nodeSetting
     * @return {Object} view
     */
    renderDescItemGroup = (descItemGroup, descItemGroupIndex, nodeSetting) => {
        const { singleDescItemTypeEdit, singleDescItemTypeId } = this.props;

        const descItemTypes = [];
        descItemGroup.descItemTypes.forEach((descItemType, descItemTypeIndex) => {
            if (!singleDescItemTypeEdit || (singleDescItemTypeEdit && singleDescItemTypeId === descItemType.id)) {
                const i = this.renderDescItemType(descItemType, descItemTypeIndex, descItemGroupIndex, nodeSetting);
                descItemTypes.push(i);
            }
        });
        const cls = classNames({
            'desc-item-group': true,
            active: descItemGroup.hasFocus,
        });

        if (singleDescItemTypeEdit && descItemTypes.length === 0) {
            return null;
        }

        return (
            <div key={'type-' + descItemGroup.code + '-' + descItemGroupIndex} className={cls}>
                <div className='desc-item-types'>
                    {descItemTypes}
                </div>
            </div>
        );
    };

    render() {
        const { handleSubmit, submitting, fields: { items }, onClose, subNodeForm, subNodeForm: { formData }, nodeSetting } = this.props;

        if (!subNodeForm || !subNodeForm.fetched) {
            return <Loading/>;
        }


        const descItemGroups = [];
        formData.descItemGroups.forEach((group, groupIndex) => {
            const i = this.renderDescItemGroup(group, groupIndex, nodeSetting);
            if (i !== null) {
                descItemGroups.push(i);
            }
        });


        return (
            <Form className="structure-extensions-form" onSubmit={handleSubmit}>
                <Modal.Body>
                    <div className="structure-item-form-container">
                        {items.length}
                        {this.descItemTypeItems().length > 0 &&
                        <NoFocusButton onClick={this.handleAddDescItemType}><Icon
                            glyph="fa-plus-circle"/>{i18n('subNodeForm.section.item')}</NoFocusButton>}
                        <div className='node-form'>
                            {/*unusedGeneratedItems*/}
                            <div ref='nodeForm' className='desc-item-groups'>
                                {descItemGroups}
                            </div>
                        </div>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" disabled={submitting}>{i18n('global.action.update')}</Button>
                    <Button variant="link" disabled={submitting}
                            onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </Form>
        );
    }
}

function mapStateToProps(state) {
    const { arrRegion, focus, refTables, userDetail } = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }

    return {
        subNodeForm: fund ? fund.structureNodeForm.subNodeForm : null,
        userDetail,
        fund,
        focus,
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
    };
}

export default reduxForm({
    form: 'UpdateMultipleStructureForm',
    initialValues: {
        autoincrementItemTypeIds: [],
        deleteItemTypeIds: [],
        items: {},
        structureDataIds: [],
    },
    fields: ['structureDataIds', 'autoincrementItemTypeIds', 'deleteItemTypeIds', 'items'],
}, mapStateToProps)(UpdateMultipleSub);
