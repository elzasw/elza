import React from 'react';
import {AbstractReactComponent, i18n, Icon, Loading, NoFocusButton} from 'components/shared';
import {connect} from 'react-redux';
import {formValueSelector, reduxForm} from 'redux-form';
import {Form, FormCheck, Modal} from 'react-bootstrap';
import {Button} from '../../ui';
import '../NodeSubNodeForm.scss';
import {structureFormActions} from '../../../actions/arr/subNodeForm';
import PropTypes from 'prop-types';
import {getDescItemsAddTree, getOneSettings} from '../ArrUtils';
import AddDescItemTypeForm from '../nodeForm/AddDescItemTypeForm';
import {modalDialogShow} from '../../../actions/global/modalDialog';
import {structureNodeFormFetchIfNeeded, structureNodeFormSelectId} from '../../../actions/arr/structureNodeForm';
import DescItemType from '../nodeForm/DescItemType';
import classNames from 'classnames';

import './StructureSubNodeForm.scss';

/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */
class UpdateMultipleSub extends React.Component {
    static formName = 'UpdateMultipleStructureForm';

    static propTypes = {
        fundVersionId: PropTypes.number.isRequired,
        fundId: PropTypes.number.isRequired,
        id: PropTypes.number.isRequired,
        // Store
        rulDataTypes: PropTypes.object,
        descItemTypes: PropTypes.object,
        subNodeForm: PropTypes.object,
        focus: PropTypes.object,
        descItemFactory: PropTypes.func.isRequired,
    };

