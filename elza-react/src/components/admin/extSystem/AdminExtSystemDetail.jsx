import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, StoreHorizontalLoader} from 'components/shared';
import {AREA_EXT_SYSTEM_DETAIL, extSystemDetailFetchIfNeeded} from 'actions/admin/extSystem.jsx';
import {storeFromArea} from 'shared/utils';

import './AdminExtSystemDetail.scss';
import {AP_EXT_SYSTEM_TYPE, DigitalRepositoryType, JAVA_ATTR_CLASS} from '../../../constants';
import {WebApi} from 'actions/index.jsx';
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
} from './ExtSystemForm';
import { Api } from 'api';
import { Button } from '@fluentui/react-components';
import { FormattedMessage } from 'react-intl';
import { MaskedValue } from 'components/shared/MaskedValue';
import { defineMessages } from 'react-intl';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    noSelectionTitle: { id: 'admin.extSystem.noSelection.title', defaultMessage: 'Není vybrán externí systém' },
    noSelectionMessage: {
        id: 'admin.extSystem.noSelection.message',
        defaultMessage: 'Prosím vyberte externí systém ze seznamu nebo vytvořte nový',
    },
    synchronize: { id: 'admin.extSystem.synchronize', defaultMessage: 'Synchronizovat' },
});

/**
 * Komponenta detailu osoby
 */
/** Popisek pole detailu; pole jsou uzavřená množina sdílená s formulářem. */
function renderFieldLabel(field) {
    const descriptor = fieldMessages[field];
    if (!descriptor) {
        console.warn(`i18n: chybí popisek pole '${field}' v fieldMessages`);
        return field;
    }
    return <FormattedMessage {...descriptor} />;
}

class AdminExtSystemDetail extends AbstractReactComponent {
    static state = {
        defaultScopes: [],
    };

    componentDidMount() {
        this.fetchIfNeeded();
        WebApi.getAllScopes().then(scopes => {
            this.setState({
                defaultScopes: scopes,
            });
        });
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        if (nextProps.extSystemDetail.id !== this.props.extSystemDetail.id) {
            // Test results describe the previously selected system, not the new one.
            this.setState({
                repoTestState: null,
                repoTestResult: null,
                aiTestState: null,
                aiTestInfo: null,
            });
        }
        this.fetchIfNeeded(nextProps);
    }

    fetchIfNeeded = (props = this.props) => {
        const {
            extSystemDetail: {id},
        } = props;
        if (id) {
            this.props.dispatch(extSystemDetailFetchIfNeeded(id));
        }
    };

    renderValue = (extSystem, field) => {
        const value = extSystem[field];
        if (value != null) {
            return <>
                <h4>{renderFieldLabel(field)}</h4>
                <span>{value}</span>
            </>
        }
    };

    renderSecret = (extSystem, field) => {
        const value = extSystem[field];
        if (value != null) {
            return <>
                <h4>{renderFieldLabel(field)}</h4>
                <div><MaskedValue value={value} /></div>
            </>
        }
    };

    scopeValue = (id) => {
        const scope = this.state?.defaultScopes.find(e => e.id === id);
        if (scope != null) {
            return <>
                <h4><FormattedMessage {...fieldMessages.sysScope} /></h4>
                <span>{scope.name}</span>
            </>
        }
    };

    handleResyncExtSystem = () => {
        const { extSystemDetail: {id}, } = this.props;
        Api.externalSystems.externalSystemExternalSystemResync(id);
    }

    handleTestAiConnection = () => {
        const { extSystemDetail: {id}, } = this.props;
        this.setState({aiTestState: 'pending', aiTestInfo: null});
        Api.aiprovider
            .aiProviderGetInfo(String(id))
            .then(response => {
                if (this.props.extSystemDetail.id !== id) {
                    return;
                }
                this.setState({aiTestState: 'ok', aiTestInfo: response.data});
            })
            .catch(() => {
                if (this.props.extSystemDetail.id !== id) {
                    return;
                }
                this.setState({aiTestState: 'failed', aiTestInfo: null});
            });
    }

    handleTestRepository = () => {
        const { extSystemDetail: {id}, } = this.props;
        this.setState({repoTestState: 'pending', repoTestResult: null});
        Api.externalSystems
            .externalSystemTestDigitalRepository(id)
            .then(response => {
                if (this.props.extSystemDetail.id !== id) {
                    return;
                }
                this.setState({repoTestState: 'done', repoTestResult: response.data});
            })
            .catch(() => {
                if (this.props.extSystemDetail.id !== id) {
                    return;
                }
                this.setState({repoTestState: 'error', repoTestResult: null});
            });
    }

