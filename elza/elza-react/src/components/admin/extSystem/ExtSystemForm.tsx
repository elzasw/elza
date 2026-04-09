import React, { useEffect, useState } from 'react';
import { Form, Modal } from 'react-bootstrap';
import { Form as FinalForm, Field, useFormState } from 'react-final-form';
import { Button } from 'components/ui';
import { i18n } from 'components';
import { FormInputField } from 'components/shared';
import { JAVA_ATTR_CLASS, GisSystemType, AP_EXT_SYSTEM_TYPE } from '../../../constants';
import { useAppThunkDispatch } from 'utils/hooks';
import { WebApi } from 'actions/index.jsx';
import { modalDialogHide } from 'actions/global/modalDialog';

export const EXT_SYSTEM_CLASS = {
    ApExternalSystem: '.ApExternalSystemVO',
    ArrDigitalRepository: '.ArrDigitalRepositoryVO',
    ArrDigitizationFrontdesk: '.ArrDigitizationFrontdeskVO',
    GisExternalSystem: '.GisExternalSystemVO',
} as const;

type ExtSystemClassValue = typeof EXT_SYSTEM_CLASS[keyof typeof EXT_SYSTEM_CLASS];

export const EXT_SYSTEM_CLASS_LABEL: Record<ExtSystemClassValue, string> = {
    [EXT_SYSTEM_CLASS.ApExternalSystem]: i18n('admin.extSystem.class.ApExternalSystemVO'),
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: i18n('admin.extSystem.class.ArrDigitalRepositoryVO'),
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: i18n('admin.extSystem.class.ArrDigitizationFrontdeskVO'),
    [EXT_SYSTEM_CLASS.GisExternalSystem]: i18n('admin.extSystem.class.GisExternalSystemVO'),
};

export const AP_EXT_SYSTEM_LABEL: Record<string, string> = {
    [AP_EXT_SYSTEM_TYPE.CAM]: i18n('admin.extSystem.cam'),
    [AP_EXT_SYSTEM_TYPE.CAM_V2]: i18n('admin.extSystem.cam-v2'),
    [AP_EXT_SYSTEM_TYPE.CAM_UUID]: i18n('admin.extSystem.cam-uuid'),
    [AP_EXT_SYSTEM_TYPE.CAM_COMPLETE]: i18n('admin.extSystem.cam-complete'),
    [AP_EXT_SYSTEM_TYPE.CAM_COMPLETE_V2]: i18n('admin.extSystem.cam-complete-v2'),
};

export const GIS_SYSTEM_TYPE_LABEL: Record<string, string> = {
    [GisSystemType.FrameApiView]: i18n('admin.extSystem.gis-view'),
    [GisSystemType.FrameApiEdit]: i18n('admin.extSystem.gis-edit'),
};

type ExtSystemFormValues = {
    id?: number;
    [JAVA_ATTR_CLASS]?: ExtSystemClassValue;
    code?: string;
    name?: string;
    url?: string;
    username?: string;
    password?: string;
    elzaCode?: string;
    type?: string;
    apiKeyId?: string;
    apiKeyValue?: string;
    publishOnlyApproved?: boolean;
    userInfo?: string;
    scope?: number;
    viewDaoUrl?: string;
    viewFileUrl?: string;
    viewThumbnailUrl?: string;
    sendNotification?: boolean;
};

type Scope = {
    id: number;
    name: string;
};

type Props = {
    initialValues?: ExtSystemFormValues;
    onSubmitForm: (data: ExtSystemFormValues) => Promise<unknown>;
};

const REQUIRED_FIELDS = {
    abstractExtSystem: [JAVA_ATTR_CLASS, 'code', 'name'] as string[],
    [EXT_SYSTEM_CLASS.ApExternalSystem]: ['type', 'apiKeyId', 'apiKeyValue', 'url'] as string[],
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: ['sendNotification'] as string[],
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: [] as string[],
    [EXT_SYSTEM_CLASS.GisExternalSystem]: ['type', 'url'] as string[],
};

function validate(values: ExtSystemFormValues) {
    const classJ = values[JAVA_ATTR_CLASS];
    let requiredFields = [...REQUIRED_FIELDS.abstractExtSystem];

    if (classJ === EXT_SYSTEM_CLASS.ApExternalSystem) {
        requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.ApExternalSystem]);
    } else if (classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository) {
        requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.ArrDigitalRepository]);
    } else if (classJ === EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk) {
        requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]);
    } else if (classJ === EXT_SYSTEM_CLASS.GisExternalSystem) {
        requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.GisExternalSystem]);
    }

    return requiredFields.reduce((errors: Record<string, string>, name) => {
        if (!(values as Record<string, unknown>)[name]) {
            errors[name] = i18n('global.validation.required');
        }
        return errors;
    }, {});
}

