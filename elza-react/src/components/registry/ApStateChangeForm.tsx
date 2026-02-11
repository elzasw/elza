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
    initialValues?: Omit<Partial<ApStateChangeVO>, 'assignedUser'> & { assignedTo: number };
};

type ApStateChangeVO = {
    state: StateApproval;
    comment: string;
    typeId: number;
    scopeId: number;
    assignedUser?: Participant;
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
    const [assignedUser, setAssignedUser] = useState<Partial<Participant>>(undefined);

    let preselectedScopeId: number | null | undefined = initialValues?.scopeId;

    if (preselectedScopeId == undefined) {
        const index = scopesData.scopes.findIndex(({versionId: _versionId}) => versionId === _versionId);
        if (index && scopesData.scopes[index].scopes && scopesData.scopes[index].scopes[0].id) {
            preselectedScopeId = scopesData.scopes[index].scopes[0].id
        }
    }

    useEffect(() => {
        (async () => {
            const [states, { data: _lastParticipants }, user] = await Promise.all([
                WebApi.getStateApproval(accessPointId),
                Api.accesspoints.accessPointGetLastParticipants(accessPointId),
                initialValues?.assignedTo != undefined ? await WebApi.getUser(initialValues?.assignedTo) : Promise.resolve(undefined)
            ]);

            setLastParticipants(_lastParticipants);
            if (user) {
                setAssignedUser({
                    userId: user.id,
                    username: user.username,
                    name: user.accessPoint.name,
                });
            }

            if (states.indexOf(initialValues.state) < 0) {
                states.push(initialValues.state);
            }
            setStates(states);
        })()
    }, [accessPointId])

    const stateOptions = states.map(stateToOption);
    const uniqueParticipantsMap = new Map(lastParticipants.map((lastParticipant) => [lastParticipant.userId, lastParticipant]));
    const uniqueParticipants = Array.from(uniqueParticipantsMap.values()).filter(({userId}) => userId !== currentUserId);

    return (
        <FinalForm onSubmit={onSubmit} initialValues={{ ...initialValues, scopeId: preselectedScopeId, assignedUser }}>
            {({ submitting, handleSubmit, form }) => {
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
                        <Button type="submit" variant="outline-secondary" disabled={submitting} onClick={handleSubmit}>
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
