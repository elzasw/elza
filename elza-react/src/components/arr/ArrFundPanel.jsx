import './ArrFundPanel.scss';

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import {Button} from '../ui';
import {dateToString} from 'components/Utils.jsx';
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx';
import {fundChangeReadMode} from 'actions/arr/fund.jsx';
import {getOneSettings, setSettings} from 'components/arr/ArrUtils.jsx';
import * as perms from 'actions/user/Permission.jsx';
import classNames from 'classnames';
import TooltipTrigger from '../shared/tooltip/TooltipTrigger';

class ArrFundPanel extends AbstractReactComponent {
    setReadMode = readMode => {
        const {fund, userDetail} = this.props;
        let settings = userDetail.settings;

        let item = getOneSettings(settings, 'FUND_READ_MODE', 'FUND', fund.id);
        item.value = readMode;
        settings = setSettings(settings, item.id, item);
        this.props.dispatch(fundChangeReadMode(fund.versionId, readMode));
        this.props.dispatch(userDetailsSaveSettings(settings));
    };

    render() {
        const {fund, userDetail} = this.props;

        let cls = ['arr-fund-panel'];
        let action;

        const fundId = fund.id;

        const settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', fundId);
        let readMode = settings.value !== 'false';

        let arrPerm = false;
        if (fund.nodes && fund.nodes.activeIndex !== null) {
            const node = fund.nodes.nodes[fund.nodes.activeIndex];
            const subNodeForm = node.subNodeForm;
            arrPerm = subNodeForm.data && subNodeForm.data.arrPerm;
        }

        if (fund.lockDate || (!userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId}) && !arrPerm)) {
            readMode = true;
            action = (
                <div className="action">
                    <span>{i18n('arr.fund.panel.readOnly')}</span>
                </div>
            );
        } else {
            if (readMode) {
                action = (
                    <div className="action">
                        <Button variant="outline-secondary" onClick={this.setReadMode.bind(this, false)}>
                            {i18n('arr.fund.panel.allowEdit')}
                        </Button>
                    </div>
                );
            } else {
                action = (
                    <div className="action">
                        <Button variant="outline-secondary" onClick={this.setReadMode.bind(this, true)}>
                            {i18n('arr.fund.panel.forbidEdit')}
                        </Button>
                    </div>
                );
            }
        }

        if (readMode) {
            cls.push('read-mode');
        }

        const name = <span className="name">{fund.name}</span>;
        const version =
            fund.lockDate != null ? (
                <span className="lock">
                    <span className="lockTitle">{i18n('arr.fund.panel.lockTitle')}</span>
                    {dateToString(new Date(fund.lockDate))}
                </span>
            ) : null;

        let comments = null;
        const activeVersion = fund.activeVersion;
        if (activeVersion.issues && activeVersion.issues.length > 0) {
            const tooltip = (
                <span>
                    {activeVersion.issues.map(i => (
                        <div key={i.id}>
                            #{i.number} - {i.description}
                        </div>
                    ))}
                </span>
            );
            comments = (
                <span className="comments">
                    <TooltipTrigger
                        content={tooltip}
                        holdOnHover
                        placement="auto"
                        className="status"
                        showDelay={50}
                        hideDelay={0}
                    >
                        <Icon glyph="fa-commenting" />
                    </TooltipTrigger>
                </span>
            );
        }

        return (
            <div key="arr-fund-panel" className={classNames(cls)}>
                {name}
                {version}
                {comments}
                {action}
            </div>
        );
    }
}

function mapStateToProps(state) {
    const {arrRegion, userDetail} = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }
    return {
        fund,
        userDetail,
    };
}

export default connect(mapStateToProps)(ArrFundPanel);
