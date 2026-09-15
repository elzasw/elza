import React, { useEffect, useState } from 'react';
import {} from 'components/shared';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { getIntl } from 'components/shared/lang/intlInstance';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    state: { id: 'ap.state.title.state', defaultMessage: 'Stav' },
    comment: { id: 'ap.state.title.comment', defaultMessage: 'Komentář' },
    sendToSystem: { id: 'ap.revMerge.sendToSystem', defaultMessage: 'Změny zapsat do {system}' },
});
import { Form, Modal } from 'react-bootstrap';
import { Form as FinalForm, Field } from 'react-final-form';
import { Button } from '../ui';
import FormInputField from '../../components/shared/form/FormInputField';
import { StateApprovalCaption } from '../../api/StateApproval';
import { WebApi } from 'actions';
import { ApStateUpdate, ApStateApproval } from 'elza-api';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import * as perms from 'actions/user/Permission';
import { ExtEntityBinding } from 'elza-api';

const stateToOption = (item: ApStateApproval) => ({
    id: item,
    name: StateApprovalCaption(item),
});

type Props = {
    accessPointId: number;
    onClose?: Function;
    onSubmit: (values: ApStateUpdate) => void;
    states: string[];
    bindings?: ExtEntityBinding[];
    initialValues: ApStateUpdate;
};

export function RevMergeFormFn({
    accessPointId,
    onClose,
    onSubmit,
    bindings,
    initialValues
}: Props) {
    const intl = useIntl();

    const [states, setStates] = useState<ApStateApproval[]>([]);
    const userDetail = useSelector(({ userDetail }: AppState) => userDetail);

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
            errors.stateApproval = getIntl().formatMessage(globalMessages.validationRequired);
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
                            label={intl.formatMessage(messages.state)}
                            items={getStateWithAll()}
                            name={'stateApproval'}
                        />
                        <Field
                            component={FormInputField}
                            disabled={submitting}
                            type="textarea"
                            label={intl.formatMessage(messages.comment)}
                            name={'comment'}
                        />
                        {bindings && bindings.length === 1
                            && userDetail.hasOne(perms.AP_EXTERNAL_WR)
                            &&  <Field<boolean>
                                    name={'sendToCam'}
                                    component={FormInputField}
                                    label={<span><FormattedMessage {...messages.sendToSystem} values={{ system: bindings[0].externalSystemCode }} /></span>}
                                    type='checkbox'
                                    defaultValue={true}>
                                </Field>
                        }
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="button" onClick={handleSubmit} variant="outline-secondary" disabled={submitting}>
                            <FormattedMessage {...globalMessages.save} />
                        </Button>
                        <Button variant="link" onClick={onClose}>
                            <FormattedMessage {...globalMessages.cancel} />
                        </Button>
                    </Modal.Footer>
                </Form>
            }}
        </FinalForm>
    );
}

export default RevMergeFormFn
