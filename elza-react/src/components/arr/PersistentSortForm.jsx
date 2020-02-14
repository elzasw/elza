/**
 * Formulář nastavení funkce pro perzistentní řazení
 *
 */
import PropTypes from 'prop-types';

import React from 'react';
import {reduxForm} from 'redux-form';
import {Form, Checkbox} from 'react-bootstrap';
import {decorateFormField} from 'components/form/FormUtils.jsx';
import AbstractReactComponent from "../AbstractReactComponent";
import FormInput from "../shared/form/FormInput";
import i18n from "../i18n";
import {Autocomplete} from "../shared";
import {connect} from 'react-redux'
import {WebApi} from "../../actions/WebApi";
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {PERSISTENT_SORT_CODE} from "../../constants.tsx";
import {modalDialogHide} from "../../actions/global/modalDialog";
import {refRulDataTypesFetchIfNeeded} from "../../actions/refTables/rulDataTypes";


const transformSubmitData = (values) => {
    return {
        asc: values.direction === DIRECTION.ASC,
        sortChildren: values.sortChildren,
        itemTypeCode: values.itemType.code,
        itemSpecCode: values.itemSpec ? values.itemSpec.code : null,
    };
};

const handleSubmit = (values, dispatch, versionId, id) => {
    dispatch(modalDialogHide());
    return WebApi.queuePersistentSortByIds(versionId, PERSISTENT_SORT_CODE, [id], transformSubmitData(values));
};

const DIRECTION = {
    ASC: "ASC",
    DESC: "DESC"
};

const allowedDatatypes = [
    "INT",
    "STRING",
    "TEXT",
    "UNITDATE",
    "FORMATTED_TEXT",
    "DECIMAL",
];

class PersistentSortForm extends AbstractReactComponent {

    static propTypes = {
        versionId: PropTypes.number.isRequired,
        node: PropTypes.object
    };

    componentDidMount = () => {
        this.props.dispatch(descItemTypesFetchIfNeeded());
        this.props.dispatch(refRulDataTypesFetchIfNeeded());
    };

    findDataTypeById = (rulDataTypes, id) => {
        return rulDataTypes.find((dataType) => dataType.id === id);
    };

    filterDescItems = (descItems, rulDataTypes, allowedDatatypes) => {
        return descItems.filter((item) => {
            const dataType = this.findDataTypeById(rulDataTypes, item.dataTypeId);
            let find = allowedDatatypes.find(allowedDataType => {
                return dataType && dataType.code === allowedDataType;
            });
            return !!find
        })
    };

    render() {

        const {fields: {itemType, itemSpec, direction, sortChildren}, descItemTypes, rulDataTypes} = this.props;

        const filteredDescItems = this.filterDescItems(descItemTypes.items, rulDataTypes.items, allowedDatatypes);


        return <Form>

            <Autocomplete
                name="itemType"
                alwaysExpanded
                label={i18n('arr.fund.bulkModifications.descItemType')}
                {...itemType}
                items={filteredDescItems}
                onBlur={() => {}}
            />

            {
                itemType.value.useSpecification === true && <Autocomplete
                    name="itemSpec"
                    alwaysExpanded
                    label={i18n('arr.functions.persistentSort.spec')}
                    {...itemSpec}
                    items={itemType.value.descItemSpecs}
                    getItemRenderClass={item => item.name.toLowerCase()}
                    onBlur={() => {}}
                />
            }

            <FormInput type="radio"
                       name="direction"
                       {...direction}
                       checked={direction.value === DIRECTION.ASC}
                       value={DIRECTION.ASC}
                       label={i18n("arr.functions.persistentSort.direction.asc")}
                       inline
            />

            <FormInput type="radio"
                       name="direction"
                       {...direction}
                       checked={direction.value === DIRECTION.DESC}
                       value={DIRECTION.DESC}
                       label={i18n("arr.functions.persistentSort.direction.desc")}
                       inline
            />

            <Checkbox type="checkbox"
                      name="sortChildren"
                      {...sortChildren}
                      value={sortChildren.checked}
            >{i18n("arr.functions.persistentSort.sortChildren")}</Checkbox>

        </Form>
    }
}

const formComponent = reduxForm({
    form: 'persistentSortForm',
    fields: ['itemType', 'itemSpec', 'direction', 'sortChildren'],
})(PersistentSortForm);

export default connect((state, props) => {
    const {initialValues} = props;

    const descItemTypes = state.refTables.descItemTypes;
    const rulDataTypes = state.refTables.rulDataTypes;

    const itemTypeCode = initialValues && initialValues.itemTypeCode;
    const itemSpecCode = initialValues && initialValues.itemTypeCode;

    let descItemType = descItemTypes.items.find(i => i.code === itemTypeCode);

    //zpětné poskládání dat pokud je form použit na /action, pro zapamatované nastavení akce
    const transformedInitialValues = initialValues ? {
        itemType: descItemType,
        itemSpec: itemSpecCode && descItemType.descItemSpecs.find(i => i.code === itemSpecCode),
        direction: initialValues.direction === true ? DIRECTION.ASC : DIRECTION.DESC,
        sortChildren: initialValues.sortChildren
    } : {
        direction: DIRECTION.ASC,
        sortChildren: false
    };

    return {
        descItemTypes,
        rulDataTypes,
        onSubmit: (values, dispatch) => props.onSubmit
            ? props.onSubmit(props.versionId, transformSubmitData(values))
            : handleSubmit(values, dispatch, props.versionId, props.node.id),
        validate: validate,
        initialValues: transformedInitialValues,
        enableReinitialize: true
    }
})(formComponent)


const validate = (values) => {
    const errors = {};
    if (!values.itemType) {
        errors.itemType = i18n('arr.functions.persistentSort.noSelection.item');
    }

    if (values.itemType && values.itemType.useSpecification && !values.itemSpec) {
        errors.itemSpec = i18n('arr.functions.persistentSort.noSelection.spec');
    }

    return errors;
};
