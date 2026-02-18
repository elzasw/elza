import React, { useEffect, useState } from 'react';
import { Icon, i18n } from 'components/shared';
import { Form, Modal } from 'react-bootstrap';
import { Form as FinalForm, Field } from 'react-final-form';
import { Button } from '../ui';
import Scope from '../shared/scope/Scope';
import FormInputField from '../../components/shared/form/FormInputField';
import { useSelector } from 'react-redux';
import { StateApproval, StateApprovalCaption } from '../../api/StateApproval';
import { AppState } from "typings/store";
import { WebApi } from 'actions';
import { ApTypeVO } from 'api/ApTypeVO';
import UserField from 'components/admin/UserField';
import { Api } from 'api';
import { Participant } from 'elza-api';
import { UsrUserVO } from 'api/UsrUserVO';
import { useAppSelector } from 'utils/hooks/useAppSelector';

const stateToOption = (item: StateApproval) => ({
    id: item,
    name: StateApprovalCaption(item),
});

type Props = {
    accessPointId: number;
    versionId?: number;
    hideType?: boolean;
    onClose?: () => void;
    onSubmit: (data: ApStateChangeVO) => void;
    states: string[];
    scopeId?: number;
    initialValues?: Partial<ApStateChangeVO>;
};

type ApStateChangeVO = {
    state: StateApproval;
    comment: string;
    typeId: number;
    scopeId: number;
    assignedTo?: number;
};

export const ApStateChangeForm = ({
    accessPointId,
    hideType = false,
    versionId,
    onClose,
    onSubmit,
    initialValues,
}: Props) => {
    const scopesData = useSelector((appState: AppState) => appState.refTables.scopesData);
    const apTypes = useSelector((appState: AppState) => appState.refTables.apTypes)
    const {id: currentUserId} = useAppSelector(({ userDetail }) => userDetail);

    const [states, setStates] = useState<string[]>([]);
    const [lastParticipants, setLastParticipants] = useState<Participant[]>([]);

    let preselectedScopeId: number | null | undefined = initialValues?.scopeId;

    if (preselectedScopeId == undefined) {
        const index = scopesData.scopes.findIndex(({versionId: _versionId}) => versionId === _versionId);
        if (index && scopesData.scopes[index].scopes && scopesData.scopes[index].scopes[0].id) {
            preselectedScopeId = scopesData.scopes[index].scopes[0].id
        }
    }

    useEffect(() => {
        (async () => {
            const [states, { data: _lastParticipants }] = await Promise.all([
                WebApi.getStateApproval(accessPointId),
                Api.accesspoints.accessPointGetLastParticipants(accessPointId),
            ]);

            setLastParticipants(_lastParticipants);

            if (states.indexOf(initialValues.state) < 0) {
                states.push(initialValues.state);
            }
            setStates(states);
        })()
    }, [accessPointId])

    const stateOptions = states.map(stateToOption);
    const uniqueParticipantsMap = new Map(lastParticipants.map((lastParticipant) => [lastParticipant.userId, lastParticipant]));
    const uniqueParticipants = Array.from(uniqueParticipantsMap.values()).filter(({userId}) => userId !== currentUserId);

    function handleSubmit(data: ApStateChangeVO) {
        // Remove assigned user, when changing state to 'Approved'
        if (data.state === StateApproval.APPROVED) {
            delete data.assignedTo;
        }

        onSubmit(data)
    }

    function validate(values: ApStateChangeVO){
        const errors:Partial<Record<keyof ApStateChangeVO, string>> = {};
        const isToApproveSameUser =
            values.state === StateApproval.TO_APPROVE
            && values.assignedTo === currentUserId;
        if (isToApproveSameUser) {
            errors.assignedTo = i18n("ap.state.title.assignedUser.error.toApproveSameUser")
        }
        return errors;
    }

    return (
        <FinalForm validate={validate} onSubmit={handleSubmit} initialValues={{ ...initialValues, scopeId: preselectedScopeId }}>
            {({ submitting, handleSubmit, form, values, valid }) => {
                const isApproved = values.state === StateApproval.APPROVED;

                return <Form>
                    <Modal.Body>
                        <Field name={'scopeId'} >
                            {({ input }) => {
                                return <Scope
                                    {...input}
                                    disabled={submitting}
                                    versionId={versionId}
                                    label={i18n('ap.state.title.scope')}
                                />
                            }}
                        </Field>
                        {!hideType && (
                            <Field
                                component={FormInputField}
                                type="autocomplete"
                                label={i18n('ap.state.title.type')}
                                items={apTypes.items ? apTypes.items : []}
                                tree={true}
                                alwaysExpanded={true}
                                allowSelectItem={(item: ApTypeVO) => item.addRecord}
                                name={'typeId'}
                                useIdAsValue={true}
                                disabled={submitting}
                            />
                        )}
                        <Field
                            component={FormInputField}
                            type="autocomplete"
                            disabled={submitting}
                            useIdAsValue
                            required
                            label={i18n('ap.state.title.state')}
                            items={stateOptions}
                            name={'state'}
                        />
                        <Field
                            component={FormInputField}
                            disabled={submitting}
                            type="textarea"
                            label={i18n('ap.state.title.comment')}
                            name={'comment'}
                        />
                        {!isApproved && <>
                            <Field<number>
                                name={'assignedTo'}
                            >{({input, meta}) => {
                                function handleChange(user?: UsrUserVO){
                                    input.onChange(user?.id);
                                }
                                //@ts-expect-error TODO wrong types on FormInputField
                                return <FormInputField {...meta} type="static" label={i18n('ap.state.title.assignedUser')}>
                                    <div style={{display: 'flex'}}>
                                        <UserField
                                        disabled={submitting}
                                        value={input.value || undefined} //workaround for empty string in value
                                        onChange={handleChange}
                                        excludeUserIds={
                                            values.state === StateApproval.TO_APPROVE
                                            ? [currentUserId]
                                            : undefined
                                        }
                                        all={true}
                                        />
                                        {input.value && <div style={{ position: 'absolute', right: '16px' }}>
                                            <Button type="button" variant="subtle" onClick={() => handleChange()}>
                                                <Icon glyph="fa-times" />
                                            </Button>
                                        </div>}
                                    </div>
                                    {(meta.error) && <div style={{ color: 'var(--color-red)' }}>
                                        {meta.error}
                                    </div>}
                                </FormInputField>
                            }}</Field>
                            {(uniqueParticipants || []).length > 0 && <Field name="lastParticipants">
                                {() => {
                                    return <div style={{marginTop: "16px"}}>
                                        {uniqueParticipants.map((participant) => {
                                            function handleClick() {
                                                form.change('assignedTo', participant.userId);
                                            }

                                            return <div style={{margin: '4px 0'}}>
                                                <Button disabled={submitting} type="button" variant="outline-secondary" onClick={handleClick}>
                                                    {participant.name} ({participant.username})
                                                </Button>
                                            </div>
                                        })}
                                    </div>
                                }}
                            </Field>}
                        </>}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button type="submit" variant="outline-secondary" disabled={submitting || !valid} onClick={handleSubmit}>
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
export default ApStateChangeForm;