    /**
     * @param isFsRepo true for a filesystem repository, the only kind whose test lists a
     *                 root directory; a digital archive is probed over its API and has none.
     */
    renderRepoTestResult = (isFsRepo) => {
        const repoTestState = this.state?.repoTestState;
        if (repoTestState === 'error') {
            return (
                <div className="repo-test-result">
                    <FormattedMessage
                        id="admin.extSystemDetail.repoTestFailed"
                        defaultMessage="Test se nepodařilo provést"
                    />
                </div>
            );
        }
        if (repoTestState !== 'done') {
            return null;
        }

        const result = this.state?.repoTestResult || {};
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
                            values={{path: result.path}}
                        />
                    </div>
                )}
                {result.message && <div>{result.message}</div>}
                {isFsRepo && result.items?.length > 0 && (
                    <>
                        <div>
                            <FormattedMessage
                                id="admin.extSystemDetail.repoTestContent"
                                defaultMessage="Obsah kořenového adresáře (prvních {count}):"
                                values={{count: result.items.length}}
                            />
                        </div>
                        <ul className="repo-test-result__items">
                            {result.items.map(item => (
                                <li key={item.name}>
                                    {item.itemType === 'FOLDER' ? '📁' : '📄'} {item.name}
                                </li>
                            ))}
                        </ul>
                    </>
                )}
                {isFsRepo && result.available && !result.items?.length && (
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

    renderAiTestResult = () => {
        const aiTestState = this.state?.aiTestState;
        if (aiTestState === 'ok') {
            const info = this.state?.aiTestInfo || {};
            return (
                <span>
                    <FormattedMessage
                        id="admin.extSystemDetail.aiTestConnectionOk"
                        defaultMessage="Připojení funguje — {provider} (protokol {version})"
                        values={{
                            provider: info.providerName || '?',
                            version: info.protocolVersion || '?',
                        }}
                    />
                </span>
            );
        }
        if (aiTestState === 'failed') {
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

    render() {
        const {extSystemDetail} = this.props;
        const extSystem = extSystemDetail.data;

        if (!extSystemDetail.isFetching && !extSystemDetail.fetched) {
            return (
                <div className="unselected-msg">
                    <div className="title"><FormattedMessage {...messages.noSelectionTitle} /></div>
                    <div className="msg-text"><FormattedMessage {...messages.noSelectionMessage} /></div>
                </div>
            );
        }

        let content;
        if (extSystemDetail.fetched && extSystem) {
            const classJ = extSystem[JAVA_ATTR_CLASS];
            // A filesystem repository is served by ELZA itself — settings describing how to
            // reach and notify an external repository system do not apply to it.
            const isFsRepo = classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository
                && extSystem.digitalRepositoryType === DigitalRepositoryType.Filesystem;
            content = (
                <div className="ext-system-detail">
                    {classJ === EXT_SYSTEM_CLASS.ApExternalSystem && (
                        <div>
                            <h4><FormattedMessage {...fieldMessages.class} /></h4>
                            <span><FormattedMessage {...EXT_SYSTEM_CLASS_MESSAGE[EXT_SYSTEM_CLASS.ApExternalSystem]} /></span>

                            <h4><FormattedMessage {...fieldMessages.type} /></h4>
                            <span><FormattedMessage {...AP_EXT_SYSTEM_MESSAGE[extSystem.type]} /></span>

                            {this.scopeValue(extSystem.scope)}
                            {this.renderValue(extSystem, 'syncDelay')}
                        </div>
                    )}
                    {classJ === EXT_SYSTEM_CLASS.GisExternalSystem && (
                        <div>
                            <h4><FormattedMessage {...fieldMessages.class} /></h4>
                            <span><FormattedMessage {...EXT_SYSTEM_CLASS_MESSAGE[EXT_SYSTEM_CLASS.GisExternalSystem]} /></span>

                            <h4><FormattedMessage {...fieldMessages.type} /></h4>
                            <span><FormattedMessage {...GIS_SYSTEM_TYPE_MESSAGE[extSystem.type]} /></span>

                            {this.scopeValue(extSystem.scope)}
                        </div>
                    )}
                    {classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository && (
                        <div>
                            <h4><FormattedMessage {...fieldMessages.class} /></h4>
                            <span><FormattedMessage {...EXT_SYSTEM_CLASS_MESSAGE[EXT_SYSTEM_CLASS.ArrDigitalRepository]} /></span>

                            <h4><FormattedMessage {...fieldMessages.type} /></h4>
                            <span><FormattedMessage {...DIGITAL_REPOSITORY_TYPE_MESSAGE[extSystem.digitalRepositoryType]} /></span>

                            {!isFsRepo && this.renderValue(extSystem, 'viewDaoUrl')}
                            {!isFsRepo && this.renderValue(extSystem, 'viewFileUrl')}
                            {!isFsRepo && this.renderValue(extSystem, 'viewThumbnailUrl')}

                            {!isFsRepo && (
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
                            {extSystem.digitalRepositoryType === DigitalRepositoryType.Da && (
                                <>
                                    <h4><FormattedMessage {...daSettingsMessages.downloadMethod} /></h4>
                                    <span>
                                        {DA_DOWNLOAD_METHOD_MESSAGE[extSystem.downloadMethod] && (
                                            <FormattedMessage {...DA_DOWNLOAD_METHOD_MESSAGE[extSystem.downloadMethod]} />
                                        )}
                                    </span>

                                    <h4><FormattedMessage {...daSettingsMessages.onReceived} /></h4>
                                    <span>
                                        {DA_ON_RECEIVED_MESSAGE[extSystem.onReceived] && (
                                            <FormattedMessage {...DA_ON_RECEIVED_MESSAGE[extSystem.onReceived]} />
                                        )}
                                    </span>

                                    <h4><FormattedMessage {...daSettingsMessages.syncDelay} /></h4>
                                    <span>{extSystem.syncDelay}</span>
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
                        {this.renderValue(extSystem, 'name')}
                        {this.renderValue(extSystem, 'code')}
                        {this.renderValue(extSystem, 'url')}
                        {!isFsRepo && this.renderValue(extSystem, 'username')}
                        {!isFsRepo && this.renderSecret(extSystem, 'password')}
                        {this.renderValue(extSystem, 'apiKeyId')}
                        {this.renderSecret(extSystem, 'apiKeyValue')}
                        {this.renderValue(extSystem, 'elzaCode')}
                        {this.renderValue(extSystem, 'userInfo')}
                        {extSystem.publishOnlyApproved != null && (
                            <>
                            <h4><FormattedMessage {...fieldMessages.publishOnlyApproved} /></h4>
                            <span><FormattedMessage {...(extSystem.publishOnlyApproved ? fieldMessages.publishOnlyApprovedTrue : fieldMessages.publishOnlyApprovedFalse)} /></span>
                            </>
                        )}
                    </div>
                    {(extSystem.type === AP_EXT_SYSTEM_TYPE.CAM_COMPLETE
                        || extSystem.type === AP_EXT_SYSTEM_TYPE.CAM_COMPLETE_V2)
                        && <div style={{margin: "8px 0"}}>
                        <Button onClick={this.handleResyncExtSystem}><FormattedMessage {...messages.synchronize} /></Button>
                    </div>}
                    {classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository
                        && (extSystem.digitalRepositoryType === DigitalRepositoryType.Filesystem
                            || extSystem.digitalRepositoryType === DigitalRepositoryType.Da) && (
                        <div style={{margin: "8px 0"}}>
                            <Button
                                onClick={this.handleTestRepository}
                                disabled={this.state?.repoTestState === 'pending'}
                            >
                                <FormattedMessage
                                    id="admin.extSystemDetail.repoTest"
                                    defaultMessage="Vyzkoušet nastavení"
                                />
                            </Button>
                            {this.renderRepoTestResult(isFsRepo)}
                        </div>
                    )}
                    {classJ === EXT_SYSTEM_CLASS.AiExternalSystem && (
                        <div style={{margin: "8px 0"}}>
                            <Button
                                onClick={this.handleTestAiConnection}
                                disabled={this.state?.aiTestState === 'pending'}
                            >
                                <FormattedMessage
                                    id="admin.extSystemDetail.aiTestConnection"
                                    defaultMessage="Vyzkoušet připojení"
                                />
                            </Button>
                            {this.renderAiTestResult()}
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
}

export default connect(state => {
    const extSystemDetail = storeFromArea(state, AREA_EXT_SYSTEM_DETAIL);
    return {
        extSystemDetail,
    };
})(AdminExtSystemDetail);
