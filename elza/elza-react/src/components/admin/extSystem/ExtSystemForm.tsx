import React, { useEffect, useState } from 'react';
import { Form, Modal, Col, Row } from 'react-bootstrap';
import { Form as FinalForm, Field, useFormState } from 'react-final-form';
import { defineMessages, useIntl, MessageDescriptor } from 'react-intl';
import { Button } from 'components/ui';
import { globalMessages } from 'components/shared/lang';
import { getIntl } from 'components/shared/lang/intlInstance';
import { FormInputField } from 'components/shared';
import {
    JAVA_ATTR_CLASS,
    GisSystemType,
    AP_EXT_SYSTEM_TYPE,
    DigitalRepositoryType,
    DaDownloadMethod,
    DaOnReceivedAction,
} from '../../../constants';
import { useAppThunkDispatch } from 'utils/hooks';
import { WebApi } from 'actions/index.jsx';
import { modalDialogHide } from 'actions/global/modalDialog';

export const EXT_SYSTEM_CLASS = {
    ApExternalSystem: '.ApExternalSystemVO',
    ArrDigitalRepository: '.ArrDigitalRepositoryVO',
    ArrDigitizationFrontdesk: '.ArrDigitizationFrontdeskVO',
    GisExternalSystem: '.GisExternalSystemVO',
    AiExternalSystem: '.AiExternalSystemVO',
} as const;

type ExtSystemClassValue = typeof EXT_SYSTEM_CLASS[keyof typeof EXT_SYSTEM_CLASS];

// Popisky voleb. Dřív se skládaly voláním helperu i18n při načtení modulu, tedy
// dávno před tím, než je znám jazyk - po přepnutí by zůstaly v původním jazyce.
// Deskriptor se formátuje až na místě použití.
export const extSystemTypeMessages = defineMessages({
    classApExternalSystem: {
        id: 'admin.extSystem.class.ApExternalSystemVO',
        defaultMessage: 'Externí systém pro rejstříky/osoby',
    },
    classArrDigitalRepository: {
        id: 'admin.extSystem.class.ArrDigitalRepositoryVO',
        defaultMessage: 'Uložiště digitalizátů',
    },
    classArrDigitizationFrontdesk: {
        id: 'admin.extSystem.class.ArrDigitizationFrontdeskVO',
        defaultMessage: 'Digitalizační linka',
    },
    classGisExternalSystem: {
        id: 'admin.extSystem.class.GisExternalSystemVO',
        defaultMessage: 'Systém GIS (mapové podklady)',
    },
    classAiExternalSystem: {
        id: 'admin.extSystem.class.AiExternalSystemVO',
        defaultMessage: 'Poskytovatel AI služeb',
    },
    cam: { id: 'admin.extSystem.cam', defaultMessage: 'CAM' },
    camV2: { id: 'admin.extSystem.cam-v2', defaultMessage: 'CAM v2' },
    camUuid: { id: 'admin.extSystem.cam-uuid', defaultMessage: 'CAM - UUID' },
    camComplete: { id: 'admin.extSystem.cam-complete', defaultMessage: 'CAM - Kompletní' },
    camCompleteV2: { id: 'admin.extSystem.cam-complete-v2', defaultMessage: 'CAM - Kompletní v2' },
    gisView: { id: 'admin.extSystem.gis-view', defaultMessage: 'Zobrazení' },
    gisEdit: { id: 'admin.extSystem.gis-edit', defaultMessage: 'Editace' },
    wsdl: { id: 'admin.extSystem.wsdl', defaultMessage: 'WSDL' },
    filesystem: { id: 'admin.extSystem.filesystem', defaultMessage: 'Souborový systém' },
    da: { id: 'admin.extSystem.da', defaultMessage: 'Digitální archiv' },
});

