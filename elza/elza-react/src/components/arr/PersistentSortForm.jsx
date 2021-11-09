/**
 * Formulář nastavení funkce pro perzistentní řazení
 *
 */
import PropTypes from 'prop-types';

import React from 'react';
import {Field, formValueSelector, reduxForm} from 'redux-form';
import {Form} from 'react-bootstrap';
import AbstractReactComponent from '../AbstractReactComponent';
import i18n from '../i18n';
import {connect} from 'react-redux';
import {WebApi} from '../../actions/WebApi';
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx';
import {PERSISTENT_SORT_CODE} from '../../constants.tsx';
import {modalDialogHide} from '../../actions/global/modalDialog';
import {refRulDataTypesFetchIfNeeded} from '../../actions/refTables/rulDataTypes';
import FormInputField from '../shared/form/FormInputField';

const transformSubmitData = values => {
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
    ASC: 'ASC',
    DESC: 'DESC',
};

const allowedDatatypes = ['INT', 'STRING', 'TEXT', 'UNITDATE', 'FORMATTED_TEXT', 'DECIMAL'];

const RULE_CODE_ZP2015 = 'ZP2015';

class PersistentSortForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
    }

    static propTypes = {
        versionId: PropTypes.number.isRequired,
        node: PropTypes.object,
    };

    static FORM = 'persistentSortForm';

    componentDidMount = () => {
        this.props.dispatch(descItemTypesFetchIfNeeded());
        this.props.dispatch(refRulDataTypesFetchIfNeeded());
        this.getItemTypeCodesByRuleSet(RULE_CODE_ZP2015);
    };

    getItemTypeCodesByRuleSet = (ruleSetCode) => {
        WebApi.getItemTypeCodesByRuleSet(ruleSetCode).then(items => {
            this.setState({
                itemTypeCodes: items
            });
        });
    };

    filterDescItems = (descItems, itemTypeCodes) => {
        return descItems.filter(item => {
            if (itemTypeCodes !== undefined) {
                return itemTypeCodes.indexOf(item.code) != -1;
            }
            return false;
        });
    };

    render() {
        const {descItemTypes, itemType, submitting} = this.props;

        const {itemTypeCodes} = this.state;

        const filteredDescItems = this.filterDescItems(descItemTypes.items, itemTypeCodes);

        filteredDescItems.sort((a, b) => a.name.localeCompare(b.name));

        return (
            <Form>
                <Field
                    alwaysExpanded
                    label={i18n('arr.fund.bulkModifications.descItemType')}
                    items={filteredDescItems}
                    name="itemType"
                    disabled={submitting}
                    type="autocomplete"
                    component={FormInputField}
                />

                {itemType && itemType.useSpecification === true && (
                    <Field
                        name="itemSpec"
                        alwaysExpanded
                        label={i18n('arr.functions.persistentSort.spec')}
                        items={itemType.descItemSpecs}
                        getItemRenderClass={item => item.name.toLowerCase()}
                        disabled={submitting}
                        type="autocomplete"
                        component={FormInputField}
                    />
                )}

                <Field
                    type="radio"
                    name="direction"
                    disabled={submitting}
                    component={FormInputField}
                    value={DIRECTION.ASC}
                    label={i18n('arr.functions.persistentSort.direction.asc')}
                    inline
                />

                <Field
                    type="radio"
                    name="direction"
                    disabled={submitting}
                    component={FormInputField}
                    value={DIRECTION.DESC}
                    label={i18n('arr.functions.persistentSort.direction.desc')}
                    inline
                />

                <Field
                    name="sortChildren"
                    type="checkbox"
                    component={FormInputField}
                    label={i18n('arr.functions.persistentSort.sortChildren')}
                    disabled={submitting}
                />
            </Form>
        );
    }
}

const formComponent = reduxForm({
    form: PersistentSortForm.FORM,
})(PersistentSortForm);

const selector = formValueSelector(PersistentSortForm.FORM);

export default connect(
    (state, props) => {
        const {initialValues} = props;

        const descItemTypes = state.refTables.descItemTypes;
        const rulDataTypes = state.refTables.rulDataTypes;

        const itemTypeCode = initialValues && initialValues.itemTypeCode;
        const itemSpecCode = initialValues && initialValues.itemTypeCode;

        let descItemType = descItemTypes.items.find(i => i.code === itemTypeCode);

        //zpětné poskládání dat pokud je form použit na /action, pro zapamatované nastavení akce
        const transformedInitialValues = initialValues
            ? {
                  itemType: descItemType,
                  itemSpec: itemSpecCode && descItemType.descItemSpecs.find(i => i.code === itemSpecCode),
                  direction: initialValues.direction === true ? DIRECTION.ASC : DIRECTION.DESC,
                  sortChildren: initialValues.sortChildren,
              }
            : {
                  direction: DIRECTION.ASC,
                  sortChildren: false,
              };

        return {
            descItemTypes,
            rulDataTypes,
            itemType: selector(state, 'itemType'),
            onSubmit: (values, dispatch) =>
                props.onSubmit
                    ? props.onSubmit(props.versionId, transformSubmitData(values))
                    : handleSubmit(values, dispatch, props.versionId, props.node.id),
            validate: validate,
            initialValues: transformedInitialValues,
            enableReinitialize: true,
        };
    },
    undefined,
    undefined,
    {forwardRef: true},
)(formComponent);

const validate = values => {
    const errors = {};
    if (!values.itemType) {
        errors.itemType = i18n('arr.functions.persistentSort.noSelection.item');
    }

    if (values.itemType && values.itemType.useSpecification && !values.itemSpec) {
        errors.itemSpec = i18n('arr.functions.persistentSort.noSelection.spec');
    }

    return errors;
};
