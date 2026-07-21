import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, StoreHorizontalLoader} from 'components/shared';
import {AREA_EXT_SYSTEM_DETAIL, extSystemDetailFetchIfNeeded} from 'actions/admin/extSystem.jsx';
import {storeFromArea} from 'shared/utils';

import './AdminExtSystemDetail.scss';
import {AP_EXT_SYSTEM_TYPE, JAVA_ATTR_CLASS} from '../../../constants';
import {WebApi} from 'actions/index.jsx';
import {
    EXT_SYSTEM_CLASS,
    EXT_SYSTEM_CLASS_LABEL,
    GIS_SYSTEM_TYPE_LABEL,
    AP_EXT_SYSTEM_LABEL,
    DIGITAL_REPOSITORY_TYPE_LABEL
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
                this.setState({aiTestState: 'ok', aiTestInfo: response.data});
            })
            .catch(() => {
                this.setState({aiTestState: 'failed', aiTestInfo: null});
            });
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

                            {this.renderValue(extSystem, 'viewDaoUrl')}
                            {this.renderValue(extSystem, 'viewFileUrl')}
                            {this.renderValue(extSystem, 'viewThumbnailUrl')}

                            <h4>{i18n('admin.extSystem.sendNotification')}</h4>
                            <span>
                                {extSystem.sendNotification
                                    ? i18n('admin.extSystem.sendNotification.true')
                                    : i18n('admin.extSystem.sendNotification.false')}
                            </span>
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
                        {this.renderValue(extSystem, 'username')}
                        {this.renderSecret(extSystem, 'password')}
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
