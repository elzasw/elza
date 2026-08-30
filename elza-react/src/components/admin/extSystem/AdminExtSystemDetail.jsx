import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, StoreHorizontalLoader} from 'components/shared';
import {AREA_EXT_SYSTEM_DETAIL, extSystemDetailFetchIfNeeded} from 'actions/admin/extSystem.jsx';
import {storeFromArea} from 'shared/utils';

import './AdminExtSystemDetail.scss';
import {AP_EXT_SYSTEM_TYPE, DigitalRepositoryType, JAVA_ATTR_CLASS} from '../../../constants';
import {WebApi} from 'actions/index.jsx';
import {
    EXT_SYSTEM_CLASS,
    EXT_SYSTEM_CLASS_LABEL,
    GIS_SYSTEM_TYPE_LABEL,
    AP_EXT_SYSTEM_LABEL,
    DIGITAL_REPOSITORY_TYPE_LABEL,
    daSettingsMessages,
    DA_DOWNLOAD_METHOD_MESSAGE,
    DA_ON_RECEIVED_MESSAGE,
} from './ExtSystemForm';
import { Api } from 'api';
import { Button } from '@fluentui/react-components';
import { FormattedMessage } from 'react-intl';
import { MaskedValue } from 'components/shared/MaskedValue';

/**
 * Komponenta detailu osoby
 */
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
                <h4>{i18n('admin.extSystem.' + field)}</h4>
                <span>{value}</span>
            </>
        }
    };

    renderSecret = (extSystem, field) => {
        const value = extSystem[field];
        if (value != null) {
            return <>
                <h4>{i18n('admin.extSystem.' + field)}</h4>
                <div><MaskedValue value={value} /></div>
            </>
        }
    };

    scopeValue = (id) => {
        const scope = this.state?.defaultScopes.find(e => e.id === id);
        if (scope != null) {
            return <>
                <h4>{i18n('admin.extSystem.sysScope')}</h4>
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

    renderRepoTestResult = () => {
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
                {result.items?.length > 0 && (
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
                {result.available && !result.items?.length && (
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
                    <div className="title">{i18n('admin.extSystem.noSelection.title')}</div>
                    <div className="msg-text">{i18n('admin.extSystem.noSelection.message')}</div>
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
                            <h4>{i18n('admin.extSystem.class')}</h4>
                            <span>{EXT_SYSTEM_CLASS_LABEL[EXT_SYSTEM_CLASS.ApExternalSystem]}</span>

                            <h4>{i18n('admin.extSystem.type')}</h4>
                            <span>{AP_EXT_SYSTEM_LABEL[extSystem.type]}</span>

                            {this.scopeValue(extSystem.scope)}
                            {this.renderValue(extSystem, 'syncDelay')}
                        </div>
                    )}
                    {classJ === EXT_SYSTEM_CLASS.GisExternalSystem && (
                        <div>
                            <h4>{i18n('admin.extSystem.class')}</h4>
                            <span>{EXT_SYSTEM_CLASS_LABEL[EXT_SYSTEM_CLASS.GisExternalSystem]}</span>

                            <h4>{i18n('admin.extSystem.type')}</h4>
                            <span>{GIS_SYSTEM_TYPE_LABEL[extSystem.type]}</span>

                            {this.scopeValue(extSystem.scope)}
                        </div>
                    )}
                    {classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository && (
                        <div>
                            <h4>{i18n('admin.extSystem.class')}</h4>
                            <span>{EXT_SYSTEM_CLASS_LABEL[EXT_SYSTEM_CLASS.ArrDigitalRepository]}</span>

                            <h4>{i18n('admin.extSystem.type')}</h4>
                            <span>{DIGITAL_REPOSITORY_TYPE_LABEL[extSystem.digitalRepositoryType]}</span>

                            {!isFsRepo && this.renderValue(extSystem, 'viewDaoUrl')}
                            {!isFsRepo && this.renderValue(extSystem, 'viewFileUrl')}
                            {!isFsRepo && this.renderValue(extSystem, 'viewThumbnailUrl')}

                            {!isFsRepo && (
                                <>
                                    <h4>{i18n('admin.extSystem.sendNotification')}</h4>
                                    <span>
                                        {extSystem.sendNotification
                                            ? i18n('admin.extSystem.sendNotification.true')
                                            : i18n('admin.extSystem.sendNotification.false')}
                                    </span>
                                </>
                            )}
                            <h4>{i18n('admin.extSystem.multipleLinks')}</h4>
                            <span>
                                {extSystem.multipleLinks
                                    ? i18n('admin.extSystem.multipleLinks.true')
                                    : i18n('admin.extSystem.multipleLinks.false')}
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
                            <h4>{i18n('admin.extSystem.class')}</h4>
                            <span>{EXT_SYSTEM_CLASS_LABEL[EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]}</span>
                        </div>
                    )}
                    {classJ === EXT_SYSTEM_CLASS.AiExternalSystem && (
                        <div>
                            <h4>{i18n('admin.extSystem.class')}</h4>
                            <span>{EXT_SYSTEM_CLASS_LABEL[EXT_SYSTEM_CLASS.AiExternalSystem]}</span>
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
                            <h4>{i18n('admin.extSystem.publishOnlyApproved')}</h4>
                            <span>{extSystem.publishOnlyApproved?i18n('admin.extSystem.publishOnlyApproved.true'):i18n('admin.extSystem.publishOnlyApproved.false')}</span>
                            </>
                        )}
                    </div>
                    {(extSystem.type === AP_EXT_SYSTEM_TYPE.CAM_COMPLETE
                        || extSystem.type === AP_EXT_SYSTEM_TYPE.CAM_COMPLETE_V2)
                        && <div style={{margin: "8px 0"}}>
                        <Button onClick={this.handleResyncExtSystem}>{i18n('admin.extSystem.synchronize')}</Button>
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
                            {this.renderRepoTestResult()}
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
