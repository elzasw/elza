import React from 'react';
import { Form as FinalForm, Field } from 'react-final-form';
import { i18n } from 'components/shared';
import { Form, Modal } from 'react-bootstrap';
import { Button } from '../ui';
import FormInputField from '../../components/shared/form/FormInputField';
import { useSelector } from 'react-redux';
import { AppState } from "typings/store";
import { RevStateApproval, RevStateApprovalCaption } from "../../api/RevStateApproval";
import { ApTypeVO } from 'api/ApTypeVO';
import { RevStateChange } from 'elza-api';
import { ApValidationErrorsVO } from 'api/ApValidationErrorsVO';

interface Props {
    accessPointId: number;
    versionId?: number;
    hideType?: boolean;
    onClose?: Function;
    onSubmit: (data: any) => void;
    states: string[];
    initialValues?: Partial<RevStateChange>;
};

type FormErrors<T> = Partial<Record<keyof T, string>>;

export const RevStateChangeFormFn = ({
    onClose,
    hideType = false,
    onSubmit,
    initialValues,
}: Props) => {

    const apTypes = useSelector((appState: AppState) => appState.refTables.apTypes)
    const { data: validationData } = useSelector((appState: AppState) => appState.app.apValidation);
    const isValid = (!validationData?.errors || validationData.errors?.length <= 0) && (!validationData?.partErrors || validationData.partErrors?.length <= 0);

    const getStateOptions = () => {

        const options = [
            RevStateApproval.ACTIVE,
            RevStateApproval.TO_AMEND,
        ]

        if (isValid) {
            options.push(RevStateApproval.TO_APPROVE)
        }

        const stateToOption = (item: RevStateApproval) => ({
            id: item,
            name: RevStateApprovalCaption(item),
        });

        return options.map(stateToOption)
    }

    const stateOptions = getStateOptions();

    const validate = (values: RevStateChange) => {
        const errors: FormErrors<RevStateChange> = {};

        if (!values.state) {
            errors.state = i18n('global.validation.required');
        }

        return errors;
    }

    const renderValidationErrors = (errors: ApValidationErrorsVO) => {
        return <ul>
            {errors?.errors?.map((value, index) => (
                <li key={index}>
                    {value}
                </li>
            ))}
            {errors?.partErrors?.map((value, index) => (
                <ul>
                    <li key={index}>
                        {value?.errors?.map((value, index) => (
                            <li key={index}>
                                {value}
                            </li>
                        ))}
                    </li>
                </ul>
            ))}
        </ul>
    };

    return (
        <FinalForm<RevStateChange>
            initialValues={initialValues}
            onSubmit={onSubmit}
            validate={validate}
        >
            {({ submitting, handleSubmit }) => {
                return <Form>
                    <Modal.Body>
                        {!isValid && validationData &&
                            <div className="ap-validation-alert">
                                <h3>{i18n('ap.validation.errors')}</h3>
                                {renderValidationErrors(validationData)}
                            </div>
                        }
                        {!hideType && (
                            <Field
                                name={'typeId'}
                                component={FormInputField}
                                type="autocomplete"
                                label={i18n('ap.state.title.type')}
                                items={apTypes.items ? apTypes.items : []}
                                tree={true}
                                alwaysExpanded={true}
                                allowSelectItem={(item: ApTypeVO) => item.addRecord}
                                useIdAsValue={true}
                                disabled={submitting}
                            />
                        )}
                        <Field
                            name={'state'}
                            component={FormInputField}
                            type="autocomplete"
                            label={i18n('ap.state.title.state')}
                            items={stateOptions}
                            useIdAsValue={true}
                            required={true}
                            disabled={submitting}
                        />
                        <Field
                            name={'comment'}
                            component={FormInputField}
                            type="textarea"
                            label={i18n('ap.state.title.comment')}
                            disabled={submitting}
                        />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" onClick={handleSubmit} variant="outline-secondary" disabled={submitting}>
                            {i18n('global.action.store')}
                        </Button>
                        <Button variant="link" onClick={onClose}>
                            {i18n('global.action.cancel')}
                        </Button>
                    </Modal.Footer>
                </Form>
            }}
        </FinalForm>
    );
}

export default RevStateChangeFormFn;
