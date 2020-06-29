import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, ListBox, Search, StoreHorizontalLoader} from 'components/shared';
import * as daoActions from 'actions/arr/daoActions.jsx';
import classNames from 'classnames';
import './ArrDaoPackages.scss';

class ArrDaoPackages extends AbstractReactComponent {
    static propTypes = {
        fund: PropTypes.object.isRequired,
        unassigned: PropTypes.bool.isRequired,
        onSelect: PropTypes.func.isRequired,
    };

    componentDidMount() {
        this.fetchIfNeeded(this.props);
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        this.fetchIfNeeded(this.props);
    }

    fetchIfNeeded = props => {
        const {fund, unassigned} = props;

        if (unassigned) {
            this.props.dispatch(daoActions.fetchDaoUnassignedPackageListIfNeeded(fund.versionId));
        } else {
            this.props.dispatch(daoActions.fetchDaoPackageListIfNeeded(fund.versionId));
        }
    };

    handleSearch = text => {
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
                <StoreHorizontalLoader store={list} />
                {list.fetched && (
                    <ListBox
                        key="list"
                        items={list.rows}
                        onFocus={this.handleSelect}
                        activeIndex={activeIndex}
                        renderItemContent={props => (
                            <div className={classNames({active: props.active})}>
                                {props.item.batchInfoLabel || '[' + props.item.code + ']'}
                            </div>
                        )}
                    />
                )}
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
    };
}

export default connect(mapStateToProps)(ArrDaoPackages);
