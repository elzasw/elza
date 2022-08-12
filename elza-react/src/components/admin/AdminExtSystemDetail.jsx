import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, StoreHorizontalLoader} from 'components/shared';
import {AREA_EXT_SYSTEM_DETAIL, extSystemDetailFetchIfNeeded} from 'actions/admin/extSystem.jsx';
import {storeFromArea} from 'shared/utils';

import './AdminExtSystemDetail.scss';
import {JAVA_ATTR_CLASS} from '../../constants';
import {WebApi} from 'actions/index.jsx';

const EXT_SYSTEM_CLASS = {
    ApExternalSystem: '.ApExternalSystemVO',
    ArrDigitalRepository: '.ArrDigitalRepositoryVO',
    ArrDigitizationFrontdesk: '.ArrDigitizationFrontdeskVO',
};

const EXT_SYSTEM_CLASS_LABEL = {
    [EXT_SYSTEM_CLASS.ApExternalSystem]: i18n('admin.extSystem.class.ApExternalSystemVO'),
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: i18n('admin.extSystem.class.ArrDigitalRepositoryVO'),
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: i18n('admin.extSystem.class.ArrDigitizationFrontdeskVO'),
};

/**
 * Komponenta detailu osoby
 */
class AdminExtSystemDetail extends AbstractReactComponent {
    static state = {
        defaultScopes: []
    };

    componentDidMount() {
        this.fetchIfNeeded();
        WebApi.getAllScopes().then(json => {
            this.setState({
                defaultScopes: json,
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

    scopeValue = (id) => {
        const scope = this.state?.defaultScopes.find(e => e.id === id);
        if (scope != null) {
            return <>
                <h4>{i18n('admin.extSystem.sysScope')}</h4>
                <span>{scope.name}</span>
            </>
        }
    };

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
                            <span>{extSystem.type}</span>

                            {this.scopeValue(extSystem.scope)}
                        </div>
                    )}
                    {classJ === EXT_SYSTEM_CLASS.ArrDigitalRepository && (
                        <div>
                            <h4>{i18n('admin.extSystem.class')}</h4>
                            <span>{EXT_SYSTEM_CLASS_LABEL[EXT_SYSTEM_CLASS.ArrDigitalRepository]}</span>

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
                    <div>
                        {this.renderValue(extSystem, 'name')}
                        {this.renderValue(extSystem, 'code')}
                        {this.renderValue(extSystem, 'url')}
                        {this.renderValue(extSystem, 'username')}
                        {this.renderValue(extSystem, 'password')}
                        {this.renderValue(extSystem, 'apiKeyId')}
                        {this.renderValue(extSystem, 'apiKeyValue')}
                        {this.renderValue(extSystem, 'elzaCode')}
                        {this.renderValue(extSystem, 'userInfo')}
                        {extSystem.publishOnlyApproved != null && (
                            <>
                            <h4>{i18n('admin.extSystem.publishOnlyApproved')}</h4>
                            <span>{extSystem.publishOnlyApproved?i18n('admin.extSystem.publishOnlyApproved.true'):i18n('admin.extSystem.publishOnlyApproved.false')}</span>
                            </>
                        )}
                    </div>
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
