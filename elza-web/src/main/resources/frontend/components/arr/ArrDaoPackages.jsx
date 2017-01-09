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

    handleSelect = (item) => {
        this.props.onSelect(item);
    };

    render() {
        const {fund, unassigned} = this.props;

        const list = unassigned ? fund.daoUnassignedPackageList : fund.daoPackageList;

        if (!list.fetched) {
            return <Loading/>;
        }

        return (
            <div>
                <Search
                    key="search"
                    placeholder={i18n('search.input.search')}
                    filterText={null}
                    onSearch={this.handleSearch}
                    onClear={this.handleClear}
                />
                <ListBox
                    key="list"
                    items={list.rows}
                    onFocus={this.handleSelect}
                    renderItemContent={(item, isActive, index) => <div>{item.code}</div>}
                    />
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
