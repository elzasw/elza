import React from "react";
import {connect} from "react-redux";
import {FormInput, Icon, AbstractReactComponent, Search, i18n, Loading, ListBox} from "components/index.jsx";
import {indexById} from "stores/app/utils.jsx";
import {Form, Button} from "react-bootstrap";
import {dateToString} from "components/Utils.jsx";
import {userDetailsSaveSettings} from "actions/user/userDetail.jsx";
import {fundChangeReadMode} from "actions/arr/fund.jsx";
import {setSettings, getOneSettings} from "components/arr/ArrUtils.jsx";
import {humanFileSize} from "components/Utils.jsx";
import * as daoActions from 'actions/arr/daoActions.jsx';
import classNames from 'classnames';
require("./ArrDaoPackages.less")

class ArrDaoPackages extends AbstractReactComponent {

    constructor(props) {
        super(props);
    }

    static PropTypes = {
        fund: React.PropTypes.object.isRequired,
        unassigned: React.PropTypes.bool.isRequired,
        onSelect: React.PropTypes.func.isRequired,
    };

    componentDidMount() {
        this.fetchIfNeeded(this.props);
    }

    componentWillReceiveProps(nextProps) {
        this.fetchIfNeeded(nextProps);
    }

    fetchIfNeeded = (props) => {
        const {fund, unassigned} = props;

        if (unassigned) {
            this.props.dispatch(daoActions.fetchDaoUnassignedPackageListIfNeeded(fund.versionId));
        } else {
            this.props.dispatch(daoActions.fetchDaoPackageListIfNeeded(fund.versionId));
        }
    };

    handleSearch = (text) => {
        const {fund, unassigned} = this.props;

        if (unassigned) {
            this.props.dispatch(daoActions.filterDaoUnassignedPackageList(fund.versionId, {fulltext: text}));
        } else {
            this.props.dispatch(daoActions.filterDaoPackageList(fund.versionId, {fulltext: text}));
        }
    };

    handleClear = () => {
        this.handleSearch(null);
    };

    handleSelect = (item, index) => {
        this.props.onSelect(item, index);
    };

    render() {
        const {fund, unassigned, activeIndex} = this.props;

        const list = unassigned ? fund.daoUnassignedPackageList : fund.daoPackageList;

        return (
            <div className="dao-packages-container">
                <Search
                    key="search"
                    placeholder={i18n('search.input.search')}
                    filterText={null}
                    onSearch={this.handleSearch}
                    onClear={this.handleClear}
                />
                {list.fetched && <ListBox
                    key="list"
                    items={list.rows}
                    onFocus={this.handleSelect}
                    activeIndex={activeIndex}
                    renderItemContent={(item, isActive, index) => {
                        console.warn(item, isActive)
                        const cls = classNames({
                            active: isActive,
                        });
                        return <div className={cls}>{item.code}</div>
                    }}
                />}
                {!list.fetched && <Loading/>}
            </div>
        );
    }
}

function mapStateToProps(state) {
    const {arrRegion} = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }
    return {
        fund,
    }
}

export default connect(mapStateToProps)(ArrDaoPackages);