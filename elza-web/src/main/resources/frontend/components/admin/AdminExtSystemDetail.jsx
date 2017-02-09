import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {
    AbstractReactComponent,
    Search,
    i18n,
    FormInput,
    Icon,
    CollapsablePanel
} from 'components/index.jsx';
import {Form, Button} from 'react-bootstrap';
import {AppActions} from 'stores/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {findExtSystemFetchIfNeeded, extSystemDetailFetchIfNeeded, AREA_EXT_SYSTEM_DETAIL} from 'actions/admin/extSystem.jsx'
import {Utils} from 'components/index.jsx';
import {objectById, indexById} from 'stores/app/utils.jsx';
import {setInputFocus, dateTimeToString} from 'components/Utils.jsx'
import {setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {getMapFromList} from 'stores/app/utils.jsx'
import {storeFromArea} from 'shared/utils'

import './AdminExtSystemDetail.less';

const EXT_SYSTEM_CLASS = {
    RegExternalSystem: ".RegExternalSystemVO",
    ArrDigitalRepository: ".ArrDigitalRepositoryVO",
    ArrDigitizationFrontdesk: ".ArrDigitizationFrontdeskVO"
};

const EXT_SYSTEM_CLASS_LABEL = {
    [EXT_SYSTEM_CLASS.RegExternalSystem]: i18n("admin.extSystem.class.RegExternalSystemVO"),
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


        if (!extSystem) {

            if (extSystemDetail.isFetching) {
                return <div>{i18n('admin.extSystem.detail.finding')}</div>
            }

            return <div className="unselected-msg">
                <div className="title">{i18n('admin.extSystem.noSelection.title')}</div>
                <div className="msg-text">{i18n('admin.extSystem.noSelection.message')}</div>
            </div>
        }
        const classJ = extSystem["@class"];

        return <div tabIndex={0} ref='extSystemDetail' className="ext-system-detail">

            {classJ == EXT_SYSTEM_CLASS.RegExternalSystem && <div>
                <h4>{i18n('admin.extSystem.class')}</h4>
                <span>{EXT_SYSTEM_CLASS_LABEL[EXT_SYSTEM_CLASS.RegExternalSystem]}</span>

                <h4>{i18n('admin.extSystem.type')}</h4>
                <span>{extSystem.type}</span>
            </div>}

            {classJ == EXT_SYSTEM_CLASS.ArrDigitalRepository && <div>
                <h4>{i18n('admin.extSystem.class')}</h4>
                <span>{EXT_SYSTEM_CLASS_LABEL[EXT_SYSTEM_CLASS.ArrDigitalRepository]}</span>

                <h4>{i18n('admin.extSystem.viewDaoUrl')}</h4>
                <span>{extSystem.viewDaoUrl}</span>

                <h4>{i18n('admin.extSystem.viewFileUrl')}</h4>
                <span>{extSystem.viewFileUrl}</span>

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

                <h4>{i18n('admin.extSystem.url')}</h4>
                <span>{extSystem.url}</span>

                <h4>{i18n('admin.extSystem.username')}</h4>
                <span>{extSystem.username}</span>

                <h4>{i18n('admin.extSystem.password')}</h4>
                <span>{extSystem.password}</span>

                <h4>{i18n('admin.extSystem.elzaCode')}</h4>
                <span>{extSystem.elzaCode}</span>
            </div>
        </div>;
    }
}

export default connect((state) => {
    const extSystemDetail = storeFromArea(state, AREA_EXT_SYSTEM_DETAIL);
    return {
        extSystemDetail,
    }
})(AdminExtSystemDetail);
