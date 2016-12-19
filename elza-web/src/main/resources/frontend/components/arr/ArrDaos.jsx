require ('./ArrDaos.less');

import React from "react";
import {connect} from "react-redux";
import {Icon, AbstractReactComponent, i18n, ArrDao} from "components/index.jsx";
import {indexById} from "stores/app/utils.jsx";
import {Button} from "react-bootstrap";
import {dateToString} from "components/Utils.jsx";
import {userDetailsSaveSettings} from "actions/user/userDetail.jsx";
import {fundChangeReadMode} from "actions/arr/fund.jsx";
import {setSettings, getOneSettings} from "components/arr/ArrUtils.jsx";

var classNames = require('classnames');

var ArrDaos = class ArrDaos extends AbstractReactComponent {

    constructor(props) {
        super(props);
    }

    renderItem = (item) => {
        return <div key={"daos" + item} className="item">Název {item + 1}<br /><i><small>5a0e5e5f-21c4-4767-a2e7-2be4ee61b05e</small></i></div>
    };

    render() {
        return (
            <div className="daos-container">
                <div className="daos-list">
                    <div className="title">Digitální entity</div>
                    <div className="daos-list-items">
                        {Array.apply(null, {length: 50}).map(Number.call, Number).map((item) => this.renderItem(item))}
                    </div>
                </div>
                <div className="daos-detail">
                    <ArrDao />
                </div>
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

module.exports = connect(mapStateToProps)(ArrDaos);