const ExtSystemFormFields = ({ isUpdate, defaultScopes }: { isUpdate: boolean; defaultScopes: Scope[] }) => {
    const { values, submitting } = useFormState<ExtSystemFormValues>();
    const classJ = values[JAVA_ATTR_CLASS];

    return (
        <Modal.Body>
            <Field
                name={JAVA_ATTR_CLASS}
                type="select"
                component={FormInputField}
                label={i18n('admin.extSystem.class')}
                disabled={isUpdate}
            >
                <option key={null} />
                {Object.values(EXT_SYSTEM_CLASS).map((i, index) => (
                    <option key={index} value={i}>
                        {EXT_SYSTEM_CLASS_LABEL[i]}
                    </option>
                ))}
            </Field>

            {classJ === EXT_SYSTEM_CLASS.ApExternalSystem && (
                <div>
                    <Field
                        name="type"
                        type="select"
                        component={FormInputField}
                        label={i18n('admin.extSystem.type')}
                        disabled={isUpdate}
                    >
                        <option key={null} />
                        {Object.values(AP_EXT_SYSTEM_TYPE).map((i, index) => (
                            <option key={index} value={i}>
                                {AP_EXT_SYSTEM_LABEL[i]}
                            </option>
                        ))}
                    </Field>
                    <Field
                        name="scope"
                        type="select"
                        component={FormInputField}
                        label={i18n('admin.extSystem.sysScope')}
                    >
                        <option key={null} />
                        {defaultScopes.map((i, index) => (
                            <option key={index} value={i.id}>
                                {i.name}
                            </option>
                        ))}
                    </Field>
                </div>
            )}
            {classJ === EXT_SYSTEM_CLASS.GisExternalSystem && (
                <div>
                    <Field
                        name="type"
                        type="select"
                        component={FormInputField}
                        label={i18n('admin.extSystem.type')}
                        disabled={isUpdate}
                    >
                        <option key={null} />
                        {Object.values(GisSystemType).map((i, index) => (
                            <option key={index} value={i}>
                                {GIS_SYSTEM_TYPE_LABEL[i]}
                            </option>
                        ))}
                    </Field>
                </div>
            )}
            {classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository && (
                <div>
                    <Field
                        name="viewDaoUrl"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.viewDaoUrl')}
                    />
                    <Field
                        name="viewFileUrl"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.viewFileUrl')}
                    />
                    <Field
                        name="viewThumbnailUrl"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.viewThumbnailUrl')}
                    />
                    <Field
                        name="sendNotification"
                        type="select"
                        component={FormInputField}
                        label={i18n('admin.extSystem.sendNotification')}
                    >
                        <option key={null} />
                        <option key="true" value={true as any}>
                            {i18n('admin.extSystem.sendNotification.true')}
                        </option>
                        <option key="false" value={false as any}>
                            {i18n('admin.extSystem.sendNotification.false')}
                        </option>
                    </Field>
                </div>
            )}
            {classJ === EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk && <div />}
            <Field
                name="code"
                type="text"
                component={FormInputField}
                label={i18n('admin.extSystem.code')}
                disabled={isUpdate}
            />
            <Field name="name" type="text" component={FormInputField} label={i18n('admin.extSystem.name')} />
            <Field name="url" type="text" component={FormInputField} label={i18n('admin.extSystem.url')} />
            {classJ !== EXT_SYSTEM_CLASS.ApExternalSystem && classJ !== EXT_SYSTEM_CLASS.GisExternalSystem && (
                <>
                    <Field
                        name="username"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.username')}
                    />
                    <Field
                        name="password"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.password')}
                    />
                </>
            )}
            {classJ !== EXT_SYSTEM_CLASS.ApExternalSystem && classJ !== EXT_SYSTEM_CLASS.GisExternalSystem && (
                <Field
                    name="elzaCode"
                    type="text"
                    component={FormInputField}
                    label={i18n('admin.extSystem.elzaCode')}
                />
            )}
            {(classJ === EXT_SYSTEM_CLASS.ApExternalSystem || classJ === EXT_SYSTEM_CLASS.GisExternalSystem) && (
                <>
                    <Field
                        name="apiKeyId"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.apiKeyId')}
                    />
                    <Field
                        name="apiKeyValue"
                        type="text"
                        component={FormInputField}
                        label={i18n('admin.extSystem.apiKeyValue')}
                    />
                </>
            )}
            {classJ === EXT_SYSTEM_CLASS.ApExternalSystem && (
                <>
                    <div title={i18n('admin.extSystem.userInfo.title')}>
                        <Field
                            name="userInfo"
                            type="text"
                            component={FormInputField}
                            label={i18n('admin.extSystem.userInfo')}
                        />
                    </div>
                    <Field
                        name="publishOnlyApproved"
                        type="select"
                        component={FormInputField}
                        label={i18n('admin.extSystem.publishOnlyApproved')}
                    >
                        <option key={null} />
                        <option key="true" value={true as any}>
                            {i18n('admin.extSystem.publishOnlyApproved.true')}
                        </option>
                        <option key="false" value={false as any}>
                            {i18n('admin.extSystem.publishOnlyApproved.false')}
                        </option>
                    </Field>
                </>
            )}
        </Modal.Body>
    );
};

const ExtSystemForm = ({ initialValues, onSubmitForm }: Props) => {
    const [defaultScopes, setDefaultScopes] = useState<Scope[]>([]);
    const isUpdate = !!initialValues?.id;
    const dispatch = useAppThunkDispatch();

    const handleSubmit = async (values: ExtSystemFormValues) => {
        await onSubmitForm(values);
        dispatch(modalDialogHide());
    };

    useEffect(() => {
        WebApi.getAllScopes().then((json: Scope[]) => {
            setDefaultScopes(json);
        });
    }, []);

    return (
        <FinalForm<ExtSystemFormValues>
            onSubmit={handleSubmit}
            validate={validate}
            initialValues={initialValues}
        >
            {({ handleSubmit, submitting, pristine }) => (
                <Form onSubmit={handleSubmit}>
                    <ExtSystemFormFields isUpdate={isUpdate} defaultScopes={defaultScopes} />
                    <Modal.Footer>
                        <Button type="submit" variant="outline-secondary" disabled={pristine || submitting}>
                            {isUpdate ? i18n('admin.extSystem.submit.edit') : i18n('admin.extSystem.submit.add')}
                        </Button>
                    </Modal.Footer>
                </Form>
            )}
        </FinalForm>
    );
};

export default ExtSystemForm;
