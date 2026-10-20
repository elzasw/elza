import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Button } from '@fluentui/react-components';
import { FormattedMessage, defineMessages } from 'react-intl';
import type { AiProviderInfo, DigitalRepositoryTestResult } from 'elza-api';
import { StoreHorizontalLoader } from 'components/shared';
import { MaskedValue } from 'components/shared/MaskedValue';
import { AREA_EXT_SYSTEM_DETAIL, extSystemDetailFetchIfNeeded } from 'actions/admin/extSystem.jsx';
import { WebApi } from 'actions/index.jsx';
import { Api } from 'api';
import { storeFromArea } from 'shared/utils';
import { useAppSelector, useAppThunkDispatch } from 'utils/hooks';
import type { ExternalSystem, ItemDetail } from 'typings/store';
import {
    AP_EXT_SYSTEM_TYPE,
    DaDownloadMethod,
    DigitalRepositoryType,
    JAVA_ATTR_CLASS,
    urlAdminDaQueue,
} from '../../../constants';
import {
    EXT_SYSTEM_CLASS,
    EXT_SYSTEM_CLASS_MESSAGE,
    GIS_SYSTEM_TYPE_MESSAGE,
    AP_EXT_SYSTEM_MESSAGE,
    DIGITAL_REPOSITORY_TYPE_MESSAGE,
    fieldMessages,
    daSettingsMessages,
    DA_DOWNLOAD_METHOD_MESSAGE,
    DA_ON_RECEIVED_MESSAGE,
    deriveFileTransferUrl,
} from './ExtSystemForm';

import './AdminExtSystemDetail.scss';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    noSelectionTitle: { id: 'admin.extSystem.noSelection.title', defaultMessage: 'Není vybrán externí systém' },
    noSelectionMessage: {
        id: 'admin.extSystem.noSelection.message',
        defaultMessage: 'Prosím vyberte externí systém ze seznamu nebo vytvořte nový',
    },
    synchronize: { id: 'admin.extSystem.synchronize', defaultMessage: 'Synchronizovat' },
    daQueue: { id: 'admin.extSystem.daQueue', defaultMessage: 'Fronta požadavků digitálního archivu' },
});

type FieldName = keyof typeof fieldMessages;

type Scope = {
    id: number;
    name: string;
};

/**
 * State of a connection test. It remembers the system it was run for, so a result is never
 * shown for another system selected in the meantime.
 */
type TestState<T> = {
    systemId?: number;
    status?: 'pending' | 'done' | 'error';
    result?: T;
};

function FieldValue({ extSystem, field }: { extSystem: ExternalSystem; field: FieldName }) {
    const value = extSystem[field as keyof ExternalSystem];
    if (value == null) {
        return null;
    }
    return (
        <>
            <h4><FormattedMessage {...fieldMessages[field]} /></h4>
            <span>{String(value)}</span>
        </>
    );
}

function SecretValue({ extSystem, field }: { extSystem: ExternalSystem; field: 'password' | 'apiKeyValue' }) {
    const value = extSystem[field];
    if (value == null) {
        return null;
    }
    return (
        <>
            <h4><FormattedMessage {...fieldMessages[field]} /></h4>
            <div><MaskedValue value={value} /></div>
        </>
    );
}

function ScopeValue({ scopes, id }: { scopes: Scope[]; id?: number | null }) {
    const scope = scopes.find((e) => e.id === id);
    if (scope == null) {
        return null;
    }
    return (
        <>
            <h4><FormattedMessage {...fieldMessages.sysScope} /></h4>
            <span>{scope.name}</span>
        </>
    );
}

/**
 * @param isFsRepo true for a filesystem repository, the only kind whose test lists a
 *                 root directory; a digital archive is probed over its API and has none.
 */
