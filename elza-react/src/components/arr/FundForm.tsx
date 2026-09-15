import { refInstitutionsFetchIfNeeded } from 'actions/refTables/institutions.jsx';
import { refRuleSetFetchIfNeeded } from 'actions/refTables/ruleSet.jsx';
import { WebApi } from 'actions/WebApi';
import { ScopesField } from 'components/admin/ScopesField';
import { FundScope, IFundFormData } from '../../types';
import React, { memo, useEffect } from 'react';
import { Form, Modal } from 'react-bootstrap';
import { connect, ConnectedProps } from 'react-redux';
import { Field, FieldArray, FormErrors, InjectedFormProps, reduxForm } from 'redux-form';
import { renderUserOrGroupLabel } from '../admin/adminRenderUtils';
import UserAndGroupField from '../admin/UserAndGroupField';
import { submitForm } from '../form/FormUtils.jsx';
import { validateFundForm } from './fundFormValidation';
import { FormInputField} from '../shared';
import { FormattedMessage } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { fundFormMessages } from './fundFormMessages';
import TagsField from '../TagsField';
import { Button } from '../ui';

import './FundForm.scss';
import { useThunkDispatch } from 'utils/hooks/useThunkDispatch.js';

interface IFundForm extends ConnectedProps<typeof connector> {
    onClose?: () => void;
    onSubmitForm: any;
    create?: boolean;
    update?: boolean;
    approve?: boolean;
    ruleSet?: any;
    refTables: any;
    scopeList: FundScope[];
}

/**
 * Formulář přidání nebo uzavření AS.
 */
const FundForm: React.FC<IFundForm & InjectedFormProps<object, IFundForm>> = memo((props) => {

    const dispatch = useThunkDispatch();
    const {handleSubmit, onClose, create, update, approve, ruleSet, refTables, pristine, submitting} = props;

    const validate = (values: IFundFormData): FormErrors<IFundFormData> => validateFundForm(values, props);

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
                <span className="text-danger">{<FormattedMessage {...fundFormMessages.fundApproveVersionRunningBulkAction} />}</span>
            );
        } else {
            approveButton = (
                <Button type="submit" variant="outline-secondary" disabled={submitting}>
                    {<FormattedMessage {...fundFormMessages.fundApproveVersionApprove} />}
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
                        label={<FormattedMessage {...fundFormMessages.fundName} />}
                    />
                )}

                {(create || update) && (
                    <Field
                        name="internalCode"
                        type="text"
                        component={FormInputField}
                        label={<FormattedMessage {...fundFormMessages.fundInternalCode} />}
                    />
                )}

                {(create || update) && (
                    <Field
                        name="institutionIdentifier"
                        type="select"
                        component={FormInputField}
                        label={<FormattedMessage {...fundFormMessages.fundInstitution} />}
                    >
                        <option key="-institutionId"/>
                        {institutions.map(i => {
                            return <option value={i.code}>{i.name}</option>;
                        })}
                    </Field>
                )}

                {(create || ruleSet) && (
                    <Field
                        name="ruleSetCode"
                        type="select"
                        component={FormInputField}
                        label={<FormattedMessage {...fundFormMessages.fundRuleSet} />}
                    >
                        <option key="-ruleSetCode"/>
                        {ruleSets.filter(rs => rs.ruleType === 'ARRANGEMENT').map(i => {
                            return <option value={i.code}>{i.name}</option>;
                        })}
                    </Field>
                )}

                {approve && (
                    <span className="h4">{<FormattedMessage {...fundFormMessages.fundApproveVersionConfirm} />}</span>
                )}

                {(create || update) && (
                    <FieldArray
                        name="scopes"
                        component={ScopesField}
                        label={<FormattedMessage {...fundFormMessages.fundRegScope} />}
                        scopeList={props.scopeList}
                        disabled={submitting}
                    />
                )}

                {create && (
                    <Field
                        name="fundAdmins"
                        component={FormInputField}
                        label={<FormattedMessage {...fundFormMessages.fundFundAdmins} />}
                        as={TagsField}
                        renderTagItem={renderUserOrGroupLabel}
                        fieldComponent={UserAndGroupField}
                        fieldComponentProps={{
                            findUserApi: WebApi.findUserWithFundCreate,
                            findGroupApi: WebApi.findGroupWithFundCreate,
                        }}
                    />
                )}
                {(create || update) && <Field name={"fundNumber"} component={FormInputField} label={<FormattedMessage {...fundFormMessages.fundNumber} />} type={"number"} />}
                {(create || update) && <Field name={"unitdate"} component={FormInputField} label={<FormattedMessage {...fundFormMessages.fundUnitdate} />} />}
                {(create || update) && <Field name={"mark"} component={FormInputField} label={<FormattedMessage {...fundFormMessages.fundMark} />} />}
            </Modal.Body>
            <Modal.Footer>
                {create && (
                    <Button type="submit" variant="outline-secondary" disabled={pristine || submitting}>
                        {<FormattedMessage {...globalMessages.create} />}
                    </Button>
                )}
                {approve && approveButton}
                {(update || ruleSet) && (
                    <Button type="submit" variant="outline-secondary" disabled={pristine || submitting}>
                        {<FormattedMessage {...globalMessages.save} />}
                    </Button>
                )}
                <Button variant="link" onClick={onClose}>
                    {<FormattedMessage {...globalMessages.cancel} />}
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

export default connector(reduxForm<object, IFundForm>({
    form: 'fundForm',
})(FundForm));