export const EXT_SYSTEM_CLASS_MESSAGE: Record<ExtSystemClassValue, MessageDescriptor> = {
    [EXT_SYSTEM_CLASS.ApExternalSystem]: extSystemTypeMessages.classApExternalSystem,
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: extSystemTypeMessages.classArrDigitalRepository,
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: extSystemTypeMessages.classArrDigitizationFrontdesk,
    [EXT_SYSTEM_CLASS.GisExternalSystem]: extSystemTypeMessages.classGisExternalSystem,
    [EXT_SYSTEM_CLASS.AiExternalSystem]: extSystemTypeMessages.classAiExternalSystem,
};

export const AP_EXT_SYSTEM_MESSAGE: Record<string, MessageDescriptor> = {
    [AP_EXT_SYSTEM_TYPE.CAM]: extSystemTypeMessages.cam,
    [AP_EXT_SYSTEM_TYPE.CAM_V2]: extSystemTypeMessages.camV2,
    [AP_EXT_SYSTEM_TYPE.CAM_UUID]: extSystemTypeMessages.camUuid,
    [AP_EXT_SYSTEM_TYPE.CAM_COMPLETE]: extSystemTypeMessages.camComplete,
    [AP_EXT_SYSTEM_TYPE.CAM_COMPLETE_V2]: extSystemTypeMessages.camCompleteV2,
};

export const GIS_SYSTEM_TYPE_MESSAGE: Record<string, MessageDescriptor> = {
    [GisSystemType.FrameApiView]: extSystemTypeMessages.gisView,
    [GisSystemType.FrameApiEdit]: extSystemTypeMessages.gisEdit,
};

export const DIGITAL_REPOSITORY_TYPE_MESSAGE: Record<string, MessageDescriptor> = {
    [DigitalRepositoryType.Wsdl]: extSystemTypeMessages.wsdl,
    [DigitalRepositoryType.Filesystem]: extSystemTypeMessages.filesystem,
    [DigitalRepositoryType.Da]: extSystemTypeMessages.da,
};

/**
 * Popisky polí. Exportované, protože stejná pole vypisuje i detail
 * (AdminExtSystemDetail), kde se klíč skládal z názvu pole - množina je
 * uzavřená, takže stačí indexovat tuhle mapu.
 */
export const fieldMessages = defineMessages({
    class: { id: 'admin.extSystem.class', defaultMessage: 'Třída' },
    name: { id: 'admin.extSystem.name', defaultMessage: 'Název' },
    code: { id: 'admin.extSystem.code', defaultMessage: 'Kód' },
    url: { id: 'admin.extSystem.url', defaultMessage: 'URL' },
    username: { id: 'admin.extSystem.username', defaultMessage: 'Username' },
    password: { id: 'admin.extSystem.password', defaultMessage: 'Heslo' },
    elzaCode: { id: 'admin.extSystem.elzaCode', defaultMessage: 'Kód ELZA' },
    type: { id: 'admin.extSystem.type', defaultMessage: 'Typ' },
    sysScope: { id: 'admin.extSystem.sysScope', defaultMessage: 'Oblast entit' },
    syncDelay: { id: 'admin.extSystem.syncDelay', defaultMessage: 'Interval synchronizace (s)' },
    viewDaoUrl: { id: 'admin.extSystem.viewDaoUrl', defaultMessage: 'DaoURL' },
    viewFileUrl: { id: 'admin.extSystem.viewFileUrl', defaultMessage: 'FileURL' },
    viewThumbnailUrl: { id: 'admin.extSystem.viewThumbnailUrl', defaultMessage: 'ThumbnailURL' },
    sendNotification: { id: 'admin.extSystem.sendNotification', defaultMessage: 'Zasílání upozornění' },
    sendNotificationTrue: { id: 'admin.extSystem.sendNotification.true', defaultMessage: 'Ano' },
    sendNotificationFalse: { id: 'admin.extSystem.sendNotification.false', defaultMessage: 'Ne' },
    multipleLinks: { id: 'admin.extSystem.multipleLinks', defaultMessage: 'Vícenásobné napojení' },
    multipleLinksTrue: { id: 'admin.extSystem.multipleLinks.true', defaultMessage: 'Ano' },
    multipleLinksFalse: { id: 'admin.extSystem.multipleLinks.false', defaultMessage: 'Ne' },
    apiKeyId: { id: 'admin.extSystem.apiKeyId', defaultMessage: 'ApiKey - ID' },
    apiKeyValue: { id: 'admin.extSystem.apiKeyValue', defaultMessage: 'ApiKey - hodnota' },
    userInfo: { id: 'admin.extSystem.userInfo', defaultMessage: 'Autor změny - šablona' },
    userInfoTitle: {
        id: 'admin.extSystem.userInfo.title',
        defaultMessage: "Šablona pro označení uživatele, které je předáváno do externího systému, jako osoby zodpovědné za provedení a zaslání změny.\nStandardně obsahuje 'název instituce: jméno uživatele', pro vyplnění jména lze použít proměnné:\n%u - uživatelské jméno\n%i - ID uživatele\n%n - preferované označení osoby uživatele\n%s - zkrácené označení osoby uživatele",
    },
    publishOnlyApproved: { id: 'admin.extSystem.publishOnlyApproved', defaultMessage: 'Odeslání jen schválených' },
    publishOnlyApprovedTrue: { id: 'admin.extSystem.publishOnlyApproved.true', defaultMessage: 'Ano' },
    publishOnlyApprovedFalse: { id: 'admin.extSystem.publishOnlyApproved.false', defaultMessage: 'Ne' },
    submitAdd: { id: 'admin.extSystem.submit.add', defaultMessage: 'Přidat' },
    submitEdit: { id: 'admin.extSystem.submit.edit', defaultMessage: 'Upravit' },
});

