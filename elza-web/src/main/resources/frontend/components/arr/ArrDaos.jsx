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
import {LazyListBox} from 'components/index.jsx';
import {WebApi} from 'actions/index.jsx';

var classNames = require('classnames');

var ArrDaos = class ArrDaos extends AbstractReactComponent {

    constructor(props) {
        super(props);

        this.state = {
            node: props.node,
            selectedItem: null,
            selectedIndex: null,
            activeIndex: null,
        }
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            node: nextProps.node,
            selectedItem: null,
            selectedIndex: null,
            activeIndex: null,
        }, this.refreshRows);
    }

    refreshRows = () => {
        if (this.refs.listbox) {
            this.refs.listbox.reload();
        }
    };

    renderItem = (item) => {
        return <div key={"daos" + item.id} className="item">{item.label}<br /><i><small>{item.code}</small></i></div>
    };

    getItems = (fromIndex, toIndex) => {
        const {versionId} = this.props;
        const {node} = this.state;

        return WebApi.getFundNodeDaos(versionId, node.id, true)
            .then(json => {
                return {
                    items: json,
                    count: json.length,
                };
            })
    };

    handleSelect = (item) => {
        this.setState({selectedItem: item});
    };

    render() {
        const {selectedItem, selectedIndex, activeIndex, node} = this.state;

        if (node == null) {
            return (<div></div>);
        }

        return (
            <div className="daos-container">
                <div className="daos-list">
                    <div className="title">Digitální entity</div>
                    <div className="daos-list-items">
                        <LazyListBox
                            ref="listbox"
                            className="data-container"
                            selectedIndex={selectedIndex}
                            activeIndex={activeIndex}
                            getItems={this.getItems}
                            itemHeight={43} // nutne dat stejne cislo i do css jako .pokusny-listbox-container .listbox-item { height: 24px; }
                            renderItemContent={this.renderItem}
                            onSelect={this.handleSelect}
                        />
                    </div>
                </div>
                <div className="daos-detail">
                    <ArrDao dao={selectedItem} />
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
