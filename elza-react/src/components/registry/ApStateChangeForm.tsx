import React, { useEffect, useState } from 'react';
import { i18n } from 'components/shared';
import { Form, Modal } from 'react-bootstrap';
import { Form as FinalForm, Field } from 'react-final-form';
import { Button } from '../ui';
import { indexByProperty } from 'stores/app/utils';
import Scope from '../shared/scope/Scope';
import FormInputField from '../../components/shared/form/FormInputField';
import { useSelector } from 'react-redux';
import { StateApproval, StateApprovalCaption } from '../../api/StateApproval';
import { AppState, UserDetail } from "typings/store";
import { WebApi } from 'actions';
import { ApTypeVO } from 'api/ApTypeVO';
import UserField from 'components/admin/UserField';
import { Api } from 'api';
import { Participant } from 'elza-api';
import { UserInfoVO } from 'api/UserInfoVO';
import { UserVO } from 'api/UserVO';
import { UsrUserVO } from 'api/UsrUserVO';

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

    const [states, setStates] = useState<string[]>([]);
  // const [lastParticipants, setLastParticipants] = useState<Participant[]>([]);

    let preselectedScopeId: number | null | undefined = initialValues?.scopeId;
    if (preselectedScopeId == undefined) {
        let index = scopesData.scopes ? indexByProperty(scopesData.scopes, versionId, "versionId") : false;
        if (index && scopesData.scopes[index].scopes && scopesData.scopes[index].scopes[0].id) {
            preselectedScopeId = scopesData.scopes[index].scopes[0].id
        }
    }

    useEffect(() => {
        (async () => {
            const data = await WebApi.getStateApproval(accessPointId)
            // const {data: _lastParticipants} = await Api.accesspoints.accessPointGetLastParticipants(accessPointId);
            // setLastParticipants(_lastParticipants)
            setStates(data)
        })()
    }, [accessPointId])

    const stateOptions = states.map(stateToOption);

    return (
        <FinalForm onSubmit={onSubmit} initialValues={{ ...initialValues, scopeId: preselectedScopeId }}>
            {({ submitting, handleSubmit }) => {
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
                        <Field
                            name={'assignedUser'}
                        >{({input}) => {
                          function handleChange(user: UsrUserVO){
                            input.onChange(user);
                          }
                          //@ts-expect-error TODO wrong types on FormInputField
                          return <FormInputField type="static" label={i18n('ap.state.title.assignedUser')}>
                            <UserField
                            disabled={submitting}
                            value={input.value}
                            onChange={handleChange}
                              getItemName={(user: UsrUserVO) => {
                                if(!user){
                                  return "";
                                }
                                return `${user.accessPoint?.name} (${user.username})`;
                              }}
                            />
                          </FormInputField>
                        }}</Field>
                        {/*<Field name="lastParticipants">
                          {() => {
                            //@ts-expect-error TODO wrong types on FormInputField
                            return <FormInputField type="static" label={i18n('ap.state.title.lastParticipants')}>
                              {lastParticipants.map((particpant) => {
                                return <div>
                                  {particpant.name} ({particpant.username})
                                </div>
                              })}
                            </FormInputField>
                          }}
                        </Field>*/}
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
