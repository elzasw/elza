import React, { useEffect, useState } from 'react';
import { Form as FinalForm, Field } from 'react-final-form';
import { Icon, i18n } from 'components/shared';
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
import { WebApi } from 'actions';

export interface RevStateFormFields extends RevStateChange {
  assignedUser?: Partial<Participant>;
}

export interface Props {
    accessPointId: number;
    versionId?: number;
    hideType?: boolean;
    onClose?: () => void;
    onSubmit: (data: RevStateFormFields) => void;
    states: string[];
    initialValues?: Omit<Partial<RevStateFormFields>, 'assignedUser'> & {assignedTo: number};
}

type FormErrors<T> = Partial<Record<keyof T, string>>;

export const RevStateChangeFormFn = ({
    onClose,
    hideType = false,
    onSubmit,
    initialValues,
    accessPointId,
}: Props) => {

    const apTypes = useAppSelector(({refTables}) => refTables.apTypes)
    const { data: validationData } = useAppSelector(({app}) => app.apValidation);
    const {id: currentUserId} = useAppSelector(({ userDetail }) => userDetail);

    const isValid = (!validationData?.errors || validationData.errors?.length <= 0) && (!validationData?.partErrors || validationData.partErrors?.length <= 0);

    const [lastParticipants, setLastParticipants] = useState<Participant[]>([]);
    const [assignedUser, setAssignedUser] = useState<Partial<Participant>>(undefined);

    useEffect(() => {
        (async () => {
            const [{ data: _lastParticipants }, user] = await Promise.all([
                Api.accesspoints.accessPointGetLastParticipants(accessPointId),
                initialValues?.assignedTo != undefined ? await WebApi.getUser(initialValues?.assignedTo) : Promise.resolve(undefined)
            ]);

            if (user) {
                setAssignedUser({
                    userId: user.id,
                    username: user.username,
                    name: user.accessPoint.name,
                });
            }
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
        <FinalForm<RevStateFormFields>
            initialValues={{ ...initialValues, assignedUser}}
            onSubmit={onSubmit}
            validate={validate}
        >
            {({ submitting, handleSubmit, form }) => {
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
                        <Field<Participant>
                            name={'assignedUser'}
                        >{({input}) => {
                            function handleChange(user?: UsrUserVO){
                                input.onChange(user ? {
                                    name: user.accessPoint.name,
                                    username: user.username,
                                    userId: user.id,
                                } : undefined);
                            }
                            //@ts-expect-error TODO wrong types on FormInputField
                            return <FormInputField type="static" label={i18n('ap.state.title.assignedUser')}>
                                <div style={{display: 'flex'}}>
                                    <UserField
                                    disabled={submitting}
                                    value={input.value ? {
                                        accessPoint: {
                                            name: input.value.name
                                        },
                                        username: input.value.username,
                                        id: input.value.userId,
                                    } : undefined}
                                    onChange={handleChange}
                                    getItemName={(user: UsrUserVO) => {
                                        if(!user?.id){
                                            return "";
                                        }
                                        return `${user.accessPoint?.name} (${user.username})`;
                                    }}
                                    all={true}
                                    />
                                    {input.value && <div style={{ position: 'absolute', right: '16px' }}>
                                        <Button type="button" variant="subtle" onClick={() => handleChange()}>
                                            <Icon glyph="fa-times" />
                                        </Button>
                                    </div>}
                                </div>
                            </FormInputField>
                        }}</Field>
                        {(uniqueParticipants || []).length > 0 && <Field name="lastParticipants">
                            {() => {
                                //@ts-expect-error TODO wrong types on FormInputField
                                return <FormInputField type="static" label={i18n('ap.state.title.lastParticipants')}>
                                    {uniqueParticipants.map((participant) => {
                                        function handleClick() {
                                            form.change('assignedUser', participant)
                                        }

                                        return <div style={{margin: '4px 0'}}>
                                            <Button type="button" variant="outline-secondary" onClick={handleClick}>
                                                {participant.name} ({participant.username})
                                            </Button>
                                        </div>
                                    })}
                                </FormInputField>
                            }}
                        </Field>}
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
