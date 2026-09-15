import React, { useEffect, useState } from 'react';
import { Form as FinalForm, Field } from 'react-final-form';
import { Icon} from 'components/shared';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { getIntl } from 'components/shared/lang/intlInstance';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    scope: { id: 'ap.state.title.scope', defaultMessage: 'Oblast' },
    type: { id: 'ap.state.title.type', defaultMessage: 'Podtřída' },
    state: { id: 'ap.state.title.state', defaultMessage: 'Stav' },
    comment: { id: 'ap.state.title.comment', defaultMessage: 'Komentář' },
    assignedUser: { id: 'ap.state.title.assignedUser', defaultMessage: 'Přiděleno' },
    toApproveSameUser: {
        id: 'ap.state.title.assignedUser.error.toApproveSameUser',
        defaultMessage: 'Záznam může schválit pouze jiný uživatel',
    },
    validationErrors: { id: 'ap.validation.errors', defaultMessage: 'Chyby validace' },
});
import { Form, Modal } from 'react-bootstrap';
import { Button } from '../ui';
import FormInputField from '../../components/shared/form/FormInputField';
import { RevStateApproval, RevStateApprovalCaption } from "../../api/RevStateApproval";
import { ApTypeVO } from 'api/ApTypeVO';
import { Participant, RevStateChange } from 'elza-api';
import { ApValidationErrorsVO } from 'api/ApValidationErrorsVO';
import { UsrUserVO } from 'api/UsrUserVO';
import UserField from 'components/admin/UserField';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { Api } from 'api';

export interface RevStateFormFields extends RevStateChange {
  assignedTo?: number;
}

export interface Props {
    accessPointId: number;
    versionId?: number;
    hideType?: boolean;
    onClose?: () => void;
    onSubmit: (data: RevStateFormFields) => void;
    states: string[];
    initialValues?: Partial<RevStateFormFields>;
}

type FormErrors<T> = Partial<Record<keyof T, string>>;

export const RevStateChangeFormFn = ({
    onClose,
    hideType = false,
    onSubmit,
    initialValues,
    accessPointId,
}: Props) => {
    const intl = useIntl();

    const apTypes = useAppSelector(({refTables}) => refTables.apTypes)
    const { data: validationData } = useAppSelector(({app}) => app.apValidation);
    const {id: currentUserId} = useAppSelector(({ userDetail }) => userDetail);

    const isValid = (!validationData?.errors || validationData.errors?.length <= 0) && (!validationData?.partErrors || validationData.partErrors?.length <= 0);

    const [lastParticipants, setLastParticipants] = useState<Participant[]>([]);

    useEffect(() => {
        (async () => {
            const [{ data: _lastParticipants }] = await Promise.all([
                Api.accesspoints.accessPointGetLastParticipants(accessPointId),
            ]);

            setLastParticipants(_lastParticipants)
        })()
    }, [accessPointId])

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
    const uniqueParticipantsMap = new Map(lastParticipants.map((lastParticipant) => [lastParticipant.userId, lastParticipant]));
    const uniqueParticipants = Array.from(uniqueParticipantsMap.values()).filter(({userId}) => userId !== currentUserId);

    const validate = (values: RevStateFormFields) => {
        const errors: FormErrors<RevStateFormFields> = {};

        if (!values.state) {
            errors.state = getIntl().formatMessage(globalMessages.validationRequired);
        }

        const isToApproveSameUser =
            values.state === RevStateApproval.TO_APPROVE
            && values.assignedTo === currentUserId;
        if (isToApproveSameUser) {
            errors.assignedTo = getIntl().formatMessage(messages.toApproveSameUser)
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
        <FinalForm<RevStateFormFields>
            initialValues={{ ...initialValues}}
            onSubmit={onSubmit}
            validate={validate}
        >
            {({ submitting, handleSubmit, form, values, valid }) => {
                return <Form>
                    <Modal.Body>
                        {!isValid && validationData &&
                            <div className="ap-validation-alert">
                                <h3>{intl.formatMessage(messages.validationErrors)}</h3>
                                {renderValidationErrors(validationData)}
                            </div>
                        }
                        {!hideType && (
                            <Field
                                name={'typeId'}
                                component={FormInputField}
                                type="autocomplete"
                                label={intl.formatMessage(messages.type)}
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
                            label={intl.formatMessage(messages.state)}
                            items={stateOptions}
                            useIdAsValue={true}
                            required={true}
                            disabled={submitting}
                        />
                        <Field
                            name={'comment'}
                            component={FormInputField}
                            type="textarea"
                            label={intl.formatMessage(messages.comment)}
                            disabled={submitting}
                        />
                        <Field<number>
                            name={'assignedTo'}
                        >{({input, meta}) => {
                            function handleChange(user?: UsrUserVO){
                                input.onChange(user?.id);
                            }
                            //@ts-expect-error TODO wrong types on FormInputField
                            return <FormInputField type="static" label={intl.formatMessage(messages.assignedUser)}>
                                <div style={{display: 'flex'}}>
                                    <UserField
                                    disabled={submitting}
                                    value={input.value || undefined}
                                    onChange={handleChange}
                                    all={true}
                                    excludeUserIds={
                                        values.state === RevStateApproval.TO_APPROVE
                                        ? [currentUserId]
                                        : undefined
                                    }
                                    />
                                    {input.value && <div style={{ position: 'absolute', right: '16px' }}>
                                        <Button type="button" variant="subtle" onClick={() => handleChange()}>
                                            <Icon glyph="fa-times" />
                                        </Button>
                                    </div>}
                                </div>
                                {meta.error && <div style={{ color: 'var(--color-red)' }}>
                                    {meta.error}
                                </div>}
                            </FormInputField>
                        }}</Field>
                        {(uniqueParticipants || []).length > 0 && <Field name="lastParticipants">
                            {() => {
                                return <div style={{marginTop: "16px"}}>
                                    {uniqueParticipants.map((participant) => {
                                        function handleClick() {
                                            form.change('assignedTo', participant.userId)
                                        }

                                        return <div style={{margin: '4px 0'}}>
                                            <Button type="button" variant="outline-secondary" onClick={handleClick}>
                                                {participant.name} ({participant.username})
                                            </Button>
                                        </div>
                                    })}
                                </div>
                            }}
                        </Field>}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" onClick={handleSubmit} variant="outline-secondary" disabled={submitting || !valid}>
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

export default RevStateChangeFormFn;