function RepoTestResult({ test, isFsRepo }: { test: TestState<DigitalRepositoryTestResult>; isFsRepo: boolean }) {
    if (test.status === 'error') {
        return (
            <div className="repo-test-result">
                <FormattedMessage
                    id="admin.extSystemDetail.repoTestFailed"
                    defaultMessage="Test se nepodařilo provést"
                />
            </div>
        );
    }
    if (test.status !== 'done' || !test.result) {
        return null;
    }

    const result = test.result;
    const items = result.items ?? [];
    return (
        <div className="repo-test-result">
            <div className={result.available ? 'repo-test-result__ok' : 'repo-test-result__failed'}>
                {result.available ? (
                    <FormattedMessage
                        id="admin.extSystemDetail.repoTestOk"
                        defaultMessage="Repozitář je dostupný"
                    />
                ) : (
                    <FormattedMessage
                        id="admin.extSystemDetail.repoTestUnavailable"
                        defaultMessage="Repozitář není dostupný"
                    />
                )}
            </div>
            {result.path && (
                <div>
                    <FormattedMessage
                        id="admin.extSystemDetail.repoTestPath"
                        defaultMessage="Ověřená cesta: {path}"
                        values={{ path: result.path }}
                    />
                </div>
            )}
            {result.message && <div>{result.message}</div>}
            {isFsRepo && items.length > 0 && (
                <>
                    <div>
                        <FormattedMessage
                            id="admin.extSystemDetail.repoTestContent"
                            defaultMessage="Obsah kořenového adresáře (prvních {count}):"
                            values={{ count: items.length }}
                        />
                    </div>
                    <ul className="repo-test-result__items">
                        {items.map((item) => (
                            <li key={item.name}>
                                {item.itemType === 'FOLDER' ? '📁' : '📄'} {item.name}
                            </li>
                        ))}
                    </ul>
                </>
            )}
            {isFsRepo && result.available && items.length === 0 && (
                <div>
                    <FormattedMessage
                        id="admin.extSystemDetail.repoTestEmpty"
                        defaultMessage="Kořenový adresář je prázdný"
                    />
                </div>
            )}
        </div>
    );
}

function AiTestResult({ test }: { test: TestState<AiProviderInfo> }) {
    if (test.status === 'done') {
        return (
            <span>
                <FormattedMessage
                    id="admin.extSystemDetail.aiTestConnectionOk"
                    defaultMessage="Připojení funguje — {provider} (protokol {version})"
                    values={{
                        provider: test.result?.providerName || '?',
                        version: test.result?.protocolVersion || '?',
                    }}
                />
            </span>
        );
    }
    if (test.status === 'error') {
        return (
            <span>
                <FormattedMessage
                    id="admin.extSystemDetail.aiTestConnectionFailed"
                    defaultMessage="Připojení selhalo"
                />
            </span>
        );
    }
    return null;
}

/**
 * Runs a connection test of the given system. A response arriving after another test was
 * started, or for a system no longer shown, does not overwrite the current state.
 */
function useConnectionTest<T>(id: number | undefined, run: (id: number) => Promise<T>) {
    const [test, setTest] = useState<TestState<T>>({});

    const start = () => {
        if (id == null) {
            return;
        }
        setTest({ systemId: id, status: 'pending' });
        run(id)
            .then((result) => setTest((prev) => (prev.systemId === id ? { systemId: id, status: 'done', result } : prev)))
            .catch(() => setTest((prev) => (prev.systemId === id ? { systemId: id, status: 'error' } : prev)));
    };

    return [test.systemId === id ? test : {}, start] as const;
}

/**
 * Detail of the external system selected in the administration.
 */