    componentDidMount() {
        const {fundVersionId, id} = this.props;
        this.props.dispatch(structureNodeFormSelectId(fundVersionId, id));
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, id));
        this.props.dispatch(structureFormActions.fundSubNodeFormFetchIfNeeded(fundVersionId, id));
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const {fundVersionId, id} = nextProps;
        this.props.dispatch(structureNodeFormFetchIfNeeded(fundVersionId, id));
        this.props.dispatch(structureFormActions.fundSubNodeFormFetchIfNeeded(fundVersionId, id));
    }

    /**
     * Zobrazení dialogu pro přidání atributu.
     */
    handleAddDescItemType = () => {
        const {fundVersionId, id} = this.props;

        const descItemTypes = this.descItemTypeItems();

        // Modální dialog
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('subNodeForm.descItemType.title.add'),
                <AddDescItemTypeForm
                    descItemTypes={descItemTypes}
                    onSubmitForm={data =>
                        this.props.dispatch(
                            structureFormActions.fundSubNodeFormDescItemTypeAdd(
                                fundVersionId,
                                id,
                                data.descItemTypeId.id,
                            ),
                        )
                    }
                />,
            ),
        );
    };

    handleNotModify = id => {
        const {autoincrementItemTypeIds, deleteItemTypeIds, items, change} = this.props;
        const incrementIndex = autoincrementItemTypeIds.indexOf(id);
        if (incrementIndex !== -1) {
            change('autoincrementItemTypeIds', [
                ...autoincrementItemTypeIds.slice(0, incrementIndex),
                ...autoincrementItemTypeIds.slice(incrementIndex + 1),
            ]);
        }
        const deleteIndex = deleteItemTypeIds.indexOf(id);
        if (deleteIndex !== -1) {
            change('deleteItemTypeIds', [
                ...deleteItemTypeIds.slice(0, deleteIndex),
                ...deleteItemTypeIds.slice(deleteIndex + 1),
            ]);
        }

        if (items.hasOwnProperty(id)) {
            const newItems = {...items};
            delete newItems[id];
            change('items', newItems);
        }
    };

    handleModify = (id, descItems) => {
        const {items, change} = this.props;
        if (!items.hasOwnProperty(id)) {
            change('items', {
                ...items,
                [id]: descItems.map(i => ({...i, id: undefined})),
            });
        }
    };

    handleDeleteToggle = id => {
        const {autoincrementItemTypeIds, deleteItemTypeIds, items, change} = this.props;
        const indexInDeleted = deleteItemTypeIds.indexOf(id);
        if (indexInDeleted !== -1) {
            change('deleteItemTypeIds', [
                ...deleteItemTypeIds.slice(0, indexInDeleted),
                ...deleteItemTypeIds.slice(indexInDeleted + 1),
            ]);
        } else {
            const indexInAutoincrement = autoincrementItemTypeIds.indexOf(id);
            if (indexInAutoincrement !== -1) {
                change('autoincrementItemTypeIds', [
                    ...autoincrementItemTypeIds.slice(0, indexInAutoincrement),
                    ...autoincrementItemTypeIds.slice(indexInAutoincrement + 1),
                ]);
            }

            if (items.hasOwnProperty(id)) {
                const newItems = {...items};
                delete newItems[id];
                change('items', newItems);
            }

            change('deleteItemTypeIds', [...deleteItemTypeIds, id]);
        }
    };

    customInRender = (descItems, code, infoType) => {
        const {autoincrementItemTypeIds, deleteItemTypeIds, items, change} = this.props;
        const parts = [];

        let glyph = 'fa-lock';
        let clickFunction = this.handleModify.bind(this, infoType.id, descItems);

        const isInDeleted = deleteItemTypeIds.indexOf(infoType.id) !== -1;
        if (autoincrementItemTypeIds.indexOf(infoType.id) !== -1 || isInDeleted || items.hasOwnProperty(infoType.id)) {
            glyph = 'fa-unlock';
            clickFunction = this.handleNotModify.bind(this, infoType.id);
        }

        parts.push(
            <NoFocusButton className={'lock'} key="lock" onClick={clickFunction}>
                <Icon glyph={glyph} />
            </NoFocusButton>,
        );

        parts.push(
            <NoFocusButton
                className={'delete'}
                key="delete"
                active={isInDeleted}
                onClick={this.handleDeleteToggle.bind(this, infoType.id)}
            >
                <Icon glyph={'fa-trash'} />
            </NoFocusButton>,
        );

        if (code === 'INT' && items.hasOwnProperty(infoType.id)) {
            const index = autoincrementItemTypeIds.indexOf(infoType.id);
            const checked = index !== -1;

            parts.push(
                <FormCheck
                    key="increment"
                    checked={checked}
                    onChange={() => {
                        if (checked) {
                            change('autoincrementItemTypeIds', [
                                ...autoincrementItemTypeIds.slice(0, index),
                                ...autoincrementItemTypeIds.slice(index + 1),
                            ]);
                        } else {
                            change('autoincrementItemTypeIds', [...autoincrementItemTypeIds, infoType.id]);
                        }
                    }}
                    label={i18n('arr.structure.modal.increment')}
                />,
            );
        }

        return parts;
    };

    getStrictMode = () => {
        const {fund, userDetail} = this.props;
        let {
            activeVersion: {strictMode},
        } = fund;

        const userStrictMode = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', fund.id);
        if (userStrictMode && userStrictMode.value !== null) {
            strictMode = userStrictMode.value === 'true';
        }
        return strictMode;
    };

    descItemTypeItems = () => {
        const {subNodeForm} = this.props;

        const strictMode = this.getStrictMode();
        return getDescItemsAddTree(
            subNodeForm.formData.descItemGroups,
            subNodeForm.infoTypesMap,
            subNodeForm.refTypesMap,
            subNodeForm.infoGroups,
            strictMode,
        );
    };

    handleBlank = (...e) => {
        //console.log(e);
    };

    handleChange = (id, index, value) => {
        const {items, change} = this.props;
        if (items.hasOwnProperty(id)) {
            change('items', {
                ...items,
                [id]: [...items[id].slice(0, index), {...items[id][index], value}, ...items[id].slice(index + 1)],
            });
        }
    };

    handleChangeSpec = (id, index, descItemSpecId) => {
        const {items, change} = this.props;
        if (items.hasOwnProperty(id)) {
            change('items', {
                ...items,
                [id]: [
                    ...items[id].slice(0, index),
                    {...items[id][index], descItemSpecId},
                    ...items[id].slice(index + 1),
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

    handleOnDescItemAdd = id => {
        const {items, change} = this.props;
        if (items.hasOwnProperty(id)) {
            const lastAdded = items[id][items[id].length - 1];

            change('items', {
                ...items,
                [id]: [
                    ...items[id],
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
        const {items, change} = this.props;
        if (items.hasOwnProperty(id)) {
            change('items', {
                ...items,
                [id]: [...items[id].slice(0, index), ...items[id].slice(index + 1)],
            });
        }
    };

    renderDescItemType = (descItemType, descItemTypeIndex, descItemGroupIndex, nodeSetting) => {
        const {
            items,
            deleteItemTypeIds,
            subNodeForm,
            descItemCopyFromPrevEnabled,
            singleDescItemTypeEdit,
            closed,
            showNodeAddons,
            fundVersionId,
            typePrefix,
            descItemFactory,
        } = this.props;
        const fundId = this.props.fund.id;

        const refType = subNodeForm.refTypesMap[descItemType.id];
        const infoType = subNodeForm.infoTypesMap[descItemType.id];
        const rulDataType = refType.dataType;

        const strictMode = this.getStrictMode();
        const itemModified = items.hasOwnProperty(infoType.id);

        const {descItems, ...allowedProps} = descItemType;

        let hackedDescItems = [];

        let overrideInfo = null;

        if (itemModified) {
            hackedDescItems = items[infoType.id];
        } else {
            const itemDeleted = deleteItemTypeIds.indexOf(infoType.id) !== -1;
            let value = i18n(
                itemDeleted
                    ? 'arr.structure.modal.updateMultiple.deletedValue'
                    : 'arr.structure.modal.updateMultiple.originalValue',
            );
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

        return (
            <DescItemType
                key={descItemType.id}
                typePrefix={typePrefix}
                ref={'descItemType' + descItemType.id}
                descItemType={hackedDescItemType}
                singleDescItemTypeEdit={singleDescItemTypeEdit}
                refType={refType}
                infoType={overrideInfo || infoType}
                rulDataType={rulDataType}
                onDescItemAdd={this.handleOnDescItemAdd.bind(this, infoType.id)}
                onDescItemRemove={this.handleOnDescItemDelete.bind(this, infoType.id)}
                onChange={itemModified ? this.handleChange.bind(this, infoType.id) : this.handleBlank}
                onChangeSpec={this.handleChangeSpec.bind(this, infoType.id)}
                onBlur={this.handleBlank}
                onFocus={this.handleBlank}
                showNodeAddons={showNodeAddons}
                locked={!itemModified}
                closed={closed}
                conformityInfo={{missings: [], errors: []}}
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
            />
        );
    };

    /**
     * Renderování skupiny atributů.
     * @param descItemGroup {Object} skupina
     * @param descItemGroupIndex {Integer} index skupiny v seznamu
     * @param nodeSetting
     * @return {Object} view
     */
    renderDescItemGroup = (descItemGroup, descItemGroupIndex, nodeSetting) => {
        const {singleDescItemTypeEdit, singleDescItemTypeId} = this.props;

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
                <div className="desc-item-types">{descItemTypes}</div>
            </div>
        );
    };

    render() {
        const {handleSubmit, submitting, items, onClose, subNodeForm, nodeSetting} = this.props;

        if (!subNodeForm || !subNodeForm.fetched || !items) {
            return <Loading />;
        }

        const {
            subNodeForm: {formData},
        } = this.props;

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
                        {this.descItemTypeItems().length > 0 && (
                            <NoFocusButton onClick={this.handleAddDescItemType}>
                                <Icon glyph="fa-plus-circle" />
                                {i18n('subNodeForm.section.item')}
                            </NoFocusButton>
                        )}
                        <div className="node-form">
                            {/*unusedGeneratedItems*/}
                            <div ref="nodeForm" className="desc-item-groups">
                                {descItemGroups}
                            </div>
                        </div>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        {i18n('global.action.update')}
                    </Button>
                    <Button variant="link" disabled={submitting} onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

const rf = reduxForm({
    form: UpdateMultipleSub.formName,
    initialValues: {
        autoincrementItemTypeIds: [],
        deleteItemTypeIds: [],
        items: {},
        structureDataIds: [],
    },
    destroyOnUnmount: true,
})(UpdateMultipleSub);

export default connect(function (state, props) {
    const {arrRegion, focus, refTables, userDetail, structures} = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }

    const selector = formValueSelector(props.form || UpdateMultipleSub.formName);

    const {items, deleteItemTypeIds, autoincrementItemTypeIds} = selector(
        state,
        'items',
        'deleteItemTypeIds',
        'autoincrementItemTypeIds',
    );
    return {
        items,
        deleteItemTypeIds,
        autoincrementItemTypeIds,
        subNodeForm: structures.stores.hasOwnProperty(props.id) ? structures.stores[props.id].subNodeForm : null,
        userDetail,
        fund,
        focus,
        rulDataTypes: refTables.rulDataTypes,
        descItemTypes: refTables.descItemTypes,
    };
})(rf);