export const daSettingsMessages = defineMessages({
    downloadMethod: {
        id: 'admin.extSystem.downloadMethod',
        defaultMessage: 'Způsob stahování AIP',
    },
    downloadMethodStandard: {
        id: 'admin.extSystem.downloadMethodStandard',
        defaultMessage: 'Standardní (HTTP)',
    },
    downloadMethodFileTransfer: {
        id: 'admin.extSystem.downloadMethodFileTransfer',
        defaultMessage: 'File Transfer',
    },
    onReceived: {
        id: 'admin.extSystem.onReceived',
        defaultMessage: 'Po přijetí AIP',
    },
    onReceivedNone: {
        id: 'admin.extSystem.onReceivedNone',
        defaultMessage: 'Nic nedělat',
    },
    onReceivedDownloadMetadata: {
        id: 'admin.extSystem.onReceivedDownloadMetadata',
        defaultMessage: 'Stáhnout metadata',
    },
    syncDelay: {
        id: 'admin.extSystem.daSyncDelay',
        defaultMessage: 'Interval synchronizace (s), 0 = bez synchronizace',
    },
});

export const DA_DOWNLOAD_METHOD_MESSAGE: Record<string, MessageDescriptor> = {
    [DaDownloadMethod.Standard]: daSettingsMessages.downloadMethodStandard,
    [DaDownloadMethod.FileTransfer]: daSettingsMessages.downloadMethodFileTransfer,
};

export const DA_ON_RECEIVED_MESSAGE: Record<string, MessageDescriptor> = {
    [DaOnReceivedAction.None]: daSettingsMessages.onReceivedNone,
    [DaOnReceivedAction.DownloadMetadata]: daSettingsMessages.onReceivedDownloadMetadata,
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
    digitalRepositoryType?: string;
    multipleLinks?: boolean;
    downloadMethod?: string;
    onReceived?: string;
    syncDelay?: number;
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
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: ['digitalRepositoryType', 'sendNotification'] as string[],
    daRepository: ['downloadMethod', 'onReceived'] as string[],
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: [] as string[],
    [EXT_SYSTEM_CLASS.GisExternalSystem]: ['type', 'url'] as string[],
    [EXT_SYSTEM_CLASS.AiExternalSystem]: ['url'] as string[],
};

/**
 * A filesystem repository is served by ELZA itself — settings describing how to reach and
 * notify an external repository system do not apply to it and stay hidden.
 */
function isFilesystemRepository(values: ExtSystemFormValues) {
    return values[JAVA_ATTR_CLASS] === EXT_SYSTEM_CLASS.ArrDigitalRepository
        && values.digitalRepositoryType === DigitalRepositoryType.Filesystem;
}

