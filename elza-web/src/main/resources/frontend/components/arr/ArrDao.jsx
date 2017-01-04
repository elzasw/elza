require ('./ArrDao.less');

import React from "react";
import {connect} from "react-redux";
import {Icon, AbstractReactComponent, i18n, ListBox} from "components/index.jsx";
import {indexById} from "stores/app/utils.jsx";
import {Button} from "react-bootstrap";
import {dateToString} from "components/Utils.jsx";
import {userDetailsSaveSettings} from "actions/user/userDetail.jsx";
import {fundChangeReadMode} from "actions/arr/fund.jsx";
import {setSettings, getOneSettings} from "components/arr/ArrUtils.jsx";

var classNames = require('classnames');

class ArrDao extends AbstractReactComponent {

    constructor(props) {
        super(props);
        this.state = {
            activeIndex: null
        };
    }

    static PropTypes = {
        dao: React.PropTypes.object.isRequired,
        fund: React.PropTypes.object.isRequired,
    };

    componentDidMount() {

    }

    componentWillReceiveProps(nextProps) {
        if (this.props.dao !== nextProps.dao) {
            this.setState({
                activeIndex: null
            });
        }
    }

    renderFile = (item) => {
        return <div key={"dao" + item.id} className="item">{item.code}</div>
    };

    handleSelect = (item, index) => {
        this.setState({activeIndex: index});
    };

    renderFileDetail = (item) => {
        return <div className="dao-files-detail">
            {item.code}<br />
            {item.code}<br />
        </div>
    };

    render() {
        const {fund, dao} = this.props;
        const {activeIndex} = this.state;

        if (dao == null) {
            return (<div>...</div>)
        }

        let items = [];

        if (dao.fileList) {
            dao.fileList.forEach((item) => items.push(item));
        }

        return (
                <div className="dao-container">
                    <div className="dao-detail">
                        <div className="title"><i>Digitalizát</i>: {dao.label}</div>
                        <div className="info">
                            <span>ID: {dao.code}</span><br />
                            {dao.url && <span>Url: <a target="_blank" href={dao.url}>{dao.url}</a></span>}
                        </div>
                    </div>
                    <div className="dao-files">
                        <ListBox
                            className='dao-files-list'
                            ref='dao-files-list'
                            items={items}
                            activeIndex={activeIndex}
                            renderItemContent={this.renderFile}
                            onFocus={this.handleSelect}
                            onSelect={this.handleSelect}
                        />
                        {activeIndex != null && this.renderFileDetail(items[activeIndex])}
                    </div>
                </div>
        );
    }
}

export default connect()(ArrDao);
