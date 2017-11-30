/**
 * Seznam balíčků se zobrazením detailu po kliknutí na balíček.
 */
import './ArrDaos.less';

import React from "react";
import {connect} from "react-redux";
import {Icon, HorizontalLoader, AbstractReactComponent, i18n} from 'components/shared';
import {indexById} from "stores/app/utils.jsx";
import {Button} from "react-bootstrap";
import {dateToString} from "components/Utils.jsx";
import {userDetailsSaveSettings} from "actions/user/userDetail.jsx";
import * as daoActions from "actions/arr/daoActions.jsx";
import {fundChangeReadMode} from "actions/arr/fund.jsx";
import {setSettings, getOneSettings} from "components/arr/ArrUtils.jsx";
import {LazyListBox, ListBox} from 'components/shared';
import {WebApi} from 'actions/index.jsx';
import ArrDao from "./ArrDao";

class ArrDaos extends AbstractReactComponent {

    state = {
        selectedItemId: null,
    };

    static PropTypes = {
        type: React.PropTypes.oneOf(['PACKAGE', 'NODE', 'NODE_ASSIGN']).isRequired,
        unassigned: React.PropTypes.bool,   // jen v případě packages
        fund: React.PropTypes.object.isRequired,
        selectedDaoId: React.PropTypes.object,
        nodeId: React.PropTypes.number,
        daoPackageId: React.PropTypes.number,
        onSelect: React.PropTypes.func,
        readMode: React.PropTypes.bool.isRequired
    };

    static defaultProps = {
        unassigned: false
    };

    componentDidMount() {
        this.handleFetch({}, this.props);
    }

    componentWillReceiveProps(nextProps) {
        this.handleFetch(this.props, nextProps);
    }

    handleFetch = (prevProps, nextProps) => {
        const {onSelect, type, unassigned, fund, nodeId, daoPackageId} = nextProps;

        let prevDaoList;
        let nextDaoList;
        if (type === "NODE") {
            if (prevProps.fund) {
                prevDaoList = prevProps.fund.nodeDaoList;
            }
            nextDaoList = nextProps.fund.nodeDaoList;
            if (nodeId != null) {
                this.props.dispatch(daoActions.fetchNodeDaoListIfNeeded(fund.versionId, nodeId));
            }
        } else if (type === "NODE_ASSIGN") {
            if (prevProps.fund) {
                prevDaoList = prevProps.fund.nodeDaoListAssign;
            }
            nextDaoList = nextProps.fund.nodeDaoListAssign;
            if (nodeId != null) {
                this.props.dispatch(daoActions.fetchNodeDaoListAssignIfNeeded(fund.versionId, nodeId));
            }
        } else if (type === "PACKAGE") {
            if (prevProps.fund) {
                prevDaoList = prevProps.fund.packageDaoList;
            }
            nextDaoList = nextProps.fund.packageDaoList;
            if (daoPackageId != null) {
                this.props.dispatch(daoActions.fetchDaoPackageDaoListIfNeeded(fund.versionId, daoPackageId, unassigned));
            }
        }

        if ((prevProps.fund && prevDaoList.isFetching || !prevProps.fund) && nextDaoList.fetched && !nextDaoList.isFetching || prevProps.selectedDaoId !== nextProps.selectedDaoId) {  // donačetl data, pokusíme se nastavit aktuálně vybranou položku
            const index = indexById(nextDaoList.rows, nextProps.selectedDaoId);
            if (index !== null) {
                const selectedItem = nextDaoList.rows[index];
                this.setState({
                    selectedItemId: selectedItem.id
                });
                onSelect && onSelect(selectedItem);
            } else {
                if (nextProps.selectedDaoId == null) {
                    this.setState({
                        selectedItemId: nextProps.selectedDaoId
                    });
                }
            }
        }
    };

    renderItem = (item) => {
        return <div key={"daos" + item.id} className="item">{item.label}<br /><i><small>{item.code}</small></i></div>
    };

    handleSelect = (item) => {
        const {onSelect} = this.props;
        this.setState({ selectedItemId: item.id });
        onSelect && onSelect(item);
    };

    handleUnlink = (dao) => {
        const {fund} = this.props;
        WebApi.deleteDaoLink(fund.versionId, dao.daoLink.id);
    };

    render() {
        const {type, fund, nodeId, daoPackageId, readMode} = this.props;
        const {selectedItemId} = this.state;

        let daoList = {};
        if (type === "NODE") {
            daoList = fund.nodeDaoList;
        } else if (type === "NODE_ASSIGN") {
            daoList = fund.nodeDaoListAssign;
        } else if (type === "PACKAGE") {
            daoList = fund.packageDaoList;
        }

        let activeIndex = null;
        let selectedItem;
        if (selectedItemId !== null) {
            activeIndex = indexById(daoList.rows, selectedItemId);
            if (activeIndex !== null) {
                selectedItem = daoList.rows[activeIndex];
            }
        }

        return (
            <div className="daos-container">
                <div className="daos-list">
                    <div className="title">Digitální entity</div>
                    <div className="daos-list-items">
                        {!(!daoList.fetched && daoPackageId) && <ListBox
                            activeIndex={activeIndex}
                            className="data-container"
                            items={daoList.rows}
                            renderItemContent={this.renderItem}
                            onFocus={this.handleSelect}
                        />}
                        {!daoList.fetched && daoPackageId && <HorizontalLoader/>}
                    </div>
                </div>
                <div className="daos-detail">
                    {/*selectedItem &&*/ <ArrDao fund={fund} readMode={readMode} dao={selectedItem} onUnlink={() => this.handleUnlink(selectedItem) } />}
                </div>
            </div>
        );
    }
}

export default connect()(ArrDaos);
