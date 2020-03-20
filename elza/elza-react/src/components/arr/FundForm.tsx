import { refInstitutionsFetchIfNeeded } from 'actions/refTables/institutions.jsx';
import { refRuleSetFetchIfNeeded } from 'actions/refTables/ruleSet.jsx';
import { WebApi } from 'actions/WebApi';
import { ScopesField } from 'components/admin/ScopesField';
import { FundScope, IFundFormData } from 'components/arr/FundForm.d';
import React, { memo, useEffect } from 'react';
import { Form, Modal } from 'react-bootstrap';
import { connect, ConnectedProps, useDispatch } from 'react-redux';
import { Field, FieldArray, FormErrors, InjectedFormProps, reduxForm } from 'redux-form';
import { renderUserOrGroupLabel } from '../admin/adminRenderUtils';
import UserAndGroupField from '../admin/UserAndGroupField';
import { submitForm } from '../form/FormUtils.jsx';
import { FormInputField, i18n } from '../shared';
import TagsField from '../TagsField';
import { Button } from '../ui';

import './FundForm.scss';

interface IFundForm extends ConnectedProps<typeof connector> {
    onClose: any
    onSubmitForm: any
    create: boolean
    update: boolean
    approve: boolean
    ruleSet: any
    refTables: any
    scopeList: FundScope[]
}

/**
 * Formulář přidání nebo uzavření AS.
 */
const FundForm: React.FC<IFundForm & InjectedFormProps<{}, IFundForm>> = memo((props) => {

    const dispatch = useDispatch();
    const {handleSubmit, onClose, create, update, approve, ruleSet, refTables, pristine, submitting} = props;

    const validate = (values: IFundFormData): FormErrors<IFundFormData> => {
        const {userDetail} = props;
        const admin = userDetail.isAdmin();

        const errors: FormErrors<IFundFormData> = {};

        if ((props.create || props.update) && !values.name) {
            errors.name = i18n('global.validation.required');
        }
        if ((props.create || props.ruleSet) && !values.ruleSetId) {
            errors.ruleSetId = i18n('global.validation.required');
        }
        if ((props.create || props.update) && !values.institutionId) {
            errors.institutionId = i18n('global.validation.required');
        }

        if (props.create && !admin && (!values.fundAdmins || values.fundAdmins.length === 0)) {
            errors.fundAdmins = i18n('global.validation.required');
        }

        return errors;
    };

    useEffect(() => {
        dispatch(refRuleSetFetchIfNeeded());
        dispatch(refInstitutionsFetchIfNeeded());
    }, [dispatch]);

    /**
     * Zkontroluje zda některé z hromadných akcí běží
     *
     * @returns {boolean}
     */
    const isBulkActionRunning = () => {
        let result = false;
        props.bulkActions &&
        props.bulkActions.states.forEach(item => {
            if (item.state !== 'ERROR' && item.state !== 'FINISH') {
                result = true;
            }
        });
        return result;
    };

    const submitReduxForm = (values, dispatch) =>
        submitForm(validate, values, props, props.onSubmitForm, dispatch);

    let approveButton;
    if (approve) {
        if (isBulkActionRunning()) {
            approveButton = (
                <span className="text-danger">{i18n('arr.fund.approveVersion.runningBulkAction')}</span>
            );
        } else {
            approveButton = (
                <Button type="submit" disabled={submitting}>
                    {i18n('arr.fund.approveVersion.approve')}
                </Button>
            );
        }
    }
    const ruleSets = refTables.ruleSet.items;
    const institutions = refTables.institutions.items;

    return (
        <Form onSubmit={handleSubmit(submitReduxForm)}>
            <Modal.Body>
                {(create || update) && (
                    <Field
                        name="name"
                        type="text"
                        component={FormInputField}
                        label={i18n('arr.fund.name')}
                    />
                )}

                {(create || update) && (
                    <Field
                        name="internalCode"
                        type="text"
                        component={FormInputField}
                        label={i18n('arr.fund.internalCode')}
                    />
                )}

                {(create || update) && (
                    <Field
                        name="institutionId"
                        type="select"
                        component={FormInputField}
                        label={i18n('arr.fund.institution')}
                    >
                        <option key="-institutionId"/>
                        {institutions.map(i => {
                            return <option value={i.id}>{i.name}</option>;
                        })}
                    </Field>
                )}

                {(create || ruleSet) && (
                    <Field
                        name="ruleSetId"
                        type="select"
                        component={FormInputField}
                        label={i18n('arr.fund.ruleSet')}
                    >
                        <option key="-ruleSetId"/>
                        {ruleSets.map(i => {
                            return <option value={i.id}>{i.name}</option>;
                        })}
                    </Field>
                )}

                {approve && (
                    <Field
                        name="dateRange"
                        type="textarea"
                        component={FormInputField}
                        label={i18n('arr.fund.dateRange')}
                    />
                )}

                {update && (
                    <FieldArray
                        name="apScopes"
                        component={ScopesField}
                        label={i18n('arr.fund.regScope')}
                        scopeList={props.scopeList}
                    />
                )}

                {create && (
                    <Field
                        name="fundAdmins"
                        component={FormInputField}
                        label={i18n('arr.fund.fundAdmins')}
                        as={TagsField}
                        renderTagItem={renderUserOrGroupLabel}
                        fieldComponent={UserAndGroupField}
                        fieldComponentProps={{
                            findUserApi: WebApi.findUserWithFundCreate,
                            findGroupApi: WebApi.findGroupWithFundCreate,
                        }}
                    />
                )}
            </Modal.Body>
            <Modal.Footer>
                {create && (
                    <Button type="submit" variant="outline-secondary" disabled={pristine || submitting}>
                        {i18n('global.action.create')}
                    </Button>
                )}
                {approve && approveButton}
                {(update || ruleSet) && (
                    <Button type="submit" variant="outline-secondary" disabled={pristine || submitting}>
                        {i18n('global.action.update')}
                    </Button>
                )}
                <Button variant="link" onClick={onClose}>
                    {i18n('global.action.cancel')}
                </Button>
            </Modal.Footer>
        </Form>
    );
});

const mapState = (state: any) => ({
    userDetail: state.userDetail,
    refTables: state.refTables,
    bulkActions:
        state.arrRegion.activeIndex !== null
            ? state.arrRegion.funds[state.arrRegion.activeIndex].bulkActions
            : undefined,
    versionValidation:
        state.arrRegion.activeIndex !== null
            ? state.arrRegion.funds[state.arrRegion.activeIndex].versionValidation
            : undefined,
});
const connector = connect(mapState);

export default reduxForm({
    form: 'fundForm',
})(connector(FundForm));