export function AdminExtSystemDetail() {
    const dispatch = useAppThunkDispatch();
    const extSystemDetail: ItemDetail<ExternalSystem> = useAppSelector((state) =>
        storeFromArea(state, AREA_EXT_SYSTEM_DETAIL),
    );
    const id = extSystemDetail.id;
    const [scopes, setScopes] = useState<Scope[]>([]);
    const [repoTest, startRepoTest] = useConnectionTest(id, (systemId) =>
        Api.externalSystems.externalSystemTestDigitalRepository(systemId).then((response) => response.data),
    );
    const [aiTest, startAiTest] = useConnectionTest(id, (systemId) =>
        Api.aiprovider.aiProviderGetInfo(String(systemId)).then((response) => response.data),
    );

    useEffect(() => {
        WebApi.getAllScopes().then((json: Scope[]) => setScopes(json));
    }, []);

    // The store invalidates the detail after an update; asking on every change of it refetches.
    useEffect(() => {
        if (id) {
            dispatch(extSystemDetailFetchIfNeeded(id));
        }
    }, [dispatch, id, extSystemDetail]);

    const handleResync = () => {
        if (id != null) {
            Api.externalSystems.externalSystemExternalSystemResync(String(id));
        }
    };

    if (!extSystemDetail.isFetching && !extSystemDetail.fetched) {
        return (
            <div className="unselected-msg">
                <div className="title"><FormattedMessage {...messages.noSelectionTitle} /></div>
                <div className="msg-text"><FormattedMessage {...messages.noSelectionMessage} /></div>
            </div>
        );
    }

    const extSystem = extSystemDetail.data;
    let content = null;
    if (extSystemDetail.fetched && extSystem) {
        const classJ = extSystem[JAVA_ATTR_CLASS];
        // A filesystem repository is served by ELZA itself — settings describing how to
        // reach and notify an external repository system do not apply to it.
        const isFsRepo = classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository
            && extSystem.digitalRepositoryType === DigitalRepositoryType.Filesystem;
        const isDaRepo = classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository
            && extSystem.digitalRepositoryType === DigitalRepositoryType.Da;
        content = (
            <div className="ext-system-detail">
                {classJ === EXT_SYSTEM_CLASS.ApExternalSystem && (
                    <div>
                        <h4><FormattedMessage {...fieldMessages.class} /></h4>
                        <span><FormattedMessage {...EXT_SYSTEM_CLASS_MESSAGE[EXT_SYSTEM_CLASS.ApExternalSystem]} /></span>

                        <h4><FormattedMessage {...fieldMessages.type} /></h4>
                        <span><FormattedMessage {...AP_EXT_SYSTEM_MESSAGE[extSystem.type!]} /></span>

                        <ScopeValue scopes={scopes} id={extSystem.scope} />
                        <FieldValue extSystem={extSystem} field="syncDelay" />
                    </div>
                )}
                {classJ === EXT_SYSTEM_CLASS.GisExternalSystem && (
                    <div>
                        <h4><FormattedMessage {...fieldMessages.class} /></h4>
                        <span><FormattedMessage {...EXT_SYSTEM_CLASS_MESSAGE[EXT_SYSTEM_CLASS.GisExternalSystem]} /></span>

                        <h4><FormattedMessage {...fieldMessages.type} /></h4>
                        <span><FormattedMessage {...GIS_SYSTEM_TYPE_MESSAGE[extSystem.type!]} /></span>

                        <ScopeValue scopes={scopes} id={extSystem.scope} />
                    </div>
                )}
                {classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository && (
                    <div>
                        <h4><FormattedMessage {...fieldMessages.class} /></h4>
                        <span><FormattedMessage {...EXT_SYSTEM_CLASS_MESSAGE[EXT_SYSTEM_CLASS.ArrDigitalRepository]} /></span>

                        <h4><FormattedMessage {...fieldMessages.type} /></h4>
                        <span><FormattedMessage {...DIGITAL_REPOSITORY_TYPE_MESSAGE[extSystem.digitalRepositoryType!]} /></span>

                        {!isFsRepo && (
                            <>
                                <FieldValue extSystem={extSystem} field="viewDaoUrl" />
                                <FieldValue extSystem={extSystem} field="viewFileUrl" />
                                <FieldValue extSystem={extSystem} field="viewThumbnailUrl" />
                            </>
                        )}
                        {extSystem.digitalRepositoryType === DigitalRepositoryType.Wsdl && (
                            <>
                                <h4><FormattedMessage {...fieldMessages.sendNotification} /></h4>
                                <span>
                                    {extSystem.sendNotification
                                        ? <FormattedMessage {...fieldMessages.sendNotificationTrue} />
                                        : <FormattedMessage {...fieldMessages.sendNotificationFalse} />}
                                </span>
                            </>
                        )}
                        <h4><FormattedMessage {...fieldMessages.multipleLinks} /></h4>
                        <span>
                            {extSystem.multipleLinks
                                ? <FormattedMessage {...fieldMessages.multipleLinksTrue} />
                                : <FormattedMessage {...fieldMessages.multipleLinksFalse} />}
                        </span>
                        {isDaRepo && (
                            <>
                                <h4><FormattedMessage {...daSettingsMessages.downloadMethod} /></h4>
                                <span>
                                    {DA_DOWNLOAD_METHOD_MESSAGE[extSystem.downloadMethod!] && (
                                        <FormattedMessage {...DA_DOWNLOAD_METHOD_MESSAGE[extSystem.downloadMethod!]} />
                                    )}
                                </span>

                                {extSystem.downloadMethod === DaDownloadMethod.FileTransfer && extSystem.fileTransferType && (
                                    <>
                                        <h4><FormattedMessage {...daSettingsMessages.fileTransferType} /></h4>
                                        <span>{extSystem.fileTransferType}</span>
                                    </>
                                )}

                                <h4><FormattedMessage {...daSettingsMessages.fileTransferUrl} /></h4>
                                <span>
                                    {extSystem.fileTransferUrl || (
                                        <FormattedMessage
                                            {...daSettingsMessages.fileTransferUrlDerived}
                                            values={{ url: deriveFileTransferUrl(extSystem.url) }}
                                        />
                                    )}
                                </span>

                                <h4><FormattedMessage {...daSettingsMessages.tlsVerify} /></h4>
                                <span>
                                    <FormattedMessage
                                        {...(extSystem.tlsVerify === false
                                            ? daSettingsMessages.tlsVerifyFalse
                                            : daSettingsMessages.tlsVerifyTrue)}
                                    />
                                </span>

                                <h4><FormattedMessage {...daSettingsMessages.onReceived} /></h4>
                                <span>
                                    {DA_ON_RECEIVED_MESSAGE[extSystem.onReceived!] && (
                                        <FormattedMessage {...DA_ON_RECEIVED_MESSAGE[extSystem.onReceived!]} />
                                    )}
                                </span>

                                <h4><FormattedMessage {...daSettingsMessages.syncDelay} /></h4>
                                <span>{extSystem.syncDelay}</span>

                                <h4><FormattedMessage {...daSettingsMessages.statusPollInterval} /></h4>
                                <span>{extSystem.statusPollInterval}</span>
                            </>
                        )}
                    </div>
                )}
                {classJ === EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk && (
                    <div>
                        <h4><FormattedMessage {...fieldMessages.class} /></h4>
                        <span><FormattedMessage {...EXT_SYSTEM_CLASS_MESSAGE[EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]} /></span>
                    </div>
                )}
                {classJ === EXT_SYSTEM_CLASS.AiExternalSystem && (
                    <div>
                        <h4><FormattedMessage {...fieldMessages.class} /></h4>
                        <span><FormattedMessage {...EXT_SYSTEM_CLASS_MESSAGE[EXT_SYSTEM_CLASS.AiExternalSystem]} /></span>
                    </div>
                )}
                <div>
                    <FieldValue extSystem={extSystem} field="name" />
                    <FieldValue extSystem={extSystem} field="code" />
                    <FieldValue extSystem={extSystem} field="url" />
                    {!isFsRepo && <FieldValue extSystem={extSystem} field="username" />}
                    {!isFsRepo && <SecretValue extSystem={extSystem} field="password" />}
                    <FieldValue extSystem={extSystem} field="apiKeyId" />
                    <SecretValue extSystem={extSystem} field="apiKeyValue" />
                    <FieldValue extSystem={extSystem} field="elzaCode" />
                    <FieldValue extSystem={extSystem} field="userInfo" />
                    {extSystem.publishOnlyApproved != null && (
                        <>
                            <h4><FormattedMessage {...fieldMessages.publishOnlyApproved} /></h4>
                            <span>
                                <FormattedMessage
                                    {...(extSystem.publishOnlyApproved
                                        ? fieldMessages.publishOnlyApprovedTrue
                                        : fieldMessages.publishOnlyApprovedFalse)}
                                />
                            </span>
                        </>
                    )}
                </div>
                {(extSystem.type === AP_EXT_SYSTEM_TYPE.CAM_COMPLETE
                    || extSystem.type === AP_EXT_SYSTEM_TYPE.CAM_COMPLETE_V2) && (
                    <div style={{ margin: '8px 0' }}>
                        <Button onClick={handleResync}><FormattedMessage {...messages.synchronize} /></Button>
                    </div>
                )}
                {(isFsRepo || isDaRepo) && (
                    <div style={{ margin: '8px 0' }}>
                        <Button onClick={startRepoTest} disabled={repoTest.status === 'pending'}>
                            <FormattedMessage
                                id="admin.extSystemDetail.repoTest"
                                defaultMessage="Vyzkoušet nastavení"
                            />
                        </Button>
                        {isDaRepo && (
                            <RouterLink to={urlAdminDaQueue(extSystem.id)} style={{ marginLeft: 8 }}>
                                <FormattedMessage {...messages.daQueue} />
                            </RouterLink>
                        )}
                        <RepoTestResult test={repoTest} isFsRepo={isFsRepo} />
                    </div>
                )}
                {classJ === EXT_SYSTEM_CLASS.AiExternalSystem && (
                    <div style={{ margin: '8px 0' }}>
                        <Button onClick={startAiTest} disabled={aiTest.status === 'pending'}>
                            <FormattedMessage
                                id="admin.extSystemDetail.aiTestConnection"
                                defaultMessage="Vyzkoušet připojení"
                            />
                        </Button>
                        <AiTestResult test={aiTest} />
                    </div>
                )}
            </div>
        );
    }

    return (
        <div>
            <StoreHorizontalLoader store={extSystemDetail} />
            {content}
        </div>
    );
}
