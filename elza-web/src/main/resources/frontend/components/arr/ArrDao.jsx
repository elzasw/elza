require ('./ArrDao.less');

import React from "react";
import {connect} from "react-redux";
import {Icon, AbstractReactComponent, i18n} from "components/index.jsx";
import {indexById} from "stores/app/utils.jsx";
import {Button} from "react-bootstrap";
import {dateToString} from "components/Utils.jsx";
import {userDetailsSaveSettings} from "actions/user/userDetail.jsx";
import {fundChangeReadMode} from "actions/arr/fund.jsx";
import {setSettings, getOneSettings} from "components/arr/ArrUtils.jsx";

var classNames = require('classnames');

var ArrDao = class ArrDao extends AbstractReactComponent {

    constructor(props) {
        super(props);
    }

    renderItem = (item) => {
        return <div key={"dao" + item} className="item">Soubor {item + 1}</div>
    };

    render() {
        return (
                <div className="dao-container">
                    <div className="dao-detail">
                        <div className="title"><i>Digitalizát</i>: Název 1</div>
                        <div className="info">
                            asdasdf<br />
                            asdasdf<br />
                            asdasdf<br />
                            asdasdf<br />asdasdf<br />
                            asdasdf<br />
                            asdasdf<br />
                        </div>
                    </div>
                    <div className="dao-files">
                        <div className="dao-files-list">
                            {Array.apply(null, {length: 20}).map(Number.call, Number).map((item) => this.renderItem(item))}
                        </div>
                        <div className="dao-files-detail">
                            MIME: jpeg<br />
                            rozlišení: 1920x1080 px<br />
                            velikost: 3 MB<br />
                            link: /test/xxx.jpg<br />
                        </div>
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

module.exports = connect(mapStateToProps)(ArrDao);
