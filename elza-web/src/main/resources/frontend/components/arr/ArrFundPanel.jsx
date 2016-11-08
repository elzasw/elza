require ('./ArrFundPanel.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Icon, AbstractReactComponent, i18n} from 'components/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {Button} from 'react-bootstrap';
import {dateToString} from 'components/Utils.jsx'
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'
import {fundChangeReadMode} from 'actions/arr/fund.jsx'
import {setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx';
import * as perms from 'actions/user/Permission.jsx';

var classNames = require('classnames');

var ArrFundPanel = class ArrFundPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('setReadMode');
    }

    setReadMode(readMode) {
        const {fund, userDetail} = this.props;
        var settings = userDetail.settings;

        var item = getOneSettings(settings, 'FUND_READ_MODE', 'FUND', fund.id);
        item.value = readMode;
        settings = setSettings(settings, item.id, item);
        this.dispatch(fundChangeReadMode(fund.versionId, readMode));
        this.dispatch(userDetailsSaveSettings(settings));
    }

    render() {
        const {fund, userDetail} = this.props;

        var cls = ['arr-fund-panel'];
        var action;

        var fundId = fund.id;

        var settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', fundId);
        var readMode = settings.value != 'false';

        if (fund.lockDate || !userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId})) {
            readMode = true;
            action = <div className="action"><span>{i18n('arr.fund.panel.readOnly')}</span></div>
        } else {
            if (readMode) {
                action = <div className="action"><Button onClick={this.setReadMode.bind(this, false)}>{i18n('arr.fund.panel.allowEdit')}</Button></div>
            } else {
                action = <div className="action"><Button onClick={this.setReadMode.bind(this, true)}>{i18n('arr.fund.panel.forbidEdit')}</Button></div>
            }
        }

        if (readMode) {
            cls.push('read-mode');
        }

        const name = <span className="name">{fund.name}</span>
        const version = fund.lockDate != null ? <span className="lock"><span className="lockTitle">{i18n('arr.fund.panel.lockTitle')}</span>{dateToString(new Date(fund.lockDate))}</span> : null

        return (
                <div key='arr-fund-panel' className={classNames(cls)}>
                    {name}{version}{action}
                </div>
        );
    }
}

function mapStateToProps(state) {
    const {arrRegion, userDetail} = state
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }
    return {
        fund,
        userDetail,
    }
}

module.exports = connect(mapStateToProps)(ArrFundPanel);