/**
 * Download settings (method, action on a received AIP) exist only for a digital archive (DA)
 * repository, the only type ELZA downloads AIP packages from.
 */
function isDaRepository(values: ExtSystemFormValues) {
    return values[JAVA_ATTR_CLASS] === EXT_SYSTEM_CLASS.ArrDigitalRepository
        && values.digitalRepositoryType === DigitalRepositoryType.Da;
}

function validate(values: ExtSystemFormValues) {
    const classJ = values[JAVA_ATTR_CLASS];
    let requiredFields = [...REQUIRED_FIELDS.abstractExtSystem];

    if (classJ === EXT_SYSTEM_CLASS.ApExternalSystem) {
        requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.ApExternalSystem]);
    } else if (classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository) {
        requiredFields = requiredFields.concat(
            REQUIRED_FIELDS[EXT_SYSTEM_CLASS.ArrDigitalRepository]
                .filter((name) => !(name === 'sendNotification' && isFilesystemRepository(values))),
        );
        if (isDaRepository(values)) {
            requiredFields = requiredFields.concat(REQUIRED_FIELDS.daRepository);
        }
    } else if (classJ === EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk) {
        requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]);
    } else if (classJ === EXT_SYSTEM_CLASS.GisExternalSystem) {
        requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.GisExternalSystem]);
    } else if (classJ === EXT_SYSTEM_CLASS.AiExternalSystem) {
        requiredFields = requiredFields.concat(REQUIRED_FIELDS[EXT_SYSTEM_CLASS.AiExternalSystem]);
    }

    return requiredFields.reduce((errors: Record<string, string>, name) => {
        // A stored boolean false (e.g. sendNotification) is a filled value, not a missing one.
        const value = (values as Record<string, unknown>)[name];
        if (value == null || value === '') {
            errors[name] = getIntl().formatMessage(globalMessages.validationRequired);
        }
        return errors;
    }, {});
}

const INTERCHANGEABLE_TYPES: AP_EXT_SYSTEM_TYPE[][] = [
    [AP_EXT_SYSTEM_TYPE.CAM, AP_EXT_SYSTEM_TYPE.CAM_V2],
    [AP_EXT_SYSTEM_TYPE.CAM_COMPLETE, AP_EXT_SYSTEM_TYPE.CAM_COMPLETE_V2],
];

