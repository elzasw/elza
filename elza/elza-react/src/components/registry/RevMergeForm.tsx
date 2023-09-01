import React, { useEffect, useState } from 'react';
import { i18n } from 'components/shared';
import { Form, Modal } from 'react-bootstrap';
import { Form as FinalForm, Field } from 'react-final-form';
import { Button } from '../ui';
import FormInputField from '../../components/shared/form/FormInputField';
import { StateApprovalCaption } from '../../api/StateApproval';
import { WebApi } from 'actions';
import { ApStateUpdate, ApStateApproval } from 'elza-api';

const stateToOption = (item: ApStateApproval) => ({
    id: item,
    name: StateApprovalCaption(item),
});

type Props = {
    accessPointId: number;
    onClose?: Function;
    onSubmit: (values: ApStateUpdate) => void;
    states: string[];
    initialValues: ApStateUpdate;
};

export function RevMergeFormFn({
    accessPointId,
    onClose,
    onSubmit,
    initialValues
}: Props) {

    const [states, setStates] = useState<ApStateApproval[]>([]);

    function getStateWithAll() {
        if (states) {
            return Object.values(states).map(stateToOption);
        } else {
            return [];
        }
    }

    function validate(values: ApStateUpdate) {
        const errors: Partial<Record<keyof ApStateUpdate, string>> = {};

        if (!values.stateApproval) {
            errors.stateApproval = i18n('global.validation.required');
        }

        return errors;
    }

    useEffect(() => {
        (async () => {
            const data: ApStateApproval[] = await WebApi.getStateApprovalRevision(accessPointId);
            setStates(data);
        })()
    }, [accessPointId])

    return (
        <FinalForm<ApStateUpdate> initialValues={initialValues} validate={validate} onSubmit={onSubmit}>
            {({ handleSubmit, submitting }) => {
                return <Form onSubmit={handleSubmit}>
                    <Modal.Body>
                        <Field
                            component={FormInputField}
                            type="autocomplete"
                            disabled={submitting}
                            useIdAsValue
                            required
                            label={i18n('ap.state.title.state')}
                            items={getStateWithAll()}
                            name={'stateApproval'}
                        />
                        <Field
                            component={FormInputField}
                            disabled={submitting}
                            type="textarea"
                            label={i18n('ap.state.title.comment')}
                            name={'comment'}
                        />
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="button" onClick={handleSubmit} variant="outline-secondary" disabled={submitting}>
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

export default RevMergeFormFn
