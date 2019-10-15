import React from 'react';
import {connect} from 'react-redux'
import {
    AbstractReactComponent,
    Search,
    i18n,
    FormInput,
    Icon,
    CollapsablePanel,
    StoreHorizontalLoader
} from 'components/shared';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {
    findExtSystemFetchIfNeeded,
    extSystemDetailFetchIfNeeded,
    AREA_EXT_SYSTEM_DETAIL
} from 'actions/admin/extSystem.jsx'
import {Utils} from 'components/shared';
import {objectById, indexById} from 'stores/app/utils.jsx';
import {setInputFocus, dateTimeToString} from 'components/Utils.jsx'
import {setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {getMapFromList} from 'stores/app/utils.jsx'
import {storeFromArea} from 'shared/utils'

import './AdminExtSystemDetail.less';

const EXT_SYSTEM_CLASS = {
    ApExternalSystem: ".ApExternalSystemVO",
    ArrDigitalRepository: ".ArrDigitalRepositoryVO",
    ArrDigitizationFrontdesk: ".ArrDigitizationFrontdeskVO"
};

const EXT_SYSTEM_CLASS_LABEL = {
    [EXT_SYSTEM_CLASS.ApExternalSystem]: i18n("admin.extSystem.class.ApExternalSystemVO"),
    [EXT_SYSTEM_CLASS.ArrDigitalRepository]: i18n("admin.extSystem.class.ArrDigitalRepositoryVO"),
    [EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]: i18n("admin.extSystem.class.ArrDigitizationFrontdeskVO"),
};

/**
 * Komponenta detailu osoby
 */
class AdminExtSystemDetail extends AbstractReactComponent {

    componentDidMount() {
        this.fetchIfNeeded();
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
    }

    fetchIfNeeded = (props = this.props) => {
        const {extSystemDetail: {id}} = props;
        if (id) {
            this.dispatch(extSystemDetailFetchIfNeeded(id));
        }
    };

    render() {
        const {extSystemDetail} = this.props;
        const extSystem = extSystemDetail.data;

        if (!extSystemDetail.isFetching && !extSystemDetail.fetched) {
            return <div className="unselected-msg">
                <div className="title">{i18n('admin.extSystem.noSelection.title')}</div>
                <div className="msg-text">{i18n('admin.extSystem.noSelection.message')}</div>
            </div>
        }

        let content;
        if (extSystemDetail.fetched && extSystem) {
            const classJ = extSystem["@class"];
            content = <div className="ext-system-detail">
                {classJ == EXT_SYSTEM_CLASS.ApExternalSystem && <div>
                    <h4>{i18n('admin.extSystem.class')}</h4>
                    <span>{EXT_SYSTEM_CLASS_LABEL[EXT_SYSTEM_CLASS.ApExternalSystem]}</span>

                    <h4>{i18n('admin.extSystem.type')}</h4>
                    <span>{extSystem.type}</span>
                </div>}
                {classJ == EXT_SYSTEM_CLASS.ArrDigitalRepository && <div>
                    <h4>{i18n('admin.extSystem.class')}</h4>
                    <span>{EXT_SYSTEM_CLASS_LABEL[EXT_SYSTEM_CLASS.ArrDigitalRepository]}</span>

                    {extSystem.viewDaoUrl != '' &&
                    <div>
                        <h4>{i18n('admin.extSystem.viewDaoUrl')}</h4>
                        <span>{extSystem.viewDaoUrl}</span>
                    </div>
                    }

                    {extSystem.viewFileUrl &&
                    <div>
                        <h4>{i18n('admin.extSystem.viewFileUrl')}</h4>
                        <span>{extSystem.viewFileUrl}</span>
                    </div>
                    }

                    {extSystem.viewThumbnailUrl &&
                    <div>
                        <h4>{i18n('admin.extSystem.viewThumbnailUrl')}</h4>
                        <span>{extSystem.viewThumbnailUrl}</span>
                    </div>
                    }

                    <h4>{i18n('admin.extSystem.sendNotification')}</h4>
                    <span>{extSystem.sendNotification ? i18n('admin.extSystem.sendNotification.true') : i18n('admin.extSystem.sendNotification.false')}</span>
                </div>}
                {classJ == EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk && <div>
                    <h4>{i18n('admin.extSystem.class')}</h4>
                    <span>{EXT_SYSTEM_CLASS_LABEL[EXT_SYSTEM_CLASS.ArrDigitizationFrontdesk]}</span>
                </div>}
                <div>
                    <h4>{i18n('admin.extSystem.name')}</h4>
                    <span>{extSystem.name}</span>

                    <h4>{i18n('admin.extSystem.code')}</h4>
                    <span>{extSystem.code}</span>

                    {extSystem.url &&
                    <div>
                        <h4>{i18n('admin.extSystem.url')}</h4>
                        <span>{extSystem.url}</span>
                    </div>
                    }

                    {extSystem.username &&
                    <div>
                        <h4>{i18n('admin.extSystem.username')}</h4>
                        <span>{extSystem.username}</span>
                    </div>
                    }

                    {extSystem.password &&
                    <div>
                        <h4>{i18n('admin.extSystem.password')}</h4>
                        <span>{extSystem.password}</span>
                    </div>
                    }

                    {extSystem.elzaCode &&
                    <div>
                        <h4>{i18n('admin.extSystem.elzaCode')}</h4>
                        <span>{extSystem.elzaCode}</span>
                    </div>
                    }
                </div>
            </div>;
        }

        return <div>
            <StoreHorizontalLoader store={extSystemDetail} />
            {content}
        </div>
    }
}

export default connect((state) => {
    const extSystemDetail = storeFromArea(state, AREA_EXT_SYSTEM_DETAIL);
    return {
        extSystemDetail,
    }
})(AdminExtSystemDetail);