const ExtSystemFormFields = ({ isUpdate, defaultScopes }: { isUpdate: boolean; defaultScopes: Scope[] }) => {
    const { values, submitting } = useFormState<ExtSystemFormValues>();
    const intl = useIntl();
    const classJ = values[JAVA_ATTR_CLASS];
    const isFsRepo = isFilesystemRepository(values);
    const isDaRepo = isDaRepository(values);
    const allowedApTypes = isUpdate
        ? INTERCHANGEABLE_TYPES.find((group) => group.includes(values.type as AP_EXT_SYSTEM_TYPE)) ?? [values.type]
        : Object.values(AP_EXT_SYSTEM_TYPE);
    const isTypeDisabled = isUpdate && allowedApTypes.length <= 1;

    return (
        <Modal.Body>
            <Field
                name={JAVA_ATTR_CLASS}
                type="select"
                component={FormInputField}
                label={intl.formatMessage(fieldMessages.class)}
                disabled={isUpdate}
            >
                <option key={null} />
                {Object.values(EXT_SYSTEM_CLASS).map((i, index) => (
                    <option key={index} value={i}>
                        {intl.formatMessage(EXT_SYSTEM_CLASS_MESSAGE[i])}
                    </option>
                ))}
            </Field>
            {classJ === EXT_SYSTEM_CLASS.ApExternalSystem && (
                <div>
                    <Field
                        name="type"
                        type="select"
                        component={FormInputField}
                        label={intl.formatMessage(fieldMessages.type)}
                        disabled={isTypeDisabled}
                    >
                        <option key={null} />
                        {allowedApTypes.map((i, index) => (
                            <option key={index} value={i}>
                                {intl.formatMessage(AP_EXT_SYSTEM_MESSAGE[i])}
                            </option>
                        ))}
                    </Field>
                    <Row>
                        <Col xs={6}>
                            <Field
                                name="scope"
                                type="select"
                                component={FormInputField}
                                label={intl.formatMessage(fieldMessages.sysScope)}
                            >
                                <option key={null} />
                                {defaultScopes.map((i, index) => (
                                    <option key={index} value={i.id}>
                                        {i.name}
                                    </option>
                                ))}
                            </Field>
                        </Col>
                        <Col xs={6}>
                            <Field
                                name="syncDelay"
                                type="number"
                                component={FormInputField}
                                label={intl.formatMessage(fieldMessages.syncDelay)}
                                parse={(v) => (v === '' || v == null ? undefined : Number(v))}
                            />
                        </Col>
                    </Row>
                </div>
            )}
            {classJ === EXT_SYSTEM_CLASS.GisExternalSystem && (
                <div>
                    <Field
                        name="type"
                        type="select"
                        component={FormInputField}
                        label={intl.formatMessage(fieldMessages.type)}
                        disabled={isUpdate}
                    >
                        <option key={null} />
                        {Object.values(GisSystemType).map((i, index) => (
                            <option key={index} value={i}>
                                {intl.formatMessage(GIS_SYSTEM_TYPE_MESSAGE[i])}
                            </option>
                        ))}
                    </Field>
                </div>
            )}
            {classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository && (
                <div>
                    <Field
                        name="digitalRepositoryType"
                        type="select"
                        component={FormInputField}
                        label={intl.formatMessage(fieldMessages.type)}
                        disabled={isUpdate}
                    >
                        <option key={null} />
                        {Object.values(DigitalRepositoryType).map((i, index) => (
                            <option key={index} value={i}>
                                {intl.formatMessage(DIGITAL_REPOSITORY_TYPE_MESSAGE[i])}
                            </option>
                        ))}
                    </Field>
                    {!isFsRepo && (
                        <>
                            <Field
                                name="viewDaoUrl"
                                type="text"
                                component={FormInputField}
                                label={intl.formatMessage(fieldMessages.viewDaoUrl)}
                            />
                            <Field
                                name="viewFileUrl"
                                type="text"
                                component={FormInputField}
                                label={intl.formatMessage(fieldMessages.viewFileUrl)}
                            />
                            <Field
                                name="viewThumbnailUrl"
                                type="text"
                                component={FormInputField}
                                label={intl.formatMessage(fieldMessages.viewThumbnailUrl)}
                            />
                        </>
                    )}
                    {!isFsRepo && (
                        <Field
                            name="sendNotification"
                            type="select"
                            component={FormInputField}
                            label={intl.formatMessage(fieldMessages.sendNotification)}
                        >
                            <option key={null} />
                            <option key="true" value={true as any}>
                                {intl.formatMessage(fieldMessages.sendNotificationTrue)}
                            </option>
                            <option key="false" value={false as any}>
                                {intl.formatMessage(fieldMessages.sendNotificationFalse)}
                            </option>
                        </Field>
                    )}
                    <Field
                        name="multipleLinks"
                        type="select"
                        component={FormInputField}
                        label={intl.formatMessage(fieldMessages.multipleLinks)}
                    >
                        <option key={null} />
                        <option key="true" value={true as any}>
                            {intl.formatMessage(fieldMessages.multipleLinksTrue)}
                        </option>
                        <option key="false" value={false as any}>
                            {intl.formatMessage(fieldMessages.multipleLinksFalse)}
                        </option>
                    </Field>
                    {isDaRepo && (
                        <>
                            <Field
                                name="downloadMethod"
                                type="select"
                                component={FormInputField}
                                label={intl.formatMessage(daSettingsMessages.downloadMethod)}
                            >
                                <option key={null} />
                                {Object.values(DaDownloadMethod).map((i) => (
                                    <option key={i} value={i}>
                                        {intl.formatMessage(DA_DOWNLOAD_METHOD_MESSAGE[i])}
                                    </option>
                                ))}
                            </Field>
                            <Field
                                name="onReceived"
                                type="select"
                                component={FormInputField}
                                label={intl.formatMessage(daSettingsMessages.onReceived)}
                            >
                                <option key={null} />
                                {Object.values(DaOnReceivedAction).map((i) => (
                                    <option key={i} value={i}>
                                        {intl.formatMessage(DA_ON_RECEIVED_MESSAGE[i])}
                                    </option>
                                ))}
                            </Field>
                            <Field
                                name="syncDelay"
                                type="number"
                                component={FormInputField}
                                label={intl.formatMessage(daSettingsMessages.syncDelay)}
                                parse={(v) => (v === '' || v == null ? undefined : Number(v))}
                            />
                        </>
                    )}
                </div>
            )}
            {classJ === EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk && <div />}
            <Field
                name="code"
                type="text"
                component={FormInputField}
                label={intl.formatMessage(fieldMessages.code)}
                disabled={isUpdate}
            />
            <Field name="name" type="text" component={FormInputField} label={intl.formatMessage(fieldMessages.name)} />
            <Field name="url" type="text" component={FormInputField} label={intl.formatMessage(fieldMessages.url)} />
            {classJ !== EXT_SYSTEM_CLASS.ApExternalSystem && classJ !== EXT_SYSTEM_CLASS.GisExternalSystem
                && !isFsRepo && (
                <>
                    <Field
                        name="username"
                        type="text"
                        component={FormInputField}
                        label={intl.formatMessage(fieldMessages.username)}
                    />
                    <Field
                        name="password"
                        type="text"
                        component={FormInputField}
                        label={intl.formatMessage(fieldMessages.password)}
                    />
                </>
            )}
            {classJ !== EXT_SYSTEM_CLASS.ApExternalSystem &&
                classJ !== EXT_SYSTEM_CLASS.GisExternalSystem &&
                classJ !== EXT_SYSTEM_CLASS.AiExternalSystem && (
                <Field
                    name="elzaCode"
                    type="text"
                    component={FormInputField}
                    label={intl.formatMessage(fieldMessages.elzaCode)}
                />
            )}
            {(classJ === EXT_SYSTEM_CLASS.ApExternalSystem ||
                classJ === EXT_SYSTEM_CLASS.GisExternalSystem ||
                classJ === EXT_SYSTEM_CLASS.AiExternalSystem) && (
                <>
                    <Field
                        name="apiKeyId"
                        type="text"
                        component={FormInputField}
                        label={intl.formatMessage(fieldMessages.apiKeyId)}
                    />
                    <Field
                        name="apiKeyValue"
                        type="text"
                        component={FormInputField}
                        label={intl.formatMessage(fieldMessages.apiKeyValue)}
                    />
                </>
            )}
            {classJ === EXT_SYSTEM_CLASS.ApExternalSystem && (
                <>
                    <div title={intl.formatMessage(fieldMessages.userInfoTitle)}>
                        <Field
                            name="userInfo"
                            type="text"
                            component={FormInputField}
                            label={intl.formatMessage(fieldMessages.userInfo)}
                        />
                    </div>
                    <Field
                        name="publishOnlyApproved"
                        type="select"
                        component={FormInputField}
                        label={intl.formatMessage(fieldMessages.publishOnlyApproved)}
                    >
                        <option key={null} />
                        <option key="true" value={true as any}>
                            {intl.formatMessage(fieldMessages.publishOnlyApprovedTrue)}
                        </option>
                        <option key="false" value={false as any}>
                            {intl.formatMessage(fieldMessages.publishOnlyApprovedFalse)}
                        </option>
                    </Field>
                </>
            )}
        </Modal.Body>
    );
};

const ExtSystemForm = ({ initialValues, onSubmitForm }: Props) => {
    const intl = useIntl();
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
                            {intl.formatMessage(isUpdate ? fieldMessages.submitEdit : fieldMessages.submitAdd)}
                        </Button>
                    </Modal.Footer>
                </Form>
            )}
        </FinalForm>
    );
};

export default ExtSystemForm;
